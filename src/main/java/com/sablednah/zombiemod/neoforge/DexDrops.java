package com.sablednah.zombiemod.neoforge;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.sablednah.zombiemod.core.Genus;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * What a genus's loot table actually contains, as item ids.
 *
 * <p>For the ZombieDex's "leaves behind" section. Loot tables are server-only data — they do not
 * sync the way the genus registry does — so the server reads them and ships the answer, gated on the
 * player having killed one, because knowing the reward before the first kill is a spoiler rather
 * than a record.
 *
 * <p>Enumerated by encoding the table back to JSON and walking it for item entries, because
 * {@link LootTable} exposes no way to ask "what might you give me" — rolling it repeatedly is a
 * sampler, not an answer, and misses rare entries by construction. The walk is recursive, so
 * groups, alternatives and nested pools all surrender their items.
 */
public final class DexDrops {

    private DexDrops() {}

    /** Item ids the table can yield, in file order, deduplicated. Empty for no or unresolved table. */
    public static List<String> itemIds(MinecraftServer server, Genus genus) {
        if (genus.loot().isEmpty()) {
            return List.of();
        }
        LootTable table = server.reloadableRegistries().getLootTable(genus.loot().get().table());
        if (table == LootTable.EMPTY) {
            return List.of();
        }
        var ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        return LootTable.DIRECT_CODEC.encodeStart(ops, table).result()
                .map(json -> {
                    Set<String> items = new LinkedHashSet<>();
                    walk(json, items);
                    return List.copyOf(items);
                })
                .orElse(List.of());
    }

    private static void walk(JsonElement element, Set<String> into) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("type") && obj.has("name")
                    && obj.get("type").getAsString().endsWith("item")) {
                into.add(obj.get("name").getAsString());
            }
            for (var entry : obj.entrySet()) {
                walk(entry.getValue(), into);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                walk(child, into);
            }
        }
    }
}
