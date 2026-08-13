package com.sablednah.zombiemod.core.goal;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/**
 * The goal types a genus can name in JSON. Each is a record whose fields are the vanilla goal's
 * constructor arguments, so the JSON reads like the Java it builds.
 *
 * <p>Deliberately a small, curated set for now — the ones that reproduce the original mod's
 * signature behaviours. Adding a type is a record plus one {@code register} line.
 */
public final class GoalSpecs {

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("zombiemod", path);
    }

    // ---------------------------------------------------------------- movement / idle

    /**
     * Run away. This is the coward — the 1.8 mod built it from {@code PathfinderGoalAvoidTarget}
     * and so do we.
     */
    public record AvoidEntity(int priority, Class<? extends LivingEntity> target, float distance,
            double walkSpeed, double sprintSpeed) implements GoalSpec {

        public static final Identifier TYPE = id("avoid_entity");

        public static final MapCodec<AvoidEntity> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                priorityField(7).forGetter(AvoidEntity::priority),
                TargetClass.CODEC.fieldOf("target").forGetter(AvoidEntity::target),
                com.mojang.serialization.Codec.FLOAT.optionalFieldOf("distance", 8.0F).forGetter(AvoidEntity::distance),
                com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("walk_speed", 1.0D).forGetter(AvoidEntity::walkSpeed),
                com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("sprint_speed", 1.35D).forGetter(AvoidEntity::sprintSpeed))
                .apply(i, AvoidEntity::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public Goal build(Mob mob) {
            // AvoidEntityGoal navigates away, so it needs a mob that can path.
            return mob instanceof PathfinderMob pf
                    ? new AvoidEntityGoal<>(pf, target, distance, walkSpeed, sprintSpeed)
                    : null;
        }
    }

    /** Wander. Without this a mob that has nothing else to do stands perfectly still. */
    public record RandomStroll(int priority, double speed) implements GoalSpec {

        public static final Identifier TYPE = id("random_stroll");

        public static final MapCodec<RandomStroll> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                priorityField(8).forGetter(RandomStroll::priority),
                com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("speed", 1.0D).forGetter(RandomStroll::speed))
                .apply(i, RandomStroll::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public Goal build(Mob mob) {
            return mob instanceof PathfinderMob pf ? new RandomStrollGoal(pf, speed) : null;
        }
    }

    /** Swim rather than sink. */
    public record Float_(int priority) implements GoalSpec {

        public static final Identifier TYPE = id("float");

        public static final MapCodec<Float_> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                priorityField(0).forGetter(Float_::priority))
                .apply(i, Float_::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public Goal build(Mob mob) {
            return new FloatGoal(mob);
        }
    }

    // ---------------------------------------------------------------- looking

    /**
     * Stare. The 1.8 mod's HEROBRINE zombie was this goal with a 32-block range — it would watch
     * you from across the map and never close in.
     */
    public record LookAtPlayer(int priority, Class<? extends LivingEntity> target, float distance,
            float probability) implements GoalSpec {

        public static final Identifier TYPE = id("look_at");

        public static final MapCodec<LookAtPlayer> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                priorityField(9).forGetter(LookAtPlayer::priority),
                TargetClass.CODEC.optionalFieldOf("target", net.minecraft.world.entity.player.Player.class)
                        .forGetter(LookAtPlayer::target),
                com.mojang.serialization.Codec.FLOAT.optionalFieldOf("distance", 8.0F).forGetter(LookAtPlayer::distance),
                com.mojang.serialization.Codec.FLOAT.optionalFieldOf("probability", 0.02F).forGetter(LookAtPlayer::probability))
                .apply(i, LookAtPlayer::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public Goal build(Mob mob) {
            return new LookAtPlayerGoal(mob, target, distance, probability);
        }
    }

    /** Idle head movement. Pure flavour, but its absence reads as "broken". */
    public record RandomLook(int priority) implements GoalSpec {

        public static final Identifier TYPE = id("random_look");

        public static final MapCodec<RandomLook> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                priorityField(9).forGetter(RandomLook::priority))
                .apply(i, RandomLook::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public Goal build(Mob mob) {
            return new RandomLookAroundGoal(mob);
        }
    }

    // ---------------------------------------------------------------- attacking

    /** Walk up and hit it. */
    public record MeleeAttack(int priority, double speed, boolean followEvenIfNotSeen) implements GoalSpec {

        public static final Identifier TYPE = id("melee_attack");

        public static final MapCodec<MeleeAttack> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                priorityField(3).forGetter(MeleeAttack::priority),
                com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("speed", 1.0D).forGetter(MeleeAttack::speed),
                com.mojang.serialization.Codec.BOOL.optionalFieldOf("follow_unseen", false)
                        .forGetter(MeleeAttack::followEvenIfNotSeen))
                .apply(i, MeleeAttack::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public Goal build(Mob mob) {
            if (!(mob instanceof PathfinderMob pf)) {
                return null;
            }
            // Some mobs have no attack_damage attribute at all - squid, villager, snow golem, ghast,
            // shulker, the ender dragon. Reading one an entity type never declared does not return a
            // default, it throws: AttributeSupplier.getAttributeInstance raises
            // IllegalArgumentException. So a melee goal on a squid is not a squid that hits for
            // nothing, it is a server crash the first time it reaches a target. Skipping is the same
            // "cannot apply here" answer this method already gives for a non-PathfinderMob.
            if (!mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                return null;
            }
            return new MeleeAttackGoal(pf, speed, followEvenIfNotSeen);
        }
    }

    /**
     * Draw a bow properly — nock, pull, hold, release — using vanilla's own goal.
     *
     * <p>Whether it <em>looks</em> right is a renderer question, and the answer is narrow:
     * {@code AbstractSkeletonRenderer} is the only humanoid renderer with a {@code BOW_AND_ARROW}
     * arm pose, gated on the mob being aggressive and holding a bow. {@code AbstractZombieRenderer}
     * has no such branch, so a zombie will fire arrows perfectly and never draw the bow. If you want
     * the animation, give the genus a skeleton {@code base}.
     *
     * <p>Only applies to mobs vanilla considers ranged ({@code Monster & RangedAttackMob}) —
     * skeletons, strays, bogged, drowned, witches, illusioners. Anything else is skipped with a log
     * line, like any other goal that does not fit.
     */
    public record BowAttack(int priority, double speed, int interval, float range) implements GoalSpec {

        public static final Identifier TYPE = id("bow_attack");

        public static final MapCodec<BowAttack> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                priorityField(4).forGetter(BowAttack::priority),
                com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("speed", 1.0D).forGetter(BowAttack::speed),
                com.mojang.serialization.Codec.INT.optionalFieldOf("interval", 20).forGetter(BowAttack::interval),
                com.mojang.serialization.Codec.FLOAT.optionalFieldOf("range", 15.0F).forGetter(BowAttack::range))
                .apply(i, BowAttack::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public Goal build(Mob mob) {
            if (!(mob instanceof net.minecraft.world.entity.monster.Monster)
                    || !(mob instanceof net.minecraft.world.entity.monster.RangedAttackMob)) {
                return null;
            }
            return new net.minecraft.world.entity.ai.goal.RangedBowAttackGoal(
                    (net.minecraft.world.entity.monster.Monster) mob, speed, interval, range);
        }
    }

    // ---------------------------------------------------------------- targeting (targetSelector)

    /** Pick a victim. Goes in the genus's {@code target_goals}, not {@code goals}. */
    public record NearestTarget(int priority, Class<? extends LivingEntity> target, boolean mustSee,
            int unseenMemory) implements GoalSpec {

        public static final Identifier TYPE = id("nearest_target");

        public static final MapCodec<NearestTarget> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                priorityField(2).forGetter(NearestTarget::priority),
                TargetClass.CODEC.fieldOf("target").forGetter(NearestTarget::target),
                com.mojang.serialization.Codec.BOOL.optionalFieldOf("must_see", true).forGetter(NearestTarget::mustSee),
                com.mojang.serialization.Codec.INT.optionalFieldOf("unseen_memory", 60)
                        .forGetter(NearestTarget::unseenMemory))
                .apply(i, NearestTarget::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public Goal build(Mob mob) {
            NearestAttackableTargetGoal<?> goal = new NearestAttackableTargetGoal<>(mob, target, mustSee);
            // How long it holds a target it cannot see. Vanilla's 60 ticks is three seconds, which
            // is fine for a mob that walks round obstacles and exactly wrong for one that digs
            // through them: the moment the wall blocked line of sight it forgot why it was digging.
            goal.setUnseenMemoryTicks(unseenMemory);
            return goal;
        }
    }

    /** Fight back when struck. */
    public record HurtByTarget(int priority) implements GoalSpec {

        public static final Identifier TYPE = id("hurt_by_target");

        public static final MapCodec<HurtByTarget> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                priorityField(1).forGetter(HurtByTarget::priority))
                .apply(i, HurtByTarget::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public Goal build(Mob mob) {
            return mob instanceof PathfinderMob pf ? new HurtByTargetGoal(pf) : null;
        }
    }

    /** Shared {@code priority} field so every spec spells it the same way, with a sane default. */
    private static com.mojang.serialization.MapCodec<Integer> priorityField(int fallback) {
        return com.mojang.serialization.Codec.INT.optionalFieldOf("priority", fallback);
    }

    private GoalSpecs() {}
}
