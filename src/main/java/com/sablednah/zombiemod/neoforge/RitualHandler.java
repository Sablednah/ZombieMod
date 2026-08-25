package com.sablednah.zombiemod.neoforge;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.platform.Items;
import com.sablednah.zombiemod.platform.BlockTypes;
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

        Holder<net.minecraft.world.level.block.Block> block = BlockTypes.holderOf(state);
        Holder<net.minecraft.world.item.Item> item = Items.holderOf(held);

        for (Holder.Reference<SummonRitual> holder :
                level.registryAccess().lookupOrThrow(ZombieModRegistries.RITUAL).listElements().toList()) {
            SummonRitual ritual = holder.value();
            if (!ritual.matches(block, item)) {
                continue;
            }
            int rotation = matchPattern(level, pos, ritual);
            if (rotation < 0) {
                continue; // right block, right item, wrong structure
            }
            perform(level, player, pos, ritual, holder.key().identifier().toString(), rotation);
            // Cancel so the held item's own use doesn't also fire - otherwise summoning with a
            // flint and steel sets the ritual site on fire on the way out.
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
    }

    /**
     * @return the rotation (0-3, quarter turns) the structure was found in, or -1 if it is absent.
     *         A ritual with no pattern always matches at rotation 0.
     */
    static int matchPattern(ServerLevel level, BlockPos pos, SummonRitual ritual) {
        if (ritual.pattern().isEmpty()) {
            return 0;
        }
        // Try all four horizontal orientations, so the shape can be built facing any way. Nobody
        // wants to discover their ritual only works pointing north.
        for (int rotation = 0; rotation < 4; rotation++) {
            boolean matched = true;
            for (SummonRitual.PatternBlock required : ritual.pattern()) {
                BlockPos at = pos.offset(rotate(required.x(), required.y(), required.z(), rotation));
                if (!required.block().contains(BlockTypes.holderOf(level.getBlockState(at)))) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return rotation;
            }
        }
        return -1;
    }

    /**
     * Quarter turns about the vertical axis.
     *
     * <p>Rotation is horizontal, so y passes through untouched — an earlier version took only
     * (x, z) and hardcoded y to 0, which silently discarded every vertical offset in every pattern.
     * Patterns describing blocks above or below the anchor could never match.
     */
    static net.minecraft.core.Vec3i rotate(int x, int y, int z, int rotation) {
        return switch (rotation) {
            case 1 -> new net.minecraft.core.Vec3i(-z, y, x);
            case 2 -> new net.minecraft.core.Vec3i(-x, y, -z);
            case 3 -> new net.minecraft.core.Vec3i(z, y, -x);
            default -> new net.minecraft.core.Vec3i(x, y, z);
        };
    }

    private void perform(ServerLevel level, ServerPlayer player, BlockPos pos, SummonRitual ritual,
            String ritualId, int rotation) {
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
            // Consume the structure too. Leaving it standing would let one build summon endlessly,
            // which is fine for a trash mob and wrong for a boss.
            for (SummonRitual.PatternBlock part : ritual.pattern()) {
                BlockPos at = pos.offset(rotate(part.x(), part.y(), part.z(), rotation));
                if (part.block().contains(BlockTypes.holderOf(level.getBlockState(at)))) {
                    level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                }
            }
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
            mob.snapTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    level.getRandom().nextFloat() * 360.0F, 0.0F);
            GenusApplier.assign(mob, holder);
            level.addFreshEntity(mob);
        }
    }
}
