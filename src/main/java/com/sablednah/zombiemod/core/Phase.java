package com.sablednah.zombiemod.core;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sablednah.zombiemod.core.ability.Ability;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * "At half health it changes." A stage that opens up as a mob is worn down.
 *
 * <p>Abilities and attributes are handled differently on purpose. Abilities are <em>gated</em> on
 * the health threshold, so they switch off again if the mob heals — a boss with a regeneration
 * ability shouldn't keep its enrage phase after clawing back to full. Attributes are applied
 * <em>once</em> on entry and stay, because ramping an attribute up and down each time health
 * crosses a line would make a fight feel unstable rather than escalating.
 *
 * @param belowHealth fraction of max health at or under which this phase applies, 0–1
 * @param abilities   extra abilities while in this phase
 * @param attributes  attribute changes applied once, on entering
 * @param sound       played once, on entering
 * @param title       shown once as an action-bar line to players nearby, on entering
 */
public record Phase(
        double belowHealth,
        List<Ability> abilities,
        Map<Holder<Attribute>, Double> attributes,
        Optional<Holder<SoundEvent>> sound,
        Optional<String> title) {

    public static final Codec<Phase> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("below_health").forGetter(Phase::belowHealth),
            Ability.CODEC.listOf().optionalFieldOf("abilities", List.of()).forGetter(Phase::abilities),
            Codec.unboundedMap(BuiltInRegistries.ATTRIBUTE.holderByNameCodec(), Codec.DOUBLE)
                    .optionalFieldOf("attributes", Map.of()).forGetter(Phase::attributes),
            BuiltInRegistries.SOUND_EVENT.holderByNameCodec().optionalFieldOf("sound").forGetter(Phase::sound),
            Codec.STRING.optionalFieldOf("title").forGetter(Phase::title))
            .apply(i, Phase::new));
}
