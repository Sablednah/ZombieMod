package com.sablednah.zombiemod.neoforge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.ZombieModConfig;
import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.compat.FtbChunks;
import com.sablednah.zombiemod.core.Announce;
import com.sablednah.zombiemod.core.Genus;
import com.sablednah.zombiemod.core.HordeSpec;

import org.slf4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Decides when several monsters arrive together.
 *
 * <p>One horde runs per player at a time, tracked by their id, so two players in different places get
 * their own night rather than sharing one. The bar shows what is left alive rather than what has been
 * spawned, because "twelve still out there" is the number a player actually wants.
 *
 * <p>Every mob it places is tagged with the horde that made it, which is what lets the bar count them
 * and what lets a horde end when its last one falls rather than on a timer that may be wrong.
 */
public final class HordeDirector {

    private static final Logger LOG = LogUtils.getLogger();

    /** Which horde a mob belongs to, so it can be counted and cleaned up. */
    static final String HORDE_TAG = "zombiemod:horde";

    private static final Map<UUID, Active> RUNNING = new HashMap<>();
    private static final Map<UUID, Long> LAST_FINISHED = new HashMap<>();

    private int ticks;

    /** One horde in progress, for one player. */
    private static final class Active {
        final UUID id = UUID.randomUUID();
        final HordeSpec spec;
        final ServerPlayer player;
        final ServerBossEvent bar;
        int wave;
        int countdown;
        int placed;

        Active(HordeSpec spec, ServerPlayer player) {
            this.spec = spec;
            this.player = player;
            this.bar = spec.barColor()
                    .map(colour -> new ServerBossEvent(Announce.format(spec.name()), colour,
                            BossEvent.BossBarOverlay.NOTCHED_10))
                    .orElse(null);
            if (bar != null) {
                bar.addPlayer(player);
            }
        }
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!ZombieModConfig.ENABLED.get()) {
            return;
        }
        RUNNING.values().removeIf(active -> !advance(active));

