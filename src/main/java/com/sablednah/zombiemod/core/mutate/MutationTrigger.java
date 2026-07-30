package com.sablednah.zombiemod.core.mutate;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/**
 * One test about a monster right now: is it burning, is it wading, is it nearly dead.
 *
 * <p>Deliberately a different interface from {@link com.sablednah.zombiemod.core.spawn.SpawnCondition}
 * rather than an extension of it, because the two ask different questions. A spawn condition knows
 * about a <em>place</em> and nothing else — that narrowness is what lets it be reused for spawning,
 * for behaviours and for hordes. A mutation trigger needs the entity: its health, what it is standing
 * in, what is happening to it.
 *
 * <p>The two meet at {@link MutationTriggers.Where}, which wraps any spawn condition and tests it
 * where the mob is standing. That adapter is worth more than any trigger type here: it means every
 * condition the mod already has — dimension, biome, height, light, time, sky, claim, and the
 * {@code any_of}/{@code not} combinators — becomes a mutation trigger for free, and any condition an
 * optional integration contributes later does too.
 *
 * <p>Implementations must be stateless; one instance is shared across every mob of a genus.
 */
public interface MutationTrigger {

    Codec<MutationTrigger> CODEC =
            Identifier.CODEC.dispatch("type", MutationTrigger::type, MutationTriggerTypes::codecOf);

    /** Registered id of this trigger's type, e.g. {@code zombiemod:in_water}. */
    Identifier type();

    /** @return whether this holds for that mob at this instant. */
    boolean test(ServerLevel level, Mob mob);
}
