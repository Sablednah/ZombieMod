package com.sablednah.zombiemod.core;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sablednah.zombiemod.core.goal.GoalSpec;
import com.sablednah.zombiemod.core.spawn.SpawnCondition;

/**
 * Goals that apply only while a condition holds.
 *
 * <p>This is what turns a monster into a world: the same genus docile at noon and hunting at
 * midnight, or placid in a village and vicious in the wilds. A genus's plain {@code goals} are
 * always active; each behaviour's goals switch on and off underneath them.
 *
 * <p>The condition is an ordinary {@link SpawnCondition}, so everything that gates spawning gates
 * behaviour too — time, light, biome, dimension, height, and whatever an optional adapter adds.
 * That reuse is why the condition interface only knows about places.
 *
 * @param when        when these goals apply
 * @param goals       added to the mob's goalSelector, gated on {@code when}
 * @param targetGoals added to the targetSelector, gated on {@code when}
 */
public record Behaviour(SpawnCondition when, List<GoalSpec> goals, List<GoalSpec> targetGoals) {

    public static final Codec<Behaviour> CODEC = RecordCodecBuilder.create(i -> i.group(
            SpawnCondition.CODEC.fieldOf("when").forGetter(Behaviour::when),
            GoalSpec.CODEC.listOf().optionalFieldOf("goals", List.of()).forGetter(Behaviour::goals),
            GoalSpec.CODEC.listOf().optionalFieldOf("target_goals", List.of()).forGetter(Behaviour::targetGoals))
            .apply(i, Behaviour::new));
}
