package com.sablednah.zombiemod.core.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;

/**
 * Go and find a block you have an opinion about.
 *
 * <p>Written for the Blight, which had a problem that only showed up in play: with nobody to chase
 * it stood still, and a genus whose whole point is killing greenery was doing nothing while
 * surrounded by the moss it hates. Its {@code break_blocks} could only act on what it happened to be
 * standing in, and nothing was walking it anywhere.
 *
 * <p>Built on vanilla's {@link MoveToBlockGoal}, which already does the hard half — a spiral search
 * out from the mob, a path to what it finds, and a give-up timer. All this adds is which blocks
 * count and when to bother looking.
 *
 * <p>Idles out of the way when the mob has a target: hunting something is more urgent than hunting
 * moss, and a Blight that wandered off mid-fight to weed a lawn would read as broken.
 */
public final class SeekBlocksGoal extends MoveToBlockGoal {

    private final HolderSet<Block> wanted;
    private final boolean onlyWhenIdle;

    public SeekBlocksGoal(PathfinderMob mob, HolderSet<Block> wanted, double speed, int range,
            int verticalRange, boolean onlyWhenIdle) {
        super(mob, speed, range, verticalRange);
        this.wanted = wanted;
        this.onlyWhenIdle = onlyWhenIdle;
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        return wanted.contains(level.getBlockState(pos).getBlockHolder());
    }

    @Override
    public boolean canUse() {
        return (!onlyWhenIdle || mob.getTarget() == null) && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return (!onlyWhenIdle || mob.getTarget() == null) && super.canContinueToUse();
    }

    /**
     * Vanilla waits 200–400 ticks between searches, which suits a mob for which this is an errand.
     * For one whose entire job it is, ten to twenty seconds of standing about is the bug being
     * fixed, so it looks roughly four times as often.
     */
    @Override
    protected int nextStartTick(PathfinderMob mob) {
        return reducedTickDelay(50 + mob.getRandom().nextInt(50));
    }

    /** Close enough to act on. break_blocks reaches its own square and the ring around its feet. */
    @Override
    public double acceptedDistance() {
        return 1.5D;
    }
}
