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

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Everyone who has played on this server.
 *
 * <p>Exists for one ability: the 1.8 {@code GHOST} named itself after a random offline player, and
 * there is no public "list every profile the server has seen" lookup — the profile cache answers
 * questions but will not enumerate. Recording logins is the honest way to know.
 *
 * <p>Capped, and oldest-first eviction, because on a long-lived server this would otherwise grow
 * without limit for a feature that only ever needs one random name.
 */
public final class KnownPlayers extends SavedData {

    private static final int MAX = 512;

    public record Seen(UUID id, String name) {
        public static final Codec<Seen> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Seen::id),
                Codec.STRING.fieldOf("name").forGetter(Seen::name))
                .apply(i, Seen::new));
    }

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

    public void remember(GameProfile profile) {
        UUID id = profile.id();
        String name = profile.name();
        if (name == null || name.isBlank()) {
            return;
        }
        Seen existing = seen.get(id);
        if (existing != null && existing.name().equals(name)) {
            return;
        }
        seen.put(id, new Seen(id, name));
        while (seen.size() > MAX) {
            seen.remove(seen.keySet().iterator().next());
        }
        setDirty();
    }

    public Optional<Seen> random(RandomSource random) {
        if (seen.isEmpty()) {
            return Optional.empty();
        }
        List<Seen> all = new ArrayList<>(seen.values());
        return Optional.of(all.get(random.nextInt(all.size())));
    }
}
