package com.sablednah.zombiemod.core.ability;

import java.util.List;

import com.sablednah.zombiemod.compat.StandardsVanish;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/** Resolves an {@link Abilities.Target} to the entities an ability should act on. */
final class Targets {

    static List<LivingEntity> of(Abilities.Target target, ServerLevel level, Mob mob, double radius) {
        return switch (target) {
            case SELF -> List.of(mob);
            case VICTIM -> {
                LivingEntity victim = mob.getTarget();
                // Range-check the victim: a mob keeps its target for a while after losing sight of
                // it, and an ability that reaches across the map is a bug, not a feature.
                //
                // Standards already refuses to let a mob take a vanished player as a target, and
                // clears the target of anything already hunting one - so in the ordinary case this
                // never sees a vanished victim and the test costs a field read. It is here for the
                // window Standards itself documents as open: vanishing does not rewind a blow
                // already in flight, and an ability mid-wind-up is exactly that.
                yield victim != null && victim.isAlive() && victim.distanceToSqr(mob) <= radius * radius
                                && !StandardsVanish.isVanished(victim)
                        ? List.of(victim)
                        : List.of();
            }
            case NEARBY_PLAYERS -> nearbyPlayers(level, mob, radius);
        };
    }

    /**
     * The players an ability should treat as present.
     *
     * <p>Spectators and creative players were already excluded, and <b>a vanished player belongs in
     * that list for the same reason</b>: they are all "here, but not participating". Vanish is the
     * only one of the three that another mod owns, so it is the only one that can be absent — and
     * when Standards is not installed this is exactly the filter it always was.
     *
     * <p>This is the line that fixes the Boomer. Its fuse is a proximity trigger and never consults
     * the mob's target at all, so target-level guards do nothing for it: it asks who is standing
     * nearby, and a vanished admin was answering.
     */
    static List<LivingEntity> nearbyPlayers(ServerLevel level, Mob mob, double radius) {
        // One field read on Standards' side, and false on virtually every server - so the
        // per-player call below is skipped entirely in the ordinary case.
        boolean anyVanished = StandardsVanish.anyVanished();
        return level.getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(radius),
                        p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
                                && !(anyVanished && StandardsVanish.isVanished(p)))
                .stream()
                .map(p -> (LivingEntity) p)
                .toList();
    }

    private Targets() {}
}
