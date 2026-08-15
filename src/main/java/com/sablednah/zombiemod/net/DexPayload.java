package com.sablednah.zombiemod.net;

import java.util.List;

import com.sablednah.zombiemod.ZombieMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * One player's ZombieDex, as the client needs to draw it.
 *
 * <p>The whole roster each time rather than deltas. Fifty-odd short entries is nothing on the wire,
 * and a full snapshot cannot drift out of step with the server the way an accumulated series of
 * deltas can after one dropped or reordered message.
 *
 * <p>Note what is <em>not</em> here: anything the client could act on. This carries what a player has
 * already met and already killed — facts they can read in a book on a vanilla client. A modded
 * client gets a nicer window onto the same information, never more of it, which is what keeps the
 * two kinds of player playing the same game.
 *
 * @param entries one per genus the server has loaded
 */
public record DexPayload(List<Entry> entries) implements CustomPacketPayload {

    public static final Type<DexPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ZombieMod.MOD_ID, "dex"));

    /**
     * @param genus the registry id, so the client can key on something stable
     * @param name  the display name, already formatted server-side - the client has no genus registry
     *              of its own to look names up in
     */
    public record Entry(Identifier genus, String name, boolean met, int kills, List<String> drops) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC =
                StreamCodec.composite(
                        Identifier.STREAM_CODEC, Entry::genus,
                        ByteBufCodecs.STRING_UTF8, Entry::name,
                        ByteBufCodecs.BOOL, Entry::met,
                        ByteBufCodecs.VAR_INT, Entry::kills,
                        // Item ids from the genus's loot table - sent only once this player has
                        // killed one, because the reward before the first kill is a spoiler.
                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Entry::drops,
                        Entry::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, DexPayload> STREAM_CODEC =
            StreamCodec.composite(
                    Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), DexPayload::entries,
                    DexPayload::new);

    @Override
    public Type<DexPayload> type() {
        return TYPE;
    }
}
