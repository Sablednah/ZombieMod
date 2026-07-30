package com.sablednah.zombiemod.core.mutate;

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
 * The lookup {@link MutationTrigger#CODEC} dispatches through.
 *
 * <p>{@code register} is public for the same reason it is on the spawn conditions: the interesting
 * triggers may live outside this mod, and an optional integration must be able to contribute one
 * without ZombieMod taking a dependency on it.
 */
public final class MutationTriggerTypes {

    private static final Map<Identifier, MapCodec<? extends MutationTrigger>> TYPES = new LinkedHashMap<>();

    static {
        register(MutationTriggers.HealthBelow.TYPE, MutationTriggers.HealthBelow.CODEC);
        register(MutationTriggers.InWater.TYPE, MutationTriggers.InWater.CODEC);
        register(MutationTriggers.InLava.TYPE, MutationTriggers.InLava.CODEC);
        register(MutationTriggers.OnFire.TYPE, MutationTriggers.OnFire.CODEC);
        register(MutationTriggers.Touching.TYPE, MutationTriggers.Touching.CODEC);
        register(MutationTriggers.Where.TYPE, MutationTriggers.Where.CODEC);
        register(MutationTriggers.AllOf.TYPE, MutationTriggers.AllOf.CODEC);
        register(MutationTriggers.AnyOf.TYPE, MutationTriggers.AnyOf.CODEC);
        register(MutationTriggers.Not.TYPE, MutationTriggers.Not.CODEC);
    }

    /** Add a trigger type. Safe to call from another mod during construction. */
    public static void register(Identifier id, MapCodec<? extends MutationTrigger> codec) {
        TYPES.put(id, codec);
    }

    public static MapCodec<? extends MutationTrigger> codecOf(Identifier id) {
        MapCodec<? extends MutationTrigger> codec = TYPES.get(id);
        return codec != null ? codec : new UnknownType(id);
    }

    public static Set<Identifier> known() {
        return TYPES.keySet();
    }

    /** Fails both ways, naming the offending type and listing the valid ones. */
    private static final class UnknownType extends MapCodec<MutationTrigger> {

        private final Identifier id;

        UnknownType(Identifier id) {
            this.id = id;
        }

        private DataResult<MutationTrigger> fail() {
            return DataResult.error(
                    () -> "Unknown ZombieMod mutation trigger '" + id + "'; known types are " + TYPES.keySet());
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.empty();
        }

        @Override
        public <T> DataResult<MutationTrigger> decode(DynamicOps<T> ops, MapLike<T> input) {
            return fail();
        }

        @Override
        public <T> RecordBuilder<T> encode(MutationTrigger input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return prefix.withErrorsFrom(fail());
        }
    }

    private MutationTriggerTypes() {}
}
