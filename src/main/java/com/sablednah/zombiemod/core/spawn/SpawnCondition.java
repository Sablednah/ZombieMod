package com.sablednah.zombiemod.core.spawn;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.resources.ResourceKey;

/**
 * One test a spawn must pass for a genus to claim it.
 *
 * <p>Conditions are a dispatched registry rather than fields on a record, for one specific reason:
 * the interesting conditions live outside this mod. "Only in CityWorld's wilderness", "only on a
 * city lot", "only inside a claimed chunk" all need a dependency ZombieMod must not require. A
 * registry lets an optional adapter contribute a condition type without the core knowing anything
 * about it — the same shape {@code GoalSpec} uses for AI.
 *
 * <p>Implementations must be stateless; one instance is shared across every spawn check.
 */
public interface SpawnCondition {

    Codec<SpawnCondition> CODEC =
            Identifier.CODEC.dispatch("type", SpawnCondition::type, SpawnConditionTypes::codecOf);

    /** Registered id of this condition's type, e.g. {@code zombiemod:light}. */
    Identifier type();

    /**
     * @param level     the level the spawn is happening in
     * @param pos       where the mob would appear
     * @param dimension the level's dimension key, passed separately because it is not derivable
     *                  from a {@link LevelReader}
     * @param reason    why the mob is being spawned
     * @return whether this condition permits the spawn
     */
    boolean test(LevelReader level, BlockPos pos, ResourceKey<Level> dimension, EntitySpawnReason reason);
}
