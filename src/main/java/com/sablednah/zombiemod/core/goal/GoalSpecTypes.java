package com.sablednah.zombiemod.core.goal;

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
 * The lookup {@link GoalSpec#CODEC} dispatches through.
 *
 * <p>A plain map rather than a NeoForge registry, deliberately: nothing outside this mod needs to
 * add goal types yet, and a map keeps the spike small. If third-party goal types ever matter this
 * becomes a {@code NewRegistryEvent} registry with no change to the JSON.
 */
public final class GoalSpecTypes {

    private static final Map<Identifier, MapCodec<? extends GoalSpec>> TYPES = new LinkedHashMap<>();

    static {
        register(GoalSpecs.AvoidEntity.TYPE, GoalSpecs.AvoidEntity.CODEC);
        register(GoalSpecs.RandomStroll.TYPE, GoalSpecs.RandomStroll.CODEC);
        register(GoalSpecs.RandomSwim.TYPE, GoalSpecs.RandomSwim.CODEC);
        register(GoalSpecs.Float_.TYPE, GoalSpecs.Float_.CODEC);
        register(GoalSpecs.LookAtPlayer.TYPE, GoalSpecs.LookAtPlayer.CODEC);
        register(GoalSpecs.RandomLook.TYPE, GoalSpecs.RandomLook.CODEC);
        register(GoalSpecs.MeleeAttack.TYPE, GoalSpecs.MeleeAttack.CODEC);
        register(GoalSpecs.BowAttack.TYPE, GoalSpecs.BowAttack.CODEC);
        register(GoalSpecs.SeekBlocks.TYPE, GoalSpecs.SeekBlocks.CODEC);
        register(GoalSpecs.SoundTarget.TYPE, GoalSpecs.SoundTarget.CODEC);
        register(GoalSpecs.NearestTarget.TYPE, GoalSpecs.NearestTarget.CODEC);
        register(GoalSpecs.HurtByTarget.TYPE, GoalSpecs.HurtByTarget.CODEC);
    }

    private static void register(Identifier id, MapCodec<? extends GoalSpec> codec) {
        TYPES.put(id, codec);
    }

    /**
     * @return the codec for a goal type. Never null: an unknown id yields a codec that fails with
     *         a message naming the id and listing what is available, which is what a datapack
     *         author needs to see in the log.
     */
    public static MapCodec<? extends GoalSpec> codecOf(Identifier id) {
        MapCodec<? extends GoalSpec> codec = TYPES.get(id);
        return codec != null ? codec : new UnknownType(id);
    }

    /**
     * Fails both ways with a message naming the offending type and listing the valid ones.
     *
     * <p>Spelled out rather than assembled from {@code MapCodec.unit(null).flatXmap(...)} because a
     * unit codec that decodes to null on the happy path would turn a typo in a datapack into a
     * later NullPointerException instead of a line in the log.
     */
    private static final class UnknownType extends MapCodec<GoalSpec> {

        private final Identifier id;

        UnknownType(Identifier id) {
            this.id = id;
        }

        private DataResult<GoalSpec> fail() {
            return DataResult.error(
                    () -> "Unknown ZombieMod goal type '" + id + "'; known types are " + TYPES.keySet());
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.empty();
        }

        @Override
        public <T> DataResult<GoalSpec> decode(DynamicOps<T> ops, MapLike<T> input) {
            return fail();
        }

        @Override
        public <T> RecordBuilder<T> encode(GoalSpec input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return prefix.withErrorsFrom(fail());
        }
    }

    public static Set<Identifier> known() {
        return TYPES.keySet();
    }

    private GoalSpecTypes() {}
}
