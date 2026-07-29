package com.sablednah.zombiemod.neoforge;

import java.util.Optional;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.core.Genus;
import com.sablednah.zombiemod.core.ability.Convert;

import org.slf4j.Logger;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Raises a corpse as a genus. The decision is {@link Convert}'s; the doing is here. */
public final class Conversions {

    private static final Logger LOG = LogUtils.getLogger();

    private Conversions() {}

    public static boolean raise(ServerLevel level, Mob killer, LivingEntity victim, Convert convert) {
        if (!convert.accepts(victim)) {
            return false;
        }
        // Rate limit first, and cheapest. Independent of any world query, so it holds even when
        // several kills land in the same tick.
        long now = level.getGameTime();
        // -1 as "never", not Long.MIN_VALUE: now - Long.MIN_VALUE overflows to a negative number,
        // which passes a "< cooldown" test and silently blocks the first conversion forever.
        long last = killer.getPersistentData().getLongOr(Convert.COOLDOWN_TAG, -1L);
        if (last >= 0L && now - last < convert.cooldown()) {
            return false;
        }
        // Never convert something that is already one of ours - that is the chain reaction.
        if (victim instanceof Mob m && m.getPersistentData().getString(GenusApplier.GENUS_TAG).isPresent()) {
            return false;
        }
        if (killer.getRandom().nextFloat() >= convert.chance()) {
            return false;
        }

        HolderLookup.RegistryLookup<Genus> lookup =
                level.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS);

        // The corpse's own undead form, unless the genus overrides the base itself.
        EntityType<?> risenType = Convert.undeadFormOf(victim.getType());

        Optional<Holder.Reference<Genus>> chosen = convert.genus()
                .flatMap(id -> lookup.get(ResourceKey.create(ZombieModRegistries.GENUS, id)))
                .or(() -> weighted(lookup, risenType, level.getRandom()));

        if (chosen.isEmpty()) {
            // No genus wants this shape. Better to leave a plain corpse than to raise something
            // arbitrary that the pack author never asked for.
            return false;
        }
        Holder.Reference<Genus> holder = chosen.get();
        EntityType<?> base = holder.value().base();

        if (tooMany(level, victim, base, convert)) {
            return false;
        }

        var created = base.create(level, EntitySpawnReason.CONVERSION);
        if (!(created instanceof Mob risen)) {
            return false;
        }
        risen.snapTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), 0.0F);
        GenusApplier.assign(risen, holder);

        if (convert.inheritName() && victim.getCustomName() != null) {
            risen.setCustomName(victim.getCustomName());
            risen.setCustomNameVisible(victim.isCustomNameVisible());
        }
        if (convert.inheritEquipment()) {
            // Take what it was wearing, at zero drop chance. A converted guard should still look
            // like a guard - and its armour should not become farmable by killing it twice.
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
                    continue;
                }
                var worn = victim.getItemBySlot(slot);
                if (!worn.isEmpty() && risen.getItemBySlot(slot).isEmpty()) {
                    risen.setItemSlot(slot, worn.copy());
                    risen.setDropChance(slot, 0.0F);
                }
            }
        }

        Convert.announce(level, victim);
        level.addFreshEntity(risen);
        killer.getPersistentData().putLong(Convert.COOLDOWN_TAG, now);
        return true;
    }

    /** Genera whose base matches the risen shape, drawn by weight. Weight 0 stays opt-in only. */
    private static Optional<Holder.Reference<Genus>> weighted(
            HolderLookup.RegistryLookup<Genus> lookup, EntityType<?> base, RandomSource random) {
        int total = 0;
        for (Holder.Reference<Genus> holder : lookup.listElements().toList()) {
            if (holder.value().base() == base && holder.value().weight() > 0) {
                total += holder.value().weight();
            }
        }
        if (total <= 0) {
            return Optional.empty();
        }
        int roll = random.nextInt(total);
        for (Holder.Reference<Genus> holder : lookup.listElements().toList()) {
            if (holder.value().base() == base && holder.value().weight() > 0) {
                roll -= holder.value().weight();
                if (roll < 0) {
                    return Optional.of(holder);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean tooMany(ServerLevel level, LivingEntity at, EntityType<?> base, Convert convert) {
        int nearby = level.getEntitiesOfClass(Mob.class, at.getBoundingBox().inflate(convert.radius()),
                m -> m.getType() == base
                        && m.getPersistentData().getString(GenusApplier.GENUS_TAG).isPresent()).size();
        if (nearby >= convert.maxNearby()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("ZombieMod: conversion skipped, {} of {} already nearby", nearby, base);
            }
            return true;
        }
        return false;
    }
}
