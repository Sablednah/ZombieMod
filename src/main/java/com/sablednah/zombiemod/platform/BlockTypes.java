package com.sablednah.zombiemod.platform;

import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Getting the registry {@link Holder} out of a {@link BlockState}.
 *
 * <p><b>Why this exists.</b> {@code BlockState.getBlockHolder()} is {@code typeHolder()} from 26.1
 * onward — {@code BlockStateBase} now implements {@code TypedInstance<Block>} and the accessor was
 * renamed to match. Pure rename, five call sites, and the kind of change that is invisible until it
 * is a compile error on a version you have not built yet.
 *
 * <p><b>Named {@code BlockTypes}, not {@code Blocks}</b>, because {@code net.minecraft.world.level
 * .block.Blocks} is imported all over this codebase and a single-type-import clash is a confusing
 * error for a class that exists to reduce confusion. Platform seams should not borrow the name of a
 * common vanilla class.
 *
 * <p>Everything that reads it is a tag test — "is this block one a Blight will eat", "is this the
 * block the ritual pattern wants" — so this is on the path of every {@code seek_blocks} tick and
 * every ritual check. It stays a one-line delegation for that reason.
 */
public final class BlockTypes {

    private BlockTypes() {}

    /**
     * The holder for a state's block, for testing against a {@code HolderSet} (a block tag).
     *
     * <p><b>Differs per version.</b> On 26.1+ this becomes {@code state.typeHolder()}.
     */
    public static Holder<Block> holderOf(BlockState state) {
        return state.getBlockHolder();
    }
}
