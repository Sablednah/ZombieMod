package com.sablednah.zombiemod.neoforge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.ZombieModConfig;

import org.slf4j.Logger;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Advancements, for the things only this mod can see happen.
 *
 * <p>Advancements are vanilla: datapack files, judged on the server, drawn by an unmodded client -
 * toast, tab and all. So they cost the vanilla-client promise nothing, <em>provided we register no
 * trigger type of our own</em>. A custom trigger is a registry entry, and a registry entry is one
 * more thing for a client that has never heard of us to be asked about. There is no need to find
 * out: vanilla already ships a trigger that does nothing, {@code minecraft:impossible}, which exists
 * precisely so that something else can grant the criterion by hand.
 *
 * <p>So this is a convention rather than an API. <b>Any advancement, in any namespace, with a
 * criterion whose <em>name</em> starts {@code zombiemod:} is ours to grant</b>, and the name says
 * when:
 *
 * <pre>
 *   zombiemod:meet/&lt;genus&gt;          zombiemod:kill/&lt;genus&gt;        (and bare: zombiemod:kill)
 *   zombiemod:met_count/&lt;n&gt;         zombiemod:killed_count/&lt;n&gt;    distinct genera, from the dex
 *   zombiemod:met_all                zombiemod:killed_all           the year-round roster you can see
 *   zombiemod:defuse/&lt;genus&gt;        killed with its fuse lit, before it went off
 *   zombiemod:corpse/own             zombiemod:corpse/other         whose player zombie you put down
 *   zombiemod:ritual/&lt;ritual&gt;       zombiemod:horde_cleared/&lt;horde&gt;   zombiemod:cured/&lt;entity type&gt;
 * </pre>
 *
 * Every event with a subject is also offered bare, so "perform any ritual" needs no list. That makes
 * the shipped advancements ordinary data: a pack that adds a genus can add "kill it" beside it, with
 * no code, and a server that dislikes ours can override or empty any file of them.
 *
 * <p>The index from criterion name to advancements is rebuilt when the advancement tree changes
 * identity, which is what {@code /reload} does to it. No reload listener, so nothing to register and
 * nothing that is named differently on the next Minecraft version.
 */
public final class Feats {

    private static final Logger LOG = LogUtils.getLogger();
    private static final String PREFIX = "zombiemod:";
    private static final String MET_COUNT = PREFIX + "met_count/";
    private static final String KILLED_COUNT = PREFIX + "killed_count/";

    /** Criteria granted this session, and how many names the loaded datapacks are listening for. */
    public static final class Counters {
        public int granted;
        public int listening;

        @Override
        public String toString() {
            return granted + " criteria granted this session, " + listening + " criterion names in use";
        }
    }

    public static final Counters COUNTERS = new Counters();

    private static Object indexedTree;
    private static Map<String, List<AdvancementHolder>> index = Map.of();
    private static TreeSet<Integer> metSteps = new TreeSet<>();
    private static TreeSet<Integer> killedSteps = new TreeSet<>();

    // ------------------------------------------------------------------ what other classes call

    /** Something happened. {@code subject} may be null for an event that has none. */
    public static void fire(ServerPlayer player, String event, String subject) {
        if (!listening(player)) {
            return;
        }
        award(player, PREFIX + event);
        if (subject != null) {
            award(player, PREFIX + event + "/" + subject);
        }
    }

