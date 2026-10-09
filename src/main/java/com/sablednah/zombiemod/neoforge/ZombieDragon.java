package com.sablednah.zombiemod.neoforge;

import java.lang.reflect.Field;
import java.util.Optional;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.ZombieModConfig;
import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.core.DragonSpec;
import com.sablednah.zombiemod.core.Genus;
import com.sablednah.zombiemod.core.ability.Infect;
import com.sablednah.zombiemod.core.ability.Targets;
import com.sablednah.zombiemod.net.Net;
import com.sablednah.zombiemod.net.ZombieDragonPayload;
import com.sablednah.zombiemod.platform.EntityState;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * The Ender Dragon dies, and gets back up.
 *
 * <p><b>One dragon, start to finish.</b> The Zombie Dragon is not a second mob: the first death is
 * refused, the same entity lies still for {@code dragon.interlude} ticks, and then a genus is put on
 * it. {@code EnderDragonFight} follows its dragon by UUID, and the UUID never changes, so vanilla's
 * own portal, egg, experience and "Free the End" all wait for the real death and arrive by vanilla's
 * own code. Nothing here holds any of them back.
 *
 * <p><b>Where the first death is refused.</b> A killing blow reaches {@code die()}, and so
 * {@code LivingDeathEvent}, on every version we ship - before vanilla's dying flight and before any
 * drops or kill credit. Cancelling there and putting the health back to 1 in the same call means
 * {@code reallyHurt}'s "is it dead? then fly to the podium" check (1.21.11-26.1) and
 * {@code handleKillingBlow} (26.2+) both find a living dragon. The tick check below is the backstop
 * for a death that reaches us without the event.
 *
 * <p><b>The dragon never ticks its goal selector</b> - {@code EnderDragon.aiStep} does not call
 * {@code super}, its AI is a phase machine of its own. Every ZombieMod ability and phase rides goals,
 * so a risen dragon's goal selector is ticked from here, which makes the ordinary genus fields (the
 * minion trickle in {@code abilities}, the waves in {@code phases[].on_enter}) just work.
 */
public final class ZombieDragon {

    private static final Logger LOG = LogUtils.getLogger();

    /** On the dragon once it has risen. A public contract: Threadwork's music cue reads it. */
    public static final String TAG = "zombiemod.zombie_dragon";
    /** On a breath cloud we have rotted, so its tick knows to infect. */
    static final String BREATH_TAG = "zombiemod.rot_breath";
    /** Ticks left lying dead; present only during the interlude, and saved, so a restart resumes it. */
    static final String INTERLUDE = "zombiemod:dragon_interlude";
    /** Present while it makes vanilla's dying flight to the podium, before the interlude starts. */
    static final String FALLING = "zombiemod:dragon_falling";
    /** Longest the dying flight may take before it is cut short where it is. */
    private static final int FLIGHT_LIMIT = 300;
    /** How many phase thresholds it has roared at, so a reload does not roar them all again. */
    private static final String PHASES_ROARED = "zombiemod:dragon_roared";

    private static Field barField;
    private static boolean barFieldSearched;

    // ------------------------------------------------------------------ lookups

    static boolean enabled() {
        return ZombieModConfig.ENABLED.get() && ZombieModConfig.ZOMBIE_DRAGON.get();
    }

