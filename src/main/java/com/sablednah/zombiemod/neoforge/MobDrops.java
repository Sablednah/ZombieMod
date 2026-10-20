package com.sablednah.zombiemod.neoforge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.sablednah.zombiemod.ZombieModConfig;
import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.core.Genus;
import com.sablednah.zombiemod.net.MobDropsPayload;
import com.sablednah.zombiemod.net.Net;

import org.slf4j.Logger;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * What every mob and every genus drops, for JEI's "Mob Drops" category, and who may see which.
 *
 * <p><b>Read from the tables, never rolled.</b> Rolling a table a few thousand times is a sampler,
 * and it misses rare drops by construction - the very ones someone opens JEI to find. So each table
 * is encoded back to JSON through its own codec, as {@link DexDrops} does, and walked: pools, rolls,
 * entry weights, {@code random_chance} conditions and {@code set_count}. The walk reads both loot
 * dialects - 26.3 renamed {@code functions}/{@code conditions} to {@code modifier}/{@code condition}
 * and typed its ranges - so this file is the same on every branch.
 *
 * <p><b>The chance is "at least one, per kill, before Looting", and it is an estimate</b> wherever a
 * table's own logic decides: a condition this does not model (on fire, killed by a frog) counts as
 * met, so the figure is the most generous honest one. A "killed by player" condition is met, because
 * this is a list of what you get for killing it.
 *
 * <p><b>Sent, not filtered.</b> With {@code killedOnly} the server never sends what a player has not
 * killed, so there is nothing on the client to reveal. Vanilla mobs go by the kill statistic, which
 * the game has always kept; genera go by the bestiary, and a genus the dex conceals is concealed here
 * too, killed or not - a server that keeps one as a rumour should not have it given away by JEI.
 */
public final class MobDrops {

    private static final Logger LOG = LogUtils.getLogger();

    /** Tables reached through {@code loot_table} entries, nested. Vanilla goes two deep; four is plenty. */
    private static final int MAX_DEPTH = 4;

    /** Built once per set of loot tables; {@code /reload} replaces the holder, so identity says when. */
    private static Object builtFor;
    private static Map<Identifier, MobDropsPayload.Entry> genera = Map.of();
    private static Map<Identifier, MobDropsPayload.Entry> mobs = Map.of();

    /** Keys already sent to each player since they joined, so a kill sends only what is new. */
    private static final Map<UUID, Set<Identifier>> SENT = new HashMap<>();

