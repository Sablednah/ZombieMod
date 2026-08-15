package com.sablednah.zombiemod.core.goal;

import java.util.EnumSet;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * Targets by ear, not by eye.
 *
 * <p>The Clicker's fantasy was always "blind, but it hears you", and until now it was faked with a
 * short follow range — which is not the same thing at all: a low range is deaf <em>and</em> blind,
 * and it cannot distinguish a sprinting player from a sneaking one, which is the entire game the
 * trope is asking you to play.
 *
 * <p>This goal listens instead. How far away it notices you depends on how much noise you are
 * making: sprinting carries a long way, walking a middling one, and sneaking or standing still
 * almost nothing. Line of sight is never consulted — walls do not stop it, which is what makes it
 * feel like hearing — and neither is invisibility, which buys you nothing against something that
 * never used its eyes.
 *
 * <p>A target it can no longer hear is dropped after a short memory, so the counterplay is real:
 * break into a crouch, go quiet, and it loses you.
 */
public final class SoundTargetGoal extends Goal {

    /** How long it remembers a noise after the noise stops, in ticks. */
    private static final int MEMORY = 60;
    /** Re-listen cadence. Hearing does not need to be frame-perfect. */
    private static final int LISTEN_EVERY = 10;

    private final Mob mob;
    private final double sprintRadius;
    private final double walkRadius;
    private final double sneakRadius;

    private Player heard;
    private int sinceHeard;
    private int cooldown;

    public SoundTargetGoal(Mob mob, double sprintRadius, double walkRadius, double sneakRadius) {
        this.mob = mob;
        this.sprintRadius = sprintRadius;
        this.walkRadius = walkRadius;
        this.sneakRadius = sneakRadius;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    /** How far this player's noise carries right now. Public because it IS the policy - the probe
     * and any future ability that cares about noise should ask this, not re-derive it. */
    public double audibleRange(Player player) {
        if (player.isSprinting()) {
            return sprintRadius;
        }
        // Sneaking is quiet however fast you crab about; standing still is quiet by definition.
        // isShiftKeyDown is the synced INTENT and answers immediately; isCrouching is the computed
        // POSE and lags a tick behind (and never updates at all on an unticked entity) - ask both.
        boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;
        if (player.isShiftKeyDown() || player.isCrouching() || !moving) {
            return sneakRadius;
        }
        return walkRadius;
    }

    private Player listen() {
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player player : mob.level().players()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive()) {
                continue;
            }
            double dist = mob.distanceTo(player);
            if (dist <= audibleRange(player) && dist < bestDist) {
                best = player;
                bestDist = dist;
            }
        }
        return best;
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0) {
            return false;
        }
        cooldown = LISTEN_EVERY;
        heard = listen();
        return heard != null;
    }

    @Override
    public void start() {
        mob.setTarget(heard);
        sinceHeard = 0;
    }

    @Override
    public boolean canContinueToUse() {
        var target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (target instanceof Player player && mob.distanceTo(player) <= audibleRange(player)) {
            sinceHeard = 0;
            return true;
        }
        // Still remembered, not currently heard. Gone quiet for long enough means gone.
        return ++sinceHeard < MEMORY;
    }

    @Override
    public void stop() {
        heard = null;
        // Only clear a target this goal set; a hurt_by target is somebody else's business.
        if (mob.getTarget() instanceof Player) {
            mob.setTarget(null);
        }
    }
}
