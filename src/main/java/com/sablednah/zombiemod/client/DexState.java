package com.sablednah.zombiemod.client;

import java.util.List;

import com.sablednah.zombiemod.net.DexPayload;

/**
 * The last dex the server sent us. Client-only, and only ever reached from inside an
 * {@code enqueueWork} lambda, so a dedicated server never loads this class.
 */
public final class DexState {

    private static volatile List<DexPayload.Entry> entries = List.of();

    private DexState() {}

    public static void accept(DexPayload payload) {
        entries = List.copyOf(payload.entries());
    }

    /**
     * Forget it on disconnect.
     *
     * <p>Otherwise the last server's roster is still here when you join the next one, and until that
     * server (if it even runs the mod) sends its own, the dex would show another world's zombies
     * with another world's kill counts.
     */
    public static void clear() {
        entries = List.of();
    }

    public static List<DexPayload.Entry> entries() {
        return entries;
    }

    public static int slain() {
        return (int) entries.stream().filter(e -> e.kills() > 0).count();
    }

    public static int met() {
        return (int) entries.stream().filter(DexPayload.Entry::met).count();
    }
}
