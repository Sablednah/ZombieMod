package com.sablednah.zombiemod.net;

import java.util.List;
import java.util.Optional;

import com.sablednah.zombiemod.ZombieMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * What mobs drop, for JEI's "Mob Drops" category. Loot tables are server-only data, so the server
 * reads them and sends the answer.
 *
 * <p>Its own channel, like {@link DexBonusPayload} and for the same reason: an older client simply
 * has no such channel, {@code sendIfAble} sends nothing, and that player's game is as it was.
 *
 * <p><b>Only what this player is allowed to see.</b> With {@code mobDrops.killedOnly} on (the
 * default), the server leaves out every mob the player has not killed, so a client cannot be talked
 * into showing the rest - there is nothing on it to show. That is the whole of the "server
 * authoritative" design: the filter is which entries are sent, not a flag the client honours.
 *
 * @param replace true for a full set that replaces what the client holds (login, {@code /reload}),
 *                false for entries to add (a first kill)
 * @param entries one per mob or genus
 */
public record MobDropsPayload(boolean replace, List<Entry> entries) implements CustomPacketPayload {

    public static final Type<MobDropsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ZombieMod.MOD_ID, "mob_drops"));

    /**
     * One thing that drops what it drops.
     *
     * @param key    unique and stable: {@code zombiemod:genus/<ns>/<path>} or {@code zombiemod:mob/<ns>/<path>}
     * @param genus  the genus, if this is one - the client draws its doll from it
     * @param entity the entity type to draw: the mob itself, or the genus's base
     * @param name   what to call it; a translatable name for a vanilla mob, the genus's own for a genus
     * @param drops  the items, most likely first
     */
    public record Entry(Identifier key, Optional<Identifier> genus, Identifier entity, Component name,
            List<Drop> drops) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Entry::key,
                ByteBufCodecs.optional(Identifier.STREAM_CODEC), Entry::genus,
                Identifier.STREAM_CODEC, Entry::entity,
                ComponentSerialization.STREAM_CODEC, Entry::name,
                Drop.STREAM_CODEC.apply(ByteBufCodecs.list()), Entry::drops,
                Entry::new);
    }

    /**
     * One item, and how much of it a kill gives.
     *
     * @param item   the item id
     * @param chance the chance a kill drops any at all, 0..1, before Looting
     * @param min    the fewest one drop gives
     * @param max    the most one drop gives
     * @param situational only some of them drop it - by what they ride, their colour, how they died -
     *               and {@code chance} is for one that does
     */
    public record Drop(Identifier item, float chance, int min, int max, boolean situational) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Drop> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Drop::item,
                ByteBufCodecs.FLOAT, Drop::chance,
                ByteBufCodecs.VAR_INT, Drop::min,
                ByteBufCodecs.VAR_INT, Drop::max,
                ByteBufCodecs.BOOL, Drop::situational,
                Drop::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, MobDropsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, MobDropsPayload::replace,
                    Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), MobDropsPayload::entries,
                    MobDropsPayload::new);

    @Override
    public Type<MobDropsPayload> type() {
        return TYPE;
    }
}
