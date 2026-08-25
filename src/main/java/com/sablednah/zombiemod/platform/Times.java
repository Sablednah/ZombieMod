package com.sablednah.zombiemod.platform;

import net.minecraft.world.level.Level;

/**
 * The world clock, for the spawn conditions that care what time it is.
 *
 * <p><b>Why this exists.</b> {@code Level.getDayTime()} is {@code getOverworldClockTime()} from 26.1
 * onward. One call site — the {@code time} spawn condition — but it decides whether every
 * day/night-gated genus may spawn, so it is not a small thing to get wrong.
 */
public final class Times {

    private Times() {}

    /**
     * Ticks since the world began, un-modulo'd; callers take {@code % 24000} themselves.
     *
     * <p><b>Differs per version.</b> On 26.1+ this becomes {@code level.getOverworldClockTime()}.
     */
    public static long dayTime(Level level) {
        return level.getDayTime();
    }
}
