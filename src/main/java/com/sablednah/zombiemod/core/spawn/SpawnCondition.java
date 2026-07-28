package com.sablednah.zombiemod.core.spawn;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/**
 * One test about a place: is it dark here, is this a forest, is it night.
 *
 * <p>Conditions are a dispatched registry rather than fields on a record, for one specific reason:
 * the interesting conditions live outside this mod. "Only in CityWorld's wilderness", "only on a
 * city lot", "only inside a claimed chunk" all need a dependency ZombieMod must not require. A
 * registry lets an optional adapter contribute a condition type without the core knowing anything
 * about it — the same shape {@code GoalSpec} uses for AI.
 *
 * <p>Used for two things now: gating where a genus may spawn, and gating which behaviours a live
 * mob currently has. That second use is why the signature is deliberately narrow — a condition
 * knows about a <em>place</em>, nothing about spawning.
 *
 * <p>Implementations must be stateless; one instance is shared across every check.
 */
public interface SpawnCondition {

    Codec<SpawnCondition> CODEC =
            Identifier.CODEC.dispatch("type", SpawnCondition::type, SpawnConditionTypes::codecOf);

    /** Registered id of this condition's type, e.g. {@code zombiemod:light}. */
    Identifier type();

    /**
     * @param level the level in question
     * @param pos   the position in question
     * @return whether this condition holds there
     */
    boolean test(Level level, BlockPos pos);
}
