package com.sablednah.zombiemod.core.goal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * A goal described in JSON, ready to be turned into a real {@link Goal} for one mob.
 *
 * <p>This is the heart of ZombieMod: the original 1.8 plugin's party trick was reaching into
 * Minecraft's own pathfinder goals and recombining them — {@code PathfinderGoalAvoidTarget} to make
 * a coward, {@code PathfinderGoalLookAtPlayer} to make something that just stares at you, the
 * spider's navigator bolted onto a zombie. Back then that meant reflection against obfuscated
 * names. Here the goals are ordinary public classes, so a genus can simply name the ones it wants
 * and we build them per instance.
 *
 * <p>Specs are stateless and shared between every mob of a genus; {@link #build} is called once per
 * spawned entity. Returning {@code null} means "not applicable to this mob" (for example a goal
 * that needs a {@code PathfinderMob} being asked for on something else) — the applier skips it
 * rather than failing the whole genus.
 */
public interface GoalSpec {

    /**
     * Dispatches on the {@code "type"} field, exactly like vanilla's own JSON-defined objects.
     * Registered types live in {@link GoalSpecTypes}.
     */
    Codec<GoalSpec> CODEC = Identifier.CODEC.dispatch("type", GoalSpec::type, GoalSpecTypes::codecOf);

    /** Registered id of this spec's type, e.g. {@code zombiemod:avoid_entity}. */
    Identifier type();

    /**
     * Lower runs first, matching vanilla's goal priorities. A genus's JSON gives this per goal so
     * authors keep the same control the hand-written 1.8 code had.
     */
    int priority();

    /**
     * @return the live goal for this mob, or {@code null} if this spec cannot apply to it.
     */
    Goal build(Mob mob);

    /** Convenience for implementations registering themselves. */
    static MapCodec<? extends GoalSpec> unit(GoalSpec instance) {
        return MapCodec.unit(instance);
    }
}
