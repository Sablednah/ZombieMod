package com.sablednah.zombiemod.platform;

import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Reading an item out of a genus file, and getting a stack's registry holder.
 *
 * <p><b>Why this exists.</b> 26.2 removed {@code ItemStack.STRICT_SINGLE_ITEM_CODEC} and
 * {@code SIMPLE_ITEM_CODEC}, leaving {@code CODEC}, {@code MAP_CODEC} and {@code OPTIONAL_CODEC}.
 * {@code getItemHolder()} moved too.
 *
 * <p><b>The codec is the one place here where a version branch must think rather than rename.</b>
 * The pair being combined is deliberate and is documented on {@code Equipment}: genus files may
 * write {@code "minecraft:iron_sword"} or the full {@code {"id": ..., "components": {...}}}, and
 * most write the former, so demanding the object form everywhere would make every genus noisier for
 * no gain. Whatever a new version offers, <b>both spellings must keep parsing</b> — every shipped
 * genus and every third-party datapack depends on it, and a codec that silently accepted only one
 * would break them at world load rather than at build time.
 */
public final class Items {

    private Items() {}

    /**
     * Accepts a bare id or the full object form.
     *
     * <p><b>Differs per version.</b> On 26.2 the two constants below are gone; the replacement must
     * preserve *both* accepted spellings — see the class note.
     */
    public static Codec<ItemStack> stackCodec() {
        // Both spellings preserved: the object form, and a bare id via the item registry.
        return Codec.withAlternative(ItemStack.CODEC,
                net.minecraft.core.registries.BuiltInRegistries.ITEM.byNameCodec()
                        .xmap(ItemStack::new, ItemStack::getItem));
    }

    /** The holder for a stack's item, for testing against an item tag. */
    public static Holder<Item> holderOf(ItemStack stack) {
        return stack.typeHolder();
    }
}
