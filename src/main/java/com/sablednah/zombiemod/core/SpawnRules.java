package com.sablednah.zombiemod.core;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;

/**
 * Where and when a genus is allowed to turn up.
 *
 * <p>Every condition is optional and absent means "don't care", so a genus with no {@code spawn}
 * block at all can appear anywhere its base mob does — the old plugin's behaviour. Conditions are
 * checked against the actual spawn position, so a genus can be pinned to caves, to deserts, to
 * night-time, or to a dimension.
 *
 * @param biomes        biome allow-list; accepts a tag (`#minecraft:is_forest`) or a list of ids
 * @param dimensions    dimension allow-list, e.g. `["minecraft:the_nether"]`
 * @param minY          lowest block Y this genus may spawn at
 * @param maxY          highest block Y this genus may spawn at
 * @param maxLight      only spawn at or below this light level (darkness — caves, night)
 * @param minLight      only spawn at or above this light level
 * @param reasons       which spawn reasons count; absent means natural spawns only
 * @param requireSeeSky only outdoors, or (inverted) only underground/indoors
 */
public record SpawnRules(
        Optional<HolderSet<Biome>> biomes,
        Optional<List<ResourceKey<Level>>> dimensions,
        Optional<Integer> minY,
        Optional<Integer> maxY,
        Optional<Integer> maxLight,
        Optional<Integer> minLight,
        Optional<List<EntitySpawnReason>> reasons,
        Optional<Boolean> requireSeeSky) {

    /** No restrictions — spawns wherever its base mob would. */
    public static final SpawnRules ANY = new SpawnRules(Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty());

    private static final Codec<ResourceKey<Level>> DIMENSION_CODEC =
            Identifier.CODEC.xmap(id -> ResourceKey.create(Registries.DIMENSION, id), ResourceKey::identifier);

    private static final Codec<EntitySpawnReason> REASON_CODEC =
            Codec.STRING.xmap(s -> EntitySpawnReason.valueOf(s.toUpperCase(java.util.Locale.ROOT)),
                    r -> r.name().toLowerCase(java.util.Locale.ROOT));

    public static final Codec<SpawnRules> CODEC = RecordCodecBuilder.create(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.BIOME).optionalFieldOf("biomes").forGetter(SpawnRules::biomes),
            DIMENSION_CODEC.listOf().optionalFieldOf("dimensions").forGetter(SpawnRules::dimensions),
            Codec.INT.optionalFieldOf("min_y").forGetter(SpawnRules::minY),
            Codec.INT.optionalFieldOf("max_y").forGetter(SpawnRules::maxY),
            Codec.INT.optionalFieldOf("max_light").forGetter(SpawnRules::maxLight),
            Codec.INT.optionalFieldOf("min_light").forGetter(SpawnRules::minLight),
            REASON_CODEC.listOf().optionalFieldOf("reasons").forGetter(SpawnRules::reasons),
            Codec.BOOL.optionalFieldOf("require_see_sky").forGetter(SpawnRules::requireSeeSky))
            .apply(i, SpawnRules::new));

    /**
     * Spawn reasons a genus is eligible for when it doesn't say otherwise.
     *
     * <p>Deliberately narrow. A genus riding {@code CONVERSION} would replace drowning zombies and
     * cured villagers; riding {@code REINFORCEMENT} would let a horde of cowards summon itself. Both
     * are surprising defaults, so a genus that wants them has to ask.
     */
    private static final List<EntitySpawnReason> DEFAULT_REASONS =
            List.of(EntitySpawnReason.NATURAL, EntitySpawnReason.CHUNK_GENERATION, EntitySpawnReason.SPAWNER);

    public boolean allows(LevelReader level, BlockPos pos, EntitySpawnReason reason) {
        if (!reasons.orElse(DEFAULT_REASONS).contains(reason)) {
            return false;
        }
        if (minY.isPresent() && pos.getY() < minY.get()) {
            return false;
        }
        if (maxY.isPresent() && pos.getY() > maxY.get()) {
            return false;
        }
        if (biomes.isPresent()) {
            Holder<Biome> biome = level.getBiome(pos);
            if (!biomes.get().contains(biome)) {
                return false;
            }
        }
        if (maxLight.isPresent() && level.getMaxLocalRawBrightness(pos) > maxLight.get()) {
            return false;
        }
        if (minLight.isPresent() && level.getMaxLocalRawBrightness(pos) < minLight.get()) {
            return false;
        }
        if (requireSeeSky.isPresent() && level.canSeeSky(pos) != requireSeeSky.get()) {
            return false;
        }
        return true;
    }

    /** Dimension is checked separately because it comes from the level, not the position. */
    public boolean allowsDimension(ResourceKey<Level> dimension) {
        return dimensions.map(list -> list.contains(dimension)).orElse(true);
    }
}
