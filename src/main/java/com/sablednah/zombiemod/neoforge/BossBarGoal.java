package com.sablednah.zombiemod.neoforge;

import java.util.EnumSet;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.core.BossSpec;

import org.slf4j.Logger;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Keeps a boss's bar in step with its health and its audience.
 *
 * <p>Rides the goal selector with an empty flag set, exactly like {@code AbilityGoal} — same
 * reasoning: no registry of live bosses to maintain, no work in chunks that aren't ticking, and it
 * disappears with the entity. Updates a few times a second rather than every tick; a health bar
 * does not need 20Hz and the viewer re-scan is the expensive part.
 */
final class BossBarGoal extends Goal {

    private static final Logger LOG = LogUtils.getLogger();
    private static final int UPDATE_INTERVAL = 5;

    private final Mob mob;
    private final BossSpec spec;
    private int ticks;
    private boolean warned;

    BossBarGoal(Mob mob, BossSpec spec) {
        this.mob = mob;
        this.spec = spec;
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
        if (++ticks < UPDATE_INTERVAL) {
            return;
        }
        ticks = 0;
        if (!mob.isAlive()) {
            return;
        }
        try {
            BossBars.update(mob, spec);
        } catch (Exception e) {
            // A boss bar is UI, and a bug in UI must not stop the server. An exception out of a goal
            // tick is a "Ticking entity" crash that takes the whole world down - which is how 3.4.0's
            // viewer-list bug ended a Borg Hive mid-fight. Same rule as AbilityGoal. Once per boss,
            // not per tick: this runs four times a second and the first stack trace says everything.
            if (!warned) {
                warned = true;
                LOG.error("ZombieMod: the boss bar for {} failed; the fight carries on without it.",
                        mob.getName().getString(), e);
            }
        }
    }
}
