package com.sablednah.zombiemod.neoforge;

import com.sablednah.zombiemod.core.LootSpec;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Genus-specific drops, rolled from a loot table on death. */
public final class LootHandler {

    @SubscribeEvent
    public void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (mob.getPersistentData().getString(GenusApplier.GENUS_TAG).isEmpty()) {
            return;
        }
        GenusApplier.genusOf(mob, level)
                .flatMap(holder -> holder.value().loot())
                .ifPresent(loot -> roll(level, mob, event, loot));
    }

    private void roll(ServerLevel level, Mob mob, LivingDropsEvent event, LootSpec loot) {
        // Loot tables are not in registryAccess() - they are a reloadable datapack registry reached
        // through the server. Looking in the wrong place fails with "Missing registry:
        // minecraft:loot_table", which reads like the table is absent when it is the lookup at fault.
        LootTable table = level.getServer().reloadableRegistries().getLootTable(loot.table());

        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, mob)
                .withParameter(LootContextParams.ORIGIN, mob.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, event.getSource())
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, event.getSource().getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, event.getSource().getDirectEntity())
                .create(LootContextParamSets.ENTITY);

        if (loot.replace()) {
            event.getDrops().clear();
        }

        Vec3 at = mob.position();
        table.getRandomItems(params, mob.getRandom()).forEach(stack ->
                event.getDrops().add(new ItemEntity(level, at.x, at.y + 0.5D, at.z, stack)));
    }
}
