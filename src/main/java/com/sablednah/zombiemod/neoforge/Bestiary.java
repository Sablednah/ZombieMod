package com.sablednah.zombiemod.neoforge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sablednah.zombiemod.ZombieModConfig;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * Who has met what, and who has killed what.
 *
 * <p>The record lives here, in saved data, and is complete whether or not anything is mirrored to a
 * scoreboard. That ordering matters: the scoreboard is a <em>view</em>, so turning per-genus
 * tracking on later shows a history that was being kept all along rather than starting from zero.
 *
 * <p>Two totals are always mirrored — kills, and how many distinct genera you have killed, which is
 * the completion number a leaderboard actually wants. The per-genus objectives are opt-in because
 * they cost one scoreboard row per genus per player and every row syncs to every client; fifty
 * genera on a big server is a lot of packets for a checklist. On a small server it is nothing, which
 * is exactly who the switch is for.
 *
 * <p>"Met" is deliberately weaker than seen: it means damage passed between you in one direction or
 * the other. Real line-of-sight would mean a visibility check per mob per tick, which is a lot of
 * work to be able to tick a box for something you glanced at across a valley.
 */
public final class Bestiary extends SavedData {

    /** Objective names. Fixed rather than configurable - a datapack keying off these needs to know. */
    public static final String SLAIN = "zombiemod.slain";
    public static final String GENERA = "zombiemod.genera";
    public static final String PER_GENUS_PREFIX = "zombiemod.";

    public record Record(UUID player, List<Identifier> met, Map<Identifier, Integer> kills) {
        public static final Codec<Record> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(Record::player),
                Identifier.CODEC.listOf().optionalFieldOf("met", List.of()).forGetter(Record::met),
                Codec.unboundedMap(Identifier.CODEC, Codec.INT)
                        .optionalFieldOf("kills", Map.of()).forGetter(Record::kills))
                .apply(i, Record::new));
    }

    private static final Codec<Bestiary> CODEC = Record.CODEC.listOf()
            .xmap(Bestiary::new, Bestiary::pack)
            .fieldOf("players").codec();

    public static final SavedDataType<Bestiary> TYPE =
            new SavedDataType<>("zombiemod_bestiary", Bestiary::new, CODEC, null);

    private final Map<UUID, Set<Identifier>> met = new HashMap<>();
    private final Map<UUID, Map<Identifier, Integer>> kills = new HashMap<>();

    private Bestiary() {}

    private Bestiary(List<Record> records) {
        for (Record r : records) {
            met.put(r.player(), new LinkedHashSet<>(r.met()));
            kills.put(r.player(), new HashMap<>(r.kills()));
        }
    }

    private List<Record> pack() {
        Set<UUID> all = new LinkedHashSet<>(met.keySet());
        all.addAll(kills.keySet());
        List<Record> out = new ArrayList<>();
        for (UUID id : all) {
            out.add(new Record(id,
                    new ArrayList<>(met.getOrDefault(id, Set.of())),
                    new HashMap<>(kills.getOrDefault(id, Map.of()))));
        }
        return out;
    }

    public static Bestiary get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    // ------------------------------------------------------------------ recording

    /** Damage passed between them, in either direction. */
    public void meet(ServerPlayer player, Identifier genus) {
        if (!ZombieModConfig.BESTIARY.get()) {
            return;
        }
        if (met.computeIfAbsent(player.getUUID(), k -> new LinkedHashSet<>()).add(genus)) {
            setDirty();
        }
    }

    /** Counts the kill, and marks it met - you cannot kill something you never met. */
    public void kill(ServerPlayer player, Identifier genus) {
        if (!ZombieModConfig.BESTIARY.get()) {
            return;
        }
        meet(player, genus);
        Map<Identifier, Integer> mine = kills.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        boolean firstOfThisGenus = !mine.containsKey(genus);
        mine.merge(genus, 1, Integer::sum);
        setDirty();
        publish(player, genus, mine, firstOfThisGenus);
    }

    // ------------------------------------------------------------------ reading

    public int killsOf(UUID player, Identifier genus) {
        return kills.getOrDefault(player, Map.of()).getOrDefault(genus, 0);
    }

    public boolean hasMet(UUID player, Identifier genus) {
        return met.getOrDefault(player, Set.of()).contains(genus);
    }

    public int totalKills(UUID player) {
        return kills.getOrDefault(player, Map.of()).values().stream().mapToInt(Integer::intValue).sum();
    }

    public int distinctKilled(UUID player) {
        return kills.getOrDefault(player, Map.of()).size();
    }

    // ------------------------------------------------------------------ the scoreboard view

    private void publish(ServerPlayer player, Identifier genus, Map<Identifier, Integer> mine,
            boolean firstOfThisGenus) {
        Scoreboard scoreboard = player.level().getScoreboard();
        int total = mine.values().stream().mapToInt(Integer::intValue).sum();
        scoreboard.getOrCreatePlayerScore(player, objective(scoreboard, SLAIN, "Zombies Slain")).set(total);
        if (firstOfThisGenus) {
            scoreboard.getOrCreatePlayerScore(player, objective(scoreboard, GENERA, "Genera Slain"))
                    .set(mine.size());
        }
        if (ZombieModConfig.BESTIARY_PER_GENUS.get()) {
            String name = PER_GENUS_PREFIX + genus.getPath();
            scoreboard.getOrCreatePlayerScore(player, objective(scoreboard, name, genus.getPath()))
                    .set(mine.get(genus));
        }
    }

    /**
     * Ours are created on demand, unlike the bounty objective which an admin must add by hand.
     *
     * <p>Different jobs: the bounty objective is a hook into whatever economy a server already runs,
     * so creating one uninvited would be presumptuous. A bestiary that requires fifty
     * {@code /scoreboard objectives add} lines before it records anything is just broken.
     */
    private static Objective objective(Scoreboard scoreboard, String name, String display) {
        Objective existing = scoreboard.getObjective(name);
        if (existing != null) {
            return existing;
        }
        return scoreboard.addObjective(name, ObjectiveCriteria.DUMMY, Component.literal(display),
                ObjectiveCriteria.RenderType.INTEGER, false, null);
    }
}
