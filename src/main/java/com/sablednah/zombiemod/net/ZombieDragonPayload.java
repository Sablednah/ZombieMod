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
 * <p>Two states, because the fake death is drawn too: {@code risen == false} is "it is lying dead -
 * draw vanilla's death beams and dissolve", which vanilla's renderer only does for a dragon whose
 * health is actually zero.
 *
 * @param entityId the dragon's network id in the receiver's level
 * @param risen    false while it lies dead, true once it is the Zombie Dragon
 */
public record ZombieDragonPayload(int entityId, boolean risen) implements CustomPacketPayload {

    public static final Type<ZombieDragonPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ZombieMod.MOD_ID, "zombie_dragon"));

    public static final StreamCodec<ByteBuf, ZombieDragonPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ZombieDragonPayload::entityId,
                    ByteBufCodecs.BOOL, ZombieDragonPayload::risen, ZombieDragonPayload::new);

    @Override
    public Type<ZombieDragonPayload> type() {
        return TYPE;
    }
}
