package com.sablednah.zombiemod.client;

import java.util.ArrayList;
import java.util.List;

import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.core.DexEntry;
import com.sablednah.zombiemod.core.Genus;
import com.sablednah.zombiemod.net.DexPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;

/**
 * The ZombieDex, dressed as a field guide in the same binding as LegendQuest's Players Handbook —
 * deliberately: Sable runs both mods, and two books on one shelf should match. Same parchment
 * gradient, bronze-and-gold frame, corner stars, scissored columns and gold scrollbars; a size up
 * from the handbook, because a bestiary carries a mob doll where the handbook carries text.
 *
 * <p>Two panes, one screen. The left column lists every genus, coloured by progress; the right pane
 * opens on an introduction and becomes the entry when a name is clicked. Everything it shows a
 * vanilla player can already read from {@code /zm bestiary} and {@code /zm bestiary info} — this is
 * a nicer window onto the same record, never a better one.
 *
 * <p>All text colour parameters are opaque white ({@code 0xFFFFFFFF}) with §-codes carrying the
 * colour, the handbook's convention — and the safe one, since the parameter is ARGB and
 * {@code 0xFFFFFF} is fully transparent.
 */
public final class DexScreen extends Screen {

    private static final int LIST_W = 112;
    private static final int HEADER_H = 26;
    private static final int DOLL_W = 116;

    // The tome palette, shared with the LegendQuest handbook by copying, not by dependency.
    private static final int PARCHMENT_TOP = 0xF81E1610;
    private static final int PARCHMENT_BOTTOM = 0xF8120C08;
    private static final int BRONZE = 0xFF6B4A1B;
    private static final int GOLD = 0xFFDAA520;
    private static final int GOLD_DIM = 0x80DAA520;
    private static final int INK_DIVIDER = 0xFF44382A;

    /** Clickable regions, rebuilt every frame (immediate-mode style, as the handbook does it). */
    private record Hot(int x0, int y0, int x1, int y1, Runnable action) {}

    private final List<Hot> hotspots = new ArrayList<>();

    /** Null means the intro page. */
    private Identifier selectedId;
    private int expandedAbility = -1;
    private double scroll;
    private double maxScroll;
    private double listScroll;
    private double maxListScroll;

    public DexScreen() {
        super(Component.literal("ZombieDex"));
    }

    // --- layout ---

    private int bookW() {
        return Math.min(440, width - 16);
    }

    private int bookH() {
        return Math.min(300, height - 16);
    }

    private int bookX() {
        return (width - bookW()) / 2;
    }

    private int bookY() {
        return (height - bookH()) / 2;
    }

    private int paneX() {
        return bookX() + LIST_W + 14;
    }

    private int paneW() {
        return bookX() + bookW() - 10 - paneX();
    }

    private int paneY() {
        return bookY() + HEADER_H;
    }

    private int paneH() {
        return bookY() + bookH() - 22 - paneY();
    }

    private DexPayload.Entry selected() {
        if (selectedId == null) {
            return null;
        }
        for (DexPayload.Entry e : DexState.entries()) {
            if (e.genus().equals(selectedId)) {
                return e;
            }
        }
        return null;
    }

    private void select(Identifier id) {
        selectedId = id;
        expandedAbility = -1;
        scroll = 0;
    }

