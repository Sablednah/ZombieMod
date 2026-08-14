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

    private static final int ROW = 12;
    private static final int COLUMNS = 3;

    private int scroll;

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
        var entries = DexState.entries();

        gfx.drawCenteredString(font, Component.literal("ZombieDex").withStyle(ChatFormatting.GOLD),
                width / 2, 14, 0xFFFFFF);
        gfx.drawCenteredString(font, Component.literal(
                        DexState.slain() + " of " + entries.size() + " slain, " + DexState.met() + " met")
                .withStyle(ChatFormatting.GRAY), width / 2, 26, 0xFFFFFF);

        if (entries.isEmpty()) {
            gfx.drawCenteredString(font, Component.literal("Nothing yet - the server has not sent one.")
                    .withStyle(ChatFormatting.DARK_GRAY), width / 2, height / 2, 0xFFFFFF);
            return;
        }

        int columnWidth = Math.min(160, (width - 40) / COLUMNS);
        int left = (width - columnWidth * COLUMNS) / 2;
        int top = 46;
        int rows = Math.max(1, (height - top - 20) / ROW);
        int perPage = rows * COLUMNS;
        int start = Math.min(scroll, Math.max(0, entries.size() - perPage));

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
            gfx.drawString(font, line, x, y, 0xFFFFFF, false);
        }

        if (entries.size() > perPage) {
            gfx.drawCenteredString(font, Component.literal("scroll for more")
                    .withStyle(ChatFormatting.DARK_GRAY), width / 2, height - 14, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        scroll = Math.max(0, scroll - (int) Math.signum(dy) * COLUMNS);
        return true;
    }
}
