package com.sablednah.zombiemod.core.ability;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/**
 * Something a genus <em>does</em>, repeatedly, while it exists — as opposed to a goal, which decides
 * where it walks.
 *
 * <p>This is the modern shape of the 1.8 plugin's {@code Animations} class: one big per-tick sweep
 * over every tracked zombie, with an {@code intervals} counter and {@code % N} tests deciding which
 * behaviours fired this pass. Same idea, but each behaviour is now a separate declared thing with
 * its own timing, and it rides the entity's own goal ticking rather than a global scan (see
 * {@code AbilityGoal}).
 *
 * <p>Implementations must be stateless — one instance is shared by every mob of the genus, and
 * per-mob timing lives in the goal that drives it.
 */
public interface Ability {

    Codec<Ability> CODEC = Identifier.CODEC.dispatch("type", Ability::type, AbilityTypes::codecOf);

    /** Registered id, e.g. {@code zombiemod:lightning}. */
    Identifier type();

    /** How often to run, in ticks. 20 is once a second. */
    int interval();

    /** Probability of actually firing when the interval comes round, 0..1. */
    float chance();

    /**
     * Do the thing.
     *
     * @param level the server level; abilities never run client-side
     * @param mob   the mob performing it
     */
    void run(ServerLevel level, Mob mob);

    /**
     * Per-mob state, for abilities that build up over several ticks rather than firing and
     * finishing — a fuse burning down, a wind-up before a leap.
     *
     * <p>Returning {@code null} (the default) means stateless: {@link #run} is called on the
     * interval and that's the whole story. Returning a state means {@link State#tick} is called
     * instead, and the ability owns its own timing from there.
     *
     * <p>Note this is the one crack in "implementations are stateless" — deliberately, and confined
     * to the returned object. The {@code Ability} record itself stays shared and immutable; only the
     * {@code State} is per-mob, created once by {@code AbilityGoal}.
     */
    default State newState() {
        return null;
    }

    /**
     * React to being hurt, outside the interval schedule.
     *
     * <p>Some behaviours are answers rather than habits — "always blinks away when shot" is not a
     * thing that happens every N ticks, it happens when you shoot it. Abilities that don't care can
     * ignore this; the default does nothing.
     *
     * @return true if the ability acted, purely so callers can log or count
     */
    default boolean onHurt(ServerLevel level, Mob mob, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    /** Per-mob state for a stateful ability. */
    interface State {
        void tick(ServerLevel level, Mob mob);
    }
}
