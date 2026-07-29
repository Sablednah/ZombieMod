package com.sablednah.zombiemod.neoforge;

import java.util.EnumSet;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Makes a wall-climbing genus actually go up.
 *
 * <p>{@code WallClimberNavigation} alone is not enough, and the failure is quiet: it only changes
 * <em>path planning</em>, making the pathfinder willing to route straight up a wall. Executing that
 * path needs {@link net.minecraft.world.entity.LivingEntity#onClimbable()} to be true, which is how
 * the spider does it — it overrides {@code onClimbable} to return a synced climbing flag that it
 * sets from {@code horizontalCollision} every tick. A vanilla zombie's {@code onClimbable} only
 * answers for ladders and vines, so the navigator plans a climb the mob cannot perform and it stands
 * at the bottom of the wall looking foolish.
 *
 * <p>Overriding {@code onClimbable} would need a mixin. Applying the upward motion ourselves does
 * not, and lands in the same place: while the mob is pressed against something, push it up.
 *
 * <p>Matched to the spider deliberately — it climbs whenever it collides, target or not, so walls
 * get climbed rather than pathed around.
 */
final class ClimbGoal extends Goal {

    /** Roughly vanilla's ladder speed; fast enough to be threatening, slow enough to see coming. */
    private static final double CLIMB_SPEED = 0.2D;

    private final Mob mob;

    ClimbGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!mob.horizontalCollision) {
            return;
        }
        Vec3 motion = mob.getDeltaMovement();
        mob.setDeltaMovement(motion.x, CLIMB_SPEED, motion.z);
        // Cancel accumulated fall, so stepping off the top is not a punishment for climbing.
        mob.fallDistance = 0.0F;
    }
}
