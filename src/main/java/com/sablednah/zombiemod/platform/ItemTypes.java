package com.sablednah.zombiemod.platform;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Getting a stack's registry holder.
 *
 * <p><b>Why this exists.</b> 26.2 removed {@code ItemStack.STRICT_SINGLE_ITEM_CODEC} and
 * {@code SIMPLE_ITEM_CODEC}, leaving {@code CODEC}, {@code MAP_CODEC} and {@code OPTIONAL_CODEC}.
 * {@code getItemHolder()} moved too.
 *
 * <p><b>Named {@code ItemTypes}, not {@code Items}</b>, for the reason {@code BlockTypes} carries:
 * {@code net.minecraft.world.item.Items} is imported elsewhere in this codebase and a single-type
 * import clash is a confusing error for a class meant to reduce confusion.
 *
 * <p>Reading an item <em>out of a genus file</em> is no longer here at all — see {@code ItemSpec},
 * which defers building the stack and is version-agnostic, so it needs no seam.
 */
public final class ItemTypes {

    private ItemTypes() {}

    /** The holder for a stack's item, for testing against an item tag. */
    public static Holder<Item> holderOf(ItemStack stack) {
        return stack.getItemHolder();
    }
}
