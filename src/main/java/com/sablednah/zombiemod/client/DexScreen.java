package com.sablednah.zombiemod.client;

import com.sablednah.zombiemod.net.DexPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The ZombieDex.
 *
 * <p>Deliberately shows nothing a vanilla player cannot already read from {@code /zm bestiary} or
 * the book — it is a nicer window onto the same record, not a better one. A modded client that could
 * see more would be playing a different game from the friend standing next to it.
 */
public final class DexScreen extends Screen {

    /**
     * Opaque white. The colour argument is <b>ARGB</b>, so the tempting {@code 0xFFFFFF} carries an
     * alpha of zero and draws nothing at all — a screen that dims correctly and is then empty.
     * Vanilla writes this as {@code -1} everywhere for the same reason.
     */
    private static final int WHITE = 0xFFFFFFFF;

    private static final int ROW = 12;
    private static final int COLUMNS = 3;

    private int scroll;
    /** Where each row was drawn last frame, so a click can find it. Rebuilt every render. */
    private final java.util.List<int[]> hitboxes = new java.util.ArrayList<>();
    private final java.util.List<DexPayload.Entry> visible = new java.util.ArrayList<>();

    /**
     * The page itself: a dark plate with a lighter edge, translucent enough to keep the world
     * behind it. Shared with {@link DexInfoScreen} so the two read as one book rather than two
     * screens that happen to follow each other.
     */
    static void panel(GuiGraphics gfx, int width, int height) {
        int x0 = Math.max(8, width / 2 - 210);
        int x1 = Math.min(width - 8, width / 2 + 210);
        int y0 = 8;
        int y1 = height - 8;
        gfx.fill(x0, y0, x1, y1, 0xC0080808);
        gfx.fill(x0, y0, x1, y0 + 1, 0x40FFFFFF);
        gfx.fill(x0, y1 - 1, x1, y1, 0x40FFFFFF);
        gfx.fill(x0, y0, x0 + 1, y1, 0x40FFFFFF);
        gfx.fill(x1 - 1, y0, x1, y1, 0x40FFFFFF);
    }

    public DexScreen() {
        super(Component.literal("ZombieDex"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        super.render(gfx, mouseX, mouseY, partial);
        panel(gfx, width, height);
        var entries = DexState.entries();

        gfx.drawCenteredString(font, Component.literal("ZombieDex").withStyle(ChatFormatting.GOLD),
                width / 2, 14, WHITE);
        gfx.drawCenteredString(font, Component.literal(
                        DexState.slain() + " of " + entries.size() + " slain, " + DexState.met() + " met")
                .withStyle(ChatFormatting.GRAY), width / 2, 26, WHITE);

        if (entries.isEmpty()) {
            gfx.drawCenteredString(font, Component.literal("Nothing yet - the server has not sent one.")
                    .withStyle(ChatFormatting.DARK_GRAY), width / 2, height / 2, WHITE);
            return;
        }

        int columnWidth = Math.min(160, (width - 40) / COLUMNS);
        int left = (width - columnWidth * COLUMNS) / 2;
        int top = 46;
        int rows = Math.max(1, (height - top - 20) / ROW);
        int perPage = rows * COLUMNS;
        int start = Math.min(scroll, Math.max(0, entries.size() - perPage));
        hitboxes.clear();
        visible.clear();

        for (int i = 0; i < perPage && start + i < entries.size(); i++) {
            DexPayload.Entry e = entries.get(start + i);
            int x = left + (i / rows) * columnWidth;
            int y = top + (i % rows) * ROW;

            String mark = e.kills() > 0 ? "✔ " : e.met() ? "? " : "✘ ";
            ChatFormatting colour = e.kills() > 0 ? ChatFormatting.GREEN
                    : e.met() ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY;
            // Unmet entries stay legible but plainly unfinished - a checklist has to show its gaps.
            ChatFormatting nameColour = e.kills() > 0 ? ChatFormatting.WHITE
                    : e.met() ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY;

            var line = Component.literal(mark).withStyle(colour)
                    .append(Component.literal(e.name()).withStyle(nameColour));
            if (e.kills() > 1) {
                line.append(Component.literal(" x" + e.kills()).withStyle(ChatFormatting.DARK_GRAY));
            }
            boolean readable = e.met() || e.kills() > 0;
            boolean hovered = readable && mouseX >= x && mouseX <= x + columnWidth - 6
                    && mouseY >= y && mouseY < y + ROW;
            if (hovered) {
                // A faint plate rather than a colour change: the tick and the name already carry
                // meaning, and a third colour on the same row would be one signal too many.
                gfx.fill(x - 2, y - 1, x + columnWidth - 6, y + ROW - 2, 0x33FFFFFF);
            }
            gfx.drawString(font, line, x, y, WHITE, false);
            hitboxes.add(new int[] {x - 2, y - 1, x + columnWidth - 6, y + ROW - 2});
            visible.add(e);
        }

        gfx.drawCenteredString(font, Component.literal(
                        "click a name you have met to read its entry")
                .withStyle(ChatFormatting.DARK_GRAY), width / 2, height - 26, WHITE);

        if (entries.size() > perPage) {
            gfx.drawCenteredString(font, Component.literal("scroll for more")
                    .withStyle(ChatFormatting.DARK_GRAY), width / 2, height - 14, WHITE);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        if (super.mouseClicked(event, doubled)) {
            return true;
        }
        for (int i = 0; i < hitboxes.size(); i++) {
            int[] box = hitboxes.get(i);
            if (event.x() >= box[0] && event.x() <= box[2]
                    && event.y() >= box[1] && event.y() <= box[3]) {
                DexPayload.Entry e = visible.get(i);
                // The gate is the server's, and this is a courtesy copy of it: an entry you could
                // read before meeting the thing would be a manual rather than a bestiary. The
                // server enforces the same rule for /zm bestiary info.
                if (!e.met() && e.kills() == 0) {
                    return false;
                }
                net.minecraft.client.Minecraft.getInstance()
                        .setScreen(new DexInfoScreen(this, e.genus(), e.name()));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        scroll = Math.max(0, scroll - (int) Math.signum(dy) * COLUMNS);
        return true;
    }
}
