package com.sablednah.zombiemod.neoforge;

import java.util.Optional;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.ZombieMod;
import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.core.Genus;
import com.sablednah.zombiemod.core.goal.GoalSpec;

import org.slf4j.Logger;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * Turns a {@link Genus} into changes on a live vanilla mob.
 *
 * <p>Two halves, and the split matters:
 * <ul>
 *   <li><b>Persistent</b> — attributes, equipment, name, scale. Written once when the mob is first
 *       spawned; Minecraft saves these in the entity's NBT, so they survive a restart on their own.
 *   <li><b>Transient</b> — the AI. Goals are objects on a {@code GoalSelector}, not NBT, so they
 *       are gone after a reload and have to be rebuilt every time the entity joins a level.
 * </ul>
 *
 * <p>The genus id itself goes in the entity's persistent data, which is how we know on reload what
 * this thing was meant to be. (The 1.8 plugin held its equivalent in a memory map and lost every
 * player-corpse zombie on restart — a known bug in its README. This is the fix, for free.)
 */
public final class GenusApplier {

    private static final Logger LOG = LogUtils.getLogger();

    /** Key inside {@code entity.getPersistentData()} holding the genus id. */
    public static final String GENUS_TAG = "zombiemod:genus";

    /**
     * Marks the entity as belonging to a genus and applies everything that persists. Call once, at
     * spawn.
     */
    public static void assign(Mob mob, Holder.Reference<Genus> holder) {
        Genus genus = holder.value();
        mob.getPersistentData().putString(GENUS_TAG, holder.key().identifier().toString());

        setBase(mob, Attributes.MAX_HEALTH, genus.health());
        genus.health().ifPresent(h -> mob.setHealth(h.floatValue()));
        setBase(mob, Attributes.ATTACK_DAMAGE, genus.damage());
        setBase(mob, Attributes.FOLLOW_RANGE, genus.followRange());

        // Speed is a multiplier on whatever the base mob runs at, matching the old config's
        // semantics — "speed: 1.15" meant 15% quicker than a zombie, not 1.15 blocks/tick.
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && genus.speed() != 1.0D) {
            speed.setBaseValue(speed.getBaseValue() * genus.speed());
        }

        // Size is a first-class synced attribute now. In 1.8 this meant hacking the bounding box.
        if (genus.scale() != 1.0D) {
            setBase(mob, Attributes.SCALE, Optional.of(genus.scale()));
        }

        genus.displayName().ifPresent(name -> {
            mob.setCustomName(name);
            mob.setCustomNameVisible(false); // visible on look, not permanently overhead
        });

        genus.armorColor().ifPresent(rgb -> dressIn(mob, rgb));

        // Something this distinctive shouldn't quietly despawn while the player walks away.
        mob.setPersistenceRequired();
    }

    /**
     * Rebuilds the AI. Safe to call on every level join — it clears first when the genus says so.
     */
    public static void applyAi(Mob mob, Genus genus) {
        if (genus.clearGoals()) {
            // The 1.8 code did this by reflecting a private list out of the goal selector. Now it
            // is one public call.
            mob.goalSelector.removeAllGoals(g -> true);
            mob.targetSelector.removeAllGoals(g -> true);
        }
        addAll(mob, genus.goals(), mob.goalSelector, "goals");
        addAll(mob, genus.targetGoals(), mob.targetSelector, "target_goals");

        // Abilities ride the goal selector too - see AbilityGoal for why. Priority is irrelevant
        // because they carry no control flags, but a high number keeps them visually last in any
        // debug dump of the goal list.
        for (com.sablednah.zombiemod.core.ability.Ability ability : genus.abilities()) {
            mob.goalSelector.addGoal(99, new AbilityGoal(mob, ability));
        }
    }

    private static void addAll(Mob mob, java.util.List<GoalSpec> specs,
            net.minecraft.world.entity.ai.goal.GoalSelector selector, String what) {
        for (GoalSpec spec : specs) {
            Goal goal = spec.build(mob);
            if (goal == null) {
                // Not fatal: some vanilla goals demand a PathfinderMob or a TamableAnimal. Skipping
                // one bad goal beats refusing to spawn the whole genus, but it must be visible.
                LOG.warn("[{}] {} '{}' does not apply to {} - skipped",
                        ZombieMod.MOD_ID, what, spec.type(), mob.getType().builtInRegistryHolder().key().identifier());
                continue;
            }
            selector.addGoal(spec.priority(), goal);
        }
    }

    /** Reads back the genus this entity was assigned, if it still resolves in the current datapacks. */
    public static Optional<Holder.Reference<Genus>> genusOf(Mob mob, ServerLevel level) {
        CompoundTag data = mob.getPersistentData();
        return data.getString(GENUS_TAG)
                .map(Identifier::tryParse)
                .flatMap(id -> id == null
                        ? Optional.empty()
                        : level.registryAccess()
                                .lookupOrThrow(ZombieModRegistries.GENUS)
                                .get(ResourceKey.create(ZombieModRegistries.GENUS, id)));
    }

    private static void setBase(Mob mob, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            Optional<Double> value) {
        if (value.isEmpty()) {
            return;
        }
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value.get());
        }
    }

    /**
     * Dyed leather, the poor mod's reskin: four slots of arbitrary RGB that every vanilla client
     * already knows how to render. Drop chance zeroed so a colour scheme doesn't become loot.
     */
    private static void dressIn(Mob mob, int rgb) {
        equip(mob, EquipmentSlot.HEAD, Items.LEATHER_HELMET, rgb);
        equip(mob, EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE, rgb);
        equip(mob, EquipmentSlot.LEGS, Items.LEATHER_LEGGINGS, rgb);
        equip(mob, EquipmentSlot.FEET, Items.LEATHER_BOOTS, rgb);
    }

    private static void equip(Mob mob, EquipmentSlot slot, net.minecraft.world.item.Item item, int rgb) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb));
        mob.setItemSlot(slot, stack);
        mob.setDropChance(slot, 0.0F);
    }

    private GenusApplier() {}
}
