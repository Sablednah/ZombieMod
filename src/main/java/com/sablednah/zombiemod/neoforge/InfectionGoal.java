package com.sablednah.zombiemod.neoforge;

import java.util.EnumSet;
import java.util.List;

import com.sablednah.zombiemod.ZombieModConfig;
import com.sablednah.zombiemod.core.ability.Infect;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * An infected animal is contagious.
 *
 * <p>Infection already crossed species — {@code Infect.onAttack} never checked that its victim was a
 * player, so a Biter could always sicken a cow and the cow always rose when it died. What it could
 * not do was carry on: an infected cow has no genus, so it has no abilities, so the chain stopped at
 * one animal. This is the link that makes a herd a herd.
 *
 * <p>Rides the victim's own goal selector, flagless, like everything else here. That matters more
 * than usual in this case: the alternative is a global sweep looking for infected entities, and the
 * whole point of the feature is that a lot of animals are infected at once.
 *
 * <p>Attached in two places, which is the persistent/transient split the mod uses everywhere — the
 * infection itself is saved in entity NBT, the goal that acts on it is not, so it is attached when
 * the bite lands <em>and</em> rebuilt whenever a tagged mob joins a level.
 */
public final class InfectionGoal extends Goal {

    private final Mob mob;
    private int ticks;

    private InfectionGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
        // Spread out, or every animal in a herd bitten in the same tick checks in the same tick
        // forever after.
        this.ticks = mob.getRandom().nextInt(Math.max(1, ZombieModConfig.INFECT_INTERVAL.get()));
    }

    /** Attaches one if this mob is infected and does not have one already. */
    public static void attach(Mob mob) {
        if (!ZombieModConfig.INFECT_SPREAD.get()) {
            return;
        }
        if (Infect.remaining(mob, mob.level().getGameTime()) < 0L) {
            return;
        }
        boolean already = mob.goalSelector.getAvailableGoals().stream()
                .anyMatch(w -> w.getGoal() instanceof InfectionGoal);
        if (!already) {
            mob.goalSelector.addGoal(98, new InfectionGoal(mob));
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
        if (++ticks < ZombieModConfig.INFECT_INTERVAL.get()) {
            return;
        }
        ticks = 0;
        if (!ZombieModConfig.INFECT_SPREAD.get() || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        long left = Infect.remaining(mob, now);
        if (left < 0L) {
            return;
        }
        if (left <= 0L || !Infect.stillMarked(mob)) {
            // Ran out, or was cured. Tidy the tags away so nothing later mistakes a stale timer for
            // an infection; the goal then idles until the mob unloads.
            Infect.clear(mob);
            return;
        }
        if (level.getRandom().nextFloat() >= ZombieModConfig.INFECT_CHANCE.get().floatValue()) {
            return;
        }

        Holder<MobEffect> marker = Infect.markerOf(mob).orElse(null);
        if (marker == null) {
            return;
        }
        double radius = ZombieModConfig.INFECT_RADIUS.get();
        boolean toPlayers = ZombieModConfig.INFECT_PLAYERS.get();

        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(mob.blockPosition()).inflate(radius),
                other -> other != mob
                        && other.isAlive()
                        && (toPlayers || !(other instanceof Player))
                        // Not already infected: re-marking a neighbour every ten seconds would make
                        // one sick animal an unkillable timer for the whole field.
                        && Infect.remaining(other, now) < 0L);
        if (near.isEmpty()) {
            return;
        }

        // One at a time. A herd should turn over minutes, which is a thing you can watch happen and
        // still do something about - not in a single tick, which is just an event you are told about.
        LivingEntity target = near.get(level.getRandom().nextInt(near.size()));
        // Passes on what is left rather than a fresh full timer, so a chain cannot outlive its
        // source indefinitely and the far end of a herd turns soonest after the near end.
        int passed = (int) Math.max(ZombieModConfig.INFECT_INTERVAL.get() * 2L, left);
        Infect.mark(level, target, marker, passed, Infect.risesAs(mob), true);
        if (target instanceof Mob spread) {
            attach(spread);
        }
    }
}
