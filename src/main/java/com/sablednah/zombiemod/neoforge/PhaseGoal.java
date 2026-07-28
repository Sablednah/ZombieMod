package com.sablednah.zombiemod.neoforge;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import com.sablednah.zombiemod.core.Phase;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Applies the one-shot half of a phase — attributes, a sound, a line of text — on the way down.
 *
 * <p>Only ever descends. Phases are entered in order of decreasing health and never left, so a boss
 * that heals keeps the attribute changes it earned. The gated half (abilities) does the opposite and
 * switches off again, which is the split described on {@link Phase}: escalating stats read as a
 * fight getting harder, while stats that yo-yo with a regeneration ability just read as broken.
 */
final class PhaseGoal extends Goal {

    private static final int CHECK_INTERVAL = 10;

    private final Mob mob;
    private final List<Phase> phases;
    private int entered = -1;
    private int ticks;

    PhaseGoal(Mob mob, List<Phase> phases) {
        this.mob = mob;
        // Highest threshold first, so we walk down through them in order.
        this.phases = phases.stream()
                .sorted(Comparator.comparingDouble(Phase::belowHealth).reversed())
                .toList();
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (++ticks < CHECK_INTERVAL || !mob.isAlive()) {
            return;
        }
        ticks = 0;

        float max = mob.getMaxHealth();
        if (max <= 0.0F) {
            return;
        }
        double fraction = mob.getHealth() / max;

        for (int i = entered + 1; i < phases.size(); i++) {
            if (fraction > phases.get(i).belowHealth()) {
                break;
            }
            enter(phases.get(i));
            entered = i;
        }
    }

    private void enter(Phase phase) {
        phase.attributes().forEach((attribute, value) -> {
            AttributeInstance instance = mob.getAttribute(attribute);
            if (instance != null) {
                instance.setBaseValue(value);
            }
        });

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        phase.sound().ifPresent(sound -> level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                sound.value(), SoundSource.HOSTILE, 1.4F, 0.8F));
        phase.title().ifPresent(text -> {
            Component message = Component.literal(text);
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(mob) <= 64.0D * 64.0D) {
                    player.displayClientMessage(message, true);
                }
            }
        });
    }
}
