package com.sablednah.zombiemod.core.spawn;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import net.minecraft.resources.Identifier;

/**
 * The lookup {@link SpawnCondition#CODEC} dispatches through.
 *
 * <p>{@link #register} is public so an optional compatibility adapter — a CityWorld one, say — can
 * add its own condition types. Register during mod construction, before any datapack is read.
 */
public final class SpawnConditionTypes {

    private static final Map<Identifier, MapCodec<? extends SpawnCondition>> TYPES = new LinkedHashMap<>();

    static {
        register(SpawnConditions.InBiome.TYPE, SpawnConditions.InBiome.CODEC);
        register(SpawnConditions.InDimension.TYPE, SpawnConditions.InDimension.CODEC);
        register(SpawnConditions.Height.TYPE, SpawnConditions.Height.CODEC);
        register(SpawnConditions.Light.TYPE, SpawnConditions.Light.CODEC);
        register(SpawnConditions.SeeSky.TYPE, SpawnConditions.SeeSky.CODEC);
        register(SpawnConditions.Moon.TYPE, SpawnConditions.Moon.CODEC);
        register(SpawnConditions.Depth.TYPE, SpawnConditions.Depth.CODEC);
        register(SpawnConditions.TimeOfDay.TYPE, SpawnConditions.TimeOfDay.CODEC);
        register(SpawnConditions.OnDate.TYPE, SpawnConditions.OnDate.CODEC);
        register(SpawnConditions.InClaim.TYPE, SpawnConditions.InClaim.CODEC);
        register(SpawnConditions.CityDistrict.TYPE, SpawnConditions.CityDistrict.CODEC);
        register(SpawnConditions.CityLot.TYPE, SpawnConditions.CityLot.CODEC);
        register(SpawnConditions.CityNature.TYPE, SpawnConditions.CityNature.CODEC);
        register(SpawnConditions.AnyOf.TYPE, SpawnConditions.AnyOf.CODEC);
        register(SpawnConditions.Not.TYPE, SpawnConditions.Not.CODEC);
    }

    /** Add a condition type. Safe to call from another mod during construction. */
    public static void register(Identifier id, MapCodec<? extends SpawnCondition> codec) {
        TYPES.put(id, codec);
    }

    public static MapCodec<? extends SpawnCondition> codecOf(Identifier id) {
        MapCodec<? extends SpawnCondition> codec = TYPES.get(id);
        return codec != null ? codec : new UnknownType(id);
    }

    public static Set<Identifier> known() {
        return TYPES.keySet();
    }

    /** Fails both ways, naming the offending type and listing the valid ones. */
    private static final class UnknownType extends MapCodec<SpawnCondition> {

        private final Identifier id;

        UnknownType(Identifier id) {
            this.id = id;
        }

        private DataResult<SpawnCondition> fail() {
            return DataResult.error(
                    () -> "Unknown ZombieMod spawn condition '" + id + "'; known types are " + TYPES.keySet());
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.empty();
        }

        @Override
        public <T> DataResult<SpawnCondition> decode(DynamicOps<T> ops, MapLike<T> input) {
            return fail();
        }

        @Override
        public <T> RecordBuilder<T> encode(SpawnCondition input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return prefix.withErrorsFrom(fail());
        }
    }

    private SpawnConditionTypes() {}
}
