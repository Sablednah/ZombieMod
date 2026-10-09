package com.sablednah.zombiemod.core;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sablednah.zombiemod.core.ability.Abilities;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

/**
 * What only a dragon needs: the Zombie Dragon's fake death, its voice and its breath.
 *
 * <p>Read by {@code neoforge/ZombieDragon} for whichever genus {@code zombieDragon.genus} names.
 * Everything else about the Zombie Dragon - health, armour, the minion trickle (its
 * {@code abilities}) and the waves (its {@code phases[].on_enter}) - is an ordinary genus field.
 *
 * @param interlude ticks the dragon lies dead before it rises
 * @param sounds    optional sound ids by cue: {@code death_fake}, {@code rise}, {@code roar},
 *                  {@code growl}, {@code breath}. Ids, not registry holders: ours are deliberately
 *                  unregistered (see {@code ZombieDragon.cueAt}), and a datapack may name another
 *                  mod's. A listener without the sound hears the vanilla dragon's where there is one
 * @param breath    what its breath clouds become; absent leaves them vanilla
 */
public record DragonSpec(int interlude, Map<String, Identifier> sounds, Optional<Breath> breath) {

    public static final Codec<DragonSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("interlude", 160).forGetter(DragonSpec::interlude),
            Codec.unboundedMap(Codec.STRING, Identifier.CODEC).optionalFieldOf("sounds", Map.of())
                    .forGetter(DragonSpec::sounds),
            Breath.CODEC.optionalFieldOf("breath").forGetter(DragonSpec::breath))
            .apply(i, DragonSpec::new));

    /**
     * A breath cloud's replacement contents.
     *
     * @param particle what the cloud is drawn with - a plain particle id is enough
     * @param effects  replaces the cloud's own effects (vanilla's is instant damage)
     * @param infect   anything standing in it may catch ZombieMod's infection
     */
    public record Breath(ParticleOptions particle, List<Effect> effects, Optional<Infection> infect) {

        public static final Codec<Breath> CODEC = RecordCodecBuilder.create(i -> i.group(
                Abilities.Particles.PARTICLE_CODEC.optionalFieldOf("particle", ParticleTypes.ITEM_SLIME)
                        .forGetter(Breath::particle),
                Effect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Breath::effects),
                Infection.CODEC.optionalFieldOf("infect").forGetter(Breath::infect))
                .apply(i, Breath::new));
    }

    public record Effect(Holder<MobEffect> effect, int duration, int amplifier) {

        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(Effect::effect),
                Codec.INT.optionalFieldOf("duration", 100).forGetter(Effect::duration),
                Codec.INT.optionalFieldOf("amplifier", 0).forGetter(Effect::amplifier))
                .apply(i, Effect::new));
    }

    /**
     * The same infection a bite gives - see {@code ability/Infect}, including that milk cures it.
     *
     * @param chance   per check (twice a second) for each thing standing in the cloud
     * @param duration how long until it turns, in ticks
     * @param effect   the visible marker
     * @param genus    what it rises as; absent rolls the weighted table
     */
    public record Infection(float chance, int duration, Holder<MobEffect> effect, Optional<Identifier> genus) {

        public static final Codec<Infection> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("chance", 0.25F).forGetter(Infection::chance),
                Codec.INT.optionalFieldOf("duration", 2400).forGetter(Infection::duration),
                BuiltInRegistries.MOB_EFFECT.holderByNameCodec()
                        .optionalFieldOf("effect", BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffects.HUNGER.value()))
                        .forGetter(Infection::effect),
                Identifier.CODEC.optionalFieldOf("genus").forGetter(Infection::genus))
                .apply(i, Infection::new));
    }
}
