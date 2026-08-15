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
        // Once a start is often enough: the window is days, and a server that never restarts is
        // also one where nobody is waiting on a name being forgotten.
        int forgotten = KnownPlayers.get(event.getServer().overworld()).prune(event.getServer());
        if (forgotten > 0) {
            LOG.info("ZombieMod: forgot {} player name(s) - stale or banned.", forgotten);
        }

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

        // Same reasoning as the genera line: an empty registry and a working one look identical
        // from the console, and these two are easy to typo into nonexistence.
        var hordes = event.getServer().registryAccess()
                .lookupOrThrow(ZombieModRegistries.HORDE).listElements()
                .map(h -> h.key().identifier().getPath() + " (" + h.value().waves().size() + " waves, "
                        + h.value().totalCount() + " mobs, weight " + h.value().weight() + ")")
                .sorted().toList();
        var rituals = event.getServer().registryAccess()
                .lookupOrThrow(ZombieModRegistries.RITUAL).listElementIds().count();
        LOG.info("ZombieMod: {} hordes{}, {} rituals", hordes.size(),
                hordes.isEmpty() ? "" : " - " + String.join(", ", hordes), rituals);
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
            if (ZombieModConfig.BESTIARY.get()) {
                Bestiary.get(level).push(joined);
            }
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

    /**
     * Pay whoever killed a genus. Reads the source's owner, so shooting one from a distance counts -
     * a bounty that only paid for melee would quietly punish exactly the players being careful.
     */
    @SubscribeEvent
    public void onBountyDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }
        if (mob.getPersistentData().getString(GenusApplier.GENUS_TAG).isEmpty()) {
            return;
        }
        GenusApplier.genusOf(mob, level).ifPresent(holder -> holder.value().bounty().ifPresent(
                amount -> Bounties.award(level, player, amount,
                        holder.value().displayName().orElse(mob.getName()))));
    }

    /**
     * The bestiary. Rides the same death event as the bounty, but kept separate because the two
     * answer to different switches and one is a ledger while the other is a payment.
     */
    @SubscribeEvent
    public void onBestiaryDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (!ZombieModConfig.BESTIARY.get()
                || !(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }
        genusIdOf(mob).ifPresent(id -> Bestiary.get(level).kill(player, id));
    }

    /**
     * "Met" - damage in either direction. A real did-you-lay-eyes-on-it test would be a visibility
     * check per mob per tick, which is a great deal of work to tick a box for something glimpsed
     * across a valley.
     */
    @SubscribeEvent
    public void onBestiaryEncounter(LivingIncomingDamageEvent event) {
        if (!ZombieModConfig.BESTIARY.get() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer hurt
                && event.getSource().getEntity() instanceof Mob attacker) {
            genusIdOf(attacker).ifPresent(id -> Bestiary.get(level).meet(hurt, id));
        } else if (event.getEntity() instanceof Mob hurtMob
                && event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer hitter) {
            genusIdOf(hurtMob).ifPresent(id -> Bestiary.get(level).meet(hitter, id));
        }
    }

    /**
     * Milk, aimed at something that cannot drink it.
     *
     * <p>Safe to hang on the vanilla interaction because using a <em>milk</em> bucket on an entity
     * is not a vanilla interaction at all - milking a cow takes an empty one - so nothing is being
     * overridden here.
     */
    @SubscribeEvent
    public void onMilkCure(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (!ZombieModConfig.INFECT_MILK_CURE.get()
                || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getTarget() instanceof net.minecraft.world.entity.LivingEntity target)) {
            return;
        }
        net.minecraft.world.item.ItemStack held = event.getItemStack();
        if (!held.is(net.minecraft.world.item.Items.MILK_BUCKET)) {
            return;
        }
        if (com.sablednah.zombiemod.core.ability.Infect.remaining(target, level.getGameTime()) < 0L) {
            return;
        }

        com.sablednah.zombiemod.core.ability.Infect.cure(target);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),
                12, 0.3D, 0.4D, 0.3D, 0.02D);
        level.playSound(null, target.blockPosition(),
                net.minecraft.sounds.SoundEvents.GENERIC_DRINK.value(),
                net.minecraft.sounds.SoundSource.NEUTRAL, 0.8F, 1.2F);
        if (!event.getEntity().hasInfiniteMaterials()) {
            event.getEntity().setItemInHand(event.getHand(),
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));
        }
        event.getEntity().swing(event.getHand(), true);
        // Consume it, or the click falls through to whatever the mob does with a right-click.
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /** Noises the ears care about, split by how far they carry. Built from the GameEvent registry
     * keys once, so a typo is a startup crash rather than a silent deaf spot. */
    private static final java.util.Set<net.minecraft.resources.Identifier> LOUD_NOISES =
            noiseIds(net.minecraft.world.level.gameevent.GameEvent.BLOCK_DESTROY,
                    net.minecraft.world.level.gameevent.GameEvent.BLOCK_PLACE,
                    net.minecraft.world.level.gameevent.GameEvent.EXPLODE,
                    net.minecraft.world.level.gameevent.GameEvent.PRIME_FUSE,
                    net.minecraft.world.level.gameevent.GameEvent.HIT_GROUND,
                    net.minecraft.world.level.gameevent.GameEvent.INSTRUMENT_PLAY);
    private static final java.util.Set<net.minecraft.resources.Identifier> QUIET_NOISES =
            noiseIds(net.minecraft.world.level.gameevent.GameEvent.BLOCK_OPEN,
                    net.minecraft.world.level.gameevent.GameEvent.BLOCK_CLOSE,
                    net.minecraft.world.level.gameevent.GameEvent.BLOCK_ACTIVATE,
                    net.minecraft.world.level.gameevent.GameEvent.PROJECTILE_SHOOT,
                    net.minecraft.world.level.gameevent.GameEvent.SPLASH,
                    net.minecraft.world.level.gameevent.GameEvent.EAT,
                    net.minecraft.world.level.gameevent.GameEvent.DRINK,
                    net.minecraft.world.level.gameevent.GameEvent.EQUIP,
                    net.minecraft.world.level.gameevent.GameEvent.ITEM_INTERACT_FINISH);

    @SafeVarargs
    private static java.util.Set<net.minecraft.resources.Identifier> noiseIds(
            net.minecraft.core.Holder<net.minecraft.world.level.gameevent.GameEvent>... events) {
        java.util.Set<net.minecraft.resources.Identifier> out = new java.util.HashSet<>();
        for (var event : events) {
            out.add(event.unwrapKey().orElseThrow().identifier());
        }
        return out;
    }

    /**
     * Real noises for anything with ears.
     *
     * <p>The movement model in {@code SoundTargetGoal} hears how loudly you are walking; this hook
     * hears what you <em>do</em> - mine a block, open a door, land from a fall - via the same
     * vibration events sculk listens to. One global subscriber rather than a Warden-style listener
     * per mob, because a vanilla zombie cannot override the listener registration hooks and vanilla
     * mobs are the whole mod.
     *
     * <p>The noise only betrays the player if the player is <em>at</em> it: an arrow landing across
     * the valley says where the arrow is, not where you are. (Walking to investigate a remote noise
     * would be the honest response, and is future work, not a lie worth telling meanwhile.)
     */
    @SubscribeEvent
    public void onNoise(net.neoforged.neoforge.event.VanillaGameEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getCause() instanceof net.minecraft.server.level.ServerPlayer player)
                || player.isSpectator() || player.isCreative()) {
            return;
        }
        var id = event.getVanillaEvent().unwrapKey()
                .map(net.minecraft.resources.ResourceKey::identifier).orElse(null);
        boolean loud = LOUD_NOISES.contains(id);
        if (!loud && !QUIET_NOISES.contains(id)) {
            return;
        }
        var at = event.getEventPosition();
        if (player.position().distanceToSqr(at) > 16.0D) {
            return;
        }
        var reach = new net.minecraft.world.phys.AABB(at, at).inflate(48.0D);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, reach,
                m -> m.getPersistentData().getString(GenusApplier.GENUS_TAG).isPresent())) {
            for (var wrapped : mob.targetSelector.getAvailableGoals()) {
                if (wrapped.getGoal() instanceof com.sablednah.zombiemod.core.goal.SoundTargetGoal ears) {
                    double range = loud ? ears.sprintRadius() : ears.walkRadius();
                    if (mob.position().distanceToSqr(at) <= range * range) {
                        ears.notice(player);
                    }
                }
            }
        }
    }

    private static java.util.Optional<net.minecraft.resources.Identifier> genusIdOf(Mob mob) {
        return mob.getPersistentData().getString(GenusApplier.GENUS_TAG)
                .map(net.minecraft.resources.Identifier::tryParse);
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
        // The infection is saved in entity NBT; the goal that spreads it is not. Same
        // persistent/transient split as the genus AI above, and the same fix.
        InfectionGoal.attach(mob);
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
            if (!Genus.drawable(holder) || genus.base() != mob.getType()) {
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
