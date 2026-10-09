package com.sablednah.zombiemod.net;

import com.sablednah.zombiemod.ZombieMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * "That dragon is the Zombie Dragon" - the one fact the client needs to draw it rotting.
 *
 * <p>The scoreboard tag never reaches a client and the genus lives in persistent data, which is not
 * synced either, so the renderer has nothing to go on without this. Sent when the dragon rises and
 * to anyone who starts tracking it afterwards. A vanilla client never agrees the channel and simply
 * sees the vanilla dragon, which is the right way for a cosmetic to fail.
 *
 * @param entityId the dragon's network id in the receiver's level
 */
public record ZombieDragonPayload(int entityId) implements CustomPacketPayload {

    public static final Type<ZombieDragonPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ZombieMod.MOD_ID, "zombie_dragon"));

    public static final StreamCodec<ByteBuf, ZombieDragonPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ZombieDragonPayload::entityId, ZombieDragonPayload::new);

    @Override
    public Type<ZombieDragonPayload> type() {
        return TYPE;
    }
}