    // --- rendering ---

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial); // dimmed world
        hotspots.clear();

        int x = bookX();
        int y = bookY();
        int w = bookW();
        int h = bookH();

        // The binding: aged leather in a bronze frame with gold inlay, stars in the corners.
        g.fillGradient(x, y, x + w, y + h, PARCHMENT_TOP, PARCHMENT_BOTTOM);
        frame(g, x - 2, y - 2, x + w + 2, y + h + 2, BRONZE, 2);
        frame(g, x + 2, y + 2, x + w - 2, y + h - 2, GOLD_DIM, 1);
        g.drawString(font, "§6✦", x + 4, y + 8, 0xFFFFFFFF);
        g.drawString(font, "§6✦", x + w - 12, y + 8, 0xFFFFFFFF);
        g.drawString(font, "§6✦", x + 4, y + h - 13, 0xFFFFFFFF);
        g.drawString(font, "§6✦", x + w - 12, y + h - 13, 0xFFFFFFFF);

        // Title row: back chip, flourished title, close chip.
        String title = "§6§l ZombieDex ";
        int titleW = font.width(title);
        int titleX = x + (w - titleW) / 2;
        g.drawString(font, "§8── ✦ ──", titleX - 42, y + 7, 0xFFFFFFFF);
        g.drawString(font, title, titleX, y + 6, 0xFFFFFFFF);
        g.drawString(font, "§8── ✦ ──", titleX + titleW + 2, y + 7, 0xFFFFFFFF);
        chip(g, x + 15, y + 5, 14, "«", selectedId != null, mouseX, mouseY, () -> select(null));
        chip(g, x + w - 29, y + 5, 14, "✕", true, mouseX, mouseY, this::onClose);

        // Divider with the centre ornament.
        int divY = y + HEADER_H - 6;
        g.fill(x + 8, divY, x + w - 8, divY + 1, INK_DIVIDER);
        g.drawString(font, "§6❖", x + w / 2 - 3, divY - 4, 0xFFFFFFFF);

        // Footer tally, centred in the binding.
        var entries = DexState.entries();
        g.drawCenteredString(font, "§8✦ §7" + DexState.slain() + "§8/§7" + entries.size()
                        + " slain §8· §7" + DexState.met() + " met §8✦",
                x + w / 2, y + h - 13, 0xFFFFFFFF);

        drawList(g, mouseX, mouseY, entries);

        DexPayload.Entry current = selected();
        if (current == null) {
            drawIntro(g, entries);
        } else {
            drawEntry(g, mouseX, mouseY, current);
        }
    }

    /** The left column: every genus, coloured by progress, scissored and scrollable. */
    private void drawList(GuiGraphics g, int mouseX, int mouseY, List<DexPayload.Entry> entries) {
        int listX = bookX() + 6;
        int listTop = paneY() - 1;
        int listBottom = bookY() + bookH() - 20;
        int listH = listBottom - listTop;

        maxListScroll = Math.max(0, entries.size() * 12 + 2 - listH);
        listScroll = Math.max(0, Math.min(listScroll, maxListScroll));

        g.fill(listX, listTop - 1, listX + LIST_W - 4, listBottom, 0x30000000);
        g.enableScissor(listX, listTop, listX + LIST_W - 4, listBottom);
        int ly = listTop + 2 - (int) listScroll;
        for (DexPayload.Entry e : entries) {
            boolean rowVisible = ly + 10 > listTop && ly - 1 < listBottom;
            boolean readable = e.met() || e.kills() > 0;
            boolean isSelected = e.genus().equals(selectedId);
            boolean hover = rowVisible && readable
                    && mouseX >= listX && mouseX < listX + LIST_W - 4
                    && mouseY >= Math.max(ly - 1, listTop) && mouseY < Math.min(ly + 10, listBottom);
            if (isSelected) {
                g.fill(listX, ly - 1, listX + LIST_W - 4, ly + 10, 0x40DAA520);
            } else if (hover) {
                g.fill(listX, ly - 1, listX + LIST_W - 4, ly + 10, 0x28FFFFFF);
            }
            // The mark is the progress, the colour is the affordance: gold when open, warm when
            // clickable, receding into the page when not yet earned.
            String mark = e.kills() > 0 ? "§a✔ " : e.met() ? "§e? " : "§8✘ ";
            String colour = isSelected ? "§6§l" : hover ? "§e" : readable ? "§7" : "§8";
            g.drawString(font, mark + colour + trim(e.name(), LIST_W - 22), listX + 3, ly, 0xFFFFFFFF);
            if (readable && !isSelected && rowVisible) {
                Identifier id = e.genus();
                hotspots.add(new Hot(listX, Math.max(ly - 1, listTop),
                        listX + LIST_W - 4, Math.min(ly + 10, listBottom), () -> select(id)));
            }
            ly += 12;
        }
        g.disableScissor();

        if (maxListScroll > 0) {
            int barX = listX + LIST_W - 6;
            g.fill(barX, listTop, barX + 2, listBottom, 0x50000000);
            int contentH = entries.size() * 12 + 2;
            int thumbH = Math.max(10, listH * listH / contentH);
            int thumbY = listTop + (int) ((listH - thumbH) * (listScroll / maxListScroll));
            g.fill(barX, thumbY, barX + 2, thumbY + thumbH, 0xC0DAA520);
        }
    }

    /** The right pane before anything is chosen: what this book is, and how it fills in. */
    private void drawIntro(GuiGraphics g, List<DexPayload.Entry> entries) {
        int px = paneX();
        int py = paneY() + 6;
        int pw = paneW() - 4;

        if (entries.isEmpty()) {
            g.drawString(font, "§8Nothing yet — the server has not sent one.", px, py, 0xFFFFFFFF);
            return;
        }
        g.drawString(font, "§6§lA Field Guide to the Dead", px, py, 0xFFFFFFFF);
        py += 14;
        for (FormattedCharSequence line : font.split(FormattedText.of(
                "§7A record of everything you have met, and everything you have put down."), pw)) {
            g.drawString(font, line, px, py, 0xFFFFFFFF);
            py += 10;
        }
        py += 6;
        g.drawString(font, "§a✔ §7slain   §e? §7met   §8✘ not yet found", px, py, 0xFFFFFFFF);
        py += 14;
        for (FormattedCharSequence line : font.split(FormattedText.of(
                "§8Click a name you have met to read its entry. The rest stay closed until you find them."),
                pw)) {
            g.drawString(font, line, px, py, 0xFFFFFFFF);
            py += 10;
        }
    }

    /** The entry: description, the stat block, expandable abilities, and the doll. */
    private void drawEntry(GuiGraphics g, int mouseX, int mouseY, DexPayload.Entry current) {
        int px = paneX();
        int py = paneY();
        int pw = paneW() - DOLL_W - 8; // the doll keeps its column; text wraps beside it
        int ph = paneH();

        Holder.Reference<Genus> holder = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            holder = mc.level.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS)
                    .get(ResourceKey.create(ZombieModRegistries.GENUS, current.genus())).orElse(null);
        }
        if (holder == null) {
            g.drawString(font, "§8This entry is missing from the loaded datapacks.", px, py + 6, 0xFFFFFFFF);
            return;
        }
        Genus genus = holder.value();

        g.enableScissor(px, py, px + pw, py + ph);
        int cy = py + 2 - (int) scroll;

        g.drawString(font, "§6§l" + current.name(), px, cy, 0xFFFFFFFF);
        if (current.kills() > 0) {
            String tally = "§8Slain §7" + current.kills();
            g.drawString(font, tally, px + pw - font.width(tally) - 6, cy, 0xFFFFFFFF);
        }
        cy += 13;

        if (genus.description().isPresent()) {
            for (FormattedCharSequence line : font.split(FormattedText.of(
                    "§7§o" + genus.description().get()), pw)) {
                g.drawString(font, line, px, cy, 0xFFFFFFFF);
                cy += 10;
            }
            cy += 4;
        }

        for (DexEntry.Row row : DexEntry.stats(genus)) {
            g.drawString(font, "§8" + row.label() + ": §f" + row.detail(), px, cy, 0xFFFFFFFF);
            cy += 10;
        }

        List<DexEntry.Row> abilities = DexEntry.abilities(genus);
        if (!abilities.isEmpty()) {
            cy += 4;
            g.fill(px, cy + 3, px + pw - 8, cy + 4, INK_DIVIDER);
            g.drawString(font, "§8 Abilities ", px + 8, cy, 0xFFFFFFFF);
            cy += 11;
            for (int i = 0; i < abilities.size(); i++) {
                DexEntry.Row a = abilities.get(i);
                boolean open = expandedAbility == i;
                boolean hover = mouseX >= px && mouseX < px + pw
                        && mouseY >= cy - 1 && mouseY < cy + 10
                        && mouseY >= py && mouseY < py + ph;
                if (hover) {
                    g.fill(px - 1, cy - 1, px + pw - 2, cy + 10, 0x28FFFFFF);
                }
                String colour = open ? "§6" : hover ? "§e" : "§7";
                g.drawString(font, colour + (open ? "▼ " : "▶ ") + a.label(), px, cy, 0xFFFFFFFF);
                final int index = i;
                hotspots.add(new Hot(px, Math.max(cy - 1, py), px + pw - 2,
                        Math.min(cy + 10, py + ph),
                        () -> expandedAbility = expandedAbility == index ? -1 : index));
                cy += 11;
                if (open && !a.detail().isEmpty()) {
                    for (FormattedCharSequence line : font.split(
                            FormattedText.of("§7" + a.detail()), pw - 10)) {
                        g.drawString(font, line, px + 10, cy, 0xFFFFFFFF);
                        cy += 10;
                    }
                    // A sound you can read about is a sound you should be able to hear.
                    var sound = soundFor(genus, a.label());
                    if (sound != null) {
                        boolean pHover = mouseX >= px + 10 && mouseX < px + 60
                                && mouseY >= cy - 1 && mouseY < cy + 10
                                && mouseY >= py && mouseY < py + ph;
                        g.drawString(font, (pHover ? "§e" : "§6") + "▶ play", px + 10, cy, 0xFFFFFFFF);
                        var play = sound;
                        hotspots.add(new Hot(px + 10, Math.max(cy - 1, py), px + 60,
                                Math.min(cy + 10, py + ph),
                                () -> Minecraft.getInstance().getSoundManager().play(
                                        net.minecraft.client.resources.sounds.SimpleSoundInstance
                                                .forUI(play.sound().value(), play.pitch(), play.volume()))));
                        cy += 11;
                    }
                    cy += 2;
                }
            }
        }

        if (genus.loot().isPresent()) {
            cy += 4;
            g.fill(px, cy + 3, px + pw - 8, cy + 4, INK_DIVIDER);
            g.drawString(font, "§8 Leaves behind ", px + 8, cy, 0xFFFFFFFF);
            cy += 12;
            if (current.drops().isEmpty()) {
                g.drawString(font, "§8Kill one to learn what it leaves behind.", px, cy, 0xFFFFFFFF);
                cy += 11;
            } else {
                for (String dropId : current.drops()) {
                    Identifier itemId = Identifier.tryParse(dropId);
                    var item = itemId == null ? null
                            : net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(itemId);
                    if (item != null) {
                        var stack = new net.minecraft.world.item.ItemStack(item);
                        g.renderItem(stack, px + 2, cy - 3);
                        g.drawString(font, "§7" + stack.getHoverName().getString(), px + 22, cy, 0xFFFFFFFF);
                    } else {
                        g.drawString(font, "§7" + dropId, px + 2, cy, 0xFFFFFFFF);
                    }
                    cy += 14;
                }
            }
        }
        g.disableScissor();

        // Scroll affordances, the handbook's: edge shadows plus an honest little bar.
        int contentH = (cy + (int) scroll) - py;
        maxScroll = Math.max(0, contentH - ph);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        if (scroll > 0) {
            g.fillGradient(px, py, px + pw, py + 8, 0xA0000000, 0x00000000);
        }
        if (maxScroll - scroll > 0) {
            g.fillGradient(px, py + ph - 8, px + pw, py + ph, 0x00000000, 0xA0000000);
        }
        if (maxScroll > 0) {
            int barX = px + pw - 4;
            g.fill(barX, py, barX + 2, py + ph, 0x50000000);
            int thumbH = Math.max(12, (int) ((long) ph * ph / contentH));
            int thumbY = py + (int) ((ph - thumbH) * (scroll / maxScroll));
            g.fill(barX, thumbY, barX + 2, thumbY + thumbH, 0xC0DAA520);
        }

        // The plate: the doll in its own gold-dim frame, outside the scissor so the entity renderer
        // cannot be half-clipped, fixed while the text scrolls - a manuscript's margin figure.
        int dx1 = paneX() + paneW() - 2;
        int dx0 = dx1 - DOLL_W + 6;
        int dh = Math.min(180, ph - 4);
        int dy0 = py + (ph - dh) / 2;
        int dy1 = dy0 + dh;
        g.fill(dx0, dy0, dx1, dy1, 0x40000000);
        frame(g, dx0, dy0, dx1, dy1, GOLD_DIM, 1);
        LivingEntity doll = DexPreview.of(current.genus(), holder);
        if (doll != null) {
            // MULTIPLIED by the genus's scale, on purpose: renderEntityInInventoryFollowsAngle
            // explicitly normalises the render state's scale back to 1 (so a Scale-effect player
            // doesn't overflow the inventory doll) - which silently erased every genus's size.
            // Reinstating it in the size parameter is the only lever the helper leaves us.
            //
            // And CAPPED against the plate itself, because the first Tank rendered with its head
            // and boots amputated by the frame. ~3px of drawn height per size unit, measured, with
            // a margin - honesty about relative size ends where the frame does.
            int size = Math.max(16, Math.min((int) (30 * genus.scale()), (dh - 12) / 3));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, dx0 + 2, dy0 + 2, dx1 - 2, dy1 - 2,
                    size, 0.0625F, mouseX, mouseY, doll);
        }
    }

    /** The Sound ability behind an ability row's label, if that is what it is. */
    private static com.sablednah.zombiemod.core.ability.Abilities.Sound soundFor(Genus genus, String label) {
        for (var ability : genus.abilities()) {
            if (ability instanceof com.sablednah.zombiemod.core.ability.Abilities.Sound sound
                    && (sound.label().equals(label) || label.equals("Sound"))) {
                return sound;
            }
        }
        return null;
    }

    /** A chip in the handbook's language: dark plate, bronze-or-gold frame, warm on hover. */
    private void chip(GuiGraphics g, int x, int y, int w, String label, boolean enabled,
            int mouseX, int mouseY, Runnable action) {
        int h = 14;
        boolean hover = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        g.fill(x, y, x + w, y + h, hover ? 0xFF33291E : 0xFF221A12);
        frame(g, x, y, x + w, y + h, hover ? GOLD : enabled ? GOLD_DIM : BRONZE, 1);
        String colour = !enabled ? "§8" : hover ? "§e" : "§7";
        g.drawString(font, colour + label, x + (w - font.width(label)) / 2, y + 3, 0xFFFFFFFF);
        if (enabled) {
            hotspots.add(new Hot(x, y, x + w, y + h, action));
        }
    }

    /** A hollow rectangle of the given line thickness. */
    private static void frame(GuiGraphics g, int x0, int y0, int x1, int y1, int colour, int t) {
        g.fill(x0, y0, x1, y0 + t, colour);
        g.fill(x0, y1 - t, x1, y1, colour);
        g.fill(x0, y0, x0 + t, y1, colour);
        g.fill(x1 - t, y0, x1, y1, colour);
    }

    private String trim(String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        String out = text;
        while (!out.isEmpty() && font.width(out + "…") > width) {
            out = out.substring(0, out.length() - 1);
        }
        return out + "…";
    }

    /** Step the selection to the next readable entry in the given direction, and keep it on screen. */
    private void step(int direction) {
        var entries = DexState.entries();
        List<Integer> readable = new ArrayList<>();
        int currentAt = -1;
        for (int i = 0; i < entries.size(); i++) {
            DexPayload.Entry e = entries.get(i);
            if (e.met() || e.kills() > 0) {
                if (e.genus().equals(selectedId)) {
                    currentAt = readable.size();
                }
                readable.add(i);
            }
        }
        if (readable.isEmpty()) {
            return;
        }
        int next = currentAt < 0 ? (direction > 0 ? 0 : readable.size() - 1)
                : Math.max(0, Math.min(readable.size() - 1, currentAt + direction));
        int row = readable.get(next);
        select(entries.get(row).genus());
        // Bring the row into the scissored window rather than leaving the keyboard blind.
        int listH = (bookY() + bookH() - 20) - (paneY() - 1);
        double top = row * 12;
        if (top < listScroll) {
            listScroll = top;
        } else if (top + 12 > listScroll + listH) {
            listScroll = top + 12 - listH;
        }
    }

    // --- input ---

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        }
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_DOWN) {
            step(1);
            return true;
        }
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_UP) {
            step(-1);
            return true;
        }
        return false;
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (super.mouseClicked(event, doubled)) {
            return true;
        }
        if (event.button() != 0) {
            return false;
        }
        for (Hot hot : hotspots) {
            if (event.x() >= hot.x0() && event.x() < hot.x1()
                    && event.y() >= hot.y0() && event.y() < hot.y1()) {
                hot.action().run();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        if (super.mouseScrolled(mouseX, mouseY, dx, dy)) {
            return true;
        }
        // The wheel scrolls whichever column it hovers over, exactly as the handbook does.
        if (mouseX < paneX() - 2) {
            listScroll = Math.max(0, Math.min(maxListScroll, listScroll - dy * 12));
        } else {
            scroll = Math.max(0, Math.min(maxScroll, scroll - dy * 12));
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
