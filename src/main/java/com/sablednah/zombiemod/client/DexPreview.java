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

    /** Ids for dolls, kept out of the range vanilla assigns. See {@link #build}. */
    private static final java.util.concurrent.atomic.AtomicInteger NEXT_DOLL_ID =
            new java.util.concurrent.atomic.AtomicInteger(-1);

    private DexPreview() {}

    /** Null when the genus's base is not something that can be drawn as a living thing. */
    public static LivingEntity of(Identifier id, Holder.Reference<Genus> holder) {
        LivingEntity cached = CACHE.get(id);
        // The Corpse's page is the mirror - you, dead: your face, your current gear, refreshed
        // every look because your gear changes. The Ghost keeps only your face; the rest of it is
        // its own pale chestplate over an invisible body, exactly as it walks the world.
        if (cached != null && isCorpse(id)) {
            copyViewerGear(cached);
        }
        // A cached doll belongs to the level it was built in. After a rejoin or a dimension change
        // it would be an entity holding a dead ClientLevel, which the renderer may follow into
        // anything - so a stale one is rebuilt rather than trusted.
        if (cached != null && cached.level() == Minecraft.getInstance().level) {
            return cached;
        }
        CACHE.remove(id);
        LivingEntity fresh = build(id, holder.value());
        if (fresh != null) {
            CACHE.put(id, fresh);
        }
        return fresh;
    }

    /** Dropped whenever the world changes, since the entities belong to a level. */
    public static void clear() {
        CACHE.clear();
    }

    /** The player-corpse genus wears the viewer; identified by id since the client cannot read
     * the server config that names it. A renamed corpse genus loses the mirror, nothing more. */
    private static boolean isCorpse(Identifier id) {
        return id.getNamespace().equals("zombiemod") && id.getPath().equals("player_zombie");
    }

    private static LivingEntity build(Identifier id, Genus genus) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        var created = genus.base().create(mc.level, EntitySpawnReason.COMMAND);
        if (!(created instanceof LivingEntity living)) {
            return null;
        }

        // Give the doll an entity id, or Minecraft 26.2 crashes the moment it draws one holding an
        // item: ItemModelResolver.updateForLiving asks for getId(), and Entity.getId() now throws
        // "Tried to access entity ID before ID assignment" rather than returning a default. A doll
        // is created and never added to a level, so nothing ever assigned it one.
        //
        // This is the same trap as the scale note below, in its second form: *a bare entity is not
        // a whole entity*. It has no dimensions refreshed and no id, and each missing piece surfaces
        // somewhere different and late. 1.21.11 tolerated the missing id; 26.2 does not.
        //
        // Negative and descending, so it can never collide with a real entity: vanilla's counter
        // starts at 0 and only climbs. The value itself is only ever a seed for item model variation
        // here, so any stable number does.
        living.setId(NEXT_DOLL_ID.getAndDecrement());

        // NO scale attribute, deliberately. renderEntityInInventoryFollowsAngle divides the render
        // state's boundingBoxHeight by its scale and then forces that scale to 1, so the attribute
        // never reaches the model - the doll's on-screen size comes from the `size` argument alone,
        // which DexScreen multiplies by genus.scale() instead.
        //
        // Worse than useless, though: setting it moved getScale() to 2.2 for a Tank while
        // getBbHeight() stayed at the unscaled 1.95, because a bare entity that was never added to
        // a level does not refresh its dimensions. The renderer's normalised height came out as
        // 1.95/2.2 = 0.886, so the translation that anchors the feet was computed for a doll less
        // than half the height of the one being drawn, and the big genera were cut off mid-torso at
        // a fixed line. Leaving the attribute alone makes getBbHeight() the true model height and
        // the arithmetic in DexScreen honest.
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
        genus.equipment().forEach(id.toString(), (slot, stack) -> living.setItemSlot(slot, stack.copy()));

        if ((genus.ghost() || isCorpse(id)) && Minecraft.getInstance().player != null) {
            ItemStack face = new ItemStack(Items.PLAYER_HEAD);
            face.set(DataComponents.PROFILE,
                    net.minecraft.world.item.component.ResolvableProfile.createResolved(
                            Minecraft.getInstance().player.getGameProfile()));
            living.setItemSlot(EquipmentSlot.HEAD, face);
        }
        if (isCorpse(id)) {
            copyViewerGear(living);
        }
        if (genus.ghost()) {
            // The one doll that IS shown invisible: a floating face over a pale chestplate is what
            // a Ghost actually looks like, and the plate should tell the truth. Safe on a doll -
            // it never ticks, so vanilla's effects pass never stomps the flag.
            living.setInvisible(true);
        }

        if (living instanceof Mob mob) {
            // Stops the mannequin looking asleep - the renderer reads these for head and body yaw.
            mob.setNoAi(true);
        }
        living.setYRot(0.0F);
        living.yBodyRot = 0.0F;
        living.yHeadRot = 0.0F;
        return living;
    }

    /** Everything but the head, which stays the corpse-face. */
    private static void copyViewerGear(LivingEntity doll) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot != EquipmentSlot.HEAD) {
                doll.setItemSlot(slot, player.getItemBySlot(slot).copy());
            }
        }
    }

    private static void dye(LivingEntity living, EquipmentSlot slot,
            net.minecraft.world.item.Item item, int rgb) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb));
        living.setItemSlot(slot, stack);
    }
}
