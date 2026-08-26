package com.sablednah.zombiemod.net;

import java.util.function.Supplier;

import com.sablednah.zombiemod.ZombieMod;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The only way anything in this mod talks to a client.
 *
 * <p><b>{@code optional()} makes the handshake tolerant. It does not make sends safe.</b> That
 * distinction is the whole reason this class exists. Optional payloads let a vanilla client complete
 * the connection, but {@link PacketDistributor#sendToPlayer} still <em>throws</em> — synchronously,
 * on the server thread — if you hand it a payload the receiver never negotiated:
 *
 * <pre>Payload zombiemod:dex may not be sent to the client!</pre>
 *
 * <p>Thrown out of whatever handler you were in. From a login handler that means vanilla's own login
 * flow fails behind it and the player is kicked with "Invalid player data" — a cosmetic feature
 * taking down the ability to join at all. (Diagnosed the hard way in LegendQuest, on this same
 * NeoForge version, by testing with a genuinely unmodded client. Notes copied here so it only has to
 * be learnt once.)
 *
 * <p>So: one choke point, and everything goes through it. Not a rule to remember at each call site —
 * a shape that makes the unguarded version awkward to write.
 */
public final class Net {

    private Net() {}

    /** {@code optional()} first: the registrar returns derived instances, so it must precede them. */
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").optional();
        // The client half is excluded from this branch's build until the dex screen is ported to
        // 26.x, so there is nothing to hand a payload to and the channel is deliberately not
        // registered. Net.listening then answers false for everyone and every player gets the
        // written-book dex - the same path a vanilla client takes. Restore this together with the
        // screen; see docs/MULTIVERSION.md.
        if (Boolean.FALSE) {
            registrar.playToClient(DexPayload.TYPE, DexPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> { }));
        }
    }

    /** Send, if this player is one of the few who can hear it. */
    public static void sendIfAble(ServerPlayer player, CustomPacketPayload payload) {
        if (listening(player, payload.type())) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /**
     * Build and send, if the player can hear it — the supplier is not called otherwise.
     *
     * <p>For anything that costs something to assemble. The dex walks the whole genus registry, and
     * doing that for every vanilla player on every kill would be a real cost paid to produce a
     * message that is then thrown away.
     */
    public static void sendIfAble(ServerPlayer player, CustomPacketPayload.Type<?> type,
            Supplier<CustomPacketPayload> payload) {
        if (listening(player, type)) {
            PacketDistributor.sendToPlayer(player, payload.get());
        }
    }

    /** True if this player has a client that speaks {@link ZombieMod#MOD_ID}. */
    public static boolean listening(ServerPlayer player) {
        return listening(player, DexPayload.TYPE);
    }

    /**
     * Null-checks the connection as well as the channel.
     *
     * <p>A real player always has one by the time anything here runs, but fake players exist - other
     * mods use them, and so do this repo's own probes - and an NPE from a cosmetic feature would take
     * down whatever event it fired from, which is the failure this whole class exists to avoid.
     */
    private static boolean listening(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection != null && player.connection.hasChannel(type);
    }
}
