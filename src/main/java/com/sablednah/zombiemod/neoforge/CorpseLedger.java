package com.sablednah.zombiemod.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * A durable record of every player corpse and what it was carrying.
 *
 * <p>This exists because of how the 1.8 version failed. Corpses went missing, despawned, or died
 * without dropping, and when they did the items were simply gone — there was nowhere else they
 * existed. An admin had no answer except to guess and hand out replacements.
 *
 * <p>So the items are written down the moment a corpse is raised, not only carried on the entity.
 * If the corpse is later killed normally its entry is marked claimed and nothing is owed. If it
 * vanishes down a ravine, gets caught in a mob grinder, or falls out of the world, the entry is
 * still here and `/zombiemod corpse give` can put things right.
 *
 * <p>Stored on the overworld's data storage, so it's one ledger per save rather than per dimension —
 * a corpse in the Nether is still that player's corpse.
 */
public final class CorpseLedger extends SavedData {

    public record Entry(UUID id, UUID player, String playerName, String dimension,
            int x, int y, int z, long day, List<ItemStack> items, boolean claimed) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Entry::id),
                UUIDUtil.CODEC.fieldOf("player").forGetter(Entry::player),
                Codec.STRING.fieldOf("player_name").forGetter(Entry::playerName),
                Codec.STRING.fieldOf("dimension").forGetter(Entry::dimension),
                Codec.INT.fieldOf("x").forGetter(Entry::x),
                Codec.INT.fieldOf("y").forGetter(Entry::y),
                Codec.INT.fieldOf("z").forGetter(Entry::z),
                Codec.LONG.fieldOf("day").forGetter(Entry::day),
                ItemStack.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(Entry::items),
                Codec.BOOL.optionalFieldOf("claimed", false).forGetter(Entry::claimed))
                .apply(i, Entry::new));

        public Entry claim() {
            return new Entry(id, player, playerName, dimension, x, y, z, day, items, true);
        }

        public BlockPos pos() {
            return new BlockPos(x, y, z);
        }
    }

    private static final Codec<CorpseLedger> CODEC = Entry.CODEC.listOf()
            .xmap(CorpseLedger::new, ledger -> ledger.entries)
            .fieldOf("corpses").codec();

    public static final SavedDataType<CorpseLedger> TYPE =
            new SavedDataType<>("zombiemod_corpses", CorpseLedger::new, CODEC, null);

    private final List<Entry> entries;

    private CorpseLedger() {
        this.entries = new ArrayList<>();
    }

    private CorpseLedger(List<Entry> entries) {
        this.entries = new ArrayList<>(entries);
    }

    public static CorpseLedger get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void record(Entry entry) {
        entries.add(entry);
        setDirty();
    }

    /** Mark an entry settled, so an admin isn't asked to hand out items that already dropped. */
    public void claim(UUID id) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id().equals(id) && !entries.get(i).claimed()) {
                entries.set(i, entries.get(i).claim());
                setDirty();
                return;
            }
        }
    }

    public boolean forget(UUID id) {
        boolean removed = entries.removeIf(e -> e.id().equals(id));
        if (removed) {
            setDirty();
        }
        return removed;
    }

    /** Newest first, optionally for one player, optionally only those still owed. */
    public List<Entry> find(Optional<String> playerName, boolean unclaimedOnly) {
        return entries.stream()
                .filter(e -> playerName.isEmpty() || e.playerName().equalsIgnoreCase(playerName.get()))
                .filter(e -> !unclaimedOnly || !e.claimed())
                .sorted((a, b) -> Long.compare(b.day(), a.day()))
                .toList();
    }
}
