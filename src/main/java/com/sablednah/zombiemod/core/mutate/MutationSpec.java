package com.sablednah.zombiemod.core.mutate;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sablednah.zombiemod.core.ability.Abilities.Particles;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Become something else.
 *
 * <p>Everything else a genus can do describes what a monster <em>is</em>. This is the one that lets
 * it stop being that — so "kill it before it turns" applies to the monsters as well as to the
 * player, and so a place can change what walks around in it: the same zombie is a harder thing in
 * the Nether than it was in the overworld.
 *
 * @param into       the genus to become
 * @param when       what has to be true
 * @param sustained  ticks the trigger must hold <em>continuously</em> first
 * @param chance     rolled once the trigger has held long enough
 * @param keepHealth carry the current health across as a fraction rather than arriving full
 * @param sound      played where it happens
 * @param particle   burst where it happens; a mutation the player cannot see is a bug report
 */
public record MutationSpec(
        Identifier into,
        MutationTrigger when,
        int sustained,
        float chance,
        boolean keepHealth,
        Optional<Holder<SoundEvent>> sound,
        ParticleOptions particle) {

    public static final Codec<MutationSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("into").forGetter(MutationSpec::into),
            MutationTrigger.CODEC.fieldOf("when").forGetter(MutationSpec::when),
            // Defaults to a full second held rather than 0, because the triggers people reach for
            // first are the twitchy ones. A zombie crossing a stream is in water for three ticks,
            // and a mutation that fires on that reads as a bug even when it is doing exactly what
            // the JSON asked for.
            Codec.INT.optionalFieldOf("for", 20).forGetter(MutationSpec::sustained),
            Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(MutationSpec::chance),
            Codec.BOOL.optionalFieldOf("keep_health", true).forGetter(MutationSpec::keepHealth),
            BuiltInRegistries.SOUND_EVENT.holderByNameCodec().optionalFieldOf("sound")
                    .forGetter(MutationSpec::sound),
            Particles.PARTICLE_CODEC.optionalFieldOf("particle", ParticleTypes.LARGE_SMOKE)
                    .forGetter(MutationSpec::particle))
            .apply(i, MutationSpec::new));
}