    static Optional<Holder.Reference<Genus>> genus(ServerLevel level) {
        Identifier id = Identifier.tryParse(ZombieModConfig.ZOMBIE_DRAGON_GENUS.get());
        if (id == null) {
            return Optional.empty();
        }
        return level.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS)
                .get(ResourceKey.create(ZombieModRegistries.GENUS, id));
    }

    /** Is this the Zombie Dragon genus, while the feature is off? Then nobody can ever meet it. */
    public static boolean unreachable(Identifier genus) {
        return !ZombieModConfig.ZOMBIE_DRAGON.get()
                && genus.toString().equals(ZombieModConfig.ZOMBIE_DRAGON_GENUS.get());
    }

    private static boolean risen(EnderDragon dragon) {
        return dragon.getPersistentData().getString(GenusApplier.GENUS_TAG).isPresent();
    }

    /** Anywhere between the first death and the rise: the dying flight or the interlude. */
    private static boolean lyingDead(EnderDragon dragon) {
        return dragon.getPersistentData().contains(FALLING) || dragon.getPersistentData().contains(INTERLUDE);
    }

    private static Optional<DragonSpec> spec(ServerLevel level, EnderDragon dragon) {
        return GenusApplier.genusOf(dragon, level).flatMap(holder -> holder.value().dragon());
    }

    // ------------------------------------------------------------------ the first death

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !(dragon.level() instanceof ServerLevel level)) {
            return;
        }
        if (risen(dragon)) {
            // The real one. Put the fight's bar back the way vanilla made it, or the next dragon
            // somebody summons with crystals would fly in under a green "Zombie Dragon".
            bar(dragon).ifPresent(bar -> {
                bar.setColor(BossEvent.BossBarColor.PINK);
                bar.setName(Component.translatable("entity.minecraft.ender_dragon"));
            });
            return;
        }
        if (lyingDead(dragon)) {
            event.setCanceled(true); // invulnerable, but /kill-adjacent damage types still arrive
            dragon.setHealth(1.0F);
            return;
        }
        if (!enabled() || genus(level).isEmpty()) {
            return;
        }
        event.setCanceled(true);
        fall(level, dragon);
    }

    /**
     * The fake death, part one: vanilla's own dying flight.
     *
     * <p>The dragon is put in its {@code DYING} phase with its AI left on, so it flies to the podium
     * shedding explosions exactly as it would for real - the client draws those itself, off the synced
     * phase. Its AI must stay on: the dragon interpolates its position on the client inside the same
     * {@code aiStep} branch that runs its AI, so a dragon with {@code NoAi} set and moved by the server
     * freezes for everyone and then snaps to wherever it ended up. That was the first build.
     */
    private static void fall(ServerLevel level, EnderDragon dragon) {
        dragon.setHealth(1.0F);
        EntityState.setInvulnerable(dragon, true);
        dragon.getPersistentData().putInt(FALLING, 0);
        dragon.getPhaseManager().setPhase(EnderDragonPhase.DYING);
        LOG.info("ZombieMod: the Ender Dragon falls");
    }

    /**
     * The fake death, part two: it has reached the podium, which is where vanilla would start the
     * light beams and the dissolve. It holds still from here (nothing moves it, so freezing the AI is
     * safe now) while the server sends the explosions, and a client with ZombieMod is told to draw
     * vanilla's own beams and dissolve for it.
     */
    private static void land(ServerLevel level, EnderDragon dragon) {
        Optional<DragonSpec> spec = genus(level).flatMap(holder -> holder.value().dragon());
        int interlude = spec.map(DragonSpec::interlude).orElse(160);

        dragon.getPersistentData().remove(FALLING);
        dragon.setHealth(1.0F);
        dragon.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
        dragon.setNoAi(true);
        dragon.setDeltaMovement(Vec3.ZERO);
        dragon.getPersistentData().putInt(INTERLUDE, Math.max(20, interlude));

        // Heard across the island, like vanilla's own dragon death - which also starts here.
        cue(level, dragon, spec, "death_fake", 1.0F, SoundEvents.ENDER_DRAGON_DEATH, 20.0F, 1.0F, 512.0D);
        tell(level, dragon, false);
        LOG.info("ZombieMod: the Ender Dragon lies still ({} ticks)", interlude);
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public void onDragonTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !(dragon.level() instanceof ServerLevel level)) {
            return;
        }
        if (dragon.getPersistentData().contains(FALLING)) {
            return; // vanilla flies it; the Post tick below watches for the landing
        }
        if (lyingDead(dragon)) {
            interlude(level, dragon);
            return;
        }
        if (risen(dragon)) {
            if (dragon.isAlive()) {
                hunt(level, dragon);
                voice(level, dragon);
                dragon.goalSelector.tick();
            }
            return;
        }
        // Backstop: a death that reached the dying flight or zero health without LivingDeathEvent.
        if (enabled() && dragon.dragonDeathTime == 0
                && (dragon.getHealth() <= 0.0F
                        || dragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.DYING)
                && genus(level).isPresent()) {
            fall(level, dragon);
        }
    }

    /**
     * The landing. Vanilla's death phase sets health to 0 on reaching the podium, and {@code tickDeath}
     * would start next tick - the real death. Caught here, after the dragon's own tick and before the
     * health is synced, so neither the server's death nor a client's ever begins.
     */
    @SubscribeEvent
    public void onDragonTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !(dragon.level() instanceof ServerLevel level)
                || !dragon.getPersistentData().contains(FALLING)) {
            return;
        }
        int flown = dragon.getPersistentData().getIntOr(FALLING, 0) + 1;
        dragon.getPersistentData().putInt(FALLING, flown);
        if (dragon.getHealth() <= 0.0F || flown >= FLIGHT_LIMIT
                || dragon.getPhaseManager().getCurrentPhase().getPhase() != EnderDragonPhase.DYING) {
            land(level, dragon);
        }
    }

    private static void interlude(ServerLevel level, EnderDragon dragon) {
        int left = dragon.getPersistentData().getIntOr(INTERLUDE, 0) - 1;
        dragon.setHealth(Math.max(1.0F, dragon.getHealth()));

        // Vanilla's death, as the server can send it: a constant crackle of explosions round the body,
        // and the big bursts at the end.
        RandomSource random = dragon.getRandom();
        level.sendParticles(ParticleTypes.EXPLOSION,
                dragon.getX() + (random.nextFloat() - 0.5D) * 8.0D, dragon.getY() + 2.0D + (random.nextFloat() - 0.5D) * 4.0D,
                dragon.getZ() + (random.nextFloat() - 0.5D) * 8.0D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        if (left < 30 && left % 3 == 0) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    dragon.getX() + (random.nextFloat() - 0.5D) * 8.0D, dragon.getY() + 2.0D + (random.nextFloat() - 0.5D) * 4.0D,
                    dragon.getZ() + (random.nextFloat() - 0.5D) * 8.0D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        if (left < 60 && left % 2 == 0) {
            // The stirring: rot first, then the thing getting up out of it.
            puff(level, dragon, ParticleTypes.ITEM_SLIME, 10, 4.0D);
            puff(level, dragon, ParticleTypes.SCULK_SOUL, 3, 3.0D);
        }

        if (left > 0) {
            dragon.getPersistentData().putInt(INTERLUDE, left);
            return;
        }
        rise(level, dragon);
    }

    /** Tell every client with ZombieMod what to draw: dying (beams and dissolve) or risen (the rot). */
    private static void tell(ServerLevel level, EnderDragon dragon, boolean risen) {
        ZombieDragonPayload payload = new ZombieDragonPayload(dragon.getId(), risen);
        for (ServerPlayer player : level.players()) {
            Net.sendIfAble(player, payload);
        }
    }

    private static void rise(ServerLevel level, EnderDragon dragon) {
        dragon.getPersistentData().remove(INTERLUDE);
        Optional<Holder.Reference<Genus>> holder = genus(level);
        dragon.setNoAi(false);
        EntityState.setInvulnerable(dragon, false);
        if (holder.isEmpty()) {
            // The genus went away mid-interlude (a datapack removed). Let vanilla finish the death.
            dragon.setHealth(0.0F);
            return;
        }

        GenusApplier.assign(dragon, holder.get());
        GenusApplier.applyAi(dragon, holder.get().value());
        dragon.addTag(TAG);
        dragon.getPhaseManager().setPhase(EnderDragonPhase.TAKEOFF);

        bar(dragon).ifPresent(bar -> {
            bar.setColor(BossEvent.BossBarColor.GREEN);
            bar.setName(dragon.getDisplayName());
        });

        Optional<DragonSpec> spec = holder.get().value().dragon();
        cue(level, dragon, spec, "rise", 1.0F, SoundEvents.ENDER_DRAGON_GROWL, 20.0F, 0.6F, 512.0D);
        puff(level, dragon, ParticleTypes.ITEM_SLIME, 80, 6.0D);
        puff(level, dragon, ParticleTypes.SCULK_SOUL, 30, 5.0D);

        tell(level, dragon, true);
        LOG.info("ZombieMod: the Ender Dragon rises as {}", holder.get().key().identifier());
    }

    /**
     * Point the dragon at somebody, for abilities that work around a target (a wave summoned
     * {@code near_target}). The dragon's own phases choose whom to strafe and charge; this only
     * fills {@code getTarget()}, which they never read.
     */
    private static void hunt(ServerLevel level, EnderDragon dragon) {
        if (dragon.tickCount % 20 != 0) {
            return;
        }
        LivingEntity current = dragon.getTarget();
        if (current != null && current.isAlive() && current.distanceToSqr(dragon) < 160.0D * 160.0D
                && !(current instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            return;
        }
        LivingEntity nearest = null;
        for (LivingEntity candidate : Targets.nearbyPlayers(level, dragon, 160.0D)) {
            if (nearest == null || candidate.distanceToSqr(dragon) < nearest.distanceToSqr(dragon)) {
                nearest = candidate;
            }
        }
        dragon.setTarget(nearest);
    }

    // ------------------------------------------------------------------ the real death's drops

    /** Whatever it drops goes to whoever killed it; the dragon dies over the void as often as not. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !risen(dragon)
                || !(event.getSource().getEntity() instanceof Player killer)) {
            return;
        }
        for (ItemEntity drop : event.getDrops()) {
            drop.setPos(killer.getX(), killer.getY() + 0.5D, killer.getZ());
            drop.setDeltaMovement(Vec3.ZERO);
        }
    }

    // ------------------------------------------------------------------ rot breath

    @SubscribeEvent
    public void onCloud(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof AreaEffectCloud cloud) || !(event.getLevel() instanceof ServerLevel level)
                || !(cloud.getOwner() instanceof EnderDragon dragon) || !risen(dragon)) {
            return;
        }
        spec(level, dragon).flatMap(DragonSpec::breath).ifPresent(breath -> {
            cloud.setPotionContents(PotionContents.EMPTY);
            for (DragonSpec.Effect effect : breath.effects()) {
                cloud.addEffect(new MobEffectInstance(effect.effect(), effect.duration(), effect.amplifier()));
            }
            cloud.setCustomParticle(breath.particle());
            cloud.addTag(BREATH_TAG);
        });
        if (EntityState.hasTag(cloud, BREATH_TAG)) {
            cueAt(level, spec(level, dragon), "breath", cloud.getX(), cloud.getY(), cloud.getZ(), 1.0F,
                    null, 0.0F, 0.0F, 64.0D);
        }
    }

    @SubscribeEvent
    public void onCloudTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AreaEffectCloud cloud) || cloud.tickCount % 10 != 0
                || !(cloud.level() instanceof ServerLevel level) || !EntityState.hasTag(cloud, BREATH_TAG)
                || !(cloud.getOwner() instanceof EnderDragon dragon)) {
            return;
        }
        Optional<DragonSpec.Infection> infection = spec(level, dragon).flatMap(DragonSpec::breath)
                .flatMap(DragonSpec.Breath::infect);
        if (infection.isEmpty()) {
            return;
        }
        DragonSpec.Infection infect = infection.get();
        long now = level.getGameTime();
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, cloud.getBoundingBox())) {
            if (victim == dragon || !victim.isAlive() || Infect.remaining(victim, now) > 0
                    || victim instanceof Player p && (p.isCreative() || p.isSpectator())
                    || level.getRandom().nextFloat() >= infect.chance()) {
                continue;
            }
            Infect.mark(level, victim, infect.effect(), infect.duration(), infect.genus(), true);
        }
    }

    // ------------------------------------------------------------------ late joiners

    /** Anyone who starts seeing a risen dragon - logging in, coming through the portal - is told. */
    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof EnderDragon dragon) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (risen(dragon)) {
            Net.sendIfAble(player, new ZombieDragonPayload(dragon.getId(), true));
        } else if (dragon.getPersistentData().contains(INTERLUDE)) {
            Net.sendIfAble(player, new ZombieDragonPayload(dragon.getId(), false));
        }
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Growls now and then, and roars - at random, and on crossing each phase threshold, which is
     * when the waves arrive. Vanilla clients already growl on their own (the dragon's growl is played
     * client-side), so none of this has a fallback.
     */
    private static void voice(ServerLevel level, EnderDragon dragon) {
        if (dragon.tickCount % 20 != 0) {
            return;
        }
        Optional<DragonSpec> spec = spec(level, dragon);
        var genus = GenusApplier.genusOf(dragon, level);
        if (genus.isPresent() && dragon.getMaxHealth() > 0.0F) {
            double fraction = dragon.getHealth() / dragon.getMaxHealth();
            int crossed = (int) genus.get().value().phases().stream()
                    .filter(phase -> fraction <= phase.belowHealth()).count();
            int heard = dragon.getPersistentData().getIntOr(PHASES_ROARED, 0);
            if (crossed > heard) {
                dragon.getPersistentData().putInt(PHASES_ROARED, crossed);
                cue(level, dragon, spec, "roar", 1.0F, null, 0.0F, 0.0F, 256.0D);
                return;
            }
        }
        float roll = level.getRandom().nextFloat();
        if (roll < 1.0F / 30.0F) {
            cue(level, dragon, spec, "roar", 0.9F + level.getRandom().nextFloat() * 0.2F, null, 0.0F, 0.0F, 256.0D);
        } else if (roll < 1.0F / 30.0F + 1.0F / 10.0F) {
            cue(level, dragon, spec, "growl", 0.9F + level.getRandom().nextFloat() * 0.2F, null, 0.0F, 0.0F, 160.0D);
        }
    }

    private static void cue(ServerLevel level, EnderDragon dragon, Optional<DragonSpec> spec, String cue,
            float pitch, SoundEvent fallback, float fallbackVolume, float fallbackPitch, double range) {
        cueAt(level, spec, cue, dragon.getX(), dragon.getY(), dragon.getZ(), pitch, fallback, fallbackVolume,
                fallbackPitch, range);
    }

    /**
     * Plays one of the genus's {@code dragon.sounds}, chosen per listener.
     *
     * <p><b>Our own sounds are never registered</b>, deliberately. A {@code SoundEvent} in the registry
     * travels to clients as a registry number, and a vanilla client has no such number - an
     * unregistered one travels as its id instead ({@code Holder.direct}), which a client with our
     * {@code sounds.json} plays and a vanilla client would only shrug at. So a listener with ZombieMod
     * installed gets the id; anyone else gets the vanilla {@code fallback}, if the cue has one. An id
     * another mod <em>has</em> registered (Threadwork's) is sent as-is to everybody: that mod's
     * players already have it.
     */
    private static void cueAt(ServerLevel level, Optional<DragonSpec> spec, String cue, double x, double y, double z,
            float pitch, SoundEvent fallback, float fallbackVolume, float fallbackPitch, double range) {
        Identifier id = spec.map(s -> s.sounds().get(cue)).orElse(null);
        Optional<? extends Holder<SoundEvent>> registered = id == null ? Optional.empty()
                : BuiltInRegistries.SOUND_EVENT.get(id);
        Holder<SoundEvent> direct = id == null ? null : Holder.direct(SoundEvent.createVariableRangeEvent(id));
        long seed = level.getRandom().nextLong();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) > range * range) {
                continue;
            }
            if (registered.isPresent()) {
                send(player, registered.get(), x, y, z, 1.0F, pitch, seed);
            } else if (direct != null && Net.listening(player)) {
                send(player, direct, x, y, z, 1.0F, pitch, seed);
            } else if (fallback != null) {
                send(player, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(fallback), x, y, z, fallbackVolume,
                        fallbackPitch, seed);
            }
        }
    }

    private static void send(ServerPlayer player, Holder<SoundEvent> sound, double x, double y, double z,
            float volume, float pitch, long seed) {
        player.connection.send(new ClientboundSoundPacket(sound, SoundSource.HOSTILE, x, y, z, volume, pitch, seed));
    }

    private static void puff(ServerLevel level, EnderDragon dragon, ParticleOptions particle, int count, double spread) {
        level.sendParticles(particle, dragon.getX(), dragon.getY() + 2.0D, dragon.getZ(), count,
                spread, spread * 0.5D, spread, 0.02D);
    }

    /**
     * The fight's boss bar. Private in vanilla, and the fight class itself is renamed between our
     * versions ({@code EndDragonFight} to {@code EnderDragonFight}), so it is found by its type
     * rather than by any name: the one {@code ServerBossEvent} field on whatever the fight is.
     */
    private static Optional<ServerBossEvent> bar(EnderDragon dragon) {
        Object fight = dragon.getDragonFight();
        if (fight == null) {
            return Optional.empty();
        }
        try {
            if (!barFieldSearched) {
                barFieldSearched = true;
                for (Field field : fight.getClass().getDeclaredFields()) {
                    if (ServerBossEvent.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        barField = field;
                        break;
                    }
                }
                if (barField == null) {
                    LOG.warn("ZombieMod: no boss bar found on {}; the Zombie Dragon keeps the vanilla bar",
                            fight.getClass().getName());
                }
            }
            return barField == null ? Optional.empty() : Optional.ofNullable((ServerBossEvent) barField.get(fight));
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("ZombieMod: could not reach the dragon fight's boss bar", e);
            return Optional.empty();
        }
    }
}
