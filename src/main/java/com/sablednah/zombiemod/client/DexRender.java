package com.sablednah.zombiemod.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.platform.NativeImage;
import com.sablednah.zombiemod.platform.Msg;
import com.sablednah.zombiemod.ZombieMod;
import com.sablednah.zombiemod.core.Genus;
import com.sablednah.zombiemod.ZombieModRegistries;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;

/**
 * Exports one PNG per genus, with a real alpha channel, using the same doll the ZombieDex draws.
 *
 * <p><b>Why two passes.</b> The only way to get pixels back out of Minecraft is
 * {@link Screenshot#takeScreenshot}, and it ORs every pixel with {@code 0xFF000000} — so a captured
 * frame is always fully opaque and transparency can never be read off it directly. Instead each
 * genus is drawn twice, once on black and once on white, and alpha is solved for. Compositing a
 * colour C with coverage a over a background B gives {@code P = a*C + (1-a)*B}, so:
 *
 * <pre>
 *   white - black = (1-a) * 255   ->   a = 1 - (white - black) / 255
 *   C = black / a
 * </pre>
 *
 * <p>That is exact rather than approximate, and unlike chroma-keying it handles antialiased edges
 * and translucent parts correctly, with no halo and no despill. It costs a second frame per genus,
 * which is nothing.
 *
 * <p>Rendering happens on the main framebuffer through an ordinary {@link DexRenderScreen}, because
 * persuading {@code GuiGraphics} to draw into an offscreen target is a fight with the render system
 * for no benefit here.
 */
public final class DexRender {

    private static final Logger LOG = LogUtils.getLogger();

    /** Solid, and as far apart as possible: the further apart, the better conditioned the solve. */
    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;

    private static DexRender active;

    private final List<Identifier> queue;
    private final int canvas;
    private final Path outDir;
    private int index;
    private Phase phase = Phase.NEXT;
    private int waitFrames;
    private boolean capturePending;
    private NativeImage black;
    private int written;
    private int skipped;
    /** Head-texture loads, kicked off up front so the network fetches overlap. */
    private final Map<Identifier, CompletableFuture<?>> warming = new HashMap<>();
    private int warmWaited;
    /** Frames to wait for one head before giving up on it and rendering whatever is there. */
    private static final int WARM_LIMIT = 200;

    private enum Phase { NEXT, GRAB_BLACK, GRAB_WHITE, DONE }

    private DexRender(List<Identifier> queue, int canvas, Path outDir) {
        this.queue = queue;
        this.canvas = canvas;
        this.outDir = outDir;
    }

    public static boolean running() {
        return active != null;
    }

