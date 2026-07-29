package com.sablednah.zombiemod.core.ability;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.EntityTypeTags;

/**
 * Bitten now, turns later.
 *
 * <p>{@link Convert} raises a corpse the instant it dies, which is the effect but not the story. The
 * trope is a bite, a while of knowing, and then it doesn't matter what actually killed you — falling,
 * drowning, another player. This is that: the bite marks you, and if you die while marked you get up.
 *
 * <p>The marker is deliberately a real potion effect as well as a stored timer, and the death check
 * requires <em>both</em>. That buys one thing worth having: <b>milk cures it</b>. Drinking milk clears
 * the effect, the death check no longer passes, and a player who understood what happened to them has
 * something to do about it. Curable infection is a far better mechanic than inevitable infection, and
 * it costs a single extra condition.
 *
 * @param effect   the visible marker; its duration is the timer the player can read off their HUD
 * @param duration how long the infection lasts, in ticks
 * @param genus    what they rise as; absent rolls the weighted table
 * @param announce tell the victim, if it is a player
 */
public record Infect(int interval, float chance, Holder<MobEffect> effect, int duration,
        Optional<Identifier> genus, boolean announce) implements Ability {

    public static final Identifier TYPE = Identifier.fromNamespaceAndPath("zombiemod", "infect");

    /** Game time the infection expires, on the victim. */
    public static final String UNTIL_TAG = "zombiemod:infected_until";
    /** Which genus they rise as, on the victim. */
    public static final String GENUS_TAG = "zombiemod:infected_genus";
    /** Which effect is acting as the marker, so the cure check tests the right one. */
    public static final String EFFECT_TAG = "zombiemod:infected_marker";

    public static final MapCodec<Infect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("interval", 1).forGetter(Infect::interval),
            Codec.FLOAT.optionalFieldOf("chance", 0.35F).forGetter(Infect::chance),
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec()
                    .optionalFieldOf("effect", BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffects.HUNGER.value()))
                    .forGetter(Infect::effect),
            Codec.INT.optionalFieldOf("duration", 1200).forGetter(Infect::duration),
            Identifier.CODEC.optionalFieldOf("genus").forGetter(Infect::genus),
            Codec.BOOL.optionalFieldOf("announce", true).forGetter(Infect::announce))
            .apply(i, Infect::new));

    @Override
    public Identifier type() {
        return TYPE;
    }

    /** Nothing on a timer; the bite is the event. */
    @Override
    public void run(ServerLevel level, Mob mob) {}

    @Override
    public void onAttack(ServerLevel level, Mob mob, LivingEntity victim, float amount) {
        if (mob.getRandom().nextFloat() >= chance) {
            return;
        }
        // Already dead once, or already one of ours - neither can catch it.
        if (victim.getType().is(EntityTypeTags.UNDEAD)) {
            return;
        }
        if (victim instanceof Mob m
                && m.getPersistentData().getString("zombiemod:genus").isPresent()) {
            return;
        }
        // Re-biting refreshes rather than stacks, so a long fight is not a death sentence measured in
        // hits.
        victim.getPersistentData().putLong(UNTIL_TAG, level.getGameTime() + duration);
        // Record which effect is the marker. Checking "has any effect" instead would mean an
        // unrelated potion kept the infection alive through a bucket of milk.
        victim.getPersistentData().putString(EFFECT_TAG,
                BuiltInRegistries.MOB_EFFECT.getKey(effect.value()).toString());
        genus.ifPresent(id -> victim.getPersistentData().putString(GENUS_TAG, id.toString()));
        victim.addEffect(new MobEffectInstance(effect, duration, 0, false, true, true));

        if (announce && victim instanceof Player player) {
            player.displayClientMessage(
                    Component.literal("§cYou have been bitten. §7Milk will still help you."), false);
        }
    }

    /**
     * Is this thing currently infected?
     *
     * <p>Requires the effect as well as the timer, which is what makes milk a cure.
     */
    public static boolean isInfected(LivingEntity victim, Holder<MobEffect> marker, long now) {
        long until = victim.getPersistentData().getLongOr(UNTIL_TAG, -1L);
        if (until < 0L || now > until) {
            return false;
        }
        return victim.hasEffect(marker);
    }

    public static void clear(LivingEntity victim) {
        victim.getPersistentData().remove(UNTIL_TAG);
        victim.getPersistentData().remove(GENUS_TAG);
        victim.getPersistentData().remove(EFFECT_TAG);
    }

    /** Still carrying the specific marker it was bitten with? */
    public static boolean stillMarked(LivingEntity victim) {
        return victim.getPersistentData().getString(EFFECT_TAG)
                .map(Identifier::tryParse)
                .map(id -> {
                    MobEffect marker = id == null ? null : BuiltInRegistries.MOB_EFFECT.getValue(id);
                    return marker != null && victim.hasEffect(
                            BuiltInRegistries.MOB_EFFECT.wrapAsHolder(marker));
                })
                .orElse(false);
    }
}
