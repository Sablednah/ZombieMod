package com.sablednah.zombiemod.neoforge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.platform.Bars;
import com.sablednah.zombiemod.platform.Msg;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
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
        /**
         * Exactly who it made.
         *
         * <p>Counting by identity rather than by a box around the player, because a box gets the
         * last-straggler case wrong in the worst way: a mob that wandered past the edge stops being
         * counted, the horde declares itself over, and the thing is still out there. Forty UUID
         * lookups a tick is nothing, and it is right at any distance.
         */
        final Set<UUID> spawned = new HashSet<>();
        int wave;
        int countdown;
        int placed;
        /** Kill-progress watchdog, for the glow. */
        int lastAlive = -1;
        int stalled;

        Active(HordeSpec spec, ServerPlayer player) {
            this.spec = spec;
            this.player = player;
            // The first wave's own delay, so `delay: 0` means "immediately" and anything else is a
            // beat before it starts. Left at the field default of 0, the first wave always landed on
            // the first tick regardless of what its JSON asked for.
            this.countdown = spec.waves().isEmpty() ? 0 : spec.waves().get(0).delay();
            this.bar = spec.barColor()
                    .map(colour -> Bars.create(Announce.format(spec.name()), colour,
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

        spec.announce().ifPresent(text -> Msg.chat(player, Announce.format(text)));
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
        finish(active, player.level() instanceof ServerLevel level ? level : null, false);
        return true;
    }

    /** @return false when this horde is over and should be dropped. */
    private boolean advance(Active active) {
        ServerPlayer player = active.player;
        if (player.isRemoved() || !(player.level() instanceof ServerLevel level)) {
            finish(active, null, false);
            return false;
        }

        List<Mob> alive = survivors(level, active);

        if (active.wave < active.spec.waves().size()) {
            HordeSpec.Wave wave = active.spec.waves().get(active.wave);
            // Chained: the field is clear, so stop waiting. The delay stays as a ceiling, so a player
            // who hides or runs still gets the wave rather than a horde that stalls forever. Never on
            // the first wave, which has nothing to be clear of - and `placed > 0` because "nothing
            // alive" is also true of the instant before anything has been made.
            boolean chained = wave.onClear() && active.wave > 0 && active.placed > 0 && alive.isEmpty();
            if (--active.countdown <= 0 || chained) {
                active.placed += spawnWave(level, player, active, wave);
                active.wave++;
                // The delay belongs to the wave that is WAITING, not the one that just landed. Read
                // from the wave just spawned, every wave inherited its predecessor's delay: the first
                // wave's `delay: 0` therefore made the second arrive on the very next tick, the third
                // one tick after that, and the last wave's delay was never read at all. A three-wave
                // siege was over in three ticks - it never built, it just arrived, and the numbers in
                // the JSON described a horde nobody had ever seen. (Measured with a FakePlayer:
                // waves at ticks 21 and 22, then nothing for 1378 ticks.)
                if (active.wave < active.spec.waves().size()) {
                    active.countdown = active.spec.waves().get(active.wave).delay();
                    LOG.info("ZombieMod: horde '{}' wave {}/{} placed, next in {} ticks{}",
                            active.spec.name(), active.wave, active.spec.waves().size(),
                            active.countdown,
                            active.spec.waves().get(active.wave).onClear() ? " (or when cleared)" : "");
                } else {
                    LOG.info("ZombieMod: horde '{}' final wave {}/{} placed, {} mobs total",
                            active.spec.name(), active.wave, active.spec.waves().size(), active.placed);
                }
            }
        } else if (alive.isEmpty()) {
            // Last wave is out and nothing is left standing. Ending on the last kill rather than a
            // timer is what makes the quiet afterwards mean something.
            finish(active, level, true);
            LAST_FINISHED.put(player.getUUID(), level.getGameTime());
            return false;
        } else {
            // Every wave is out and some are still up: the only phase where "where is it?" is the
            // question. Count ticks since the last kill, and once that runs long enough, stop making
            // them hunt. Nothing here fires while a wave is still due, so a slow build is never
            // mistaken for a player who is stuck.
            if (alive.size() != active.lastAlive) {
                active.stalled = 0;
            } else {
                active.stalled++;
            }
            int after = ZombieModConfig.HORDE_GLOW_STALL.get();
            // Refreshed on a slow beat rather than every tick: re-applying an effect changes its
            // duration, which marks the entity dirty and ships a packet, so a per-tick refresh would
            // be twenty of them a second per mob to achieve exactly nothing.
            if (after > 0 && active.stalled >= after && active.stalled % 40 == 0) {
                glow(alive, ZombieModConfig.HORDE_GLOW_DURATION.get());
            }
        }
        active.lastAlive = alive.size();

        if (active.bar != null) {
            int total = Math.max(1, active.spec.totalCount());
            active.bar.setProgress(Math.clamp((float) alive.size() / total, 0.0F, 1.0F));
            active.bar.setName(Component.literal(
                    Announce.format(active.spec.name()).getString() + "  §7" + alive.size() + " left"));
        }
        return true;
    }

    private int spawnWave(ServerLevel level, ServerPlayer player, Active active, HordeSpec.Wave wave) {
        HolderLookup.RegistryLookup<Genus> lookup =
                level.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS);
        int placed = 0;

        for (int i = 0; i < wave.count(); i++) {
            if (survivors(level, active).size() + placed >= ZombieModConfig.HORDE_CAP.get()) {
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
            active.spawned.add(mob.getUUID());
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

    /** Everything it placed that is still standing, wherever that is. */
    private static List<Mob> survivors(ServerLevel level, Active active) {
        List<Mob> alive = new ArrayList<>();
        for (UUID id : active.spawned) {
            if (level.getEntity(id) instanceof Mob mob && mob.isAlive()) {
                alive.add(mob);
            }
        }
        return alive;
    }

    /**
     * Light them up.
     *
     * <p>Vanilla does this for raids and the gesture is worth borrowing wholesale, but none of it is
     * reusable: {@code BellBlockEntity.makeRaidersGlow} is private and filters on the
     * {@code #minecraft:raiders} entity tag, which a zombie is never in and must not be put in — that
     * tag is what makes something count towards a raid.
     */
    private static void glow(List<Mob> mobs, int duration) {
        for (Mob mob : mobs) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false));
        }
    }

    /**
     * A bell rung anywhere is a bell rung for every horde in earshot.
     *
     * <p>Hooked on the sound rather than on the interaction so that it works however the bell was
     * struck — by hand, by arrow, by redstone — which is the same set vanilla honours. The cost when
     * no horde is running is one reference comparison on a map that is empty, which is why the
     * emptiness check comes first: this event fires for every sound in the level.
     */
    @SubscribeEvent
    public void onSound(PlayLevelSoundEvent.AtPosition event) {
        if (RUNNING.isEmpty() || !ZombieModConfig.HORDE_BELL.get()
                || event.getSound().value() != SoundEvents.BELL_BLOCK
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        double radius = ZombieModConfig.HORDE_BELL_RADIUS.get();
        Vec3 at = event.getPosition();

        for (Active active : RUNNING.values()) {
            if (active.player.level() != level) {
                continue;
            }
            List<Mob> heard = survivors(level, active).stream()
                    .filter(m -> m.position().closerThan(at, radius))
                    .toList();
            if (!heard.isEmpty()) {
                glow(heard, ZombieModConfig.HORDE_GLOW_DURATION.get());
            }
        }
    }

    private static void finish(Active active, ServerLevel level, boolean cleared) {
        if (active.bar != null) {
            active.bar.removeAllPlayers();
            active.bar.setVisible(false);
        }
        if (level == null) {
            return;
        }
        if (cleared) {
            ServerPlayer player = active.player;
            Msg.chat(player, Announce.format(active.spec.victoryText()));
            if (active.spec.xp() > 0) {
                player.giveExperiencePoints(active.spec.xp());
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        LOG.info("ZombieMod: horde '{}' {} for {} ({} placed)", active.spec.name(),
                cleared ? "cleared" : "ended", active.player.getName().getString(), active.placed);
    }

    /**
     * A member mutated into something else and is now a different entity.
     *
     * <p>Without this the roster would still be watching a UUID that no longer resolves, count the
     * mutant as dead, and end the horde with it still walking around - the exact failure the
     * identity-based count was introduced to remove.
     */
    static void replaceMember(UUID was, UUID now) {
        for (Active active : RUNNING.values()) {
            if (active.spawned.remove(was)) {
                active.spawned.add(now);
                return;
            }
        }
    }

    /** For {@code /zombiemod horde}. */
    public static boolean isRunning(ServerPlayer player) {
        return RUNNING.containsKey(player.getUUID());
    }
}
