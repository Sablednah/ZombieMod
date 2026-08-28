package com.sablednah.zombiemod.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * "Is this land claimed?", asked once, answered by whichever land model the server runs.
 *
 * <p>ZombieMod used to ask FTB Chunks directly, which quietly made FTB the only land model it could
 * respect. A server running SableCraft Standards — with a faction mod or an FTB bridge behind it —
 * got no protection at all, and nothing said so.
 *
 * <p><b>Either provider answering "claimed" is enough.</b> The union is the protective reading, and
 * it is the right default for a grief check: if any land model on the server believes somebody owns
 * this ground, a zombie should not be pulling it apart. A server with both installed does not have
 * to reason about which one wins.
 *
 * <p>Both sides fail open — an absent, broken or changed provider answers "not claimed" — so the
 * worst case is that protection quietly does nothing, rather than zombies quietly stopping working.
 * That is the trade the rest of {@code compat} makes too, and it is deliberate: this is a mob mod,
 * and a mob mod that refuses to spawn mobs because a claims API moved is worse than one that
 * briefly forgets to respect a claim.
 */
public final class LandClaims {

    private LandClaims() {}

    /** True if either land model reports this position inside a claim. */
    public static boolean isClaimed(Level level, BlockPos pos) {
        return FtbChunks.isClaimed(level, pos) || StandardsClaims.isClaimed(level, pos);
    }

    /** True if any land model is present, for {@code /zombiemod status}. */
    public static boolean anyProvider() {
        return FtbChunks.available() || StandardsClaims.available();
    }

    /** Which providers answered, for {@code /zombiemod status}. */
    public static String providers() {
        boolean ftb = FtbChunks.available();
        boolean std = StandardsClaims.available();
        if (ftb && std) {
            return "FTB Chunks + Standards";
        }
        if (ftb) {
            return "FTB Chunks";
        }
        if (std) {
            return "Standards";
        }
        return "none";
    }
}
