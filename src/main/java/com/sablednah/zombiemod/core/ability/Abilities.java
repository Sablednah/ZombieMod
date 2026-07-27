package com.sablednah.zombiemod.core.ability;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The abilities ZombieMod ships.
 *
 * <p>Chosen to cover the 1.8 plugin's list by composition rather than one-for-one: {@code effect}
 * plus {@code particles} plus {@code sound} between them express most of the old flavour abilities,
 * so a genus author builds a "screamer" or a "plague carrier" out of parts rather than waiting for
 * someone to add that exact ability.
 */
public final class Abilities {

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("zombiemod", path);
    }

    /** Shared timing fields, so every ability spells them the same way. */
    private static <T extends Ability> RecordCodecBuilder<T, Integer> intervalField(int fallback) {
        return Codec.INT.optionalFieldOf("interval", fallback).forGetter(Ability::interval);
    }

    private static <T extends Ability> RecordCodecBuilder<T, Float> chanceField(float fallback) {
        return Codec.FLOAT.optionalFieldOf("chance", fallback).forGetter(Ability::chance);
    }

    /** Who an ability aims at. */
    public enum Target {
        SELF, VICTIM, NEARBY_PLAYERS;

        public static final Codec<Target> CODEC = Codec.STRING.xmap(
                s -> valueOf(s.toUpperCase(java.util.Locale.ROOT)),
                t -> t.name().toLowerCase(java.util.Locale.ROOT));
    }

    // ------------------------------------------------------------------ effects

    /**
     * Apply a potion effect. The workhorse — the 1.8 plugin's per-genus {@code potions} list, but
     * aimable, so a genus can poison whoever it hits rather than only buffing itself.
     */
    public record Effect(int interval, float chance, Target target, Holder<MobEffect> effect,
            int duration, int amplifier, double radius) implements Ability {

        public static final Identifier TYPE = id("effect");

        public static final MapCodec<Effect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Effect>intervalField(40),
                Abilities.<Effect>chanceField(1.0F),
                Target.CODEC.optionalFieldOf("target", Target.SELF).forGetter(Effect::target),
                BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(Effect::effect),
                Codec.INT.optionalFieldOf("duration", 100).forGetter(Effect::duration),
                Codec.INT.optionalFieldOf("amplifier", 0).forGetter(Effect::amplifier),
                Codec.DOUBLE.optionalFieldOf("radius", 8.0D).forGetter(Effect::radius))
                .apply(i, Effect::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public void run(ServerLevel level, Mob mob) {
            for (LivingEntity victim : Targets.of(target, level, mob, radius)) {
                victim.addEffect(new MobEffectInstance(effect, duration, amplifier));
            }
        }
    }

    /** Regenerate. The old HEAL ability. */
    public record Heal(int interval, float chance, float amount) implements Ability {

        public static final Identifier TYPE = id("heal");

        public static final MapCodec<Heal> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Heal>intervalField(60),
                Abilities.<Heal>chanceField(1.0F),
                Codec.FLOAT.optionalFieldOf("amount", 1.0F).forGetter(Heal::amount))
                .apply(i, Heal::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public void run(ServerLevel level, Mob mob) {
            if (mob.getHealth() < mob.getMaxHealth()) {
                mob.heal(amount);
            }
        }
    }

    // ------------------------------------------------------------------ violence

    /** Call down lightning. The old LIGHTNING ability. */
    public record Lightning(int interval, float chance, Target target, double radius, boolean visualOnly)
            implements Ability {

        public static final Identifier TYPE = id("lightning");

        public static final MapCodec<Lightning> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Lightning>intervalField(100),
                Abilities.<Lightning>chanceField(0.25F),
                Target.CODEC.optionalFieldOf("target", Target.VICTIM).forGetter(Lightning::target),
                Codec.DOUBLE.optionalFieldOf("radius", 16.0D).forGetter(Lightning::radius),
                Codec.BOOL.optionalFieldOf("visual_only", false).forGetter(Lightning::visualOnly))
                .apply(i, Lightning::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public void run(ServerLevel level, Mob mob) {
            for (LivingEntity victim : Targets.of(target, level, mob, radius)) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level,
                        net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                if (bolt == null) {
                    return;
                }
                bolt.snapTo(victim.getX(), victim.getY(), victim.getZ(), 0.0F, 0.0F);
                bolt.setVisualOnly(visualOnly);
                level.addFreshEntity(bolt);
            }
        }
    }

    /**
     * Detonate. The old EXPLODE ability.
     *
     * <p>{@code destroy_blocks} defaults to <b>false</b>: a zombie that eats the terrain is a very
     * different proposition from one that hurts, and griefing should be opted into rather than
     * discovered.
     */
    public record Explode(int interval, float chance, float power, boolean destroyBlocks, boolean killsSelf,
            double triggerRadius) implements Ability {

        public static final Identifier TYPE = id("explode");

        public static final MapCodec<Explode> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Explode>intervalField(20),
                Abilities.<Explode>chanceField(1.0F),
                Codec.FLOAT.optionalFieldOf("power", 2.0F).forGetter(Explode::power),
                Codec.BOOL.optionalFieldOf("destroy_blocks", false).forGetter(Explode::destroyBlocks),
                Codec.BOOL.optionalFieldOf("kills_self", true).forGetter(Explode::killsSelf),
                Codec.DOUBLE.optionalFieldOf("trigger_radius", 3.0D).forGetter(Explode::triggerRadius))
                .apply(i, Explode::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public void run(ServerLevel level, Mob mob) {
            // Only when something is actually close - an interval-timed bomb that goes off in an
            // empty field is just a mob that deletes itself.
            if (Targets.nearbyPlayers(level, mob, triggerRadius).isEmpty()) {
                return;
            }
            level.explode(mob, mob.getX(), mob.getY(), mob.getZ(), power,
                    destroyBlocks ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
            if (killsSelf) {
                mob.discard();
            }
        }
    }

    /**
     * Prime, swell, detonate — and stand down if you back off.
     *
     * <p>A creeper's actual swell can't be borrowed: {@code DATA_SWELL_DIR} is defined against
     * {@code Creeper.class} and it is {@code CreeperRenderer} that inflates the model, so a vanilla
     * client has no way to draw a swelling zombie. What it <em>can</em> draw is the
     * {@code minecraft:scale} attribute, which is synced — so this ramps scale over the fuse and the
     * mob genuinely inflates, no client mod involved.
     *
     * <p>The rest is assembled from things a vanilla client already knows: the creeper's own primed
     * sound, smoke, and pinning the mob in place while it burns. Unwinds at double speed when the
     * trigger leaves, which is what creepers do and what makes them fair.
     */
    public record Fuse(int interval, float chance, int fuseTicks, double triggerRadius, double swellTo,
            float power, boolean destroyBlocks, boolean killsSelf, Holder<SoundEvent> sound)
            implements Ability {

        public static final Identifier TYPE = id("fuse");

        public static final MapCodec<Fuse> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Fuse>intervalField(1),
                Abilities.<Fuse>chanceField(1.0F),
                Codec.INT.optionalFieldOf("fuse_ticks", 30).forGetter(Fuse::fuseTicks),
                Codec.DOUBLE.optionalFieldOf("trigger_radius", 3.0D).forGetter(Fuse::triggerRadius),
                Codec.DOUBLE.optionalFieldOf("swell_to", 1.5D).forGetter(Fuse::swellTo),
                Codec.FLOAT.optionalFieldOf("power", 3.0F).forGetter(Fuse::power),
                Codec.BOOL.optionalFieldOf("destroy_blocks", false).forGetter(Fuse::destroyBlocks),
                Codec.BOOL.optionalFieldOf("kills_self", true).forGetter(Fuse::killsSelf),
                BuiltInRegistries.SOUND_EVENT.holderByNameCodec()
                        .optionalFieldOf("sound", BuiltInRegistries.SOUND_EVENT
                                .wrapAsHolder(net.minecraft.sounds.SoundEvents.CREEPER_PRIMED))
                        .forGetter(Fuse::sound))
                .apply(i, Fuse::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        /** Never called — this ability is stateful. */
        @Override
        public void run(ServerLevel level, Mob mob) {}

        @Override
        public State newState() {
            return new FuseState(this);
        }
    }

    /** The burn-down for one {@link Fuse} on one mob. */
    private static final class FuseState implements Ability.State {

        private final Fuse fuse;
        private int burning;
        private double baseScale = Double.NaN;

        FuseState(Fuse fuse) {
            this.fuse = fuse;
        }

        @Override
        public void tick(ServerLevel level, Mob mob) {
            var scale = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
            if (Double.isNaN(baseScale) && scale != null) {
                // Capture the genus's own scale once, so swelling is relative to whatever size this
                // thing already is rather than snapping to a fixed number.
                baseScale = scale.getBaseValue();
            }

            boolean triggered = !Targets.nearbyPlayers(level, mob, fuse.triggerRadius()).isEmpty();

            if (triggered) {
                if (burning == 0) {
                    level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), fuse.sound().value(),
                            SoundSource.HOSTILE, 1.0F, 0.5F);
                }
                burning++;
                // Stop advancing: a bomb that keeps chasing while it swells reads as a bug, and it
                // denies the player the one counter-play a creeper offers.
                mob.getNavigation().stop();
            } else if (burning > 0) {
                burning = Math.max(0, burning - 2);
            } else {
                return;
            }

            float progress = Math.min(1.0F, (float) burning / Math.max(1, fuse.fuseTicks()));
            if (scale != null && !Double.isNaN(baseScale)) {
                scale.setBaseValue(baseScale * (1.0D + (fuse.swellTo() - 1.0D) * progress));
            }
            if (burning > 0 && burning % 4 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + mob.getBbHeight() * 0.7D,
                        mob.getZ(), 3, 0.15D, 0.1D, 0.15D, 0.01D);
            }

            if (burning >= fuse.fuseTicks()) {
                level.explode(mob, mob.getX(), mob.getY(), mob.getZ(), fuse.power(),
                        fuse.destroyBlocks() ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
                if (fuse.killsSelf()) {
                    mob.discard();
                } else {
                    burning = 0;
                    if (scale != null && !Double.isNaN(baseScale)) {
                        scale.setBaseValue(baseScale);
                    }
                }
            }
        }
    }

    /** Knock everything nearby into the air and hurt it. The old STOMP / SHOCKWAVE. */
    public record Shockwave(int interval, float chance, double radius, float damage, double knockup)
            implements Ability {

        public static final Identifier TYPE = id("shockwave");

        public static final MapCodec<Shockwave> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Shockwave>intervalField(60),
                Abilities.<Shockwave>chanceField(0.5F),
                Codec.DOUBLE.optionalFieldOf("radius", 5.0D).forGetter(Shockwave::radius),
                Codec.FLOAT.optionalFieldOf("damage", 2.0F).forGetter(Shockwave::damage),
                Codec.DOUBLE.optionalFieldOf("knockup", 0.8D).forGetter(Shockwave::knockup))
                .apply(i, Shockwave::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public void run(ServerLevel level, Mob mob) {
            List<LivingEntity> victims = Targets.nearbyPlayers(level, mob, radius);
            if (victims.isEmpty()) {
                return;
            }
            for (LivingEntity victim : victims) {
                victim.hurtServer(level, level.damageSources().mobAttack(mob), damage);
                Vec3 v = victim.getDeltaMovement();
                victim.setDeltaMovement(v.x, v.y + knockup, v.z);
                victim.hurtMarked = true; // or the client never sees the launch
            }
            level.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY(), mob.getZ(), 8,
                    radius / 3, 0.2D, radius / 3, 0.0D);
        }
    }

    /**
     * Launch at the current victim. The L4D Hunter, the Charger, any leaper.
     *
     * <p>Deliberately a pounce and not teleportation: it sets velocity toward the target, so walls
     * and ceilings still stop it and the player can dodge.
     */
    public record Leap(int interval, float chance, double range, double power, double lift)
            implements Ability {

        public static final Identifier TYPE = id("leap");

        public static final MapCodec<Leap> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Leap>intervalField(60),
                Abilities.<Leap>chanceField(0.5F),
                Codec.DOUBLE.optionalFieldOf("range", 8.0D).forGetter(Leap::range),
                Codec.DOUBLE.optionalFieldOf("power", 0.9D).forGetter(Leap::power),
                Codec.DOUBLE.optionalFieldOf("lift", 0.45D).forGetter(Leap::lift))
                .apply(i, Leap::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public void run(ServerLevel level, Mob mob) {
            LivingEntity victim = mob.getTarget();
            if (victim == null || !victim.isAlive() || !mob.onGround()) {
                return;
            }
            double distSqr = victim.distanceToSqr(mob);
            if (distSqr > range * range || distSqr < 4.0D) {
                return; // too far to reach, or already on top of them
            }
            Vec3 toward = new Vec3(victim.getX() - mob.getX(), 0.0D, victim.getZ() - mob.getZ()).normalize();
            mob.setDeltaMovement(toward.x * power, lift, toward.z * power);
            mob.hurtMarked = true;
        }
    }

    /**
     * Spawn more of something. The 1.8 BREEDER, and every horde-that-grows trope.
     *
     * <p>{@code max_nearby} is not optional politeness — a breeder without a cap is a server-killing
     * exponential, and the one thing a datapack author will forget.
     */
    public record Summon(int interval, float chance, EntityType<?> entity, int count, int maxNearby,
            double radius) implements Ability {

        public static final Identifier TYPE = id("summon");

        public static final MapCodec<Summon> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Summon>intervalField(200),
                Abilities.<Summon>chanceField(0.25F),
                BuiltInRegistries.ENTITY_TYPE.byNameCodec()
                        .optionalFieldOf("entity", (EntityType<?>) EntityType.ZOMBIE).forGetter(Summon::entity),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Summon::count),
                Codec.INT.optionalFieldOf("max_nearby", 6).forGetter(Summon::maxNearby),
                Codec.DOUBLE.optionalFieldOf("radius", 8.0D).forGetter(Summon::radius))
                .apply(i, Summon::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public void run(ServerLevel level, Mob mob) {
            int nearby = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    mob.getBoundingBox().inflate(radius), e -> e.getType() == entity).size();
            if (nearby >= maxNearby) {
                return;
            }
            for (int n = 0; n < count && nearby + n < maxNearby; n++) {
                var spawned = entity.create(level, net.minecraft.world.entity.EntitySpawnReason.REINFORCEMENT);
                if (spawned == null) {
                    return;
                }
                spawned.snapTo(mob.getX() + (mob.getRandom().nextDouble() - 0.5D) * 3.0D, mob.getY(),
                        mob.getZ() + (mob.getRandom().nextDouble() - 0.5D) * 3.0D,
                        mob.getRandom().nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(spawned);
            }
        }
    }

    /**
     * Drag the victim toward you. The Smoker's tongue, minus the tongue.
     */
    public record Pull(int interval, float chance, double range, double power) implements Ability {

        public static final Identifier TYPE = id("pull");

        public static final MapCodec<Pull> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Pull>intervalField(80),
                Abilities.<Pull>chanceField(0.4F),
                Codec.DOUBLE.optionalFieldOf("range", 12.0D).forGetter(Pull::range),
                Codec.DOUBLE.optionalFieldOf("power", 0.6D).forGetter(Pull::power))
                .apply(i, Pull::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public void run(ServerLevel level, Mob mob) {
            for (LivingEntity victim : Targets.nearbyPlayers(level, mob, range)) {
                if (!mob.hasLineOfSight(victim)) {
                    continue;
                }
                Vec3 toward = new Vec3(mob.getX() - victim.getX(), 0.0D, mob.getZ() - victim.getZ()).normalize();
                Vec3 v = victim.getDeltaMovement();
                victim.setDeltaMovement(v.x + toward.x * power, v.y + 0.15D, v.z + toward.z * power);
                victim.hurtMarked = true;
            }
        }
    }

    // ------------------------------------------------------------------ flavour

    /** Emit particles. Pure decoration, and the cheapest way to make a genus recognisable. */
    public record Particles(int interval, float chance, ParticleOptions particle, int count, double spread)
            implements Ability {

        public static final Identifier TYPE = id("particles");

        /**
         * Vanilla's particle codec insists on the object form, {@code {"type": "minecraft:smoke"}},
         * because a few particles carry extra data. The overwhelming majority don't, so accept a
         * bare id as well and keep genus files readable. The object form still works for the ones
         * that need it (dust colours, block/item particles).
         */
        private static final Codec<ParticleOptions> PARTICLE_CODEC = Codec.withAlternative(
                ParticleTypes.CODEC,
                Identifier.CODEC.comapFlatMap(
                        pid -> BuiltInRegistries.PARTICLE_TYPE.getValue(pid) instanceof SimpleParticleType simple
                                ? com.mojang.serialization.DataResult.success((ParticleOptions) simple)
                                : com.mojang.serialization.DataResult.error(() -> "Unknown particle '" + pid
                                        + "', or it needs the object form {\"type\": \"" + pid + "\", ...}"),
                        options -> BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType())));

        public static final MapCodec<Particles> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Particles>intervalField(10),
                Abilities.<Particles>chanceField(1.0F),
                PARTICLE_CODEC.fieldOf("particle").forGetter(Particles::particle),
                Codec.INT.optionalFieldOf("count", 6).forGetter(Particles::count),
                Codec.DOUBLE.optionalFieldOf("spread", 0.4D).forGetter(Particles::spread))
                .apply(i, Particles::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public void run(ServerLevel level, Mob mob) {
            level.sendParticles(particle, mob.getX(), mob.getY() + mob.getBbHeight() * 0.6D, mob.getZ(),
                    count, spread, spread, spread, 0.02D);
        }
    }

    /** Make a noise. A stalker that occasionally groans from the dark is a different animal. */
    public record Sound(int interval, float chance, Holder<SoundEvent> sound, float volume, float pitch)
            implements Ability {

        public static final Identifier TYPE = id("sound");

        public static final MapCodec<Sound> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Sound>intervalField(80),
                Abilities.<Sound>chanceField(0.5F),
                BuiltInRegistries.SOUND_EVENT.holderByNameCodec().fieldOf("sound").forGetter(Sound::sound),
                Codec.FLOAT.optionalFieldOf("volume", 1.0F).forGetter(Sound::volume),
                Codec.FLOAT.optionalFieldOf("pitch", 1.0F).forGetter(Sound::pitch))
                .apply(i, Sound::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public void run(ServerLevel level, Mob mob) {
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), sound.value(),
                    SoundSource.HOSTILE, volume, pitch);
        }
    }

    private Abilities() {}
}
