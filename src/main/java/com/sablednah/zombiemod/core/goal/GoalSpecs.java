package com.sablednah.zombiemod.core.goal;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
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
            return mob instanceof PathfinderMob pf ? new MeleeAttackGoal(pf, speed, followEvenIfNotSeen) : null;
        }
    }

    // ---------------------------------------------------------------- targeting (targetSelector)

    /** Pick a victim. Goes in the genus's {@code target_goals}, not {@code goals}. */
    public record NearestTarget(int priority, Class<? extends LivingEntity> target, boolean mustSee)
            implements GoalSpec {

        public static final Identifier TYPE = id("nearest_target");

        public static final MapCodec<NearestTarget> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                priorityField(2).forGetter(NearestTarget::priority),
                TargetClass.CODEC.fieldOf("target").forGetter(NearestTarget::target),
                com.mojang.serialization.Codec.BOOL.optionalFieldOf("must_see", true).forGetter(NearestTarget::mustSee))
                .apply(i, NearestTarget::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public Goal build(Mob mob) {
            return new NearestAttackableTargetGoal<>(mob, target, mustSee);
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
