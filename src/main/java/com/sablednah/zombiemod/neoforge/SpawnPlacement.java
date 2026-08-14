package com.sablednah.zombiemod.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.core.Genus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;

/** Shared placement, so the proximity spawner and the horde director agree on what a valid spot is. */
final class SpawnPlacement {

    private SpawnPlacement() {}

    /**
     * A standable spot on a ring around a point, or null after a reasonable number of tries.
     *
     * <p>Refuses unloaded chunks: generating terrain because a horde wanted somewhere to stand is how
     * an atmospheric feature becomes a performance complaint.
     */
    static BlockPos ringAround(ServerLevel level, BlockPos centre, double min, double max,
            RandomSource random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = min + random.nextDouble() * Math.max(1.0D, max - min);
            int x = (int) (centre.getX() + Math.cos(angle) * distance);
            int z = (int) (centre.getZ() + Math.sin(angle) * distance);

            if (!level.isLoaded(new BlockPos(x, centre.getY(), z))) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos candidate = new BlockPos(x, y, z);
            if (standable(level, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    static boolean standable(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.below()).isSolidRender()
                && level.getFluidState(pos).isEmpty();
    }

    /** Weighted among genera that accept this spot as a natural spawn. */
    static Optional<Holder.Reference<Genus>> weighted(HolderLookup.RegistryLookup<Genus> lookup,
            ServerLevel level, BlockPos at, RandomSource random) {
        List<Holder.Reference<Genus>> eligible = new ArrayList<>();
        int total = 0;
        for (Holder.Reference<Genus> holder : lookup.listElements().toList()) {
            Genus genus = holder.value();
            if (!Genus.drawable(holder) || !genus.spawn().allows(level, at, EntitySpawnReason.NATURAL)) {
                continue;
            }
            eligible.add(holder);
            total += genus.weight();
        }
        if (eligible.isEmpty()) {
            return Optional.empty();
        }
        int roll = random.nextInt(total);
        for (Holder.Reference<Genus> holder : eligible) {
            roll -= holder.value().weight();
            if (roll < 0) {
                return Optional.of(holder);
            }
        }
        return Optional.empty();
    }
}
