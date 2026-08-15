package com.sablednah.zombiemod.neoforge;

import java.util.EnumSet;

import com.sablednah.zombiemod.core.Genus;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Holds the looks that vanilla keeps taking back.
 *
 * <p>Stuck arrows are a <em>state</em> vanilla spends every tick undoing - they work themselves
 * loose one at a time - so a count set once at spawn is a monster that looks right for half a
 * minute and then quietly stops. This tops them back up.
 *
 * <p>Invisibility used to be upheld here too, and is not any more: it rides an infinite potion
 * effect now, which vanilla saves and re-asserts itself. The 40-tick top-up was in fact the
 * visible flicker - the mob stood exposed from the first effects pass until this goal next fired.
 *
 * <p>Flagless, like {@link AbilityGoal} and {@link MutationGoal}, and for the same reasons: no
 * registry of live mobs, nothing to leak, no cost in chunks that are not ticking.
 *
 * <p>Two things deliberately not here. <b>Burning</b> is persistent instead — {@code hasVisualFire}
 * is saved by vanilla, and is display-only, where {@code remainingFireTicks} would set the mob
 * genuinely alight and kill it. <b>Freezing</b> is not implemented at all: {@code setTicksFrozen}
 * past the threshold buys a shiver animation and a speed penalty, not the ice-blue skin it sounds
 * like, and charges a point of freeze damage every forty ticks for it.
 */
final class UpkeepGoal extends Goal {

    /** Cheap, but not free - and none of these need to be right on the exact tick. */
    private static final int PERIOD = 40;

    private final Mob mob;
    private final int arrows;
    private int ticks;

    private UpkeepGoal(Mob mob, int arrows) {
        this.mob = mob;
        this.arrows = arrows;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    /** Null when a genus asks for none of this, so the common case adds no goal at all. */
    static UpkeepGoal forGenus(Mob mob, Genus genus) {
        if (genus.arrows() <= 0) {
            return null;
        }
        UpkeepGoal goal = new UpkeepGoal(mob, genus.arrows());
        goal.apply();
        return goal;
    }

    private void apply() {
        if (arrows > 0 && mob.getArrowCount() < arrows) {
            mob.setArrowCount(arrows);
        }
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
        if (mob.isAlive()) {
            apply();
        }
    }
}
