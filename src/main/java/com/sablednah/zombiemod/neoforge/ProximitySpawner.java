package com.sablednah.zombiemod.neoforge;

import java.util.ArrayList;
import java.util.List;

import com.sablednah.zombiemod.ZombieModConfig;
import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.compat.FtbChunks;
import com.sablednah.zombiemod.core.Genus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Puts zombies near players, rather than only decorating the ones vanilla decided to make.
 *
 * <p>The 1.8 plugin's {@code ProximitySystems}, and the difference between a world that is populated
 * and one that feels occupied. Genera otherwise only ever claim spawns vanilla was already going to
 * produce, which means the mod can change *what* you meet but never *how much* is out there.
 *
 * <p>Off by default. It is the only feature here that adds mobs beyond vanilla's own rules, and a mob
 * pack should not quietly change how many things are hunting someone.
 *
 * <p>Each candidate still has to pass the genus's own {@link com.sablednah.zombiemod.core.spawn.SpawnRules}
 * at the chosen position, so a Frost still only appears in the snow and a Stalker still only in the
 * dark. This adds opportunities; it does not bypass the rules.
 */
public final class ProximitySpawner {

    private int ticks;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!ZombieModConfig.ENABLED.get() || !ZombieModConfig.PROXIMITY.get()) {
            return;
        }
        if (++ticks < ZombieModConfig.PROXIMITY_INTERVAL.get()) {
            return;
        }
        ticks = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator() || player.isCreative()) {
                    continue;
                }
                if (level.getRandom().nextDouble() >= ZombieModConfig.PROXIMITY_CHANCE.get()) {
                    continue;
                }
                trySpawnNear(level, player);
            }
        }
    }

    private void trySpawnNear(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();

        if (countNearby(level, player) >= ZombieModConfig.PROXIMITY_CAP.get()) {
            return;
        }

        BlockPos at = findSpot(level, player, random);
        if (at == null) {
            return;
        }

        if (ZombieModConfig.CLAIM_PROTECTION.get()
                && ZombieModConfig.CLAIM_SPAWNS.get() != ZombieModConfig.ClaimSpawns.ALLOW
                && FtbChunks.isClaimed(level, at)) {
            return;
        }

        HolderLookup.RegistryLookup<Genus> lookup =
                level.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS);

        // Weighted among genera that both want to spawn naturally and accept this spot. Passing
        // NATURAL means a genus restricted to spawners or breeding is not dragged in by this.
        List<Holder.Reference<Genus>> eligible = new ArrayList<>();
        int total = 0;
        for (Holder.Reference<Genus> holder : lookup.listElements().toList()) {
            Genus genus = holder.value();
            if (genus.weight() <= 0 || !genus.spawn().allows(level, at, EntitySpawnReason.NATURAL)) {
                continue;
            }
            eligible.add(holder);
            total += genus.weight();
        }
        if (eligible.isEmpty()) {
            return;
        }

        int roll = random.nextInt(total);
        Holder.Reference<Genus> chosen = null;
        for (Holder.Reference<Genus> holder : eligible) {
            roll -= holder.value().weight();
            if (roll < 0) {
                chosen = holder;
                break;
            }
        }
        if (chosen == null) {
            return;
        }

        var created = chosen.value().base().create(level, EntitySpawnReason.NATURAL);
        if (!(created instanceof Mob mob)) {
            return;
        }
        mob.snapTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);

        // Let other mods veto, exactly as they could for a natural spawn. Skipping this would make
        // proximity spawns invisible to every spawn-control mod on the server.
        if (!net.neoforged.neoforge.event.EventHooks.checkSpawnPosition(mob, level, EntitySpawnReason.NATURAL)) {
            mob.discard();
            return;
        }

        GenusApplier.assign(mob, chosen);
        level.addFreshEntity(mob);
    }

    /**
     * A spot at a random bearing and distance, on the surface, that a mob can stand in.
     */
    private BlockPos findSpot(ServerLevel level, ServerPlayer player, RandomSource random) {
        int min = ZombieModConfig.PROXIMITY_MIN.get();
        int max = Math.max(min + 1, ZombieModConfig.PROXIMITY_MAX.get());

        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = min + random.nextDouble() * (max - min);
            int x = (int) (player.getX() + Math.cos(angle) * distance);
            int z = (int) (player.getZ() + Math.sin(angle) * distance);

            // Never reach into unloaded chunks: generating terrain because someone walked around is
            // how a spawner becomes a performance complaint.
            if (!level.isLoaded(new BlockPos(x, player.getBlockY(), z))) {
                continue;
            }

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos candidate = new BlockPos(x, y, z);

            if (!standable(level, candidate)) {
                continue;
            }
            if (ZombieModConfig.PROXIMITY_UNSEEN.get() && canSee(level, player, candidate)) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    /** Two blocks of room, something solid underfoot, and not in liquid. */
    private static boolean standable(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.below()).isSolidRender()
                && level.getFluidState(pos).isEmpty();
    }

    /** Would the player watch it appear? */
    private static boolean canSee(ServerLevel level, ServerPlayer player, BlockPos pos) {
        Vec3 eye = player.getEyePosition();
        Vec3 target = Vec3.atCenterOf(pos.above());
        HitResult hit = level.clip(new ClipContext(eye, target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS;
    }

    private static int countNearby(ServerLevel level, ServerPlayer player) {
        double radius = ZombieModConfig.PROXIMITY_MAX.get() + 8.0D;
        return level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius),
                m -> m.getPersistentData().getString(GenusApplier.GENUS_TAG).isPresent()).size();
    }
}
