package com.sablednah.zombiemod.neoforge;

import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.compat.CityWorld;
import com.sablednah.zombiemod.core.Genus;
import com.sablednah.zombiemod.core.HordeSpec;
import com.sablednah.zombiemod.core.spawn.SpawnConditions;

import org.slf4j.Logger;

import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;

/**
 * Warns at server start about a {@code city_lot} or {@code city_district} class CityWorld does not
 * have.
 *
 * <p>Such a name is not an error - it parses, and the condition simply never holds - so the genus it
 * guards never spawns there and nothing says why. That is how ZARP's patient spent a day missing from
 * every hospital: {@code Hospital} for {@code HospitalLot}. The suffix is optional now, which fixes
 * that particular spelling; this catches the rest.
 *
 * <p>Walks each genus and horde <em>as JSON</em>, re-encoded through its own codec, rather than
 * following the records. Conditions turn up in spawn rules, placed weights, behaviours, mutation
 * triggers and horde gates, nested under {@code any_of} and {@code not}, and a walk that knows those
 * shapes would quietly miss the next one somebody adds. The JSON has every condition in it whatever
 * holds it.
 *
 * <p>Only with CityWorld installed. Without it every {@code city_*} condition is false by design,
 * and there is no list to check a name against.
 */
final class CityClassCheck {

    private static final Logger LOG = LogUtils.getLogger();

    private CityClassCheck() {}

    static void run(MinecraftServer server) {
        if (!CityWorld.available() || CityWorld.lotClasses().isEmpty()) {
            return;
        }
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        check(server, ops, ZombieModRegistries.GENUS, Genus.CODEC, "genus");
        check(server, ops, ZombieModRegistries.HORDE, HordeSpec.CODEC, "horde");
    }

    private static <T> void check(MinecraftServer server, RegistryOps<JsonElement> ops,
            ResourceKey<Registry<T>> key, Codec<T> codec, String kind) {
        server.registryAccess().lookupOrThrow(key).listElements().forEach(holder ->
                codec.encodeStart(ops, holder.value()).result().ifPresent(json ->
                        walk(json, kind + " " + holder.key().identifier())));
    }

    private static void walk(JsonElement json, String where) {
        if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(e -> walk(e, where));
            return;
        }
        if (!json.isJsonObject()) {
            return;
        }
        JsonObject object = json.getAsJsonObject();
        JsonElement type = object.get("type");
        if (type != null && type.isJsonPrimitive()) {
            switch (type.getAsString()) {
                case "zombiemod:city_lot" ->
                        names(object, where, "city_lot", CityWorld.lotClasses(), SpawnConditions.LOT_SUFFIX);
                case "zombiemod:city_district" ->
                        names(object, where, "city_district", CityWorld.contextClasses(),
                                SpawnConditions.CONTEXT_SUFFIX);
                default -> { }
            }
        }
        object.entrySet().forEach(e -> walk(e.getValue(), where));
    }

    private static void names(JsonObject condition, String where, String type, Set<String> known, String suffix) {
        JsonElement classes = condition.get("classes");
        if (classes == null || !classes.isJsonArray()) {
            return;
        }
        for (JsonElement e : classes.getAsJsonArray()) {
            String wanted = e.getAsString();
            if (known.stream().noneMatch(k -> SpawnConditions.classMatches(wanted, k, suffix))) {
                String near = known.stream()
                        .filter(k -> k.regionMatches(true, 0, wanted, 0, Math.min(4, wanted.length())))
                        .sorted().limit(5).toList().toString();
                LOG.warn("ZombieMod: {} - {} class \"{}\" is not one CityWorld has, so it will never match.{}",
                        where, type, wanted, near.equals("[]") ? "" : " Close: " + near);
            }
        }
    }
}
