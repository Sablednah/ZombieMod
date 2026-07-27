package com.sablednah.zombiemod.core.ability;

import java.util.List;

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
                yield victim != null && victim.isAlive() && victim.distanceToSqr(mob) <= radius * radius
                        ? List.of(victim)
                        : List.of();
            }
            case NEARBY_PLAYERS -> nearbyPlayers(level, mob, radius);
        };
    }

    static List<LivingEntity> nearbyPlayers(ServerLevel level, Mob mob, double radius) {
        return level.getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(radius),
                        p -> p.isAlive() && !p.isSpectator() && !p.isCreative())
                .stream()
                .map(p -> (LivingEntity) p)
                .toList();
    }

    private Targets() {}
}
