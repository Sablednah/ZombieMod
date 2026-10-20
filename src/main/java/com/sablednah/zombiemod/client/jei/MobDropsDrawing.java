package com.sablednah.zombiemod.client.jei;

import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.client.DexPreview;
import com.sablednah.zombiemod.net.MobDropsPayload;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;

import org.joml.Matrix3x2f;
import org.joml.Vector2f;

/**
 * The drawn part of a Mob Drops page: the doll on the left, the name across the top.
 *
 * <p><b>The one JEI file that differs per Minecraft version</b>, and only because {@code draw}
 * takes the GUI class, which 26.x renamed ({@code GuiGraphics} to {@code GuiGraphicsExtractor},
 * {@code drawString} to {@code text}, {@code renderEntityInInventoryFollowsMouse} to
 * {@code extractEntityInInventoryFollowsMouse}). Everything else about the category is shared.
 */
abstract class MobDropsDrawing implements IRecipeCategory<MobDropsPayload.Entry> {

    /** Where the drop grid starts; the doll has the column to its left, the name the row above. */
    static final int SLOT_X = 40;
    static final int SLOT_Y = 11;

    @Override
    public void draw(MobDropsPayload.Entry entry, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX,
            double mouseY) {
        Font font = Minecraft.getInstance().font;
        String name = font.plainSubstrByWidth(entry.name().getString(), getWidth() - SLOT_X);
        g.text(font, name, SLOT_X, 1, 0xFF404040, false);

        LivingEntity doll = doll(entry);
        if (doll == null) {
            return;
        }
        // Fit the box by whichever of height and width is the tighter, so a spider or a ghast is
        // as legible as a zombie. Same anchoring as the dex: feet on the floor line.
        int boxW = SLOT_X - 4;
        int boxH = getHeight() - 2;
        float h = Math.max(0.3F, doll.getBbHeight());
        float w = Math.max(0.3F, doll.getBbWidth());
        int size = Math.max(4, (int) Math.min((boxH - 4) / (h + 0.125F), (boxW - 2) / (w * 1.3F)));
        int footPx = Math.round((h / 2.0F + 0.0625F) * size);
        int headPx = Math.round((h / 2.0F - 0.0625F) * size);
        int half = Math.max(footPx, headPx) + 2;
        int centerY = boxH - footPx;
        // In screen space, not recipe space. JEI moves the pose to wherever this recipe sits, but
        // an entity is drawn as a picture-in-picture whose box is taken as raw screen coordinates
        // and never put through the pose - so a box given in recipe space lands at the top left of
        // the screen, outside JEI's scissor, and no doll appears at all. The dex never saw this
        // because it draws with an untouched pose.
        Matrix3x2f pose = g.pose();
        Vector2f from = pose.transformPosition(1, centerY - half, new Vector2f());
        Vector2f to = pose.transformPosition(boxW, centerY + half, new Vector2f());
        Vector2f mouse = pose.transformPosition((float) mouseX, (float) mouseY, new Vector2f());
        float scale = (float) Math.hypot(pose.m00(), pose.m01());
        InventoryScreen.extractEntityInInventoryFollowsMouse(g, Math.round(from.x), Math.round(from.y), Math.round(to.x),
                Math.round(to.y), Math.max(1, Math.round(size * scale)), 0.0625F, mouse.x, mouse.y, doll);
    }

    private static LivingEntity doll(MobDropsPayload.Entry entry) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        if (entry.genus().isPresent()) {
            var holder = level.registryAccess().lookup(ZombieModRegistries.GENUS)
                    .flatMap(l -> l.get(ResourceKey.create(ZombieModRegistries.GENUS, entry.genus().get())));
            if (holder.isPresent()) {
                return DexPreview.of(entry.genus().get(), holder.get());
            }
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(entry.entity()).map(DexPreview::ofType).orElse(null);
    }
}
