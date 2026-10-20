package com.sablednah.zombiemod.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.sablednah.zombiemod.net.MobDropsPayload;

import net.minecraft.resources.Identifier;

/**
 * The mob drops the server has let this player see. Client-only, reached from an
 * {@code enqueueWork} lambda, so a dedicated server never loads it.
 *
 * <p>Knows nothing about JEI. The JEI plugin listens here, so without JEI this is a small map that
 * nothing reads, and the JEI classes are never touched.
 */
public final class MobDropsState {

    /**
     * What changed: the entries now gone (or replaced) and the entries now present (or new).
     * A full set from the server is both at once.
     */
    public record Change(Collection<MobDropsPayload.Entry> removed, Collection<MobDropsPayload.Entry> added) {}

    private static final Map<Identifier, MobDropsPayload.Entry> ENTRIES = new LinkedHashMap<>();
    private static Consumer<Change> listener;

    private MobDropsState() {}

    public static void accept(MobDropsPayload payload) {
        List<MobDropsPayload.Entry> removed = new ArrayList<>();
        List<MobDropsPayload.Entry> added = new ArrayList<>();
        if (payload.replace()) {
            Map<Identifier, MobDropsPayload.Entry> next = new LinkedHashMap<>();
            payload.entries().forEach(e -> next.put(e.key(), e));
            ENTRIES.forEach((key, old) -> {
                if (!old.equals(next.get(key))) {
                    removed.add(old);
                }
            });
            next.forEach((key, entry) -> {
                if (!entry.equals(ENTRIES.get(key))) {
                    added.add(entry);
                }
            });
            ENTRIES.clear();
            ENTRIES.putAll(next);
        } else {
            for (MobDropsPayload.Entry entry : payload.entries()) {
                MobDropsPayload.Entry old = ENTRIES.put(entry.key(), entry);
                if (entry.equals(old)) {
                    continue;
                }
                if (old != null) {
                    removed.add(old);
                }
                added.add(entry);
            }
        }
        notify(new Change(removed, added));
    }

    /** Forget it on disconnect, or the last server's drops show in the next server's JEI. */
    public static void clear() {
        List<MobDropsPayload.Entry> removed = new ArrayList<>(ENTRIES.values());
        ENTRIES.clear();
        notify(new Change(removed, List.of()));
    }

    /** Everything held now, genera first: they are what someone opening this is usually after. */
    public static List<MobDropsPayload.Entry> entries() {
        List<MobDropsPayload.Entry> out = new ArrayList<>();
        ENTRIES.values().stream().filter(e -> e.genus().isPresent()).forEach(out::add);
        ENTRIES.values().stream().filter(e -> e.genus().isEmpty()).forEach(out::add);
        return out;
    }

    /** One listener: the JEI plugin, when JEI is present. */
    public static void listen(Consumer<Change> l) {
        listener = l;
    }

    private static void notify(Change change) {
        if (listener != null && (!change.removed().isEmpty() || !change.added().isEmpty())) {
            listener.accept(change);
        }
    }
}
