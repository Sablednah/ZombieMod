package com.sablednah.zombiemod.neoforge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sablednah.zombiemod.ZombieModConfig;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Whose face the Ghost is allowed to wear.
 *
 * <p>Two sources, added together. The seed list in the config is there because a brand new server
 * has nobody in the second one, and a Ghost with nothing to wear is just a zombie. Logins are the
 * other, recorded as they happen: there is no public "list every profile this server has seen"
 * lookup — the profile cache answers questions but will not enumerate.
 *
 * <p><b>Bans are filtered when a face is picked, not when the ban lands.</b> There is no ban event
 * in NeoForge to listen to, but reacting to one would be the worse design even if there were: it
 * would miss bans applied from the console or while the server was down, it would need its own copy
 * of state the server already keeps, and un-banning somebody would not put them back. Asking the
 * live ban list at the moment of the draw is one call and is always right.
 *
 * <p>Entries carry when they were last seen so the list can be pruned by age, and the whole thing
 * stays capped regardless — on a long-lived server this would otherwise grow without limit for a
 * feature that only ever needs one random name.
 */
public final class KnownPlayers extends SavedData {

    private static final int MAX = 512;

    private static final long DAY_MS = 86_400_000L;

    /**
     * @param lastSeen epoch millis of their most recent login; 0 for entries written before this
     *                 field existed, which are treated as "seen just now" rather than pruned on
     *                 sight for having been recorded by an older version
     */
    public record Seen(UUID id, String name, long lastSeen) {
        public static final Codec<Seen> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Seen::id),
                Codec.STRING.fieldOf("name").forGetter(Seen::name),
                Codec.LONG.optionalFieldOf("last_seen", 0L).forGetter(Seen::lastSeen))
                .apply(i, Seen::new));
    }

    /** One face the Ghost may wear. Config entries have no id; logins do. */
    public record Candidate(Optional<UUID> id, String name) {}

    private static final Codec<KnownPlayers> CODEC = Seen.CODEC.listOf()
            .xmap(KnownPlayers::new, k -> new ArrayList<>(k.seen.values()))
            .fieldOf("players").codec();

    public static final SavedDataType<KnownPlayers> TYPE =
            new SavedDataType<>("zombiemod_known_players", KnownPlayers::new, CODEC, null);

    private final Map<UUID, Seen> seen;

    private KnownPlayers() {
        this.seen = new LinkedHashMap<>();
    }

    private KnownPlayers(List<Seen> entries) {
        this.seen = new LinkedHashMap<>();
        entries.forEach(e -> seen.put(e.id(), e));
    }

    public static KnownPlayers get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Records a login, refreshing the timestamp even when nothing else changed. */
    public void remember(GameProfile profile) {
        if (!ZombieModConfig.GHOST_REMEMBER.get()) {
            return;
        }
        String name = profile.name();
        if (name == null || name.isBlank()) {
            return;
        }
        UUID id = profile.id();
        // Re-inserted rather than replaced in place, so the eviction order stays least-recently-seen
        // rather than first-ever-seen. LinkedHashMap keeps insertion order, and a put on an existing
        // key does not move it.
        seen.remove(id);
        seen.put(id, new Seen(id, name, System.currentTimeMillis()));
        while (seen.size() > MAX) {
            seen.remove(seen.keySet().iterator().next());
        }
        setDirty();
    }

    /**
     * Drops anyone unseen for longer than the configured window, and anyone currently banned.
     *
     * <p>Pruning bans here as well as filtering them at pick time is not redundant: filtering keeps
     * the Ghost honest, and pruning keeps the saved file from carrying names the server has decided
     * it does not want. A later un-ban simply re-adds them on their next login.
     */
    public int prune(MinecraftServer server) {
        return prune(server, System.currentTimeMillis());
    }

    /** Takes the clock as an argument so "has it been ninety days" is answerable without waiting. */
    int prune(MinecraftServer server, long now) {
        int days = ZombieModConfig.GHOST_REMEMBER_DAYS.get();
        int before = seen.size();
        seen.values().removeIf(entry -> {
            if (days > 0 && entry.lastSeen() > 0 && now - entry.lastSeen() > days * DAY_MS) {
                return true;
            }
            return ZombieModConfig.GHOST_SKIP_BANNED.get() && isBanned(server, entry);
        });
        int removed = before - seen.size();
        if (removed > 0) {
            setDirty();
        }
        return removed;
    }

    private static boolean isBanned(MinecraftServer server, Seen entry) {
        return server.getPlayerList().getBans().isBanned(new NameAndId(entry.id(), entry.name()));
    }

    /**
     * A face to wear, from the config list and the login list together.
     *
     * <p>The two are pooled rather than one preferred over the other, so a server that seeds three
     * names and has had thirty players mostly shows its own players — which is the right shape. Add
     * more seed names to weight them up.
     */
    public Optional<Candidate> random(RandomSource random, MinecraftServer server) {
        List<Candidate> pool = new ArrayList<>();
        for (String name : ZombieModConfig.GHOST_NAMES.get()) {
            if (name != null && !name.isBlank()) {
                pool.add(new Candidate(Optional.empty(), name));
            }
        }
        boolean skipBanned = ZombieModConfig.GHOST_SKIP_BANNED.get();
        for (Seen entry : seen.values()) {
            if (skipBanned && isBanned(server, entry)) {
                continue;
            }
            pool.add(new Candidate(Optional.of(entry.id()), entry.name()));
        }
        if (pool.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(pool.get(random.nextInt(pool.size())));
    }
}