    /**
     * The dex moved. Counts are distinct genera, and "all" is the roster this player can see - a
     * genus a server has concealed is not held against anybody.
     *
     * <p><b>Nor is one that only exists for a week of the real year.</b> A seasonal genus still
     * counts toward the numbered steps if you have met it, because that only ever helps; but it is
     * left out of what "all" is measured against, or finishing the set would mean being online at
     * Christmas. Seasonal is read off the genus's own spawn rules, so a pack's Easter zombie is
     * excused without anybody listing it.
     */
    public static void dex(ServerPlayer player) {
        if (!listening(player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Bestiary bestiary = Bestiary.get(level);
        int[] tally = new int[5]; // year-round roster, met of it, killed of it, met at all, killed at all
        level.registryAccess().lookupOrThrow(com.sablednah.zombiemod.ZombieModRegistries.GENUS)
                .listElements().forEach(holder -> {
                    Identifier id = holder.key().identifier();
                    if (bestiary.concealed(player.getUUID(), id, holder.value())) {
                        return;
                    }
                    boolean yearRound = !holder.value().spawn().seasonal();
                    boolean met = bestiary.hasMet(player.getUUID(), id);
                    boolean killed = bestiary.killsOf(player.getUUID(), id) > 0;
                    if (yearRound) {
                        tally[0]++;
                    }
                    if (met) {
                        tally[3]++;
                        tally[1] += yearRound ? 1 : 0;
                    }
                    if (killed) {
                        tally[4]++;
                        tally[2] += yearRound ? 1 : 0;
                    }
                });
        for (int step : metSteps.headSet(tally[3], true)) {
            award(player, MET_COUNT + step);
        }
        for (int step : killedSteps.headSet(tally[4], true)) {
            award(player, KILLED_COUNT + step);
        }
        if (tally[0] > 0 && tally[1] >= tally[0]) {
            award(player, PREFIX + "met_all");
        }
        if (tally[0] > 0 && tally[2] >= tally[0]) {
            award(player, PREFIX + "killed_all");
        }
    }

    // ------------------------------------------------------------------ what it watches itself

    /**
     * Kills, defusals and corpses. Its own handler rather than a line in the bestiary's, because
     * that one answers to the bestiary switch - and "I killed the Butcher and got nothing" should
     * not be a consequence of having turned the dex off.
     */
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        String genus = mob.getPersistentData().getString(GenusApplier.GENUS_TAG).orElse("");
        if (genus.isEmpty()) {
            return;
        }
        fire(player, "kill", genus);
        if (mob.getPersistentData().getBooleanOr(com.sablednah.zombiemod.core.ability.Abilities.FUSE_LIT, false)) {
            fire(player, "defuse", genus);
        }
        PlayerZombies.ownerOf(level, mob).ifPresent(owner ->
                fire(player, "corpse", owner.equals(player.getUUID()) ? "own" : "other"));
    }

    /**
     * Catch a player up with their own dex. Everything the dex remembers is re-offered at login, so
     * somebody forty genera in when this arrived is not asked to start again - and an advancement
     * added by a datapack later is granted to the people who had already earned it.
     */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !listening(player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Bestiary bestiary = Bestiary.get(level);
        level.registryAccess().lookupOrThrow(com.sablednah.zombiemod.ZombieModRegistries.GENUS)
                .listElementIds().forEach(key -> {
                    Identifier id = key.identifier();
                    if (bestiary.hasMet(player.getUUID(), id)) {
                        fire(player, "meet", id.toString());
                    }
                    if (bestiary.killsOf(player.getUUID(), id) > 0) {
                        fire(player, "kill", id.toString());
                    }
                });
        dex(player);
    }

    // ------------------------------------------------------------------ the index

    /**
     * Whether to bother. A fake player is turned away here only to save the work: NeoForge patches
     * {@code PlayerAdvancements.award} to refuse one outright, so another mod's grinder could never
     * have earned anything, or announced it in chat, whatever this method said.
     */
    private static boolean listening(ServerPlayer player) {
        if (!ZombieModConfig.ADVANCEMENTS.get() || player.isFakePlayer()) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }
        Object tree = server.getAdvancements().tree();
        if (tree != indexedTree) {
            rebuild(server);
            indexedTree = tree;
        }
        return !index.isEmpty();
    }

    private static void rebuild(MinecraftServer server) {
        Map<String, List<AdvancementHolder>> built = new HashMap<>();
        TreeSet<Integer> met = new TreeSet<>();
        TreeSet<Integer> killed = new TreeSet<>();
        for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
            for (String name : holder.value().criteria().keySet()) {
                if (!name.startsWith(PREFIX)) {
                    continue;
                }
                built.computeIfAbsent(name, k -> new ArrayList<>()).add(holder);
                step(name, MET_COUNT, met, holder);
                step(name, KILLED_COUNT, killed, holder);
            }
        }
        index = built;
        metSteps = met;
        killedSteps = killed;
        COUNTERS.listening = built.size();
    }

    private static void step(String name, String prefix, TreeSet<Integer> into, AdvancementHolder holder) {
        if (!name.startsWith(prefix)) {
            return;
        }
        try {
            into.add(Integer.parseInt(name.substring(prefix.length())));
        } catch (NumberFormatException e) {
            LOG.warn("ZombieMod: advancement {} has criterion '{}', which needs a whole number after "
                    + "the slash - it will never be granted", holder.id(), name);
        }
    }

    private static void award(ServerPlayer player, String criterion) {
        for (AdvancementHolder holder : index.getOrDefault(criterion, List.of())) {
            if (player.getAdvancements().award(holder, criterion)) {
                COUNTERS.granted++;
            }
        }
    }
}
