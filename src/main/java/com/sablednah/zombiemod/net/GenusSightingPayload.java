package com.sablednah.zombiemod.net;

import com.sablednah.zombiemod.ZombieMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Which genus a mob in view is. The genus key lives in the mob's persistent data, which never
 * leaves the server, so a client cannot otherwise tell a Harvester from a zombie that happens to
 * hold a hoe. Sent when a player starts tracking a genus mob, and when an already-tracked mob is
 * given a genus. Read by the JourneyMap integration, for the face and the name on the map.
 *
 * <p>Its own channel, so an older client simply lacks it - see {@link DexBonusPayload}.
 *
 * @param entityId the mob's network id
 * @param genus    its genus
 */
public record GenusSightingPayload(int entityId, Identifier genus) implements CustomPacketPayload {

    public static final Type<GenusSightingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ZombieMod.MOD_ID, "genus_sighting"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GenusSightingPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, GenusSightingPayload::entityId,
                    Identifier.STREAM_CODEC, GenusSightingPayload::genus,
                    GenusSightingPayload::new);

    @Override
    public Type<GenusSightingPayload> type() {
        return TYPE;
    }
}
