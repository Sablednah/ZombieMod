package com.sablednah.zombiemod.core.ability;

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
 * The lookup {@link Ability#CODEC} dispatches through. {@link #register} is public so other mods can
 * contribute ability types, same as spawn conditions.
 */
public final class AbilityTypes {

    private static final Map<Identifier, MapCodec<? extends Ability>> TYPES = new LinkedHashMap<>();

    static {
        register(Abilities.Effect.TYPE, Abilities.Effect.CODEC);
        register(Abilities.Heal.TYPE, Abilities.Heal.CODEC);
        register(Abilities.Lightning.TYPE, Abilities.Lightning.CODEC);
        register(Abilities.Explode.TYPE, Abilities.Explode.CODEC);
        register(Abilities.Fuse.TYPE, Abilities.Fuse.CODEC);
        register(Abilities.Shockwave.TYPE, Abilities.Shockwave.CODEC);
        register(Abilities.Leap.TYPE, Abilities.Leap.CODEC);
        register(Abilities.Summon.TYPE, Abilities.Summon.CODEC);
        register(Abilities.Pull.TYPE, Abilities.Pull.CODEC);
        register(Abilities.Alert.TYPE, Abilities.Alert.CODEC);
        register(Abilities.Teleport.TYPE, Abilities.Teleport.CODEC);
        register(Abilities.BreakBlocks.TYPE, Abilities.BreakBlocks.CODEC);
        register(Abilities.Projectile.TYPE, Abilities.Projectile.CODEC);
        register(Abilities.PlaceBlock.TYPE, Abilities.PlaceBlock.CODEC);
        register(Abilities.Adapt.TYPE, Abilities.Adapt.CODEC);
        register(Abilities.Beam.TYPE, Abilities.Beam.CODEC);
        register(Abilities.Ray.TYPE, Abilities.Ray.CODEC);
        register(Abilities.Particles.TYPE, Abilities.Particles.CODEC);
        register(Abilities.Sound.TYPE, Abilities.Sound.CODEC);
    }

    public static void register(Identifier id, MapCodec<? extends Ability> codec) {
        TYPES.put(id, codec);
    }

    public static MapCodec<? extends Ability> codecOf(Identifier id) {
        MapCodec<? extends Ability> codec = TYPES.get(id);
        return codec != null ? codec : new UnknownType(id);
    }

    public static Set<Identifier> known() {
        return TYPES.keySet();
    }

    private static final class UnknownType extends MapCodec<Ability> {

        private final Identifier id;

        UnknownType(Identifier id) {
            this.id = id;
        }

        private DataResult<Ability> fail() {
            return DataResult.error(
                    () -> "Unknown ZombieMod ability '" + id + "'; known types are " + TYPES.keySet());
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.empty();
        }

        @Override
        public <T> DataResult<Ability> decode(DynamicOps<T> ops, MapLike<T> input) {
            return fail();
        }

        @Override
        public <T> RecordBuilder<T> encode(Ability input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return prefix.withErrorsFrom(fail());
        }
    }

    private AbilityTypes() {}
}
