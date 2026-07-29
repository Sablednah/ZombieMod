package com.sablednah.zombiemod.neoforge;

import java.util.EnumSet;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.core.ability.Ability;

import org.slf4j.Logger;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Drives one {@link Ability} off the mob's own goal ticking.
 *
 * <p>Abilities could have run from a global per-tick sweep over every tracked mob, which is what the
 * 1.8 plugin's {@code Animations} did. Riding the goal selector instead is better on every axis
 * that matters here: no registry of live mobs to keep in step with the world, no leak when an entity
 * is removed, no work at all for chunks that aren't ticking, and the per-mob timer is just a field
 * on a per-mob object.
 *
 * <p>The trick that makes it safe is the empty flag set. Goals declare which controls they need
 * (MOVE, LOOK, JUMP, TARGET) and the selector only runs a goal when its flags are free. A goal with
 * <em>no</em> flags never conflicts with anything, so it ticks every tick alongside whatever the mob
 * is actually doing — exactly what a passive ability wants.
 */
final class AbilityGoal extends Goal {

    private static final Logger LOG = LogUtils.getLogger();

    private final Mob mob;
    private final Ability ability;
    private final Ability.State state;
    private int ticks;

    AbilityGoal(Mob mob, Ability ability) {
        this.mob = mob;
        this.ability = ability;
        // Stateful abilities (a fuse, a wind-up) get one state object per mob; stateless ones get
        // null and just have run() called.
        this.state = ability.newState();
        // No flags: never competes with movement, looking or targeting.
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));

        // Stagger the first firing so a horde spawned together doesn't act in lockstep.
        this.ticks = mob.getRandom().nextInt(Math.max(1, ability.interval()));
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
        if (!(mob.level() instanceof ServerLevel level) || !mob.isAlive()) {
            return;
        }

        // A stateful ability ticks every tick and owns its own timing - which is what
        // Ability.newState() promises. Applying the interval gate on top as well meant a beam that
        // needed to hold and track every tick was only touched once every 120, so it never
        // progressed and never followed its caster.
        if (state != null) {
            try {
                state.tick(level, mob);
            } catch (Exception e) {
                LOG.error("ZombieMod ability {} failed on {}", ability.type(), mob.getType(), e);
            }
            return;
        }

        if (++ticks < ability.interval()) {
            return;
        }
        ticks = 0;
        if (ability.chance() < 1.0F && mob.getRandom().nextFloat() >= ability.chance()) {
            return;
        }

        try {
            ability.run(level, mob);
        } catch (Exception e) {
            // One bad ability must not take the entity's whole tick down with it, and a datapack
            // author needs to see which one.
            LOG.error("ZombieMod ability {} failed on {}", ability.type(), mob.getType(), e);
        }
    }
}
