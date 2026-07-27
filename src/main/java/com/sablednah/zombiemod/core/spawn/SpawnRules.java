package com.sablednah.zombiemod.core.spawn;

import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

/**
 * Where and when a genus may claim a spawn.
 *
 * <p>{@code conditions} are ANDed — every one must pass. Use {@code zombiemod:any_of} for
 * alternatives and {@code zombiemod:not} to invert. An empty list means "anywhere the base mob
 * appears", which is the old plugin's behaviour and the sensible default.
 *
 * <p>{@code reasons} is deliberately <em>not</em> a condition. Spawn reason is not a property of the
 * place, and unlike every positional test it needs a restrictive default that a condition list
 * cannot express — an empty condition list has to mean "anywhere", but an unspecified reason set
 * must not mean "every reason".
 *
 * @param reasons    which spawn reasons this genus may ride
 * @param conditions positional tests, all of which must pass
 */
public record SpawnRules(List<EntitySpawnReason> reasons, List<SpawnCondition> conditions) {

    /**
     * Spawn reasons a genus is eligible for when it doesn't say otherwise.
     *
     * <p>Narrow on purpose. A genus riding {@code CONVERSION} would replace drowning zombies and
     * cured villagers; riding {@code REINFORCEMENT} would let a horde summon itself. Both are
     * surprising defaults, so a genus that wants them has to ask.
     */
    public static final List<EntitySpawnReason> DEFAULT_REASONS =
            List.of(EntitySpawnReason.NATURAL, EntitySpawnReason.CHUNK_GENERATION, EntitySpawnReason.SPAWNER);

    /** No positional restrictions, default reasons. */
    public static final SpawnRules ANY = new SpawnRules(DEFAULT_REASONS, List.of());

    private static final Codec<EntitySpawnReason> REASON = Codec.STRING.xmap(
            s -> EntitySpawnReason.valueOf(s.toUpperCase(Locale.ROOT)),
            r -> r.name().toLowerCase(Locale.ROOT));

    public static final Codec<SpawnRules> CODEC = RecordCodecBuilder.create(i -> i.group(
            REASON.listOf().optionalFieldOf("reasons", DEFAULT_REASONS).forGetter(SpawnRules::reasons),
            SpawnCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(SpawnRules::conditions))
            .apply(i, SpawnRules::new));

    public boolean allows(LevelReader level, BlockPos pos, ResourceKey<Level> dimension, EntitySpawnReason reason) {
        if (!reasons.contains(reason)) {
            return false;
        }
        for (SpawnCondition condition : conditions) {
            if (!condition.test(level, pos, dimension, reason)) {
                return false;
            }
        }
        return true;
    }
}
