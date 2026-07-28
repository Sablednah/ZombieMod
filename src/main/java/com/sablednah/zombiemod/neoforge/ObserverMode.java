package com.sablednah.zombiemod.neoforge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Take no damage while staying a completely normal target.
 *
 * <p>Deliberately not vanilla invulnerability or creative mode. Both work by making the player
 * <em>uninteresting</em>: {@code LivingEntity.canBeSeenAsEnemy()} is {@code !isInvulnerable() &&
 * canBeSeenByAnyone()}, and every targeting goal runs through it, so an invulnerable player is one
 * that no zombie will ever walk towards. That is exactly the wrong tool for watching what zombies
 * do — which is what this mod is for.
 *
 * <p>So nothing about the player changes. They are targeted, chased, swung at, knocked back and
 * teleported behind exactly as before; the damage is simply cancelled on arrival. Knockback still
 * lands, so a shockwave still throws you across the room — which is worth seeing.
 *
 * <p>Stored under NeoForge's persistent tag so it survives death and relogging. A flag you have to
 * re-enable after every server restart would be useless during a deploy-heavy testing session.
 */
final class ObserverMode {

    private static final String KEY = "zombiemod:observer";

    private ObserverMode() {}

    static boolean isOn(Player player) {
        return persisted(player).getBooleanOr(KEY, false);
    }

    static void set(Player player, boolean on) {
        if (on) {
            persisted(player).putBoolean(KEY, true);
        } else {
            persisted(player).remove(KEY);
        }
    }

    private static CompoundTag persisted(Player player) {
        CompoundTag data = player.getPersistentData();
        return data.getCompound(Player.PERSISTED_NBT_TAG).orElseGet(() -> {
            CompoundTag created = new CompoundTag();
            data.put(Player.PERSISTED_NBT_TAG, created);
            return created;
        });
    }
}
