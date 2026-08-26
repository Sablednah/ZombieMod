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
 * <p><b>THIS DOES NOT WORK ON 26.x YET, AND IT IS NOT A CODEC PROBLEM.</b> An {@code ItemStack}
 * cannot be built at all while a datapack registry is loading on 26.2: every spelling fails with
 * <em>"Item minecraft:bow does not have components yet"</em>, because genus files are parsed on a
 * worker thread before item data components are bound. Both the object form and the bare id die the
 * same way, and {@code Item.CODEC_WITH_BOUND_COMPONENTS} does not help — it is the constant that
 * *requires* them. The fix is to stop parsing equipment into an {@code ItemStack} and start parsing
 * it into a description — an item {@code Holder} plus a component patch — materialised when a mob is
 * actually equipped, which is long after bootstrap. That is a change to {@code Equipment} and
 * {@code GenusApplier} rather than to this class, it works on 1.21.11 too, and it is the last thing
 * standing between the 26.x branches and a running jar. See {@code docs/MULTIVERSION.md}.
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
        // Both spellings preserved: the object form, and a bare id.
        //
        // The bare-id path goes through Item.CODEC, which yields a Holder and does NOT touch the
        // item's data components. Going through the item registry and calling new ItemStack(Item)
        // instead throws "Components not bound yet": genus files are parsed during registry data
        // loading, on a worker thread, before item components are bound. 26.2 makes that visible by
        // adding Item.CODEC_WITH_BOUND_COMPONENTS alongside the plain one - the plain one is what a
        // datapack-loaded registry may use.
        return Codec.withAlternative(ItemStack.CODEC,
                net.minecraft.world.item.Item.CODEC.xmap(ItemStack::new, ItemStack::typeHolder));
    }

    /** The holder for a stack's item, for testing against an item tag. */
    public static Holder<Item> holderOf(ItemStack stack) {
        return stack.typeHolder();
    }
}
