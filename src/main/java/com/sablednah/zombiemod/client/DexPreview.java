package com.sablednah.zombiemod.client;

import java.util.HashMap;
import java.util.Map;

import com.sablednah.zombiemod.core.Genus;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * A live example of a genus, for the bestiary page to draw.
 *
 * <p>Built on the client from the client's own copy of the genus registry — datapack registries sync,
 * so nothing needs sending for this. It is dressed rather than <em>applied</em>: only the parts a
 * renderer reads, and deliberately not {@code GenusApplier}, which writes persistent data, attaches
 * goals and expects a server. A mannequin, not a monster.
 *
 * <p>Cached per genus, because a screen redraw is sixty times a second and building an entity each
 * frame would be absurd.
 */
public final class DexPreview {

    private static final Map<Identifier, LivingEntity> CACHE = new HashMap<>();

    private DexPreview() {}

    /** Null when the genus's base is not something that can be drawn as a living thing. */
    public static LivingEntity of(Identifier id, Holder.Reference<Genus> holder) {
        return CACHE.computeIfAbsent(id, k -> build(holder.value()));
    }

    /** Dropped whenever the world changes, since the entities belong to a level. */
    public static void clear() {
        CACHE.clear();
    }

    private static LivingEntity build(Genus genus) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        var created = genus.base().create(mc.level, EntitySpawnReason.COMMAND);
        if (!(created instanceof LivingEntity living)) {
            return null;
        }

        if (genus.scale() != 1.0D) {
            var scale = living.getAttribute(Attributes.SCALE);
            if (scale != null) {
                scale.setBaseValue(genus.scale());
            }
        }
        if (genus.baby() && living instanceof net.minecraft.world.entity.monster.zombie.Zombie zombie) {
            zombie.setBaby(true);
        }
        // Invisible is honoured everywhere else, and would make for a blank frame here. The page is
        // the one place you are meant to be able to look at the thing.
        genus.armorColor().ifPresent(rgb -> {
            dye(living, EquipmentSlot.HEAD, Items.LEATHER_HELMET, rgb);
            dye(living, EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE, rgb);
            dye(living, EquipmentSlot.LEGS, Items.LEATHER_LEGGINGS, rgb);
            dye(living, EquipmentSlot.FEET, Items.LEATHER_BOOTS, rgb);
        });
        genus.head().ifPresent(profile -> {
            ItemStack skull = new ItemStack(Items.PLAYER_HEAD);
            skull.set(DataComponents.PROFILE, profile);
            living.setItemSlot(EquipmentSlot.HEAD, skull);
        });
        genus.equipment().forEach((slot, stack) -> living.setItemSlot(slot, stack.copy()));

        if (living instanceof Mob mob) {
            // Stops the mannequin looking asleep - the renderer reads these for head and body yaw.
            mob.setNoAi(true);
        }
        living.setYRot(0.0F);
        living.yBodyRot = 0.0F;
        living.yHeadRot = 0.0F;
        return living;
    }

    private static void dye(LivingEntity living, EquipmentSlot slot,
            net.minecraft.world.item.Item item, int rgb) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb));
        living.setItemSlot(slot, stack);
    }
}
