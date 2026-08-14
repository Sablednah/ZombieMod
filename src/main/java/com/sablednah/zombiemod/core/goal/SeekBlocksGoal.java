package com.sablednah.zombiemod.core.goal;

import java.util.HashMap;
import java.util.Map;

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

    /** How long to keep trying one block before writing it off. Vanilla waits 1200 - a full minute. */
    private static final int PATIENCE = 160;
    /** And how long to remember that it was hopeless. */
    private static final int GRUDGE = 1200;

    private final HolderSet<Block> wanted;
    private final boolean onlyWhenIdle;
    /** Blocks it could not reach, and the tick each is forgiven. */
    private final Map<BlockPos, Integer> unreachable = new HashMap<>();
    private int age;
    private int patience;

    public SeekBlocksGoal(PathfinderMob mob, HolderSet<Block> wanted, double speed, int range,
            int verticalRange, boolean onlyWhenIdle) {
        super(mob, speed, range, verticalRange);
        this.wanted = wanted;
        this.onlyWhenIdle = onlyWhenIdle;
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        if (!wanted.contains(level.getBlockState(pos).getBlockHolder())) {
            return false;
        }
        // No "must have solid ground under it" rule here, deliberately, and it was a mistake to add
        // one: vines, hanging moss and glow lichen grow on the SIDES of blocks with air beneath
        // them, so the rule filtered out most of the greenery on a wall - which is precisely what a
        // Blight standing on a mossy building can see and was then told to ignore.
        //
        // Reachability is settled by trying and giving up instead, which is the honest way round:
        // the goal cannot know what the pathfinder can manage, and the pathfinder already answers
        // that question by failing.
        Integer forgiven = unreachable.get(pos);
        return forgiven == null || age >= forgiven;
    }

    @Override
    public boolean canUse() {
        age++;
        unreachable.values().removeIf(forgiven -> age >= forgiven);
        return (!onlyWhenIdle || mob.getTarget() == null) && super.canUse();
    }

    @Override
    public void start() {
        patience = 0;
        super.start();
    }

    @Override
    public boolean canContinueToUse() {
        if (onlyWhenIdle && mob.getTarget() != null) {
            return false;
        }
        // Give up in eight seconds rather than sixty, and remember why. Without the memory the
        // next search finds the same unreachable block and it starts the whole minute again, which
        // is a mob that looks like it is patrolling a roof rather than one that is stuck.
        if (!isReachedTarget() && ++patience > PATIENCE) {
            unreachable.put(blockPos.immutable(), age + GRUDGE);
            return false;
        }
        return super.canContinueToUse();
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

    /** Which block it is currently walking towards, for diagnostics. */
    public BlockPos seeking() {
        return blockPos;
    }

    /** Close enough to act on. break_blocks reaches its own square and the ring around its feet. */
    @Override
    public double acceptedDistance() {
        return 1.5D;
    }
}
