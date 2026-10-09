package com.sablednah.zombiemod.platform;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Entity calls that abilities and events make on mobs and players, whose names or signatures moved.
 *
 * <p><b>Why this exists.</b> On 26.3 {@code Entity.hurtMarked} became {@code syncVelocity},
 * {@code setInvulnerable} became {@code setPermanentlyInvulnerable}, {@code swing} takes a
 * {@code SwingAnimation}, and {@code randomTeleport} lost its no-avoidance overload. The first two
 * are renames with unchanged meaning - vanilla's Ravager and Hoglin knockback set
 * {@code syncVelocity} exactly where they set {@code hurtMarked} before.
 */
public final class EntityState {

    private EntityState() {}

    /**
     * Send this entity's new velocity to clients on the next tracker pass. Needed after
     * {@code setDeltaMovement} on anything a client predicts, or a shoved player never moves.
     *
     * <p><b>Differs per version.</b> On 26.3 this is {@code entity.syncVelocity = true}.
     */
    public static void syncVelocity(Entity entity) {
        entity.hurtMarked = true;
    }

    /**
     * Immune to ordinary damage for good (not the brief post-hit window).
     *
     * <p><b>Differs per version.</b> On 26.3 this is {@code entity.setPermanentlyInvulnerable(value)}.
     */
    public static void setInvulnerable(Entity entity, boolean value) {
        entity.setInvulnerable(value);
    }

    /**
     * Swing {@code hand}, shown to everyone tracking the entity and to the entity itself.
     *
     * <p><b>Differs per version.</b> On 26.3 this is {@code swing(hand, SwingAnimation.DEFAULT, true)}.
     */
    public static void swing(LivingEntity entity, InteractionHand hand) {
        entity.swing(hand, true);
    }

    /**
     * The enderman's teleport: walk down from the point to standable ground, refuse water and
     * collisions, and report whether it went. No particles.
     *
     * <p><b>Differs per version.</b> 26.3 requires a block predicate for places to refuse, and there
     * this is {@code randomTeleport(x, y, z, false, state -> false)} - refusing none, as this overload does.
     */
    public static boolean randomTeleport(LivingEntity entity, double x, double y, double z) {
        return entity.randomTeleport(x, y, z, false);
    }

    /**
     * Whether the entity carries a scoreboard tag (the {@code /tag} kind, not a registry tag).
     *
     * <p><b>Differs per version.</b> {@code getTags()} on 1.21.11; {@code entityTags()} on 26.1+.
     */
    public static boolean hasTag(Entity entity, String tag) {
        return entity.getTags().contains(tag);
    }
}
