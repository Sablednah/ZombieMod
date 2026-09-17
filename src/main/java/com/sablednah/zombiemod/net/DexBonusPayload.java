package com.sablednah.zombiemod.net;

import java.util.List;

import com.sablednah.zombiemod.ZombieMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Which genera in the dex are bonus entries - seasonal, starred, and outside the total.
 *
 * <p><b>A payload of its own, and that is the whole design.</b> The obvious move was a sixth field
 * on {@link DexPayload.Entry}, and it cannot be made: a payload's shape is fixed for as long as any
 * released client speaks it. Lengthen it and a 3.5.1 client mis-decodes the first dex it is sent.
 * Bump the registrar version to warn it off and NeoForge does something worse -
 * {@code optional()} forgives a channel being <em>absent</em>, not a version that differs, so a
 * mismatch fails negotiation and the player cannot join at all.
 *
 * <p>Absence is the one thing that is forgiven, so new information goes on a new channel. A client
 * too old to have it never agrees it, {@code sendIfAble} sees no channel and sends nothing, and that
 * player gets the dex they always had with seasonal genera counted like any other. A new client on
 * an old server is the mirror image: no list arrives, nothing is starred. Both directions degrade
 * to "as before", which is the only acceptable way for a cosmetic to fail.
 *
 * @param genera ids of the bonus genera currently in this player's dex
 */
public record DexBonusPayload(List<Identifier> genera) implements CustomPacketPayload {

    public static final Type<DexBonusPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ZombieMod.MOD_ID, "dex_bonus"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DexBonusPayload> STREAM_CODEC =
            StreamCodec.composite(
                    Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), DexBonusPayload::genera,
                    DexBonusPayload::new);

    @Override
    public Type<DexBonusPayload> type() {
        return TYPE;
    }
}
