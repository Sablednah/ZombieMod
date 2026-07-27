package com.sablednah.zombiemod.core.spawn;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;

/** The spawn conditions ZombieMod ships. Adding one is a record plus a line in {@link SpawnConditionTypes}. */
public final class SpawnConditions {

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("zombiemod", path);
    }

    /** Biome allow-list. Takes a tag (`#minecraft:is_forest`) or a list of ids. */
    public record InBiome(HolderSet<Biome> biomes) implements SpawnCondition {

        public static final Identifier TYPE = id("biome");

        public static final MapCodec<InBiome> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(InBiome::biomes))
                .apply(i, InBiome::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(LevelReader level, BlockPos pos, ResourceKey<Level> dimension, EntitySpawnReason reason) {
            return biomes.contains(level.getBiome(pos));
        }
    }

    /** Dimension allow-list. */
    public record InDimension(List<ResourceKey<Level>> dimensions) implements SpawnCondition {

        public static final Identifier TYPE = id("dimension");

        private static final Codec<ResourceKey<Level>> KEY_CODEC =
                Identifier.CODEC.xmap(i -> ResourceKey.create(Registries.DIMENSION, i), ResourceKey::identifier);

        public static final MapCodec<InDimension> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                KEY_CODEC.listOf().fieldOf("dimensions").forGetter(InDimension::dimensions))
                .apply(i, InDimension::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(LevelReader level, BlockPos pos, ResourceKey<Level> dimension, EntitySpawnReason reason) {
            return dimensions.contains(dimension);
        }
    }

    /** Height band. Either bound may be omitted. */
    public record Height(Optional<Integer> min, Optional<Integer> max) implements SpawnCondition {

        public static final Identifier TYPE = id("height");

        public static final MapCodec<Height> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("min").forGetter(Height::min),
                Codec.INT.optionalFieldOf("max").forGetter(Height::max))
                .apply(i, Height::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(LevelReader level, BlockPos pos, ResourceKey<Level> dimension, EntitySpawnReason reason) {
            int y = pos.getY();
            return min.map(m -> y >= m).orElse(true) && max.map(m -> y <= m).orElse(true);
        }
    }

    /**
     * Light level at the spawn point.
     *
     * <p>Reads the effective local brightness, so it follows the day/night cycle outdoors — a
     * {@code max} of 7 means "dark", whether that is a cave or midnight in a field.
     */
    public record Light(Optional<Integer> min, Optional<Integer> max) implements SpawnCondition {

        public static final Identifier TYPE = id("light");

        public static final MapCodec<Light> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("min").forGetter(Light::min),
                Codec.INT.optionalFieldOf("max").forGetter(Light::max))
                .apply(i, Light::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(LevelReader level, BlockPos pos, ResourceKey<Level> dimension, EntitySpawnReason reason) {
            int light = level.getMaxLocalRawBrightness(pos);
            return min.map(m -> light >= m).orElse(true) && max.map(m -> light <= m).orElse(true);
        }
    }

    /** Open sky above, or deliberately not. */
    public record SeeSky(boolean value) implements SpawnCondition {

        public static final Identifier TYPE = id("see_sky");

        public static final MapCodec<SeeSky> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.BOOL.optionalFieldOf("value", true).forGetter(SeeSky::value))
                .apply(i, SeeSky::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(LevelReader level, BlockPos pos, ResourceKey<Level> dimension, EntitySpawnReason reason) {
            return level.canSeeSky(pos) == value;
        }
    }

    /**
     * Passes when any nested condition passes.
     *
     * <p>The conditions on a genus are ANDed, which is the useful default; this is how you say
     * "swamps or mangroves" without needing a second genus.
     */
    public record AnyOf(List<SpawnCondition> conditions) implements SpawnCondition {

        public static final Identifier TYPE = id("any_of");

        public static final MapCodec<AnyOf> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                SpawnCondition.CODEC.listOf().fieldOf("conditions").forGetter(AnyOf::conditions))
                .apply(i, AnyOf::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(LevelReader level, BlockPos pos, ResourceKey<Level> dimension, EntitySpawnReason reason) {
            for (SpawnCondition c : conditions) {
                if (c.test(level, pos, dimension, reason)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Inverts a condition — "anywhere but the Nether", "not in daylight". */
    public record Not(SpawnCondition condition) implements SpawnCondition {

        public static final Identifier TYPE = id("not");

        public static final MapCodec<Not> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                SpawnCondition.CODEC.fieldOf("condition").forGetter(Not::condition))
                .apply(i, Not::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(LevelReader level, BlockPos pos, ResourceKey<Level> dimension, EntitySpawnReason reason) {
            return !condition.test(level, pos, dimension, reason);
        }
    }

    private SpawnConditions() {}
}
