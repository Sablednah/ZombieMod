package com.sablednah.zombiemod.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.ZombieModConfig;
import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.core.Genus;
import com.sablednah.zombiemod.core.ability.Ability;
import com.sablednah.zombiemod.core.spawn.SpawnRules;

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
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
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
                .map(h -> {
                    Genus g = h.value();
                    // Count behaviour goals too, or a genus whose whole personality is conditional
                    // reads as nearly empty here - which is the opposite of what this line is for.
                    int conditional = g.behaviours().stream()
                            .mapToInt(b -> b.goals().size() + b.targetGoals().size()).sum();
                    StringBuilder sb = new StringBuilder(h.key().identifier().getPath());
                    sb.append(" (").append(g.goals().size()).append('+').append(g.targetGoals().size());
                    if (conditional > 0) {
                        sb.append('+').append(conditional).append(" cond");
                    }
                    sb.append(" goals");
                    if (!g.abilities().isEmpty()) {
                        sb.append(", ").append(g.abilities().size()).append(" abil");
                    }
                    return sb.append(", weight ").append(g.weight()).append(')').toString();
                })
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

        if (ZombieModConfig.CLAIM_PROTECTION.get()
                && ZombieModConfig.CLAIM_SPAWNS.get() != ZombieModConfig.ClaimSpawns.ALLOW
                && com.sablednah.zombiemod.compat.FtbChunks.isClaimed(level, pos)) {
            if (ZombieModConfig.CLAIM_SPAWNS.get() == ZombieModConfig.ClaimSpawns.NO_SPAWNS) {
                event.setSpawnCancelled(true);
            }
            // VANILLA_ONLY: leave the mob alone rather than cancelling it, so a claim keeps ordinary
            // mobs and loses only ours.
            return;
        }

        rollGenus(level, mob, pos, event.getSpawnType()).ifPresent(holder -> {
            GenusApplier.assign(mob, holder);
            if (ZombieModConfig.LOG_SPAWNS.get()) {
                LOG.info("ZombieMod: {} at {} {} {} ({})", holder.key().identifier(),
                        pos.getX(), pos.getY(), pos.getZ(), event.getSpawnType());
            }
        });
    }

    /**
     * Give abilities a chance to react to damage.
     *
     * <p>Reactive behaviour can't ride the interval schedule — "blinks away when shot" happens when
     * you shoot it, not every N ticks. Most abilities ignore this; the lookup is skipped entirely
     * for anything without a genus tag, which is every ordinary mob in the world.
     */
    /**
     * Refuse griefing inside a claim.
     *
     * <p>Handles the same {@code EntityMobGriefingEvent} our abilities ask through, which means one
     * check covers block breaking and block placing without either ability knowing claims exist.
     *
     * <p>Scoped to ZombieMod's own mobs on purpose. Vetoing griefing for every mob in the game would
     * be doing a land-protection mod's job for it, silently, from a mob pack - and creepers are
     * already covered, since explosions are the part FTB Chunks does protect.
     */
    @SubscribeEvent
    public void onMobGriefing(net.neoforged.neoforge.event.entity.EntityMobGriefingEvent event) {
        if (!ZombieModConfig.CLAIM_PROTECTION.get() || !ZombieModConfig.CLAIM_NO_GRIEFING.get()) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (mob.getPersistentData().getString(GenusApplier.GENUS_TAG).isEmpty()) {
            return;
        }
        if (com.sablednah.zombiemod.compat.FtbChunks.isClaimed(level, mob.blockPosition())) {
            event.setCanGrief(false);
        }
    }

    /**
     * Let abilities react to our mobs hurting something. Separate from the "our mob was hurt" path
     * above, and read off the damage source rather than the victim.
     */
    @SubscribeEvent
    public void onOutgoingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Mob attacker)
                || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }
        if (attacker.getPersistentData().getString(GenusApplier.GENUS_TAG).isEmpty()) {
            return;
        }
        GenusApplier.genusOf(attacker, level).ifPresent(holder -> {
            for (Ability ability : holder.value().abilities()) {
                ability.onAttack(level, attacker, event.getEntity(), event.getAmount());
            }
        });
    }

    /**
     * The infected turn, whatever killed them.
     *
     * <p>Checked on the victim rather than the killer, because that is the entire point: a bite a
     * minute ago decides this, not whatever finished the job. Players included — a player who dies
     * infected rises even where the player-zombie feature is off, since they were bitten.
     */
    @SubscribeEvent
    public void onInfectedDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        var victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        long until = victim.getPersistentData().getLongOr(
                com.sablednah.zombiemod.core.ability.Infect.UNTIL_TAG, -1L);
        if (until < 0L || level.getGameTime() > until) {
            return;
        }
        // The specific effect it was bitten with, not just any effect - milk clears it, which is
        // the cure, and an unrelated potion must not stand in for it.
        if (!com.sablednah.zombiemod.core.ability.Infect.stillMarked(victim)) {
            com.sablednah.zombiemod.core.ability.Infect.clear(victim);
            return;
        }
        // A player dying infected would otherwise raise twice: once here, and once from the
        // player-zombie corpse a moment later when drops are handled. Both is the default because
        // only the corpse carries the loot, so the pair reads as a decoy rather than a duplicate -
        // but it should be a choice rather than an accident of event ordering.
        if (victim instanceof net.minecraft.world.entity.player.Player
                && ZombieModConfig.PLAYER_ZOMBIES.get()
                && !ZombieModConfig.PLAYER_ZOMBIE_INFECTED_TOO.get()) {
            com.sablednah.zombiemod.core.ability.Infect.clear(victim);
            return;
        }

        var genusId = victim.getPersistentData().getString(
                        com.sablednah.zombiemod.core.ability.Infect.GENUS_TAG)
                .map(net.minecraft.resources.Identifier::tryParse);
        com.sablednah.zombiemod.core.ability.Infect.clear(victim);
        Conversions.raiseInfected(level, victim, genusId);
    }

    /** Observer mode: cancel the damage, change nothing else about the player. */
    @SubscribeEvent
    public void onPlayerDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player
                && !player.level().isClientSide() && ObserverMode.isOn(player)) {
            event.setCanceled(true);
        }
    }

    /** Say so on login, or someone wonders for an hour why nothing can hurt them. */
    @SubscribeEvent
    public void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level
                && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer joined) {
            KnownPlayers.get(level).remember(joined.getGameProfile());
        }
        if (ObserverMode.isOn(event.getEntity())) {
            event.getEntity().displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "\u00a7eZombieMod observer mode is ON - you take no damage. \u00a77/zombiemod observe off"),
                    false);
        }
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (mob.getPersistentData().getString(GenusApplier.GENUS_TAG).isEmpty()) {
            return;
        }
        GenusApplier.genusOf(mob, level).ifPresent(holder -> {
            float amount = event.getAmount();
            for (Ability ability : holder.value().abilities()) {
                amount = ability.onHurt(level, mob, event.getSource(), amount);
            }
            if (amount <= 0.0F) {
                event.setCanceled(true);
            } else if (amount != event.getAmount()) {
                event.setAmount(amount);
            }
        });
    }

    /**
     * Clear a boss bar when its mob goes, for any reason.
     *
     * <p>Hooked on leaving rather than on dying deliberately: despawn, chunk unload and dimension
     * change all remove the entity without killing it, and each one would otherwise strand a bar on
     * someone's screen with nothing left alive to clear it.
     */
    /**
     * Give abilities a chance to react to a kill.
     *
     * <p>Reads the killer from the damage source rather than the victim, so it fires for the thing
     * that did it. Guarded on the genus tag first, which is a cheap string read and skips every
     * ordinary death in the world.
     */
    @SubscribeEvent
    public void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Mob killer)) {
            return;
        }
        if (killer.getPersistentData().getString(GenusApplier.GENUS_TAG).isEmpty()) {
            return;
        }
        GenusApplier.genusOf(killer, level).ifPresent(holder -> {
            for (Ability ability : holder.value().abilities()) {
                ability.onKill(level, killer, event.getEntity());
            }
        });
    }

    @SubscribeEvent
    public void onLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        BossBars.remove(event.getEntity().getId());

        // A beam's emitter is a separate entity, and a dead caster's goals stop ticking before the
        // beam can end itself - so without this the guardian keeps firing at the player forever.
        if (event.getEntity() instanceof Mob mob && event.getLevel() instanceof ServerLevel level) {
            mob.getPersistentData().getString(com.sablednah.zombiemod.core.ability.Abilities.EMITTER_TAG)
                    .ifPresent(id -> {
                        try {
                            var emitter = level.getEntity(java.util.UUID.fromString(id));
                            if (emitter != null) {
                                emitter.discard();
                            }
                        } catch (IllegalArgumentException ignored) {
                            // malformed tag; nothing to clean up
                        }
                        mob.getPersistentData().remove(
                                com.sablednah.zombiemod.core.ability.Abilities.EMITTER_TAG);
                    });
        }
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
        List<Holder.Reference<Genus>> candidates = new ArrayList<>();
        int total = ZombieModConfig.VANILLA_WEIGHT.get();

        for (Holder.Reference<Genus> holder : lookup.listElements().toList()) {
            Genus genus = holder.value();
            if (genus.weight() <= 0 || genus.base() != mob.getType()) {
                continue;
            }
            if (!genus.spawn().allows(level, pos, reason)) {
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