        if (!ZombieModConfig.HORDES.get() || ++ticks < ZombieModConfig.HORDE_CHECK.get()) {
            return;
        }
        ticks = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                maybeStart(level, player);
            }
        }
    }

    private void maybeStart(ServerLevel level, ServerPlayer player) {
        if (player.isSpectator() || player.isCreative() || RUNNING.containsKey(player.getUUID())) {
            return;
        }
        long since = level.getGameTime()
                - LAST_FINISHED.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
        if (since < ZombieModConfig.HORDE_COOLDOWN.get()) {
            return;
        }
        if (level.getRandom().nextDouble() >= ZombieModConfig.HORDE_CHANCE.get()) {
            return;
        }
        pick(level, player).ifPresent(spec -> start(level, player, spec));
    }

    /** Weighted among hordes whose conditions hold where the player is standing. */
    private Optional<HordeSpec> pick(ServerLevel level, ServerPlayer player) {
        HolderLookup.RegistryLookup<HordeSpec> lookup =
                level.registryAccess().lookupOrThrow(ZombieModRegistries.HORDE);
        BlockPos at = player.blockPosition();

        List<HordeSpec> eligible = new ArrayList<>();
        int total = 0;
        for (Holder.Reference<HordeSpec> holder : lookup.listElements().toList()) {
            HordeSpec spec = holder.value();
            if (spec.weight() <= 0) {
                continue;
            }
            if (!spec.conditions().stream().allMatch(c -> c.test(level, at))) {
                continue;
            }
            eligible.add(spec);
            total += spec.weight();
        }
        if (eligible.isEmpty()) {
            return Optional.empty();
        }
        int roll = level.getRandom().nextInt(total);
        for (HordeSpec spec : eligible) {
            roll -= spec.weight();
            if (roll < 0) {
                return Optional.of(spec);
            }
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------ running one

    public static boolean start(ServerLevel level, ServerPlayer player, HordeSpec spec) {
        if (RUNNING.containsKey(player.getUUID())) {
            return false;
        }
        Active active = new Active(spec, player);
        RUNNING.put(player.getUUID(), active);

        spec.announce().ifPresent(text -> player.displayClientMessage(Announce.format(text), false));
        spec.sound().ifPresent(sound -> level.playSound(null, player.getX(), player.getY(), player.getZ(),
                sound.value(), SoundSource.HOSTILE, 2.0F, 0.8F));

        LOG.info("ZombieMod: horde '{}' started for {}", spec.name(), player.getName().getString());
        return true;
    }

    public static boolean stop(ServerPlayer player) {
        Active active = RUNNING.remove(player.getUUID());
        if (active == null) {
            return false;
        }
        finish(active, player.level() instanceof ServerLevel level ? level : null);
        return true;
    }

    /** @return false when this horde is over and should be dropped. */
    private boolean advance(Active active) {
        ServerPlayer player = active.player;
        if (player.isRemoved() || !(player.level() instanceof ServerLevel level)) {
            finish(active, null);
            return false;
        }

        int alive = countAlive(level, active);

        if (active.wave < active.spec.waves().size()) {
            if (--active.countdown <= 0) {
                HordeSpec.Wave wave = active.spec.waves().get(active.wave);
                active.countdown = wave.delay();
                active.placed += spawnWave(level, player, active, wave);
                active.wave++;
            }
        } else if (alive <= 0) {
            // Last wave is out and nothing is left standing. Ending on the last kill rather than a
            // timer is what makes the quiet afterwards mean something.
            finish(active, level);
            LAST_FINISHED.put(player.getUUID(), level.getGameTime());
            return false;
        }

        if (active.bar != null) {
            int total = Math.max(1, active.spec.totalCount());
            active.bar.setProgress(Math.clamp((float) alive / total, 0.0F, 1.0F));
            active.bar.setName(Component.literal(
                    Announce.format(active.spec.name()).getString() + "  §7" + alive + " left"));
        }
        return true;
    }

    private int spawnWave(ServerLevel level, ServerPlayer player, Active active, HordeSpec.Wave wave) {
        HolderLookup.RegistryLookup<Genus> lookup =
                level.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS);
        int placed = 0;

        for (int i = 0; i < wave.count(); i++) {
            if (countAlive(level, active) + placed >= ZombieModConfig.HORDE_CAP.get()) {
                break;
            }
            BlockPos at = SpawnPlacement.ringAround(level, player.blockPosition(),
                    active.spec.minRadius(), active.spec.radius(), level.getRandom());
            if (at == null) {
                continue;
            }
            if (ZombieModConfig.CLAIM_PROTECTION.get()
                    && ZombieModConfig.CLAIM_SPAWNS.get() != ZombieModConfig.ClaimSpawns.ALLOW
                    && FtbChunks.isClaimed(level, at)) {
                continue;
            }

            Optional<Holder.Reference<Genus>> chosen = wave.genera().isEmpty()
                    ? SpawnPlacement.weighted(lookup, level, at, level.getRandom())
                    : pickNamed(lookup, wave.genera(), level.getRandom());
            if (chosen.isEmpty()) {
                continue;
            }

            var created = chosen.get().value().base().create(level, EntitySpawnReason.EVENT);
            if (!(created instanceof Mob mob)) {
                continue;
            }
            mob.snapTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D,
                    level.getRandom().nextFloat() * 360.0F, 0.0F);
            GenusApplier.assign(mob, chosen.get());
            mob.getPersistentData().putString(HORDE_TAG, active.id.toString());
            // Point it at the player immediately. A horde that wanders off looking for something to
            // do is just a crowd.
            mob.setTarget(player);
            level.addFreshEntity(mob);
            placed++;
        }
        return placed;
    }

    private static Optional<Holder.Reference<Genus>> pickNamed(
            HolderLookup.RegistryLookup<Genus> lookup, List<Identifier> ids,
            net.minecraft.util.RandomSource random) {
        Identifier id = ids.get(random.nextInt(ids.size()));
        return lookup.get(ResourceKey.create(ZombieModRegistries.GENUS, id));
    }

    private static int countAlive(ServerLevel level, Active active) {
        String id = active.id.toString();
        return level.getEntitiesOfClass(Mob.class,
                active.player.getBoundingBox().inflate(active.spec.radius() + 32.0D),
                m -> m.isAlive() && id.equals(m.getPersistentData().getString(HORDE_TAG).orElse(null)))
                .size();
    }

    private static void finish(Active active, ServerLevel level) {
        if (active.bar != null) {
            active.bar.removeAllPlayers();
            active.bar.setVisible(false);
        }
        if (level != null) {
            LOG.info("ZombieMod: horde '{}' ended for {} ({} placed)",
                    active.spec.name(), active.player.getName().getString(), active.placed);
        }
    }

    /** For {@code /zombiemod horde}. */
    public static boolean isRunning(ServerPlayer player) {
        return RUNNING.containsKey(player.getUUID());
    }
}
