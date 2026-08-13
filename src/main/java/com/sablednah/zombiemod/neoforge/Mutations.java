package com.sablednah.zombiemod.neoforge;

import java.util.Optional;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.core.Genus;
import com.sablednah.zombiemod.core.mutate.MutationSpec;

import org.slf4j.Logger;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Turning one monster into another.
 *
 * <p><b>Why this replaces the entity rather than re-dressing it.</b> Applying a second genus over a
 * live one looks cheaper and is wrong in ways that would be very hard to diagnose from a bug report.
 * {@code GenusApplier.assign} is a set of "if the JSON says so" writes, not a reset: speed is
 * <em>multiplied</em> into the existing base value, so mutating in place would compound it every
 * time; equipment slots the new genus does not mention keep the old genus's gear; a scale of 1.0 is
 * skipped rather than applied, so a shrinking mutation would silently keep the old size. Building a
 * fresh mob from the vanilla baseline and assigning the new genus once is the only version that is
 * obviously correct, and correctness is worth more here than the entity id.
 *
 * <p>What that costs, and what is carried across so it costs nothing that matters: position and
 * facing, health as a fraction, current target, fire, and the whole persistent-data tag — which is
 * how a horde member stays a horde member. The horde's own roster is told about the swap directly,
 * since it tracks members by identity.
 */
public final class Mutations {

    private static final Logger LOG = LogUtils.getLogger();

    /** When this mob last changed, so a mutation cannot chatter or ping-pong. */
    static final String MUTATED_TAG = "zombiemod:mutated_at";

    /**
     * Minimum ticks between one mob's mutations.
     *
     * <p>Not configurable: this is a correctness guard rather than a tuning knob. Two genera that
     * name each other, or one whose trigger is still true the instant it arrives, would otherwise
     * swap entities every tick forever.
     */
    private static final int COOLDOWN = 60;

    /**
     * @return the mob that exists afterwards, or empty if nothing happened.
     */
    public static Optional<Mob> mutate(ServerLevel level, Mob mob, MutationSpec spec) {
        long now = level.getGameTime();
        long last = mob.getPersistentData().getLongOr(MUTATED_TAG, -1L);
        if (last >= 0 && now - last < COOLDOWN) {
            return Optional.empty();
        }

        Optional<Holder.Reference<Genus>> target = level.registryAccess()
                .lookupOrThrow(ZombieModRegistries.GENUS)
                .get(ResourceKey.create(ZombieModRegistries.GENUS, spec.into()));
        if (target.isEmpty()) {
            LOG.warn("ZombieMod: mutation target '{}' does not exist - ignored", spec.into());
            return Optional.empty();
        }

        // Becoming what you already are is a datapack mistake, not an event.
        String current = mob.getPersistentData().getString(GenusApplier.GENUS_TAG).orElse(null);
        if (spec.into().toString().equals(current)) {
            return Optional.empty();
        }

        Mob replacement = build(level, mob, target.get(), spec);
        if (replacement == null) {
            return Optional.empty();
        }

        // Show it. A monster that silently becomes a different monster reads as one thing being
        // swapped for another by a bug, which is exactly what it is - the particles are what make
        // it legible as a mutation instead.
        level.sendParticles(spec.particle(), mob.getX(), mob.getY() + mob.getBbHeight() * 0.5D,
                mob.getZ(), 30, 0.4D, 0.6D, 0.4D, 0.02D);
        spec.sound().ifPresent(sound -> level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                sound.value(), SoundSource.HOSTILE, 1.2F, 0.8F));

        UpdateHorde(mob, replacement);
        // Discarded alive, so vanilla's own scoreboard cleanup will not fire for it.
        GenusApplier.clearGlowTeam(mob);
        mob.discard();
        level.addFreshEntity(replacement);
        return Optional.of(replacement);
    }

    private static Mob build(ServerLevel level, Mob old, Holder.Reference<Genus> genus, MutationSpec spec) {
        Entity created = genus.value().base().create(level, EntitySpawnReason.CONVERSION);
        if (!(created instanceof Mob fresh)) {
            LOG.warn("ZombieMod: mutation target '{}' has a base that is not a mob - ignored", spec.into());
            return null;
        }
        fresh.snapTo(old.getX(), old.getY(), old.getZ(), old.getYRot(), old.getXRot());

        // Carry the persistent tag across before assigning, so anything the old mob was a member of
        // still finds it, and so GenusApplier can overwrite the genus id cleanly.
        fresh.getPersistentData().merge(old.getPersistentData());
        GenusApplier.assign(fresh, genus);
        GenusApplier.applyAi(fresh, genus.value());
        fresh.getPersistentData().putLong(MUTATED_TAG, level.getGameTime());

        if (spec.keepHealth()) {
            // As a fraction, not a number: mutating into something with triple the health should not
            // be a free heal, and mutating into something frailer should not be an instant death.
            float fraction = Math.clamp(old.getHealth() / Math.max(1.0F, old.getMaxHealth()), 0.05F, 1.0F);
            fresh.setHealth(Math.max(1.0F, fresh.getMaxHealth() * fraction));
        }
        if (old.getTarget() instanceof LivingEntity victim) {
            fresh.setTarget(victim);
        }
        fresh.setRemainingFireTicks(old.getRemainingFireTicks());
        fresh.setPersistenceRequired();
        return fresh;
    }

    /** Keep a horde's roster pointing at the thing that is actually standing there. */
    private static void UpdateHorde(Mob old, Mob fresh) {
        if (old.getPersistentData().getString(HordeDirector.HORDE_TAG).isPresent()) {
            HordeDirector.replaceMember(old.getUUID(), fresh.getUUID());
        }
    }

    private Mutations() {}
}
