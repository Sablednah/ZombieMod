package com.sablednah.zombiemod.neoforge;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.core.Genus;
import com.sablednah.zombiemod.core.SummonRitual;

import org.slf4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Right-click a block holding the right item and something comes out of the ground. */
public final class RitualHandler {

    private static final Logger LOG = LogUtils.getLogger();

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (held.isEmpty()) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        Holder<net.minecraft.world.level.block.Block> block = state.getBlockHolder();
        Holder<net.minecraft.world.item.Item> item = held.getItemHolder();

        for (Holder.Reference<SummonRitual> holder :
                level.registryAccess().lookupOrThrow(ZombieModRegistries.RITUAL).listElements().toList()) {
            SummonRitual ritual = holder.value();
            if (!ritual.matches(block, item)) {
                continue;
            }
            perform(level, player, pos, ritual, holder.key().identifier().toString());
            // Cancel so the held item's own use doesn't also fire - otherwise summoning with a
            // flint and steel sets the ritual site on fire on the way out.
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
    }

    private void perform(ServerLevel level, ServerPlayer player, BlockPos pos, SummonRitual ritual,
            String ritualId) {
        var genusLookup = level.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS);
        var found = genusLookup.get(ritual.genus());
        if (found.isEmpty()) {
            LOG.warn("Ritual {} names genus {}, which is not loaded", ritualId, ritual.genus().identifier());
            return;
        }
        Holder.Reference<Genus> holder = found.get();
        Genus genus = holder.value();

        if (ritual.replaceBlock()) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        if (ritual.consume() && !player.isCreative()) {
            player.getMainHandItem().shrink(1);
        }

        for (int n = 0; n < ritual.count(); n++) {
            Entity created = genus.base().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (!(created instanceof Mob mob)) {
                LOG.warn("Ritual {} names genus {}, whose base is not a mob", ritualId, ritual.genus().identifier());
                return;
            }
            mob.snapTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    level.getRandom().nextFloat() * 360.0F, 0.0F);
            GenusApplier.assign(mob, holder);
            level.addFreshEntity(mob);
        }
    }
}
