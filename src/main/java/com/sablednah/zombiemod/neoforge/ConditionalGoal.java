package com.sablednah.zombiemod.neoforge;

import java.util.EnumSet;

import com.sablednah.zombiemod.core.spawn.SpawnCondition;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * A goal that only applies while a condition holds — the machinery behind day/night behaviour.
 *
 * <p>The obvious implementation of "swap this mob's AI at dusk" is to rebuild its goal list, and it
 * is a trap: {@code GoalSelector.tick} iterates {@code availableGoals}, so removing goals from
 * inside a goal's own tick throws {@link java.util.ConcurrentModificationException}. Rebuilding from
 * an entity-tick event instead would work but needs a per-tick hook on every entity in the world.
 *
 * <p>So nothing is ever rebuilt. Both behaviour sets are registered up front and each goal is gated
 * on its condition through {@code canUse}/{@code canContinueToUse} — which is exactly what the goal
 * system is already for. Vanilla starts and stops goals as those answers change, so a mob that
 * "becomes hostile at night" is really a mob whose hunting goals simply become usable at 13000.
 *
 * <p>The condition is re-evaluated at most every {@link #CHECK_INTERVAL} ticks, because
 * {@code canUse} is polled every tick per goal and a biome or time lookup per goal per tick is
 * needless work for something that changes on the scale of minutes.
 */
final class ConditionalGoal extends Goal {

    private static final int CHECK_INTERVAL = 20;

    private final Mob mob;
    private final Goal delegate;
    private final SpawnCondition condition;

    private int cooldown;
    private boolean cached;

    ConditionalGoal(Mob mob, Goal delegate, SpawnCondition condition) {
        this.mob = mob;
        this.delegate = delegate;
        this.condition = condition;
        // Inherit the delegate's control flags, or the selector will not know what this goal
        // competes for and two movement goals could run at once.
        this.setFlags(EnumSet.copyOf(delegate.getFlags()));
    }

    private boolean active() {
        if (--cooldown <= 0) {
            cooldown = CHECK_INTERVAL;
            cached = condition.test(mob.level(), mob.blockPosition());
        }
        return cached;
    }

    @Override
    public boolean canUse() {
        return active() && delegate.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return active() && delegate.canContinueToUse();
    }

    @Override
    public boolean isInterruptable() {
        return delegate.isInterruptable();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return delegate.requiresUpdateEveryTick();
    }

    @Override
    public void start() {
        delegate.start();
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public void tick() {
        delegate.tick();
    }

    @Override
    public String toString() {
        return "Conditional[" + condition.type() + " -> " + delegate + "]";
    }
}
