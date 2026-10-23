package com.sablednah.zombiemod.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sablednah.zombiemod.net.GenusSightingPayload;

import net.minecraft.resources.Identifier;

/**
 * Which genus each mob in view is, as the server told us. Client-only.
 *
 * <p>Keyed by entity id, which never repeats within a game session, so a mob that has gone needs no
 * clean-up beyond a disconnect: a stale id is never asked about again. Concurrent because a map mod
 * may read it from its own thread.
 */
public final class GenusSightings {

    private static final Map<Integer, Identifier> BY_ENTITY = new ConcurrentHashMap<>();

    private GenusSightings() {}

    public static void accept(GenusSightingPayload payload) {
        BY_ENTITY.put(payload.entityId(), payload.genus());
    }

    /** The genus of the mob with this id, or null for none - or one the server never mentioned. */
    public static Identifier of(int entityId) {
        return BY_ENTITY.get(entityId);
    }

    /** On disconnect: the next server's entity ids mean other mobs. */
    public static void clear() {
        BY_ENTITY.clear();
    }
}
