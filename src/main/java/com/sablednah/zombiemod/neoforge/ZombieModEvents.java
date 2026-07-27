package com.sablednah.zombiemod.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.ZombieModConfig;
import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.core.Genus;
import com.sablednah.zombiemod.core.SpawnRules;

import org.slf4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Where a plain vanilla mob becomes a ZombieMod one.
 *
 * <p>Two hooks, mirroring the persistent/transient split in {@link GenusApplier}:
 * <ul>
 *   <li>{@link FinalizeSpawnEvent} fires once, when a mob is first spawned into the world. That is
 *       where a genus is chosen and its lasting properties written.
 *   <li>{@link EntityJoinLevelEvent} fires every time an entity enters a level — on that first
 *       spawn <em>and</em> every subsequent chunk load. That is where the AI is rebuilt, because
 *       goals do not survive being saved to disk.
 * </ul>
 */
public final class ZombieModEvents {

    private static final Logger LOG = LogUtils.getLogger();

    /**
     * Report what the datapacks gave us. Worth having permanently: a genus whose JSON failed to
     * parse is dropped by the registry loader, and without this line an empty registry and a
     * working one look exactly the same from the console.
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        HolderLookup.RegistryLookup<Genus> lookup =
                event.getServer().registryAccess().lookupOrThrow(ZombieModRegistries.GENUS);

        List<String> summary = lookup.listElements()
                .map(h -> h.key().identifier().getPath()
                        + " (" + h.value().goals().size() + "+" + h.value().targetGoals().size() + " goals, weight "
                        + h.value().weight() + ")")
                .sorted()
                .toList();

        if (summary.isEmpty()) {
            LOG.warn("ZombieMod: no genera loaded - nothing will change. Check data/<pack>/zombiemod/genus/.");
        } else {
            LOG.info("ZombieMod: {} genera loaded - {}", summary.size(), String.join(", ", summary));
        }
    }

    @SubscribeEvent
    public void onFinalizeSpawn(FinalizeSpawnEvent event) {
        // getLevel() is a ServerLevelAccessor, which during chunk generation is a WorldGenRegion
        // rather than the ServerLevel itself. Going through getLevel() covers both - an instanceof
        // check here would silently skip every mob placed at world generation.
        if (!ZombieModConfig.ENABLED.get()) {
            return;
        }
        ServerLevel level = event.getLevel().getLevel();
        Mob mob = event.getEntity();
        if (mob.getPersistentData().getString(GenusApplier.GENUS_TAG).isPresent()) {
            return; // already ours (e.g. spawned by command, which assigns before adding)
        }

        BlockPos pos = mob.blockPosition();
        rollGenus(level, mob, pos, event.getSpawnType()).ifPresent(holder -> {
            GenusApplier.assign(mob, holder);
            if (ZombieModConfig.LOG_SPAWNS.get()) {
                LOG.info("ZombieMod: {} at {} {} {} ({})", holder.key().identifier(),
                        pos.getX(), pos.getY(), pos.getZ(), event.getSpawnType());
            }
        });
    }

    @SubscribeEvent
    public void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        GenusApplier.genusOf(mob, level).ifPresent(holder -> GenusApplier.applyAi(mob, holder.value()));
    }

    /**
     * Weighted pick among the genera eligible for this spawn — the job the 1.8 mod's
     * {@code WeightedProbMap} did, plus the per-genus spawn conditions it never had.
     *
     * <p>A genus qualifies when its {@code base} matches the mob vanilla was about to spawn, its
     * weight is above zero (weight 0 means "command only"), and its {@link SpawnRules} accept this
     * position and spawn reason.
     *
     * <p>The configured vanilla weight rides in the same draw as an ordinary entry. That matters:
     * without it the first genus to become eligible would claim <em>every</em> zombie in the world
     * and plain zombies would quietly cease to exist — the 1.8 plugin's actual behaviour, and not
     * one anybody chose.
     */
    private Optional<Holder.Reference<Genus>> rollGenus(
            ServerLevel level, Mob mob, BlockPos pos, EntitySpawnReason reason) {

        HolderLookup.RegistryLookup<Genus> lookup = level.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS);
        ResourceKey<Level> dimension = level.dimension();

        List<Holder.Reference<Genus>> candidates = new ArrayList<>();
        int total = ZombieModConfig.VANILLA_WEIGHT.get();

        for (Holder.Reference<Genus> holder : lookup.listElements().toList()) {
            Genus genus = holder.value();
            if (genus.weight() <= 0 || genus.base() != mob.getType()) {
                continue;
            }
            if (!genus.spawn().allowsDimension(dimension) || !genus.spawn().allows(level, pos, reason)) {
                continue;
            }
            candidates.add(holder);
            total += genus.weight();
        }
        if (candidates.isEmpty() || total <= 0) {
            return Optional.empty();
        }

        RandomSource random = level.getRandom();
        int roll = random.nextInt(total);
        for (Holder.Reference<Genus> holder : candidates) {
            roll -= holder.value().weight();
            if (roll < 0) {
                return Optional.of(holder);
            }
        }
        return Optional.empty(); // the roll landed in the vanilla slice - leave this mob alone
    }
}