    /** Returns how many genera were queued, or -1 if a run is already going. */
    public static int start(int canvas) {
        if (active != null) {
            return -1;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return -2;
        }
        // Every genus the client knows about, which is every genus the SERVER has - datapack
        // registries sync. So somebody running their own roster gets their own image set from this
        // same command, with no special casing for the shipped one.
        List<Identifier> ids = new ArrayList<>(
                mc.level.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS)
                        .listElementIds().map(ResourceKey::identifier).sorted().toList());
        if (ids.isEmpty()) {
            return 0;
        }
        Path dir = mc.gameDirectory.toPath().resolve("screenshots").resolve(ZombieMod.MOD_ID);
        DexRender run = new DexRender(ids, canvas, dir);
        run.warmHeads(mc);
        active = run;
        return ids.size();
    }

    public static void cancel() {
        if (active != null) {
            active.finish(true);
        }
    }

    /** Driven from {@code RenderFrameEvent.Post}: the frame just drawn is the one we capture. */
    public static void onFrameEnd() {
        DexRender run = active;
        if (run != null) {
            run.step();
        }
    }

    /**
     * Start every head texture loading before the first capture.
     *
     * <p>This is the Alex bug. A player-head texture is fetched and registered asynchronously, and
     * {@code SkinManager.createLookup} hands the renderer <em>the default skin</em> until that
     * finishes — so a doll drawn a frame after it was built wears Steve or Alex, chosen by the
     * profile's UUID parity, which is why the wrong ones looked random.
     *
     * <p>It never showed in the dex screen because a human takes far longer than a frame to click
     * through a roster, and it never showed on the Corpse or the Ghost because those wear the local
     * player's profile, whose skin is already loaded — the player is standing in the world wearing
     * it. Only an automated pass is fast enough to out-run the fetch.
     *
     * <p>Kicked off for every genus at once rather than one at a time, so the fetches overlap
     * instead of serialising a round trip per genus.
     */
    private void warmHeads(Minecraft mc) {
        var lookup = mc.level.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS);
        for (Identifier id : queue) {
            Optional<Genus> genus = lookup.get(ResourceKey.create(ZombieModRegistries.GENUS, id))
                    .map(Holder.Reference::value);
            genus.flatMap(Genus::head).ifPresent(profile ->
                    warming.put(id, mc.getSkinManager().get(profile.partialProfile())));
        }
        LOG.info("ZombieMod: warming {} head texture(s)", warming.size());
    }

    /** True once this genus's head is loaded, or we have waited long enough to stop caring. */
    private boolean headReady(Identifier id) {
        CompletableFuture<?> f = warming.get(id);
        if (f == null || f.isDone()) {
            warmWaited = 0;
            return true;
        }
        if (++warmWaited > WARM_LIMIT) {
            LOG.warn("ZombieMod: head for {} did not load in time; rendering without it", id);
            warmWaited = 0;
            return true;
        }
        return false;
    }

    private void step() {
        // The GPU copy has not called back yet; do nothing rather than stack up captures.
        if (capturePending) {
            return;
        }
        // Give the screen a frame to actually draw with the background we just set. Capturing the
        // frame before that one would grab the previous pass and silently produce alpha of zero.
        if (waitFrames > 0) {
            waitFrames--;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        switch (phase) {
            case NEXT -> {
                if (index >= queue.size()) {
                    finish(false);
                    return;
                }
                Identifier id = queue.get(index);
                // Wait for the face before drawing it, or the capture beats the texture.
                if (!headReady(id)) {
                    return;
                }
                Holder.Reference<Genus> holder = mc.level == null ? null
                        : mc.level.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS)
                                .get(ResourceKey.create(ZombieModRegistries.GENUS, id)).orElse(null);
                LivingEntity doll = holder == null ? null : DexPreview.of(id, holder);
                if (doll == null) {
                    // A genus whose base cannot be drawn as a living thing. Not an error.
                    LOG.warn("ZombieMod: no doll for {}, skipping", id);
                    skipped++;
                    index++;
                    return;
                }
                mc.setScreen(new DexRenderScreen(doll, holder, BLACK, canvas));
                // Two frames, not one. The skin future completing means the texture is registered;
                // giving the renderer an extra frame to actually bind and draw with it costs
                // nothing at 60fps and removes the last of the race.
                waitFrames = 2;
                phase = Phase.GRAB_BLACK;
            }
            case GRAB_BLACK -> capture(img -> {
                black = img;
                Identifier id = queue.get(index);
                Holder.Reference<Genus> holder = mc.level.registryAccess()
                        .lookupOrThrow(ZombieModRegistries.GENUS)
                        .get(ResourceKey.create(ZombieModRegistries.GENUS, id)).orElseThrow();
                mc.setScreen(new DexRenderScreen(DexPreview.of(id, holder), holder, WHITE, canvas));
                waitFrames = 1;
                phase = Phase.GRAB_WHITE;
            });
            case GRAB_WHITE -> capture(img -> {
                try {
                    write(queue.get(index), black, img);
                    written++;
                } catch (Exception e) {
                    LOG.error("ZombieMod: could not write {}", queue.get(index), e);
                    skipped++;
                } finally {
                    img.close();
                    black.close();
                    black = null;
                    index++;
                    phase = Phase.NEXT;
                }
            });
            case DONE -> { }
        }
    }

    private void capture(java.util.function.Consumer<NativeImage> then) {
        capturePending = true;
        Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget(), img -> {
            try {
                then.accept(img);
            } finally {
                capturePending = false;
            }
        });
    }

    /** Solve the two passes for colour and coverage, crop to the canvas, and write the PNG. */
    private void write(Identifier id, NativeImage onBlack, NativeImage onWhite) throws Exception {
        int w = onBlack.getWidth();
        int h = onBlack.getHeight();
        // The screen draws in GUI-scaled units; the framebuffer is in real pixels. At GUI scale 3 a
        // 256-unit canvas is 768 pixels wide, so cropping `canvas` pixels would take a third of the
        // intended region and cut the doll in half. Convert, and let the output inherit the extra
        // resolution rather than throwing it away.
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        int side = Math.min(Math.min(w, h), Math.max(1, (int) Math.round(canvas * scale)));
        int x0 = (w - side) / 2;
        int y0 = (h - side) / 2;
        try (NativeImage out = new NativeImage(side, side, false)) {
            for (int y = 0; y < side; y++) {
                for (int x = 0; x < side; x++) {
                    int b = onBlack.getPixel(x0 + x, y0 + y);
                    int wp = onWhite.getPixel(x0 + x, y0 + y);
                    // (1-a)*255, averaged over the three channels to shrug off rounding.
                    int diff = (ARGB.red(wp) - ARGB.red(b)
                              + ARGB.green(wp) - ARGB.green(b)
                              + ARGB.blue(wp) - ARGB.blue(b) + 1) / 3;
                    int alpha = Math.max(0, Math.min(255, 255 - diff));
                    if (alpha == 0) {
                        out.setPixel(x, y, 0);
                        continue;
                    }
                    // Un-premultiply: the captured colour is a*C, so C is that over a.
                    out.setPixel(x, y, ARGB.color(alpha,
                            Math.min(255, ARGB.red(b) * 255 / alpha),
                            Math.min(255, ARGB.green(b) * 255 / alpha),
                            Math.min(255, ARGB.blue(b) * 255 / alpha)));
                }
            }
            Path dir = outDir.resolve(id.getNamespace());
            Files.createDirectories(dir);
            out.writeToFile(dir.resolve(id.getPath() + ".png"));
        }
    }

    private void finish(boolean cancelled) {
        if (black != null) {
            black.close();
            black = null;
        }
        phase = Phase.DONE;
        active = null;
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(null);
        Component msg = Component.literal(cancelled
                ? "ZombieDex render cancelled after " + written + " image(s)."
                : "ZombieDex render complete: " + written + " image(s)"
                        + (skipped > 0 ? ", " + skipped + " skipped" : "") + " -> " + outDir);
        if (mc.player != null) {
            Msg.chat(mc.player, msg);
        }
        LOG.info("ZombieMod: {}", msg.getString());
    }
}
