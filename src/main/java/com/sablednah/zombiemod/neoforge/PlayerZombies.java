package com.sablednah.zombiemod.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.ZombieModConfig;
import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.core.Genus;

import org.slf4j.Logger;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * When a player dies, their corpse gets up wearing their face.
 *
 * <p>The 1.8 plugin's signature feature, and the reason people remembered it. Two things about it
 * are much better now than they were:
 *
 * <ul>
 *   <li>The corpse wears the dead player's <b>actual skin</b>, via a player head built from their
 *       resolved game profile — visible on a completely vanilla client. The original needed Spout
 *       installed on every client to manage that.
 *   <li>It <b>survives a restart</b>. The original tracked corpses in a memory map keyed by chunk,
 *       so every player zombie in the world vanished when the server went down — a known bug listed
 *       in its own README. Here the genus id and the carried items live in the entity's persistent
 *       data, so the corpse is just an entity and saves like one.
 * </ul>
 *
 * <p>Carried items are stored on the corpse and re-dropped when it dies, so your things are
 * recoverable — the point is to make death a fight, not a loss.
 */
public final class PlayerZombies {

    private static final Logger LOG = LogUtils.getLogger();
    private static final String ITEMS_TAG = "zombiemod:corpse_items";
    /** Links a live corpse back to its ledger entry, so a normal death can settle the record. */
    private static final String LEDGER_TAG = "zombiemod:corpse_id";

    @SubscribeEvent
    public void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            onPlayerDeath(player, event);
        } else if (event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel level) {
            dropCarried(level, mob, event);
        }
    }

    // ------------------------------------------------------------------ the player dies

    private void onPlayerDeath(ServerPlayer player, LivingDropsEvent event) {
        if (!ZombieModConfig.PLAYER_ZOMBIES.get() || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        Identifier genusId = Identifier.tryParse(ZombieModConfig.PLAYER_ZOMBIE_GENUS.get());
        if (genusId == null) {
            return;
        }
        var found = level.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS)
                .get(ResourceKey.create(ZombieModRegistries.GENUS, genusId));
        if (found.isEmpty()) {
            LOG.warn("Player zombies are on but genus {} is not loaded - no corpse raised", genusId);
            return;
        }
        Holder.Reference<Genus> holder = found.get();

        Entity created = holder.value().base().create(level, EntitySpawnReason.EVENT);
        if (!(created instanceof Mob corpse)) {
            return;
        }
        corpse.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        GenusApplier.assign(corpse, holder);

        // Through the same formatter, so a server owner can colour "Corpse %P" like anything else.
        corpse.setCustomName(com.sablednah.zombiemod.core.Announce.format(
                ZombieModConfig.PLAYER_ZOMBIE_NAME.get().replace("%P", player.getName().getString())));
        corpse.setCustomNameVisible(true);

        // Resolved here, because the player is right in front of us and their profile is complete.
        face(corpse, ResolvableProfile.createResolved(player.getGameProfile()));

        List<ItemStack> carried = new ArrayList<>();
        if (ZombieModConfig.PLAYER_ZOMBIE_TAKES_ITEMS.get()) {
            for (ItemEntity drop : event.getDrops()) {
                carried.add(drop.getItem().copy());
            }
            store(level, corpse, carried);
            // Taken, not copied - the whole point is that you have to go and get them.
            event.getDrops().clear();
            wearVisibleArmour(corpse, carried);
        }

        // Write it down before the corpse exists in the world. Everything that went wrong with the
        // 1.8 version went wrong *after* this point - despawns, ravines, grinders - and a record
        // made only on a clean death would have been no use in exactly those cases.
        UUID ledgerId = UUID.randomUUID();
        corpse.getPersistentData().putString(LEDGER_TAG, ledgerId.toString());
        CorpseLedger.get(level).record(new CorpseLedger.Entry(ledgerId, player.getUUID(),
                player.getName().getString(), level.dimension().identifier().toString(),
                player.blockPosition().getX(), player.blockPosition().getY(), player.blockPosition().getZ(),
                level.getGameTime() / 24000L, carried, false));

        level.addFreshEntity(corpse);
        LOG.info("ZombieMod: {} rose at {} {} {}", corpse.getName().getString(),
                (int) player.getX(), (int) player.getY(), (int) player.getZ());
    }

    /**
     * Put any armour among the carried items on the corpse for looks, at zero drop chance.
     *
     * <p>Copies, deliberately: the originals stay in the carried list and drop from there. Equipping
     * the real stacks would hand the player their armour twice.
     */
    private static void wearVisibleArmour(Mob corpse, List<ItemStack> carried) {
        for (ItemStack stack : carried) {
            EquipmentSlot slot = corpse.getEquipmentSlotForItem(stack);
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && slot != EquipmentSlot.HEAD
                    && corpse.getItemBySlot(slot).isEmpty()) {
                corpse.setItemSlot(slot, stack.copy());
                corpse.setDropChance(slot, 0.0F);
            }
        }
    }

    // ------------------------------------------------------------------ the corpse dies

    private void dropCarried(ServerLevel level, Mob mob, LivingDropsEvent event) {
        // Settle the ledger whether or not it was carrying anything - a corpse that died properly
        // owes nothing, and leaving it listed would have admins handing out duplicates.
        mob.getPersistentData().getString(LEDGER_TAG)
                .map(id -> {
                    try {
                        return UUID.fromString(id);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .ifPresent(id -> CorpseLedger.get(level).claim(id));

        List<ItemStack> carried = read(level, mob);
        if (carried.isEmpty()) {
            return;
        }
        for (ItemStack stack : carried) {
            event.getDrops().add(new ItemEntity(level, mob.getX(), mob.getY() + 0.5D, mob.getZ(), stack));
        }
        mob.getPersistentData().remove(ITEMS_TAG);
    }

    /** Re-attach a ledger entry's items to a rebuilt corpse, so recovery is a real second chance. */
    static void rebuild(ServerLevel level, Mob corpse, CorpseLedger.Entry entry) {
        corpse.getPersistentData().putString(LEDGER_TAG, entry.id().toString());
        // The face and the armour, not just the pockets. A rebuilt corpse used to come back bald
        // and unarmoured while carrying everything, which reads as the wrong corpse - and the whole
        // point of the player zombie is that it is recognisably *you*. By uuid rather than name so
        // it still finds the right skin for somebody who has since renamed.
        face(corpse, ResolvableProfile.createUnresolved(entry.player()));
        if (!entry.items().isEmpty()) {
            store(level, corpse, entry.items());
            wearVisibleArmour(corpse, entry.items());
        }
    }

    /** Their actual face, on a vanilla client. The 1.8 version needed Spout for this. */
    private static void face(Mob corpse, ResolvableProfile profile) {
        ItemStack head = new ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD);
        head.set(DataComponents.PROFILE, profile);
        corpse.setItemSlot(EquipmentSlot.HEAD, head);
        corpse.setDropChance(EquipmentSlot.HEAD, 0.0F);
    }

    // ------------------------------------------------------------------ storage

    private static void store(ServerLevel level, Mob mob, List<ItemStack> items) {
        RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        ListTag list = new ListTag();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(list::add);
            }
        }
        mob.getPersistentData().put(ITEMS_TAG, list);
    }

    private static List<ItemStack> read(ServerLevel level, Mob mob) {
        CompoundTag data = mob.getPersistentData();
        ListTag list = data.getList(ITEMS_TAG).orElse(null);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        List<ItemStack> out = new ArrayList<>();
        for (Tag tag : list) {
            ItemStack.CODEC.parse(ops, tag).result().ifPresent(out::add);
        }
        return out;
    }
}
