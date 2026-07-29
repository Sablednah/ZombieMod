package com.sablednah.zombiemod.core.ability;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * What it kills gets up as one of them.
 *
 * <p>The defining idea of the genre, and the one thing the mod had no answer to. Fires on a kill
 * rather than on a timer, so it is a consequence of the fight rather than a thing that happens
 * nearby.
 *
 * <p>Several guards, because unchecked this is how a server ends:
 * <ul>
 *   <li>{@code victims} has <b>no default</b> — a genus must name what it can turn. Converting
 *       anything that dies near a zombie is not a feature, it is an outage.
 *   <li>Nothing already undead is converted, so zombies do not endlessly re-raise each other.
 *   <li>Nothing that is already a genus is converted, for the same reason.
 *   <li>{@code max_nearby} caps how many of the resulting genus may exist within {@code radius},
 *       which is what stops a herd of cows becoming an unbounded chain reaction.
 *   <li>{@code cooldown} limits how often one killer may convert at all. This exists because
 *       {@code max_nearby} is measured with an entity query, and an entity query cannot see what was
 *       added earlier in the same tick — so several kills landing together could all pass a cap that
 *       each of them should have tripped. A rate limit needs nothing from the world to be correct.
 * </ul>
 *
 * <p>The corpse's own kind is preferred where vanilla has an undead counterpart — a villager rises as
 * a zombie villager, a piglin as a zombified piglin, a horse as a zombie horse — because "that used
 * to be my villager" lands far harder than a generic zombie standing where it fell.
 */
public record Convert(int interval, float chance, HolderSet<EntityType<?>> victims,
        Optional<Identifier> genus, int maxNearby, double radius, boolean inheritEquipment,
        boolean inheritName, int cooldown) implements Ability {

    /** Where a killer's last conversion time is kept, so the rate limit survives a reload. */
    public static final String COOLDOWN_TAG = "zombiemod:last_conversion";

    public static final Identifier TYPE = Identifier.fromNamespaceAndPath("zombiemod", "convert");

    public static final MapCodec<Convert> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("interval", 1).forGetter(Convert::interval),
            Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Convert::chance),
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).fieldOf("victims")
                    .forGetter(Convert::victims),
            Identifier.CODEC.optionalFieldOf("genus").forGetter(Convert::genus),
            Codec.INT.optionalFieldOf("max_nearby", 8).forGetter(Convert::maxNearby),
            Codec.DOUBLE.optionalFieldOf("radius", 16.0D).forGetter(Convert::radius),
            Codec.BOOL.optionalFieldOf("inherit_equipment", true).forGetter(Convert::inheritEquipment),
            Codec.BOOL.optionalFieldOf("inherit_name", true).forGetter(Convert::inheritName),
            Codec.INT.optionalFieldOf("cooldown", 20).forGetter(Convert::cooldown))
            .apply(i, Convert::new));

    @Override
    public Identifier type() {
        return TYPE;
    }

    /** Nothing on a timer; conversion is entirely a consequence of killing. */
    @Override
    public void run(ServerLevel level, Mob mob) {}

    /**
     * Raising the corpse needs the applier, which lives in the loader layer, so the layer installs
     * itself here rather than {@code core} reaching sideways into it.
     */
    public interface Raiser {
        void raise(ServerLevel level, Mob killer, LivingEntity victim, Convert convert);
    }

    private static Raiser raiser = (level, killer, victim, convert) -> {};

    public static void setRaiser(Raiser installed) {
        raiser = installed;
    }

    @Override
    public void onKill(ServerLevel level, Mob mob, LivingEntity victim) {
        raiser.raise(level, mob, victim, this);
    }

    /**
     * Which vanilla mob the corpse should rise as.
     *
     * <p>A villager becoming a zombie villager keeps the loss legible — you can see what you lost.
     */
    public static EntityType<?> undeadFormOf(EntityType<?> victim) {
        if (victim == EntityType.VILLAGER || victim == EntityType.WANDERING_TRADER) {
            return EntityType.ZOMBIE_VILLAGER;
        }
        if (victim == EntityType.PIGLIN || victim == EntityType.PIGLIN_BRUTE) {
            return EntityType.ZOMBIFIED_PIGLIN;
        }
        if (victim == EntityType.HORSE || victim == EntityType.DONKEY || victim == EntityType.MULE) {
            return EntityType.ZOMBIE_HORSE;
        }
        return EntityType.ZOMBIE;
    }

    /** True if this corpse is a legitimate candidate. The applier does the spawning. */
    public boolean accepts(LivingEntity victim) {
        if (!victims.contains(victim.getType().builtInRegistryHolder())) {
            return false;
        }
        // Already dead once. Raising the risen is how you get an unkillable feedback loop.
        return !victim.getType().is(EntityTypeTags.UNDEAD);
    }

    /** Flavour for the moment it turns, so a conversion is never silent. */
    public static void announce(ServerLevel level, LivingEntity victim) {
        level.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + 0.5D, victim.getZ(),
                24, 0.35D, 0.5D, 0.35D, 0.02D);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 1.2F, 0.8F);
    }
}
