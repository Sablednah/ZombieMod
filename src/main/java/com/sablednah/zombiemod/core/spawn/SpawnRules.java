package com.sablednah.zombiemod.core.spawn;

import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;

/**
 * Where and when a genus may claim a spawn.
 *
 * <p>{@code conditions} are ANDed — every one must pass. Use {@code zombiemod:any_of} for
 * alternatives and {@code zombiemod:not} to invert. An empty list means "anywhere the base mob
 * appears", which is the old plugin's behaviour and the sensible default.
 *
 * <p>{@code reasons} is deliberately <em>not</em> a condition. Spawn reason is not a property of the
 * place, and unlike every positional test it needs a restrictive default that a condition list
 * cannot express — an empty condition list has to mean "anywhere", but an unspecified reason set
 * must not mean "every reason".
 *
 * <p>{@code weights} changes how often, not whether. Each entry is a weight and the conditions under
 * which it replaces the genus's own; the first entry whose conditions all hold wins, and where none
 * does the base weight stands. That is how a genus can be ordinary everywhere and common in one kind
 * of place - the Vault Dweller underground in any world, and most of what you meet in a CityWorld
 * vault - without a second genus, and without binding it to a mod that a server may not have. A
 * condition would have had to choose between those two.
 *
 * @param reasons    which spawn reasons this genus may ride
 * @param conditions positional tests, all of which must pass
 * @param weights    place-dependent weights, tried in order; the first that applies replaces the base
 */
public record SpawnRules(List<EntitySpawnReason> reasons, List<SpawnCondition> conditions,
        List<PlacedWeight> weights) {

    /**
     * A weight that applies only where its conditions all hold.
     *
     * <p>May be lower than the base as well as higher - "rarer in claims" is as reasonable as "common
     * in vaults". A weight of 0 here means "not drawn at this spot", which is what a condition says
     * too, but it is here if that reads better beside the others.
     */
    public record PlacedWeight(int weight, List<SpawnCondition> conditions) {

        public static final Codec<PlacedWeight> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("weight").forGetter(PlacedWeight::weight),
                SpawnCondition.CODEC.listOf().optionalFieldOf("conditions", List.of())
                        .forGetter(PlacedWeight::conditions))
                .apply(i, PlacedWeight::new));

        boolean applies(Level level, BlockPos pos) {
            for (SpawnCondition condition : conditions) {
                if (!condition.test(level, pos)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Spawn reasons a genus is eligible for when it doesn't say otherwise.
     *
     * <p>Narrow on purpose. A genus riding {@code CONVERSION} would replace drowning zombies and
     * cured villagers; riding {@code REINFORCEMENT} would let a horde summon itself. Both are
     * surprising defaults, so a genus that wants them has to ask.
     */
    public static final List<EntitySpawnReason> DEFAULT_REASONS =
            List.of(EntitySpawnReason.NATURAL, EntitySpawnReason.CHUNK_GENERATION, EntitySpawnReason.SPAWNER);

    /** No positional restrictions, default reasons. */
    public static final SpawnRules ANY = new SpawnRules(DEFAULT_REASONS, List.of(), List.of());

    private static final Codec<EntitySpawnReason> REASON = Codec.STRING.xmap(
            s -> EntitySpawnReason.valueOf(s.toUpperCase(Locale.ROOT)),
            r -> r.name().toLowerCase(Locale.ROOT));

    public static final Codec<SpawnRules> CODEC = RecordCodecBuilder.create(i -> i.group(
            REASON.listOf().optionalFieldOf("reasons", DEFAULT_REASONS).forGetter(SpawnRules::reasons),
            SpawnCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(SpawnRules::conditions),
            PlacedWeight.CODEC.listOf().optionalFieldOf("weights", List.of()).forGetter(SpawnRules::weights))
            .apply(i, SpawnRules::new));

    /** Any one is enough: the conditions are ANDed, so a single calendar gate gates the genus. */
    public boolean seasonal() {
        return conditions.stream().anyMatch(SpawnCondition::seasonal);
    }

    /**
     * Do this genus's positional conditions hold here, ignoring spawn reason?
     *
     * <p>For deciding what something <em>rises as</em> rather than what spawns. Reason cannot be
     * consulted there: the default reason set deliberately excludes {@code CONVERSION}, so asking
     * {@link #allows} would reject every genus and conversion would stop working entirely. What is
     * wanted is only "could this thing plausibly be found here", which is exactly the conditions.
     */
    public boolean suitsPlace(Level level, BlockPos pos) {
        for (SpawnCondition condition : conditions) {
            if (!condition.test(level, pos)) {
                return false;
            }
        }
        return true;
    }

    public boolean allows(Level level, BlockPos pos, EntitySpawnReason reason) {
        if (!reasons.contains(reason)) {
            return false;
        }
        for (SpawnCondition condition : conditions) {
            if (!condition.test(level, pos)) {
                return false;
            }
        }
        return true;
    }

    /**
     * This genus's weight at {@code pos}: the first {@link PlacedWeight} that applies, or
     * {@code base}.
     *
     * <p>A base of 0 stays 0. It means "never drawn" - commands, rituals and spawners only - and a
     * placed weight is a way of tuning a genus that is drawn, not a back door into the draw for one
     * that is not. A genus that should only ever appear in vaults says so with a condition.
     *
     * <p>Every draw asks this once per candidate and keeps the answer: the conditions read terrain,
     * and asking twice per roll would also let the total and the walk disagree.
     */
    public int weightAt(int base, Level level, BlockPos pos) {
        if (base <= 0) {
            return 0;
        }
        for (PlacedWeight placed : weights) {
            if (placed.applies(level, pos)) {
                return placed.weight();
            }
        }
        return base;
    }
}
