package com.sablednah.zombiemod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sablednah.zombiemod.ZombieMod;
import com.sablednah.zombiemod.core.DexBook;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The client half, behind its own {@code dist = Dist.CLIENT} entrypoint.
 *
 * <p>Why this lives in the same jar as the server: two jars have to be kept in step by whoever
 * installs them, forever, and the failure is a quiet one — a screen drawing stale nonsense rather
 * than an error. One jar makes the mismatch impossible.
 *
 * <p>Nothing is given up. A dedicated server never constructs this class, and the promise that
 * matters was never "this jar has no client code" — it is <b>a player does not need this mod to
 * join</b>, which the optional payload registration and the guarded sends in
 * {@code Net} keep.
 */
@Mod(value = ZombieMod.MOD_ID, dist = Dist.CLIENT)
public final class ZombieModClient {

    private static final KeyMapping OPEN_DEX = new KeyMapping(
            "key.zombiemod.dex", InputConstants.Type.KEYSYM, InputConstants.KEY_J,
            KeyMapping.Category.MISC);

    public ZombieModClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::onRegisterKeys);
        NeoForge.EVENT_BUS.register(this);
    }

    private void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_DEX);
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return;
        }
        while (OPEN_DEX.consumeClick()) {
            mc.setScreen(new DexScreen());
        }
    }

    /**
     * {@code /zmdex render [size]} — write one PNG per genus to
     * {@code screenshots/zombiemod/<namespace>/<genus>.png}.
     *
     * <p>A <b>client</b> command, registered on the client dispatcher: it draws things, so it has to
     * run where the renderer is, and it needs no permission because it changes nothing on the server.
     *
     * <p>It walks the genus registry as the client has it, which is whatever the server sent — so
     * anyone running their own roster gets their own image set from the same command, and a
     * third-party pack needs no support from us to be documented the way the shipped one is.
     */
    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("zmdex")
                .then(Commands.literal("render")
                        .executes(ctx -> render(ctx.getSource(), 256))
                        .then(Commands.argument("size", IntegerArgumentType.integer(64, 1024))
                                .executes(ctx -> render(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "size")))))
                .then(Commands.literal("cancel").executes(ctx -> {
                    DexRender.cancel();
                    return 1;
                })));
    }

    private static int render(net.minecraft.commands.CommandSourceStack source, int size) {
        // The canvas is cropped out of the middle of the window, so it cannot be bigger than it.
        Minecraft mc = Minecraft.getInstance();
        int limit = Math.min(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        if (size > limit) {
            source.sendFailure(Component.literal(
                    "Canvas " + size + " is larger than the window allows (" + limit
                            + "). Use a smaller size, or a bigger window."));
            return 0;
        }
        int queued = DexRender.start(size);
        switch (queued) {
            case -1 -> source.sendFailure(Component.literal("Already rendering. /zmdex cancel to stop."));
            case -2 -> source.sendFailure(Component.literal("Join a world first - the dolls come from the world's registry."));
            case 0 -> source.sendFailure(Component.literal("No genera loaded."));
            default -> source.sendSuccess(() -> Component.literal(
                    "Rendering " + queued + " genera at " + size + "px. Do not touch the mouse."), false);
        }
        return Math.max(queued, 0);
    }

    /** The frame that just finished is the one the exporter captures. */
    @SubscribeEvent
    public void onRenderFrameEnd(RenderFrameEvent.Post event) {
        if (DexRender.running()) {
            DexRender.onFrameEnd();
        }
    }

    @SubscribeEvent
    public void onLoggingOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        DexState.clear();
    }

    /**
     * The book is the same book for everybody.
     *
     * <p>{@code /zm bestiary book} hands out an ordinary written book, and that is deliberate: a
     * vanilla player right-clicks it and reads the pages, and nothing here changes what the server
     * sent them. This handler only exists on a client that has the mod, so on that client the same
     * item opens the tome instead — one item, two readings, no second command.
     *
     * <p><b>Sneak to read it as a book.</b> Otherwise a modded client has no way to see the pages at
     * all, which costs you the ability to check what a vanilla player is actually getting without
     * keeping a second instance around for it.
     *
     * <p>This side decides <em>both</em> outcomes, on purpose. The server's job (in
     * {@code ZombieModEvents}) is only to stop opening the book itself — it never asks whether
     * anybody was sneaking, because it would be asking about a flag that syncs a tick behind the
     * click. Two sides reading their own copy of "was he crouching" disagree on the frame you press
     * or release shift, and disagreement here means either both windows or neither. One decider, no
     * race: the client opens whichever screen it wants, and the server opens nothing.
     */
    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        // Fires on both sides in single-player, and a screen is a client-thread thing.
        // No dex means no ZombieMod on the other end - the server sends the whole roster on login,
        // so an empty one is not "met nothing", it is "nobody told us anything". Leave a book named
        // ZombieDex on somebody else's server alone and let it open as the book it is.
        ItemStack stack = event.getItemStack();
        if (!event.getLevel().isClientSide() || DexState.entries().isEmpty() || !DexBook.is(stack)) {
            return;
        }
        if (event.getEntity().isShiftKeyDown()) {
            var pages = BookViewScreen.BookAccess.fromItem(stack);
            if (pages == null) {
                // A blank book called ZombieDex. Nothing to read; let vanilla have it.
                return;
            }
            Minecraft.getInstance().setScreen(new BookViewScreen(pages));
        } else {
            Minecraft.getInstance().setScreen(new DexScreen());
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
