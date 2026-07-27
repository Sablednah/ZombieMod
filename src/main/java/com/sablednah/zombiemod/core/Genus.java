package com.sablednah.zombiemod.core;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sablednah.zombiemod.core.goal.GoalSpec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

/**
 * One zombie type, as defined by a datapack file.
 *
 * <p>The 1.8 plugin called these "genera" and read them from {@code plugins/ZombieMod/genera/*.yml};
 * the modern equivalent is a datapack registry, so they live in
 * {@code data/<pack>/zombiemod/genus/<name>.json}, sync to clients, and reload with {@code /reload}.
 *
 * <p>Note what is <em>not</em> here: any notion of a custom entity type. A genus decorates an
 * instance of a <em>vanilla</em> mob — that is what lets a plain vanilla client see it.
 *
 * @param name          display name shown above the mob; absent means no name tag
 * @param base          which vanilla mob to dress up (zombie, husk, drowned, …)
 * @param weight        relative spawn frequency against other genera for the same base mob;
 *                      0 means "never spawn naturally, command/spawner only"
 * @param health        max health, or absent to keep the vanilla value
 * @param damage        attack damage, or absent to keep the vanilla value
 * @param speed         movement speed <em>multiplier</em> on the vanilla base, as in the old configs
 * @param followRange   how far it notices you (the old {@code agro} field)
 * @param scale         body size multiplier — a real synced attribute since 1.20.5, so giants and
 *                      tiddlers cost nothing and need no client mod
 * @param armorColor    dyed-leather colour, an RGB int; the cheapest way to tell genera apart on a
 *                      vanilla client
 * @param clearGoals    wipe the vanilla AI before adding ours (what the 1.8 code did by reflection)
 * @param goals         behaviours, added to the mob's goalSelector
 * @param targetGoals   who to pick a fight with, added to the targetSelector
 */
public record Genus(
        Optional<String> name,
        EntityType<?> base,
        int weight,
        Optional<Double> health,
        Optional<Double> damage,
        double speed,
        Optional<Double> followRange,
        double scale,
        Optional<Integer> armorColor,
        boolean clearGoals,
        List<GoalSpec> goals,
        List<GoalSpec> targetGoals) {

    public static final Codec<Genus> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("name").forGetter(Genus::name),
            BuiltInRegistries.ENTITY_TYPE.byNameCodec()
                    .optionalFieldOf("base", (EntityType<?>) EntityType.ZOMBIE).forGetter(Genus::base),
            Codec.INT.optionalFieldOf("weight", 0).forGetter(Genus::weight),
            Codec.DOUBLE.optionalFieldOf("health").forGetter(Genus::health),
            Codec.DOUBLE.optionalFieldOf("damage").forGetter(Genus::damage),
            Codec.DOUBLE.optionalFieldOf("speed", 1.0D).forGetter(Genus::speed),
            Codec.DOUBLE.optionalFieldOf("follow_range").forGetter(Genus::followRange),
            Codec.DOUBLE.optionalFieldOf("scale", 1.0D).forGetter(Genus::scale),
            Codec.INT.optionalFieldOf("armor_color").forGetter(Genus::armorColor),
            Codec.BOOL.optionalFieldOf("clear_goals", true).forGetter(Genus::clearGoals),
            GoalSpec.CODEC.listOf().optionalFieldOf("goals", List.of()).forGetter(Genus::goals),
            GoalSpec.CODEC.listOf().optionalFieldOf("target_goals", List.of()).forGetter(Genus::targetGoals))
            .apply(i, Genus::new));

    /** Display name as a component, or empty if this genus goes unnamed. */
    public Optional<Component> displayName() {
        return name.map(Component::literal);
    }
}
