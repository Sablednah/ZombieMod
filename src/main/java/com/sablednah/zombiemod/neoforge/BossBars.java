package com.sablednah.zombiemod.neoforge;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sablednah.zombiemod.core.BossSpec;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/**
 * One boss bar per live boss mob.
 *
 * <p>Keyed on entity id rather than held on the entity, because a boss bar is server-side UI with no
 * business being saved into the mob's NBT — it has to be rebuilt on load anyway, exactly like goals.
 * MobHealth-NeoForge keys its bars the same way.
 *
 * <p>Bars are removed when the entity leaves the level for any reason — death, despawn, chunk
 * unload, dimension change. Missing any of those leaves a bar stuck on someone's screen with no way
 * to clear it, which is why the hook is on leaving rather than on dying.
 */
final class BossBars {

    private static final Map<Integer, ServerBossEvent> BARS = new ConcurrentHashMap<>();

    private BossBars() {}

    /** Create or refresh the bar for this mob: title, progress, and who can currently see it. */
    static void update(Mob mob, BossSpec spec) {
        ServerBossEvent bar = BARS.computeIfAbsent(mob.getId(), id -> {
            ServerBossEvent created = new ServerBossEvent(title(mob, spec), spec.color(), spec.overlay());
            created.setDarkenScreen(spec.darkenSky());
            created.setPlayBossMusic(spec.bossMusic());
            created.setCreateWorldFog(spec.fog());
            return created;
        });

        float max = mob.getMaxHealth();
        bar.setProgress(max <= 0.0F ? 0.0F : Math.clamp(mob.getHealth() / max, 0.0F, 1.0F));

        // Re-sync viewers every update so the bar follows players in and out of range. Cheap at the
        // rate this is called, and it means no separate bookkeeping when someone walks away.
        double rangeSqr = spec.range() * spec.range();
        for (Iterator<ServerPlayer> it = bar.getPlayers().iterator(); it.hasNext();) {
            ServerPlayer viewer = it.next();
            if (viewer.isRemoved() || viewer.level() != mob.level()
                    || viewer.distanceToSqr(mob) > rangeSqr) {
                it.remove();
                bar.removePlayer(viewer);
            }
        }
        if (mob.level() instanceof net.minecraft.server.level.ServerLevel level) {
            for (ServerPlayer player : level.players()) {
                if (!player.isSpectator() && player.distanceToSqr(mob) <= rangeSqr) {
                    bar.addPlayer(player);
                }
            }
        }
    }

    /** Drop the bar for this entity id, if it has one. */
    static void remove(int entityId) {
        ServerBossEvent bar = BARS.remove(entityId);
        if (bar != null) {
            bar.removeAllPlayers();
            bar.setVisible(false);
        }
    }

    private static Component title(Mob mob, BossSpec spec) {
        if (spec.title().isPresent()) {
            return Component.literal(spec.title().get());
        }
        return mob.getCustomName() != null ? mob.getCustomName() : mob.getType().getDescription();
    }
}
