package com.sablednah.zombiemod.neoforge;

import java.util.EnumSet;
import java.util.List;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.core.mutate.MutationSpec;

import org.slf4j.Logger;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Watches a genus's mutation triggers.
 *
 * <p>Rides the goal selector with an empty flag set for the same reasons {@link AbilityGoal} does:
 * no registry of live mobs, nothing to leak when one is removed, no cost in chunks that are not
 * ticking, and per-mob timing that is just a field.
 *
 * <p>Checked once a second rather than every tick. Every trigger here is about a state that persists
 * for far longer than a tick — standing in water, being on fire, being nearly dead — so twenty
 * checks a second would buy nothing but block lookups, and {@code for} is measured in whole checks
 * anyway.
 */
final class MutationGoal extends Goal {

    private static final Logger LOG = LogUtils.getLogger();

    private static final int PERIOD = 20;

    private final Mob mob;
    private final List<MutationSpec> mutations;
    /** Ticks each trigger has held continuously, parallel to {@link #mutations}. */
    private final int[] held;
    private int ticks;

    MutationGoal(Mob mob, List<MutationSpec> mutations) {
        this.mob = mob;
        this.mutations = mutations;
        this.held = new int[mutations.size()];
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
        this.ticks = mob.getRandom().nextInt(PERIOD);
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
        if (++ticks < PERIOD) {
            return;
        }
        ticks = 0;
        if (!(mob.level() instanceof ServerLevel level) || !mob.isAlive()) {
            return;
        }

        for (int i = 0; i < mutations.size(); i++) {
            MutationSpec spec = mutations.get(i);
            boolean holds;
            try {
                holds = spec.when().test(level, mob);
            } catch (Exception e) {
                LOG.error("ZombieMod mutation trigger {} failed on {}", spec.when().type(), mob.getType(), e);
                continue;
            }

            if (!holds) {
                // Reset rather than decay: "for 60 ticks" has to mean continuously, or a mob that
                // steps in and out of a puddle twenty times accumulates its way to a mutation that
                // never actually happened.
                held[i] = 0;
                continue;
            }
            held[i] += PERIOD;
            if (held[i] < spec.sustained()) {
                continue;
            }
            if (spec.chance() < 1.0F && mob.getRandom().nextFloat() >= spec.chance()) {
                continue;
            }
            // The first one whose conditions are met wins, and the mob it applied to no longer
            // exists - so there is nothing left for the remaining specs to test.
            if (Mutations.mutate(level, mob, spec).isPresent()) {
                return;
            }
        }
    }
}
