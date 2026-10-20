package com.sablednah.zombiemod.client.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.sablednah.zombiemod.net.MobDropsPayload;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * One mob or genus per page: its doll and name (drawn by {@link MobDropsDrawing}, the one file here
 * that differs per Minecraft version) and up to {@value #SLOTS} drops, most likely first. Anything
 * beyond that shares the last slot, cycling, so nothing a table can give is ever missing from the
 * lookup - only from the picture.
 */
final class MobDropsCategory extends MobDropsDrawing {

    static final IRecipeType<MobDropsPayload.Entry> TYPE =
            IRecipeType.create("zombiemod", "mob_drops", MobDropsPayload.Entry.class);

    static final int COLUMNS = 7;
    static final int ROWS = 3;
    static final int SLOTS = COLUMNS * ROWS;

    private final IDrawable icon;

    MobDropsCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemLike(Items.ROTTEN_FLESH);
    }

    @Override
    public IRecipeType<MobDropsPayload.Entry> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Mob Drops");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return SLOT_X + COLUMNS * 18;
    }

    @Override
    public int getHeight() {
        return SLOT_Y + ROWS * 18;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MobDropsPayload.Entry entry, IFocusGroup focuses) {
        List<MobDropsPayload.Drop> drops = entry.drops().stream()
                .filter(d -> BuiltInRegistries.ITEM.getOptional(d.item()).isPresent())
                .toList();
        for (int slot = 0; slot < SLOTS && slot < drops.size(); slot++) {
            List<ItemStack> stacks = new ArrayList<>();
            // The last slot takes everything left over, if there is more than fits.
            int to = slot == SLOTS - 1 ? drops.size() : slot + 1;
            for (int i = slot; i < to; i++) {
                stacks.add(new ItemStack(BuiltInRegistries.ITEM.getValue(drops.get(i).item())));
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, SLOT_X + (slot % COLUMNS) * 18 + 1,
                            SLOT_Y + (slot / COLUMNS) * 18 + 1)
                    .setStandardSlotBackground()
                    .addItemStacks(stacks)
                    .addRichTooltipCallback((view, tooltip) -> view.getDisplayedItemStack().ifPresent(stack -> {
                        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                        drops.stream().filter(d -> d.item().equals(id)).findFirst().ifPresent(d -> {
                            tooltip.add(Component.literal(chance(d.chance()) + "  " + count(d))
                                    .withStyle(ChatFormatting.GOLD));
                            if (d.situational()) {
                                tooltip.add(Component.literal("only in some circumstances")
                                        .withStyle(ChatFormatting.YELLOW));
                            }
                            tooltip.add(Component.literal("per kill, before Looting")
                                    .withStyle(ChatFormatting.DARK_GRAY));
                        });
                    }));
        }
    }

    static String chance(float chance) {
        if (chance >= 0.9995F) {
            return "Always";
        }
        if (chance < 0.001F) {
            return "<0.1%";
        }
        String pct = String.format(Locale.ROOT, chance >= 0.1F ? "%.0f" : "%.1f", chance * 100.0F);
        return pct + "%";
    }

    static String count(MobDropsPayload.Drop d) {
        return d.min() == d.max() ? "x" + d.max() : "x" + d.min() + "-" + d.max();
    }
}
