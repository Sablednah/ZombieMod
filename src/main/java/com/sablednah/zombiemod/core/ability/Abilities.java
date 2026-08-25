package com.sablednah.zombiemod.core.ability;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.sablednah.zombiemod.platform.BlockTypes;
import com.sablednah.zombiemod.platform.Types;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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

    /**
     * May this mob change the world right now?
     *
     * <p>Goes through NeoForge's {@code canEntityGrief} rather than reading the {@code mobGriefing}
     * gamerule directly. The two are not equivalent: the hook fires {@code EntityMobGriefingEvent},
     * which is the documented way for a land-protection mod to veto griefing <em>per entity and per
     * position</em>. Reading the gamerule works, and silently ignores every claim mod on the server —
     * a breaker would happily eat its way into a protected base while the owner wondered why their
     * claim did nothing.
     *
     * <p>The hook still consults the gamerule when nothing objects, so the global off switch is
     * unaffected.
     */
    private static boolean canGrief(ServerLevel level, Mob mob) {
        return net.neoforged.neoforge.event.EventHooks.canEntityGrief(level, mob);
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
        public String label() {
            return "Effect (" + pretty(BuiltInRegistries.MOB_EFFECT.getKey(effect.value())) + ")";
        }

        @Override
        public String describe() {
            String name = pretty(BuiltInRegistries.MOB_EFFECT.getKey(effect.value()));
            String reach = target == Target.NEARBY_PLAYERS ? " within " + trim(radius) + " blocks" : "";
            return "Applies " + name + (amplifier > 0 ? " " + (amplifier + 1) : "")
                    + " to " + who(target) + reach + " for " + seconds(duration) + ".";
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
        public String describe() {
            return "Closes its own wounds, " + trim(amount) + " health at a time.";
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
        public String describe() {
            return visualOnly
                    ? "Calls down lightning that flashes but does not burn."
                    : "Calls down real lightning on " + who(target) + ".";
        }


        @Override
        public void run(ServerLevel level, Mob mob) {
            for (LivingEntity victim : Targets.of(target, level, mob, radius)) {
                if (!(Types.lightningBolt().create(level,
                        net.minecraft.world.entity.EntitySpawnReason.TRIGGERED)
                        instanceof LightningBolt bolt)) {
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
        public String describe() {
            return "Detonates with power " + trim(power)
                    + (destroyBlocks ? ", breaking blocks" : ", leaving the ground intact")
                    + (killsSelf ? ". It does not survive it." : ". It survives.");
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

        @Override
        public String describe() {
            return "Swells and counts down " + seconds(fuseTicks) + " once you are within "
                    + trim(triggerRadius) + " blocks.";
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
        public String describe() {
            return "Slams the ground, throwing back everything within " + trim(radius) + " blocks"
                    + (damage > 0 ? " for " + trim(damage) + " damage." : ".");
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
        public String describe() {
            return "Jumps at you from up to " + trim(range) + " blocks away.";
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
            double radius, Optional<Identifier> genus) implements Ability {

        public static final Identifier TYPE = id("summon");

        /**
         * Dresses a summoned mob in a genus. Injected by the loader half at startup, exactly as
         * {@link Convert} takes its raiser: core cannot reach {@code GenusApplier} without a
         * dependency pointing the wrong way, and a hive that breeds plain zombies instead of its
         * own kind would be a quiet lie.
         */
        private static java.util.function.BiConsumer<Mob, Identifier> dresser = (mob, id) -> {};

        public static void setDresser(java.util.function.BiConsumer<Mob, Identifier> dress) {
            dresser = dress;
        }

        public static final MapCodec<Summon> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Summon>intervalField(200),
                Abilities.<Summon>chanceField(0.25F),
                BuiltInRegistries.ENTITY_TYPE.byNameCodec()
                        .optionalFieldOf("entity", Types.zombie()).forGetter(Summon::entity),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Summon::count),
                Codec.INT.optionalFieldOf("max_nearby", 6).forGetter(Summon::maxNearby),
                Codec.DOUBLE.optionalFieldOf("radius", 8.0D).forGetter(Summon::radius),
                Identifier.CODEC.optionalFieldOf("genus").forGetter(Summon::genus))
                .apply(i, Summon::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public String describe() {
            String what = genus.map(g -> g.getPath().replace('_', ' '))
                    .orElse(pretty(BuiltInRegistries.ENTITY_TYPE.getKey(entity)).toLowerCase());
            return "Calls up " + count + " " + what
                    + (count == 1 ? "" : "s") + ", up to " + maxNearby + " at once.";
        }


        @Override
        public void run(ServerLevel level, Mob mob) {
            // With a genus set the cap counts that genus, not the raw entity type - a hive capped
            // on "zombies nearby" would starve itself in any crowd of ordinary dead.
            java.util.function.Predicate<net.minecraft.world.entity.LivingEntity> kin =
                    genus.<java.util.function.Predicate<net.minecraft.world.entity.LivingEntity>>map(
                            id -> e -> e instanceof Mob m && m.getPersistentData()
                                    .getString("zombiemod:genus").map(id.toString()::equals).orElse(false))
                            .orElse(e -> e.getType() == entity);
            int nearby = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    mob.getBoundingBox().inflate(radius), kin).size();
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
                if (genus.isPresent() && spawned instanceof Mob spawnedMob) {
                    dresser.accept(spawnedMob, genus.get());
                }
            }
        }
    }

    /**
     * Point everything nearby at whatever you're fighting.
     *
     * <p>The scariest moment in zombie fiction isn't any one monster — it's one of them noticing you
     * and the rest turning around. This is that: when the mob has a target, hand that target to
     * every eligible mob in radius that doesn't already have one.
     *
     * <p>Two deliberate restraints. It only fires when the caller <em>has</em> a target, so it can't
     * manufacture aggro out of nothing; and it skips mobs already fighting something, so a screamer
     * can't yank a horde off the player it has cornered. {@code max_alerted} caps the sweep — an
     * uncapped one in a mob farm is a tick-time problem.
     */
    public record Alert(int interval, float chance, double radius, Class<? extends LivingEntity> who,
            int maxAlerted) implements Ability {

        public static final Identifier TYPE = id("alert");

        public static final MapCodec<Alert> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Alert>intervalField(40),
                Abilities.<Alert>chanceField(1.0F),
                Codec.DOUBLE.optionalFieldOf("radius", 16.0D).forGetter(Alert::radius),
                com.sablednah.zombiemod.core.goal.TargetClass.CODEC
                        .optionalFieldOf("who", (Class<? extends LivingEntity>) net.minecraft.world.entity.monster.Monster.class)
                        .forGetter(Alert::who),
                Codec.INT.optionalFieldOf("max_alerted", 12).forGetter(Alert::maxAlerted))
                .apply(i, Alert::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public String describe() {
            return "Screams, and everything within " + trim(radius) + " blocks comes to look.";
        }


        @Override
        public void run(ServerLevel level, Mob mob) {
            LivingEntity victim = mob.getTarget();
            if (victim == null || !victim.isAlive()) {
                return;
            }
            int alerted = 0;
            for (LivingEntity other : level.getEntitiesOfClass(who, mob.getBoundingBox().inflate(radius))) {
                if (alerted >= maxAlerted) {
                    break;
                }
                if (other == mob || other == victim || !(other instanceof Mob listener)) {
                    continue;
                }
                if (listener.getTarget() != null) {
                    continue; // already busy - don't pull it off its current fight
                }
                listener.setTarget(victim);
                alerted++;
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
        public String describe() {
            return "Drags you towards it from up to " + trim(range) + " blocks away.";
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

    /**
     * Blink. The 1.8 mod had an ender zombie that appeared directly behind you, already facing you,
     * and it was the nastiest thing in the roster.
     *
     * <p>{@code behind} is the reason this exists. It reads the victim's own facing and lands on the
     * far side of them, then turns to face their back — so the tell isn't seeing it move, it's the
     * sound behind you. {@code toward} is the ordinary enderman blink and {@code away} is a
     * hit-and-run.
     *
     * <p>{@code only_when_unseen} makes it strictly worse to fight: it will not blink while you are
     * looking at it, so it closes the distance every time you turn around. Off by default, because
     * on by default is genuinely unpleasant.
     */
    public record Teleport(int interval, float chance, Mode mode, double range, double distance,
            boolean onlyWhenUnseen, double minDistance, boolean onProjectile, float vanishChance)
            implements Ability {

        public static final Identifier TYPE = id("teleport");

        public enum Mode {
            BEHIND, TOWARD, AWAY;

            public static final Codec<Mode> CODEC = Codec.STRING.xmap(
                    v -> valueOf(v.toUpperCase(java.util.Locale.ROOT)),
                    v -> v.name().toLowerCase(java.util.Locale.ROOT));
        }

        public static final MapCodec<Teleport> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Teleport>intervalField(70),
                Abilities.<Teleport>chanceField(0.5F),
                Mode.CODEC.optionalFieldOf("mode", Mode.BEHIND).forGetter(Teleport::mode),
                Codec.DOUBLE.optionalFieldOf("range", 24.0D).forGetter(Teleport::range),
                Codec.DOUBLE.optionalFieldOf("distance", 2.0D).forGetter(Teleport::distance),
                Codec.BOOL.optionalFieldOf("only_when_unseen", false).forGetter(Teleport::onlyWhenUnseen),
                Codec.DOUBLE.optionalFieldOf("min_distance", 0.0D).forGetter(Teleport::minDistance),
                Codec.BOOL.optionalFieldOf("on_projectile", false).forGetter(Teleport::onProjectile),
                Codec.FLOAT.optionalFieldOf("vanish_chance", 0.0F).forGetter(Teleport::vanishChance))
                .apply(i, Teleport::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public String describe() {
            return switch (mode) {
                case BEHIND -> "Vanishes and reappears behind you.";
                case TOWARD -> "Closes the distance without crossing it.";
                case AWAY -> "Blinks away, up to " + trim(range) + " blocks.";
            };
        }


        @Override
        public void run(ServerLevel level, Mob mob) {
            LivingEntity victim = mob.getTarget();
            if (victim == null || !victim.isAlive()) {
                return;
            }
            double distSqr = victim.distanceToSqr(mob);
            if (distSqr > range * range) {
                return;
            }
            // Too close overrides everything, including only_when_unseen: cornering it is exactly
            // when it should not be standing there.
            boolean crowded = minDistance > 0.0D && distSqr < minDistance * minDistance;
            if (!crowded && onlyWhenUnseen && isWatching(victim, mob)) {
                return;
            }

            if (vanishChance > 0.0F && mob.getRandom().nextFloat() < vanishChance) {
                vanish(level, mob);
                return;
            }

            Vec3 destination = switch (mode) {
                // The victim's own look direction, flipped - their back.
                case BEHIND -> victim.position().subtract(victim.getLookAngle().multiply(distance, 0.0D, distance));
                case TOWARD -> victim.position().add(
                        victim.position().subtract(mob.position()).normalize().scale(-distance));
                case AWAY -> mob.position().add(
                        mob.position().subtract(victim.position()).normalize().scale(distance));
            };

            Vec3 from = mob.position();
            // randomTeleport is what the enderman uses: it walks down to solid ground and refuses
            // to land in water or inside a block, so we don't have to hunt for a safe spot.
            if (!mob.randomTeleport(destination.x, destination.y, destination.z, false)) {
                return;
            }

            // Turn to face them. Body and head both, or it arrives looking the way it set off.
            Vec3 toVictim = victim.position().subtract(mob.position());
            float yaw = (float) (Math.toDegrees(Math.atan2(toVictim.z, toVictim.x)) - 90.0D);
            mob.setYRot(yaw);
            mob.setYBodyRot(yaw);
            mob.setYHeadRot(yaw);
            mob.getLookControl().setLookAt(victim, 90.0F, 90.0F);

            level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.HOSTILE, 1.0F, 1.0F);
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.HOSTILE, 1.0F, 1.0F);
            level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1.0D, from.z, 24, 0.4D, 0.6D, 0.4D, 0.1D);
            level.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + 1.0D, mob.getZ(),
                    24, 0.4D, 0.6D, 0.4D, 0.1D);
        }

        /**
         * Blink away when shot, regardless of the interval. Melee is deliberately excluded — if you
         * got close enough to hit it with a sword it should have to wear that.
         */
        @Override
        public float onHurt(ServerLevel level, Mob mob, net.minecraft.world.damagesource.DamageSource source,
                float amount) {
            if (onProjectile && source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) {
                run(level, mob);
            }
            return amount;
        }

        /** Leave, with the sound and particles but without the corpse. */
        private static void vanish(ServerLevel level, Mob mob) {
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.HOSTILE, 1.0F, 0.7F);
            level.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + 1.0D, mob.getZ(),
                    40, 0.4D, 0.8D, 0.4D, 0.15D);
            mob.discard();
        }

        /** Is the victim looking more or less at this mob? */
        private static boolean isWatching(LivingEntity victim, Mob mob) {
            Vec3 look = victim.getViewVector(1.0F).normalize();
            Vec3 toMob = mob.position().subtract(victim.getEyePosition()).normalize();
            // ~53 degrees either side of straight ahead - roughly "on screen".
            return look.dot(toMob) > 0.6D && victim.hasLineOfSight(mob);
        }
    }

    /**
     * Chew through whatever is in the way. The 1.8 {@code Break}/{@code BreakRunner} pair, and the
     * single most consequential thing the original did — it is the difference between a wall being
     * a solution to zombies and a wall being a delay.
     *
     * <p>Only fires when the mob has a target it cannot reach and has stopped making progress, so
     * it eats walls rather than scenery. {@code allowed} has no default: a genus must name what it
     * may break, which with the ability itself being opt-in makes two deliberate choices before
     * anything of yours gets damaged. It also honours the {@code mobGriefing} gamerule, so the
     * server-wide off switch everyone already knows about works.
     */
    /**
     * @param needsTarget true is the Breaker's behaviour: only dig when there is something to reach
     *                    and only when actually stuck, so a mob still making progress does not
     *                    demolish the countryside it walks past. False is the Blight's: no target,
     *                    no stuck check, and it clears whatever it is standing in as it goes. The
     *                    difference is whether breaking is a means to reach you or the point.
     */
    public record BreakBlocks(int interval, float chance, HolderSet<net.minecraft.world.level.block.Block> allowed,
            double reach, boolean infest, boolean needsTarget) implements Ability {

        public static final Identifier TYPE = id("break_blocks");

        public static final MapCodec<BreakBlocks> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<BreakBlocks>intervalField(30),
                Abilities.<BreakBlocks>chanceField(0.8F),
                net.minecraft.core.RegistryCodecs.homogeneousList(net.minecraft.core.registries.Registries.BLOCK)
                        .fieldOf("allowed").forGetter(BreakBlocks::allowed),
                Codec.DOUBLE.optionalFieldOf("reach", 2.0D).forGetter(BreakBlocks::reach),
                Codec.BOOL.optionalFieldOf("infest", false).forGetter(BreakBlocks::infest),
                Codec.BOOL.optionalFieldOf("needs_target", true).forGetter(BreakBlocks::needsTarget))
                .apply(i, BreakBlocks::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public String describe() {
            // Name what it actually eats - a sample plus the count, since "an explicit allowed
            // list" can be three doors or seventy-six kinds of greenery.
            List<String> names = allowed.stream()
                    .map(h -> pretty(BuiltInRegistries.BLOCK.getKey(h.value())).toLowerCase())
                    .limit(4).toList();
            int more = allowed.size() - names.size();
            String what = String.join(", ", names) + (more > 0 ? " and " + more + " more kinds" : "");
            return needsTarget
                    ? "Digs through " + what + " to reach you."
                    : "Destroys " + what + " as it goes, provoked or not.";
        }


        @Override
        public void run(ServerLevel level, Mob mob) {
            if (!canGrief(level, mob)) {
                return;
            }
            net.minecraft.core.BlockPos[] candidates;
            if (needsTarget) {
                LivingEntity victim = mob.getTarget();
                if (victim == null || !victim.isAlive()) {
                    return;
                }
                // Stuck, not merely walking. A mob still making progress has no business demolishing
                // the countryside it is walking past.
                if (mob.getDeltaMovement().horizontalDistanceSqr() > 0.0016D) {
                    return;
                }
                net.minecraft.world.phys.Vec3 toward =
                        victim.position().subtract(mob.position()).normalize().scale(reach);
                net.minecraft.core.BlockPos at = net.minecraft.core.BlockPos.containing(
                        mob.getX() + toward.x, mob.getY() + mob.getBbHeight() * 0.5D, mob.getZ() + toward.z);
                candidates = new net.minecraft.core.BlockPos[]{at, at.above(), at.below()};
            } else {
                // Wherever it happens to be: its own square, the ring around its feet, and the same
                // ring one step DOWN.
                //
                // That last row is not tidiness. seek_blocks stops walking once it is within its
                // accepted distance of the target, and on any stepped ground - a terraced roof, a
                // riverbank, a staircase - the block it came for is a level below the one it is
                // standing on. Without reaching down it arrives, declares itself there, and stands
                // over moss it cannot touch forever.
                net.minecraft.core.BlockPos feet = mob.blockPosition();
                net.minecraft.core.BlockPos under = feet.below();
                candidates = new net.minecraft.core.BlockPos[]{
                        feet, feet.above(),
                        feet.north(), feet.south(), feet.east(), feet.west(),
                        under, under.north(), under.south(), under.east(), under.west()};
            }

            for (net.minecraft.core.BlockPos candidate : candidates) {
                if (!allowed.contains(BlockTypes.holderOf(level.getBlockState(candidate)))) {
                    continue;
                }
                if (infest) {
                    // The old INFEST: what it chewed through became someone else's problem.
                    level.setBlockAndUpdate(candidate,
                            net.minecraft.world.level.block.Blocks.INFESTED_STONE.defaultBlockState());
                } else {
                    level.destroyBlock(candidate, true, mob);
                }
                return;
            }
        }
    }

    /**
     * Fire something. The old {@code LAZER}, which in the original was an ordinary arrow attack —
     * both branches of its check called the same goal, so it never actually differed from an
     * archer. This one lets a genus choose its projectile.
     */
    public record Projectile(int interval, float chance, EntityType<?> projectile, double range,
            double power, double inaccuracy) implements Ability {

        public static final Identifier TYPE = id("projectile");

        public static final MapCodec<Projectile> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Projectile>intervalField(40),
                Abilities.<Projectile>chanceField(0.7F),
                BuiltInRegistries.ENTITY_TYPE.byNameCodec()
                        .optionalFieldOf("projectile", Types.arrow()).forGetter(Projectile::projectile),
                Codec.DOUBLE.optionalFieldOf("range", 16.0D).forGetter(Projectile::range),
                Codec.DOUBLE.optionalFieldOf("power", 1.6D).forGetter(Projectile::power),
                Codec.DOUBLE.optionalFieldOf("inaccuracy", 6.0D).forGetter(Projectile::inaccuracy))
                .apply(i, Projectile::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public String label() {
            return "Projectile (" + pretty(BuiltInRegistries.ENTITY_TYPE.getKey(projectile)) + ")";
        }

        @Override
        public String describe() {
            return "Throws " + pretty(BuiltInRegistries.ENTITY_TYPE.getKey(projectile)).toLowerCase()
                    + " at you from up to " + trim(range) + " blocks.";
        }


        @Override
        public void run(ServerLevel level, Mob mob) {
            LivingEntity victim = mob.getTarget();
            if (victim == null || !victim.isAlive() || victim.distanceToSqr(mob) > range * range
                    || !mob.hasLineOfSight(victim)) {
                return;
            }
            var created = projectile.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            if (!(created instanceof net.minecraft.world.entity.projectile.Projectile shot)) {
                return;
            }
            shot.snapTo(mob.getX(), mob.getEyeY() - 0.1D, mob.getZ(), mob.getYRot(), 0.0F);
            shot.setOwner(mob);
            double dx = victim.getX() - mob.getX();
            double dy = victim.getEyeY() - shot.getY();
            double dz = victim.getZ() - mob.getZ();
            // Arc compensation is for projectiles that fall. Fireballs and their kin fly straight,
            // so adding lift for them simply sends the shot over the victim's head - which is
            // exactly what Spitfire was doing.
            boolean falls = !(shot instanceof net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile);
            double lift = falls ? Math.sqrt(dx * dx + dz * dz) * 0.2D : 0.0D;
            shot.shoot(dx, dy + lift, dz, (float) power, (float) inaccuracy);
            level.addFreshEntity(shot);
        }
    }

    /** Drop a block on the victim. The old WEB, generalised. */
    public record PlaceBlock(int interval, float chance, net.minecraft.world.level.block.Block block,
            Target target, double radius, boolean airOnly) implements Ability {

        public static final Identifier TYPE = id("place_block");

        public static final MapCodec<PlaceBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<PlaceBlock>intervalField(60),
                Abilities.<PlaceBlock>chanceField(0.5F),
                BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("block",
                        net.minecraft.world.level.block.Blocks.COBWEB).forGetter(PlaceBlock::block),
                Target.CODEC.optionalFieldOf("target", Target.VICTIM).forGetter(PlaceBlock::target),
                Codec.DOUBLE.optionalFieldOf("radius", 12.0D).forGetter(PlaceBlock::radius),
                Codec.BOOL.optionalFieldOf("air_only", true).forGetter(PlaceBlock::airOnly))
                .apply(i, PlaceBlock::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public String label() {
            return "Place block (" + pretty(BuiltInRegistries.BLOCK.getKey(block)) + ")";
        }

        @Override
        public String describe() {
            return "Leaves " + pretty(BuiltInRegistries.BLOCK.getKey(block)).toLowerCase()
                    + " behind it.";
        }


        @Override
        public void run(ServerLevel level, Mob mob) {
            if (!canGrief(level, mob)) {
                return;
            }
            for (LivingEntity victim : Targets.of(target, level, mob, radius)) {
                net.minecraft.core.BlockPos at = victim.blockPosition();
                // air_only by default: webbing someone in is fair, replacing their floor is not.
                if (airOnly && !level.getBlockState(at).isAir()) {
                    continue;
                }
                level.setBlockAndUpdate(at, block.defaultBlockState());
            }
        }
    }

    /**
     * Adapt. The old {@code BORG}: it remembered what hurt it and stopped taking damage from that.
     *
     * <p>Memory lives in the mob's persistent data, so a Borg that has learned your sword still
     * knows it after a restart — which is the whole joke.
     */
    public record Adapt(int interval, float chance, float resistance, int maxAdaptations,
            double shareRadius, List<Identifier> shareWith) implements Ability {

        public static final Identifier TYPE = id("adapt");
        private static final String TAG = "zombiemod:adapted";

        public static final MapCodec<Adapt> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Adapt>intervalField(100),
                Abilities.<Adapt>chanceField(1.0F),
                Codec.FLOAT.optionalFieldOf("resistance", 0.8F).forGetter(Adapt::resistance),
                Codec.INT.optionalFieldOf("max_adaptations", 4).forGetter(Adapt::maxAdaptations),
                Codec.DOUBLE.optionalFieldOf("share_radius", 0.0D).forGetter(Adapt::shareRadius),
                Identifier.CODEC.listOf().optionalFieldOf("share_with", List.of())
                        .forGetter(Adapt::shareWith))
                .apply(i, Adapt::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public String describe() {
            String base = "Learns what hurt it and takes " + Math.round(resistance * 100)
                    + "% less of it, up to " + maxAdaptations + " kinds of damage.";
            return shareRadius > 0
                    ? base + " What it learns, its kin within " + trim(shareRadius) + " blocks learn with it."
                    : base;
        }


        /** Nothing on a timer; this ability is entirely reactive. */
        @Override
        public void run(ServerLevel level, Mob mob) {}

        @Override
        public float onHurt(ServerLevel level, Mob mob, net.minecraft.world.damagesource.DamageSource source,
                float amount) {
            String key = source.getMsgId();
            net.minecraft.nbt.ListTag learned =
                    mob.getPersistentData().getList(TAG).orElseGet(net.minecraft.nbt.ListTag::new);

            for (int i = 0; i < learned.size(); i++) {
                if (learned.getString(i).map(key::equals).orElse(false)) {
                    return amount * (1.0F - Math.clamp(resistance, 0.0F, 1.0F));
                }
            }
            if (learned.size() < maxAdaptations) {
                learned.add(net.minecraft.nbt.StringTag.valueOf(key));
                mob.getPersistentData().put(TAG, learned);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, mob.getX(),
                        mob.getY() + mob.getBbHeight() * 0.6D, mob.getZ(), 12, 0.3D, 0.4D, 0.3D, 0.05D);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        net.minecraft.sounds.SoundEvents.CONDUIT_ACTIVATE, SoundSource.HOSTILE, 0.7F, 1.8F);
                if (shareRadius > 0.0D) {
                    share(level, mob, key);
                }
            }
            // The blow that teaches it still lands in full. Learning should cost you a hit, not
            // be free the first time.
            return amount;
        }

        /**
         * The hive learns as one. A freshly learnt damage type is written straight into the
         * persistent data of every kin in range — bypassing their own caps deliberately, because
         * this is the queen teaching, not the drone learning; the cap prices first-hand lessons.
         *
         * <p>Kin means: the genera named in {@code share_with}, or — when the list is empty — the
         * sharer's own genus. Written to persistent data, so a taught immunity survives a restart
         * exactly as a learnt one does.
         */
        private void share(ServerLevel level, Mob teacher, String key) {
            String own = teacher.getPersistentData().getString("zombiemod:genus").orElse(null);
            for (Mob kin : level.getEntitiesOfClass(Mob.class,
                    teacher.getBoundingBox().inflate(shareRadius), other -> other != teacher)) {
                String theirs = kin.getPersistentData().getString("zombiemod:genus").orElse(null);
                if (theirs == null) {
                    continue;
                }
                boolean matches = shareWith.isEmpty()
                        ? theirs.equals(own)
                        : shareWith.stream().anyMatch(id -> id.toString().equals(theirs));
                if (!matches) {
                    continue;
                }
                net.minecraft.nbt.ListTag taught =
                        kin.getPersistentData().getList(TAG).orElseGet(net.minecraft.nbt.ListTag::new);
                boolean known = false;
                for (int i = 0; i < taught.size() && !known; i++) {
                    known = taught.getString(i).map(key::equals).orElse(false);
                }
                if (!known) {
                    taught.add(net.minecraft.nbt.StringTag.valueOf(key));
                    kin.getPersistentData().put(TAG, taught);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, kin.getX(),
                            kin.getY() + kin.getBbHeight() * 0.6D, kin.getZ(), 8, 0.3D, 0.4D, 0.3D, 0.05D);
                }
            }
        }
    }

    /**
     * A guardian beam, from something that is not a guardian.
     *
     * <p>The beam is drawn entirely by {@code GuardianRenderer} from the guardian's synced attack
     * target, and no other renderer draws one — so unlike most things here it cannot be faked with
     * particles and cannot be given to a zombie directly. What can be done is what it looks like:
     * park a real Guardian, invisible and inert, inside the caster's head and point it at the
     * victim. The client then renders a genuine beam between the two.
     *
     * <p>The guardian is invisible, silent, invulnerable, AI-less and never persists; it is moved to
     * the caster every tick so the beam tracks, and discarded when the beam ends. Damage is applied
     * by us at the end, so the guardian is scenery rather than a second attacker.
     */
    public record Beam(int interval, float chance, double range, float damage, int duration,
            boolean elder) implements Ability {

        public static final Identifier TYPE = id("beam");

        public static final MapCodec<Beam> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Beam>intervalField(120),
                Abilities.<Beam>chanceField(0.5F),
                Codec.DOUBLE.optionalFieldOf("range", 20.0D).forGetter(Beam::range),
                Codec.FLOAT.optionalFieldOf("damage", 6.0F).forGetter(Beam::damage),
                Codec.INT.optionalFieldOf("duration", 60).forGetter(Beam::duration),
                Codec.BOOL.optionalFieldOf("elder", false).forGetter(Beam::elder))
                .apply(i, Beam::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public String describe() {
            return "Fixes a guardian's beam on you at up to " + trim(range) + " blocks.";
        }


        /** Never called - stateful. */
        @Override
        public void run(ServerLevel level, Mob mob) {}

        @Override
        public State newState() {
            return new BeamState(this);
        }
    }

    /** Where a live beam emitter is recorded on its caster, so it can be cleaned up externally. */
    public static final String EMITTER_TAG = "zombiemod:beam_emitter";

    /** One beam's lifetime: acquire, hold, fire, clean up. */
    private static final class BeamState implements Ability.State {

        private final Beam beam;
        private net.minecraft.world.entity.monster.Guardian emitter;
        private LivingEntity victim;
        private int remaining;
        private int cooldown;

        BeamState(Beam beam) {
            this.beam = beam;
        }

        @Override
        public void tick(ServerLevel level, Mob mob) {
            if (remaining > 0) {
                advance(level, mob);
                return;
            }
            if (--cooldown > 0) {
                return;
            }
            cooldown = beam.interval();
            if (mob.getRandom().nextFloat() >= beam.chance()) {
                return;
            }
            start(level, mob);
        }

        private void start(ServerLevel level, Mob mob) {
            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive() || target.distanceToSqr(mob) > beam.range() * beam.range()
                    || !mob.hasLineOfSight(target)) {
                return;
            }
            var type = beam.elder() ? Types.elderGuardian() : Types.guardian();
            if (!(type.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED)
                    instanceof net.minecraft.world.entity.monster.Guardian guardian)) {
                return;
            }
            // NOT setInvisible: LivingEntity.updateInvisibilityStatus() runs every tick and calls
            // setInvisible(false) whenever the entity has no active effects, so the flag survives
            // exactly one tick. The effect is the thing that persists.
            guardian.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.INVISIBILITY,
                    net.minecraft.world.effect.MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
            guardian.setSilent(true);
            guardian.setInvulnerable(true);
            guardian.setNoAi(true);
            guardian.setNoGravity(true);
            guardian.snapTo(mob.getX(), mob.getEyeY() - 0.2D, mob.getZ(), mob.getYRot(), 0.0F);
            level.addFreshEntity(guardian);
            guardian.setActiveAttackTarget(target.getId());
            // Remember it on the caster. A goal stops ticking the instant its mob dies, so the
            // state cannot clean up after itself - and an orphaned emitter carries on beaming the
            // player with nothing left alive to stop it.
            mob.getPersistentData().putString(EMITTER_TAG, guardian.getUUID().toString());

            this.emitter = guardian;
            this.victim = target;
            this.remaining = beam.duration();
        }

        private void advance(ServerLevel level, Mob mob) {
            remaining--;
            boolean broken = emitter == null || !emitter.isAlive() || victim == null || !victim.isAlive()
                    || !mob.isAlive() || victim.distanceToSqr(mob) > beam.range() * beam.range();

            if (!broken) {
                // Follow the caster so the beam originates from it rather than from where it stood.
                emitter.snapTo(mob.getX(), mob.getEyeY() - 0.2D, mob.getZ(), mob.getYRot(), 0.0F);
                emitter.setActiveAttackTarget(victim.getId());
            }

            if (remaining <= 0 || broken) {
                if (!broken) {
                    victim.hurtServer(level, level.damageSources().indirectMagic(mob, mob), beam.damage());
                }
                if (emitter != null) {
                    emitter.discard();
                }
                mob.getPersistentData().remove(EMITTER_TAG);
                emitter = null;
                victim = null;
                remaining = 0;
            }
        }
    }

    /**
     * A hitscan beam drawn with particles — the other way to do a laser.
     *
     * <p>Where {@code beam} borrows a real guardian and gets the authentic look at the cost of a
     * second entity to manage, this draws the line itself: pick the victim, step particles along the
     * ray, apply the damage. No entity, no cleanup, nothing to leak, and the colour is yours —
     * {@code minecraft:dust} with an RGB and a scale reads as a proper coloured laser, and
     * {@code minecraft:sonic_boom} borrows the Warden's look.
     *
     * <p>Instant by design. There is no travel time to dodge, which makes it meaningfully different
     * from {@code projectile} rather than a reskin of it — if it can see you, it has hit you.
     */
    public record Ray(int interval, float chance, double range, float damage, ParticleOptions particle,
            double density, boolean ignite, int charge, Holder<SoundEvent> chargeSound) implements Ability {

        public static final Identifier TYPE = id("ray");

        public static final MapCodec<Ray> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Abilities.<Ray>intervalField(60),
                Abilities.<Ray>chanceField(0.6F),
                Codec.DOUBLE.optionalFieldOf("range", 20.0D).forGetter(Ray::range),
                Codec.FLOAT.optionalFieldOf("damage", 4.0F).forGetter(Ray::damage),
                Particles.PARTICLE_CODEC.optionalFieldOf("particle", ParticleTypes.END_ROD)
                        .forGetter(Ray::particle),
                Codec.DOUBLE.optionalFieldOf("density", 4.0D).forGetter(Ray::density),
                Codec.BOOL.optionalFieldOf("ignite", false).forGetter(Ray::ignite),
                Codec.INT.optionalFieldOf("charge", 30).forGetter(Ray::charge),
                BuiltInRegistries.SOUND_EVENT.holderByNameCodec()
                        .optionalFieldOf("charge_sound", BuiltInRegistries.SOUND_EVENT
                                .wrapAsHolder(net.minecraft.sounds.SoundEvents.GUARDIAN_ATTACK))
                        .forGetter(Ray::chargeSound))
                .apply(i, Ray::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public String describe() {
            return "Fires a line of particles up to " + trim(range) + " blocks for " + trim(damage)
                    + " damage, after an audible wind-up you can interrupt.";
        }


        /** Never called - stateful, because a charge is several ticks long. */
        @Override
        public void run(ServerLevel level, Mob mob) {}

        @Override
        public State newState() {
            return new RayState(this);
        }
    }

    /**
     * Wind up, then fire.
     *
     * <p>The charge exists to make the shot answerable. Instant hitscan damage from something you
     * may not have noticed is not a fight, it is a tax — so the wind-up is deliberately loud as well
     * as bright: a sound at the caster carries whether or not the particles are on screen, which
     * matters most in exactly the case a beam is nastiest, when it is behind you.
     *
     * <p>Breaking line of sight or leaving range during the wind-up aborts it. That is the whole
     * point: the telegraph is only fair if there is something to do about it.
     */
    private static final class RayState implements Ability.State {

        private final Ray ray;
        private int cooldown;
        private int charging;

        RayState(Ray ray) {
            this.ray = ray;
        }

        @Override
        public void tick(ServerLevel level, Mob mob) {
            if (charging > 0) {
                advance(level, mob);
                return;
            }
            if (--cooldown > 0) {
                return;
            }
            cooldown = ray.interval();
            if (mob.getRandom().nextFloat() >= ray.chance() || !canSee(mob, mob.getTarget(), ray.range())) {
                return;
            }
            if (ray.charge() <= 0) {
                fire(level, mob);
                return;
            }
            charging = ray.charge();
            // Minecraft's audible radius is 16 blocks x volume, so scale the warning to the weapon:
            // a shot that reaches 24 blocks must be heard at 24 blocks, or the telegraph is a lie
            // for exactly the people it exists to protect.
            float warning = (float) Math.max(1.0D, ray.range() / 16.0D);
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), ray.chargeSound().value(),
                    SoundSource.HOSTILE, warning, 0.7F);
        }

        private void advance(ServerLevel level, Mob mob) {
            LivingEntity victim = mob.getTarget();
            if (!canSee(mob, victim, ray.range())) {
                charging = 0; // lost it - the counter-play worked
                return;
            }
            charging--;

            // Gather at the eye, thickening as it nears release, so the wind-up reads as a build-up
            // rather than a steady glow.
            float progress = 1.0F - (float) charging / Math.max(1, ray.charge());
            Vec3 eye = mob.getEyePosition();
            level.sendParticles(ray.particle(), eye.x, eye.y, eye.z,
                    1 + (int) (progress * 4.0F), 0.25D * (1.0F - progress), 0.25D * (1.0F - progress),
                    0.25D * (1.0F - progress), 0.0D);
            if (charging % 6 == 0) {
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), ray.chargeSound().value(),
                        SoundSource.HOSTILE, 0.6F, 1.0F + progress);
            }

            if (charging <= 0) {
                fire(level, mob);
            }
        }

        private void fire(ServerLevel level, Mob mob) {
            LivingEntity victim = mob.getTarget();
            if (!canSee(mob, victim, ray.range())) {
                return;
            }
            Vec3 from = mob.getEyePosition();
            Vec3 to = victim.getEyePosition().subtract(0.0D, victim.getBbHeight() * 0.25D, 0.0D);
            Vec3 along = to.subtract(from);
            int steps = Math.max(2, (int) (along.length() * ray.density()));

            for (int i = 0; i <= steps; i++) {
                Vec3 at = from.add(along.scale((double) i / steps));
                // Count 0 with zero speed means "exactly one particle, exactly here" - otherwise the
                // server scatters them and the line reads as a cloud rather than a beam.
                level.sendParticles(ray.particle(), at.x, at.y, at.z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
            }

            victim.hurtServer(level, level.damageSources().indirectMagic(mob, mob), ray.damage());
            if (ray.ignite()) {
                victim.igniteForSeconds(4.0F);
            }
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    net.minecraft.sounds.SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1.0F, 1.6F);
        }

        private static boolean canSee(Mob mob, LivingEntity victim, double range) {
            return victim != null && victim.isAlive() && mob.isAlive()
                    && victim.distanceToSqr(mob) <= range * range && mob.hasLineOfSight(victim);
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
        // Public because mutations reuse it: the "plain id or full options" spelling should be
        // the same everywhere a datapack names a particle.
        public static final Codec<ParticleOptions> PARTICLE_CODEC = Codec.withAlternative(
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
        public String label() {
            return "Particles (" + pretty(BuiltInRegistries.PARTICLE_TYPE.getKey(particle.getType())) + ")";
        }

        @Override
        public String describe() {
            return "Trails " + pretty(BuiltInRegistries.PARTICLE_TYPE.getKey(particle.getType())).toLowerCase()
                    + " as it goes.";
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
        public String label() {
            return "Sound (" + soundName() + ")";
        }

        @Override
        public String describe() {
            return "You will hear it before you see it: " + soundName().toLowerCase() + ".";
        }

        /** "entity.zombie.ambient" -> "Zombie ambient". Sound ids are dotted, not underscored. */
        private String soundName() {
            String path = sound.value().location().getPath();
            path = path.replaceFirst("^(entity|block|item)\\.", "").replace('.', ' ').replace('_', ' ');
            return path.isEmpty() ? path : Character.toUpperCase(path.charAt(0)) + path.substring(1);
        }


        @Override
        public void run(ServerLevel level, Mob mob) {
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), sound.value(),
                    SoundSource.HOSTILE, volume, pitch);
        }
    }

    /** "minecraft:nausea" -> "Nausea". Ids are honest and unreadable; a bestiary wants the name. */
    static String pretty(Identifier id) {
        String path = id.getPath().replace('_', ' ');
        return path.isEmpty() ? path : Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    static String who(Target target) {
        return switch (target) {
            case SELF -> "itself";
            case VICTIM -> "whoever it hits";
            case NEARBY_PLAYERS -> "players nearby";
        };
    }

    static String seconds(int ticks) {
        double s = ticks / 20.0D;
        return (s == Math.floor(s) ? String.valueOf((long) s) : String.valueOf(s)) + "s";
    }

    static String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private Abilities() {}
}
