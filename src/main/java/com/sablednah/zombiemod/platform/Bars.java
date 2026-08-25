package com.sablednah.zombiemod.platform;

import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;

/**
 * Making a boss bar.
 *
 * <p><b>Why this exists.</b> {@code ServerBossEvent}'s constructor gained a leading {@link UUID}
 * parameter in 26.1. Two call sites — a boss genus spawning, and a horde starting — and it is the
 * one drift in this package that is a *signature* change rather than a rename, so the per-version
 * bodies differ by an argument rather than by a name.
 *
 * <p>The id matters more than it looks: a boss bar is addressed by it on the wire, so two bars
 * sharing one would be the same bar to a client. {@code randomUUID} per bar is the behaviour the old
 * constructor had internally.
 */
public final class Bars {

    private Bars() {}

    /**
     * A fresh server-side boss bar.
     *
     * <p><b>Differs per version.</b> On 26.1+ this becomes
     * {@code new ServerBossEvent(UUID.randomUUID(), name, colour, overlay)}.
     */
    public static ServerBossEvent create(Component name, BossEvent.BossBarColor colour,
            BossEvent.BossBarOverlay overlay) {
        return new ServerBossEvent(name, colour, overlay);
    }
}
