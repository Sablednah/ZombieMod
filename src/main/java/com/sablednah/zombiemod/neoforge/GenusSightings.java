package com.sablednah.zombiemod.neoforge;

import com.sablednah.zombiemod.net.GenusSightingPayload;
import com.sablednah.zombiemod.net.Net;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Tells modded clients which genus each mob in view is - see {@link GenusSightingPayload}.
 *
 * <p>One small packet per genus mob per player, only to clients that have the channel. Nothing a
 * client is told here is a secret: the mob is in front of them, wearing its face and its name.
 */
public final class GenusSightings {

    /** A genus mob coming into view. */
    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Mob mob && event.getEntity() instanceof ServerPlayer player) {
            genus(mob).ifPresent(id -> Net.sendIfAble(player, new GenusSightingPayload(mob.getId(), id)));
        }
    }

    /**
     * A mob already in view has just been given a genus (the API, a summon): nobody will start
     * tracking it again, so tell whoever can see it now. Players in the same level within entity
     * view range are close enough; one too many is harmless, the client keys by entity id.
     */
    static void announce(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        genus(mob).ifPresent(id -> {
            double range = level.getServer().getPlayerList().getViewDistance() * 16.0D;
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(mob) <= range * range) {
                    Net.sendIfAble(player, new GenusSightingPayload(mob.getId(), id));
                }
            }
        });
    }

    private static java.util.Optional<Identifier> genus(Mob mob) {
        return mob.getPersistentData().getString(GenusApplier.GENUS_TAG).map(Identifier::tryParse);
    }
}
