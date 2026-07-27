package com.sablednah.zombiemod.core.goal;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/**
 * Vanilla's targeting goals are typed on a {@code Class<? extends LivingEntity>}, not on an
 * {@code EntityType}, so a genus names its quarry from this curated set rather than by entity id.
 *
 * <p>That is a real limitation and worth stating plainly: you can say "avoid wolves" but not yet
 * "avoid a modded entity". Broad categories (player, monster, animal, villager) cover the classic
 * zombie-movie behaviours, which is what this is for. Adding an entry is one line.
 */
public final class TargetClass {

    private static final Map<String, Class<? extends LivingEntity>> BY_NAME = new LinkedHashMap<>();

    static {
        register("player", Player.class);
        register("living", LivingEntity.class);
        register("mob", Mob.class);
        register("monster", Monster.class);
        register("animal", Animal.class);
        register("villager", AbstractVillager.class);
        register("zombie", Zombie.class);
        // The pets a coward runs from, and a hunter hunts — the 1.8 mod's HUNTER ability targeted
        // exactly these two.
        register("wolf", Wolf.class);
        register("ocelot", Ocelot.class);
        register("cat", Cat.class);
    }

    /**
     * Codec over the names above. Unknown names fail the genus's JSON with a readable error rather
     * than silently producing a zombie that ignores everything.
     */
    public static final Codec<Class<? extends LivingEntity>> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                Class<? extends LivingEntity> found = BY_NAME.get(name);
                return found != null
                        ? com.mojang.serialization.DataResult.success(found)
                        : com.mojang.serialization.DataResult.error(
                                () -> "Unknown target '" + name + "'; expected one of " + BY_NAME.keySet());
            },
            TargetClass::nameOf);

    private static void register(String name, Class<? extends LivingEntity> type) {
        BY_NAME.put(name, type);
    }

    private static String nameOf(Class<? extends LivingEntity> type) {
        for (Map.Entry<String, Class<? extends LivingEntity>> e : BY_NAME.entrySet()) {
            if (e.getValue() == type) {
                return e.getKey();
            }
        }
        return "living";
    }

    private TargetClass() {}
}