    // ------------------------------------------------------------------ events

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendAll(player);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    /** A {@code /reload}: tables may have changed, so everyone gets a fresh set. A join is {@link #onLogin}. */
    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            event.getPlayerList().getPlayers().forEach(MobDrops::sendAll);
        }
    }

    /** A first kill of a vanilla type unlocks it. A genus is unlocked from {@link Bestiary#kill}. */
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player
                && event.getEntity().level() instanceof ServerLevel) {
            unlock(player, BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()), false);
        }
    }

    // ------------------------------------------------------------------ sending

    /** Everything this player may see, replacing whatever their client held. */
    public static void sendAll(ServerPlayer player) {
        Net.sendIfAble(player, MobDropsPayload.TYPE, () -> {
            Set<Identifier> sent = new HashSet<>();
            List<MobDropsPayload.Entry> entries = new ArrayList<>();
            if (ZombieModConfig.MOB_DROPS.get()) {
                build(player.level().getServer());
                genera.forEach((id, entry) -> {
                    if (mayShowGenus(player, id)) {
                        entries.add(entry);
                    }
                });
                mobs.forEach((id, entry) -> {
                    if (mayShowMob(player, id)) {
                        entries.add(entry);
                    }
                });
                entries.forEach(e -> sent.add(e.key()));
            }
            SENT.put(player.getUUID(), sent);
            return new MobDropsPayload(true, entries);
        });
    }

    /** A genus this player has just killed. Called once the kill is recorded, so the dex agrees. */
    public static void unlockGenus(ServerPlayer player, Identifier genus) {
        unlock(player, genus, true);
    }

    private static void unlock(ServerPlayer player, Identifier id, boolean isGenus) {
        if (!ZombieModConfig.MOB_DROPS.get() || !ZombieModConfig.MOB_DROPS_KILLED_ONLY.get()) {
            return; // without the filter everything was sent at login; nothing to unlock
        }
        Identifier key = isGenus ? genusKey(id) : mobKey(id);
        Set<Identifier> sent = SENT.get(player.getUUID());
        // No set means no listening client: sendAll records one only when it really sent.
        if (sent == null || sent.contains(key)) {
            return;
        }
        build(player.level().getServer());
        MobDropsPayload.Entry entry = isGenus
                ? (mayShowGenus(player, id) ? genera.get(id) : null)
                : mobs.get(id);
        if (entry != null) {
            sent.add(key);
            Net.sendIfAble(player, new MobDropsPayload(false, List.of(entry)));
        }
    }

    private static boolean mayShowGenus(ServerPlayer player, Identifier id) {
        var holder = player.level().registryAccess().lookupOrThrow(ZombieModRegistries.GENUS)
                .get(ResourceKey.create(ZombieModRegistries.GENUS, id));
        if (holder.isEmpty()) {
            return false;
        }
        Bestiary bestiary = Bestiary.get((ServerLevel) player.level());
        if (bestiary.concealed(player.getUUID(), id, holder.get().value())) {
            return false;
        }
        return !ZombieModConfig.MOB_DROPS_KILLED_ONLY.get() || bestiary.killsOf(player.getUUID(), id) > 0;
    }

    private static boolean mayShowMob(ServerPlayer player, Identifier type) {
        if (!ZombieModConfig.MOB_DROPS_KILLED_ONLY.get()) {
            return true;
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(type)
                .map(t -> player.getStats().getValue(Stats.ENTITY_KILLED.get(t)) > 0)
                .orElse(false);
    }

    /** What {@link #build} last made. For probes; the sending paths read the fields. */
    static Map<Identifier, MobDropsPayload.Entry> genera() {
        return genera;
    }

    static Map<Identifier, MobDropsPayload.Entry> mobs() {
        return mobs;
    }

    static Identifier genusKey(Identifier genus) {
        return Identifier.fromNamespaceAndPath("zombiemod", "genus/" + genus.getNamespace() + "/" + genus.getPath());
    }

    static Identifier mobKey(Identifier type) {
        return Identifier.fromNamespaceAndPath("zombiemod", "mob/" + type.getNamespace() + "/" + type.getPath());
    }

    // ------------------------------------------------------------------ building

    static synchronized void build(MinecraftServer server) {
        Object owner = server.reloadableRegistries();
        if (owner == builtFor) {
            return;
        }
        long start = System.nanoTime();
        Reader reader = new Reader(server);

        Map<Identifier, MobDropsPayload.Entry> m = new LinkedHashMap<>();
        Map<Identifier, Map<String, Drop>> byType = new HashMap<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            Map<String, Drop> drops = type.getDefaultLootTable().map(reader::table).orElse(Map.of());
            byType.put(id, drops);
            if (!drops.isEmpty()) {
                m.put(id, new MobDropsPayload.Entry(mobKey(id), Optional.empty(), id, type.getDescription(),
                        sorted(drops)));
            }
        }

        Map<Identifier, MobDropsPayload.Entry> g = new LinkedHashMap<>();
        server.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS).listElements().forEach(holder -> {
            Identifier id = holder.key().identifier();
            Genus genus = holder.value();
            Identifier base = BuiltInRegistries.ENTITY_TYPE.getKey(genus.base());
            Map<String, Drop> drops = new LinkedHashMap<>();
            if (genus.loot().isPresent()) {
                drops.putAll(reader.table(genus.loot().get().table()));
            }
            // Added to the base mob's own, as in play, unless the genus replaces them.
            if (genus.loot().isEmpty() || !genus.loot().get().replace()) {
                byType.getOrDefault(base, Map.of()).forEach((item, d) -> drops.merge(item, d, Drop::or));
            }
            if (!drops.isEmpty()) {
                g.put(id, new MobDropsPayload.Entry(genusKey(id), Optional.of(id), base,
                        genus.displayName().orElse(Component.literal(id.getPath())), sorted(drops)));
            }
        });

        mobs = m;
        genera = g;
        builtFor = owner;
        LOG.debug("ZombieMod: mob drops read for {} mobs and {} genera in {} ms", m.size(), g.size(),
                (System.nanoTime() - start) / 1_000_000);
    }

    private static List<MobDropsPayload.Drop> sorted(Map<String, Drop> drops) {
        return drops.entrySet().stream()
                // What every one of them drops, likeliest first; then what only some of them do.
                .sorted(Comparator.comparing((Map.Entry<String, Drop> e) -> e.getValue().situational)
                        .thenComparingDouble(e -> -e.getValue().chance)
                        .thenComparing(Map.Entry::getKey))
                .map(e -> new MobDropsPayload.Drop(Identifier.parse(e.getKey()),
                        (float) Math.min(1.0D, e.getValue().chance), e.getValue().min, e.getValue().max,
                        e.getValue().situational))
                .toList();
    }

    /**
     * One item's figures while a table is being read.
     *
     * @param situational only some of these mobs can drop it - a baby on a chicken, a sheep of one
     *                    colour - and {@code chance} is for one that can
     */
    record Drop(double chance, int min, int max, boolean situational) {
        /**
         * Two independent ways to get it: either may happen; the counts span both. A way every one
         * of them has beats a way only some have - "zombies drop rotten flesh" stays true and
         * unhedged even though a jockey has a second route to it.
         */
        Drop or(Drop other) {
            if (situational != other.situational) {
                return situational ? other : this;
            }
            return new Drop(1.0D - (1.0D - chance) * (1.0D - other.chance),
                    Math.min(min, other.min), Math.max(max, other.max), situational);
        }

        Drop under(Cond cond) {
            return new Drop(chance * cond.chance, min, max, situational || cond.situational);
        }
    }

    /**
     * What a condition does to the odds.
     *
     * @param chance      the probability it passes, where that is a number: a random_chance
     * @param situational it depends on which mob this is or how it died - riding something, its
     *                    colour, on fire - which a list of drops for the whole type cannot know
     */
    record Cond(double chance, boolean situational) {
        static final Cond ALWAYS = new Cond(1.0D, false);
        static final Cond SOMETIMES = new Cond(1.0D, true);

        Cond and(Cond other) {
            return new Cond(chance * other.chance, situational || other.situational);
        }
    }

    /** Walks encoded loot tables. Package-private so a probe can drive it directly. */
    static final class Reader {

        private final MinecraftServer server;
        private final Map<ResourceKey<LootTable>, Map<String, Drop>> cache = new HashMap<>();

        Reader(MinecraftServer server) {
            this.server = server;
        }

        /** Every item the table can give, with its chance per kill and count per drop. */
        Map<String, Drop> table(ResourceKey<LootTable> key) {
            return table(key, 0);
        }

        private Map<String, Drop> table(ResourceKey<LootTable> key, int depth) {
            Map<String, Drop> known = cache.get(key);
            if (known != null) {
                return known;
            }
            LootTable table = server.reloadableRegistries().getLootTable(key);
            Map<String, Drop> out = table == LootTable.EMPTY ? Map.of() : encoded(table)
                    .map(json -> table(json, depth)).orElse(Map.of());
            cache.put(key, out);
            return out;
        }

        private Optional<JsonObject> encoded(LootTable table) {
            var ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
            return LootTable.DIRECT_CODEC.encodeStart(ops, table).result()
                    .filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject);
        }

        private Map<String, Drop> table(JsonObject table, int depth) {
            Map<String, Drop> out = new LinkedHashMap<>();
            for (JsonElement p : array(table, "pools")) {
                if (!p.isJsonObject()) {
                    continue;
                }
                JsonObject pool = p.getAsJsonObject();
                double rolls = average(pool.get("rolls"), 1.0D);
                Cond gate = conditions(pool);
                JsonArray entries = array(pool, "entries");
                double total = 0.0D;
                for (JsonElement e : entries) {
                    total += weight(e);
                }
                if (total <= 0.0D || rolls <= 0.0D || gate.chance <= 0.0D) {
                    continue;
                }
                // Per roll: each top-level entry's share of the pool's weight.
                Map<String, Drop> perRoll = new LinkedHashMap<>();
                for (JsonElement e : entries) {
                    if (e.isJsonObject()) {
                        entry(e.getAsJsonObject(), new Cond(weight(e) / total, false), perRoll, depth);
                    }
                }
                // Per kill: at least one of `rolls` independent rolls gives it.
                perRoll.forEach((item, d) -> out.merge(item,
                        new Drop(1.0D - Math.pow(1.0D - Math.min(1.0D, d.chance), rolls), d.min, d.max,
                                d.situational).under(gate),
                        Drop::or));
            }
            return out;
        }

        private void entry(JsonObject entry, Cond reach, Map<String, Drop> into, int depth) {
            Cond c = reach.and(conditions(entry));
            if (c.chance <= 0.0D) {
                return;
            }
            String type = string(entry, "type");
            if (type.endsWith("item")) {
                String name = string(entry, "name");
                int[] count = count(entry);
                if (!name.isEmpty() && !name.equals("minecraft:air") && count[1] > 0) {
                    // "0 to 2" is a third of the time nothing, so the chance is of at least one,
                    // and the count is what you get when you get any. A uniform count rounds down
                    // to an integer from min to max, each equally likely - and min may be below
                    // zero (wither skeleton coal is -1 to 1: one time in three).
                    double some = count[0] >= 1 ? 1.0D
                            : (double) count[1] / (count[1] - count[0] + 1);
                    into.merge(name, new Drop(some, Math.max(1, count[0]), count[1], false).under(c), Drop::or);
                }
            } else if (type.endsWith("loot_table")) {
                if (depth >= MAX_DEPTH) {
                    return;
                }
                JsonElement value = entry.get("value");
                Map<String, Drop> nested;
                if (value != null && value.isJsonPrimitive()) {
                    nested = table(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(value.getAsString())),
                            depth + 1);
                } else if (value != null && value.isJsonObject()) {
                    nested = table(value.getAsJsonObject(), depth + 1);
                } else {
                    return;
                }
                nested.forEach((item, d) -> into.merge(item, d.under(c), Drop::or));
            } else if (entry.has("children")) {
                // group, alternatives, sequence: each child at its own conditions. Alternatives
                // really take the first that passes, so this is the generous reading.
                for (JsonElement child : array(entry, "children")) {
                    if (child.isJsonObject()) {
                        entry(child.getAsJsonObject(), c, into, depth);
                    }
                }
            }
            // empty, tag and dynamic entries give nothing a list can name
        }

        /** The weight an entry takes in its pool, counting an empty one: it is what makes "sometimes nothing". */
        private static double weight(JsonElement e) {
            if (!e.isJsonObject()) {
                return 0.0D;
            }
            JsonElement w = e.getAsJsonObject().get("weight");
            return w != null && w.isJsonPrimitive() ? w.getAsDouble() : 1.0D;
        }

        // ------------------------------------------------------------------ conditions

        /** {@code conditions} (1.21-26.2, a list) or {@code condition} (26.3, one, often all_of). */
        private static Cond conditions(JsonObject owner) {
            Cond c = Cond.ALWAYS;
            for (JsonElement cond : array(owner, "conditions")) {
                c = c.and(condition(cond));
            }
            JsonElement one = owner.get("condition");
            if (one != null && one.isJsonObject()) {
                c = c.and(condition(one));
            }
            return c;
        }

        /**
         * Chances are numbers; killed-by-player is met, since this is what killing one gives; the
         * combinators combine. Anything else is about this particular mob or this particular death
         * - what it rides, its colour, the weapon - and is <em>situational</em>: the drop is real,
         * but not something every one of them gives, and pretending otherwise put "Always: Lava
         * Chicken disc" on every zombie.
         */
        private static Cond condition(JsonElement element) {
            if (!element.isJsonObject()) {
                return Cond.ALWAYS;
            }
            JsonObject cond = element.getAsJsonObject();
            String type = cond.has("condition") ? string(cond, "condition") : string(cond, "type");
            switch (type) {
                case "minecraft:random_chance":
                    return new Cond(clamp(average(cond.get("chance"), 1.0D)), false);
                case "minecraft:random_chance_with_enchanted_bonus":
                    return new Cond(clamp(average(cond.get("unenchanted_chance"), 1.0D)), false);
                case "minecraft:random_chance_with_looting":
                    return new Cond(clamp(average(cond.get("chance"), 1.0D)), false);
                case "minecraft:killed_by_player":
                    return Cond.ALWAYS;
                case "minecraft:all_of": {
                    Cond c = Cond.ALWAYS;
                    for (JsonElement t : array(cond, "terms")) {
                        c = c.and(condition(t));
                    }
                    return c;
                }
                case "minecraft:any_of": {
                    // Situational only if every way through is.
                    double none = 1.0D;
                    boolean situational = true;
                    for (JsonElement t : array(cond, "terms")) {
                        Cond term = condition(t);
                        none *= 1.0D - term.chance;
                        situational &= term.situational;
                    }
                    return new Cond(1.0D - none, situational);
                }
                case "minecraft:inverted": {
                    JsonElement term = cond.get("term");
                    Cond inner = term == null ? Cond.SOMETIMES : condition(term);
                    return inner.situational ? Cond.SOMETIMES : new Cond(1.0D - inner.chance, false);
                }
                default:
                    return Cond.SOMETIMES;
            }
        }

        // ------------------------------------------------------------------ counts

        /** {@code [min, max]} from a set_count among the entry's functions/modifiers; {@code [1, 1]} without. */
        private static int[] count(JsonObject entry) {
            List<JsonElement> functions = new ArrayList<>();
            array(entry, "functions").forEach(functions::add);
            JsonElement modifier = entry.get("modifier");
            if (modifier != null && modifier.isJsonArray()) {
                modifier.getAsJsonArray().forEach(functions::add);
            } else if (modifier != null && modifier.isJsonObject()) {
                functions.add(modifier);
            }
            for (JsonElement f : functions) {
                if (f.isJsonObject()
                        && string(f.getAsJsonObject(), "function", "type").equals("minecraft:set_count")) {
                    JsonElement count = f.getAsJsonObject().get("count");
                    return new int[] {(int) Math.floor(low(count)), (int) Math.floor(high(count))};
                }
            }
            return new int[] {1, 1};
        }

        // ------------------------------------------------------------------ number providers

        /** A bare number, {@code {min, max}}, or a typed provider: constant, uniform, binomial. */
        private static double average(JsonElement n, double fallback) {
            if (n == null || n.isJsonNull()) {
                return fallback;
            }
            if (n.isJsonPrimitive()) {
                return n.getAsDouble();
            }
            if (!n.isJsonObject()) {
                return fallback;
            }
            JsonObject o = n.getAsJsonObject();
            if (o.has("value")) {
                return average(o.get("value"), fallback);
            }
            if (o.has("n") && o.has("p")) {
                return average(o.get("n"), 0.0D) * average(o.get("p"), 0.0D);
            }
            if (o.has("min") || o.has("max")) {
                return (low(n) + high(n)) / 2.0D;
            }
            return fallback;
        }

        private static double low(JsonElement n) {
            if (n == null || n.isJsonNull()) {
                return 1.0D;
            }
            if (n.isJsonPrimitive()) {
                return n.getAsDouble();
            }
            JsonObject o = n.getAsJsonObject();
            if (o.has("n")) {
                return 0.0D;
            }
            if (o.has("value")) {
                return low(o.get("value"));
            }
            return o.has("min") ? low(o.get("min")) : 0.0D;
        }

        private static double high(JsonElement n) {
            if (n == null || n.isJsonNull()) {
                return 1.0D;
            }
            if (n.isJsonPrimitive()) {
                return n.getAsDouble();
            }
            JsonObject o = n.getAsJsonObject();
            if (o.has("n")) {
                return high(o.get("n"));
            }
            if (o.has("value")) {
                return high(o.get("value"));
            }
            return o.has("max") ? high(o.get("max")) : low(n);
        }

        // ------------------------------------------------------------------ json

        private static JsonArray array(JsonObject o, String key) {
            JsonElement e = o.get(key);
            return e != null && e.isJsonArray() ? e.getAsJsonArray() : new JsonArray();
        }

        private static String string(JsonObject o, String... keys) {
            for (String key : keys) {
                JsonElement e = o.get(key);
                if (e != null && e.isJsonPrimitive()) {
                    return e.getAsString();
                }
            }
            return "";
        }

        private static double clamp(double v) {
            return Math.max(0.0D, Math.min(1.0D, v));
        }
    }
}
