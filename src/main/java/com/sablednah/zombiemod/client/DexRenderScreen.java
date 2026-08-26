package com.sablednah.zombiemod.client;

import com.sablednah.zombiemod.core.Genus;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/**
 * A bare stage for one genus, used only by {@link DexRender}.
 *
 * <p>Nothing but a flat background and a doll standing on a fixed line. No frame, no text, no
 * buttons — every pixel that is not the mob has to be a known, uniform colour, because the exporter
 * recovers transparency by rendering the same doll on two different backgrounds and solving for
 * alpha. Anything else drawn here would differ between the two passes and come out as noise.
 *
 * <p>The sizing is {@link DexScreen}'s, deliberately: feet on a fixed line, height taken from the
 * doll's own bounding box, size multiplied by the genus's scale because the vanilla helper
 * normalises the render state's scale back to 1. That is what makes a Tank tower over a Swarmling
 * at honest relative size instead of every mob being drawn to fill its box.
 */
public final class DexRenderScreen extends Screen {

    private final LivingEntity doll;
    private final Genus genus;
    private final int background;
    private final int canvas;

    public DexRenderScreen(LivingEntity doll, Holder.Reference<Genus> holder, int background, int canvas) {
        super(Component.literal("ZombieDex render"));
        this.doll = doll;
        this.genus = holder.value();
        this.background = background;
        this.canvas = canvas;
    }

    /** No dimming: the world behind must not show through, or the two passes would not match. */
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // The canvas is a square in the middle of whatever the window happens to be. The exporter
        // crops to exactly this rectangle, so the window size does not change the output.
        int x0 = (this.width - canvas) / 2;
        int y0 = (this.height - canvas) / 2;
        g.fill(0, 0, this.width, this.height, background);

        float h = doll.getBbHeight();
        // Same two ceilings as the dex page: the genus's own size, and what the canvas can hold.
        int size = Math.max(12, (int) Math.min(30 * genus.scale() * (canvas / 180.0F), (canvas - 12) / (h + 0.125F)));
        int footPx = Math.round((h / 2.0F + 0.0625F) * size);
        int headPx = Math.round((h / 2.0F - 0.0625F) * size);
        int half = Math.max(footPx, headPx) + 2;
        // Feet on a fixed line near the bottom of the canvas, so the whole roster shares a floor.
        int centerY = y0 + canvas - 6 - footPx;
        int centerX = x0 + canvas / 2;
        // Angle rather than mouse: a fixed, repeatable pose. Passing the mouse would make the
        // output depend on where the cursor happened to be.
        InventoryScreen.renderEntityInInventoryFollowsAngle(g, centerX - half, centerY - half,
                centerX + half, centerY + half, size, 0.0625F, 0.0F, 0.0F, doll);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
