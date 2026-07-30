package com.sablednah.zombiemod.core.mutate;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sablednah.zombiemod.core.spawn.SpawnCondition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;

/**
 * The trigger types a genus can name in JSON.
 *
 * <p>Kept small on purpose. Anything that is really a question about the <em>place</em> belongs in
 * {@link SpawnCondition} and reaches mutation through {@link Where}, so this file only holds the
 * things that need the entity itself.
 */
public final class MutationTriggers {

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("zombiemod", path);
    }

    // ---------------------------------------------------------------- state of the mob

    /**
     * Hurt badly enough to change.
     *
     * <p>{@code fraction} rather than a flat number by default, because a genus's health is its own
     * business: "below a third" means the same thing on a 20-health walker and a 300-health boss,
     * where "below 8" means *nearly dead* on one and *barely scratched* on the other.
     */
    public record HealthBelow(double fraction, Optional<Double> amount) implements MutationTrigger {

        public static final Identifier TYPE = id("health_below");

        public static final MapCodec<HealthBelow> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("fraction", 0.3D).forGetter(HealthBelow::fraction),
                Codec.DOUBLE.optionalFieldOf("amount").forGetter(HealthBelow::amount))
                .apply(i, HealthBelow::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(ServerLevel level, Mob mob) {
            return amount.map(a -> mob.getHealth() <= a)
                    .orElseGet(() -> mob.getHealth() <= mob.getMaxHealth() * fraction);
        }
    }

    /** Wading, swimming or rained on hard enough to count. */
    public record InWater() implements MutationTrigger {

        public static final Identifier TYPE = id("in_water");
        public static final MapCodec<InWater> CODEC = MapCodec.unit(InWater::new);

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(ServerLevel level, Mob mob) {
            return mob.isInWater();
        }
    }

    /** Standing in the stuff. */
    public record InLava() implements MutationTrigger {

        public static final Identifier TYPE = id("in_lava");
        public static final MapCodec<InLava> CODEC = MapCodec.unit(InLava::new);

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(ServerLevel level, Mob mob) {
            return mob.isInLava();
        }
    }

    /** Actually burning — from any source, including the sun. */
    public record OnFire() implements MutationTrigger {

        public static final Identifier TYPE = id("on_fire");
        public static final MapCodec<OnFire> CODEC = MapCodec.unit(OnFire::new);

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(ServerLevel level, Mob mob) {
            return mob.isOnFire();
        }
    }

    /**
     * Touching a particular kind of block.
     *
     * <p>Checks the column the mob occupies and the block it is standing on, which between them
     * cover the three ways "touching" is meant: standing on it, standing in it, and wading through
     * it. A block <em>tag</em> is the expected value — {@code "#minecraft:ice"} rather than a list
     * of every ice variant.
     */
    public record Touching(HolderSet<Block> blocks, boolean below) implements MutationTrigger {

        public static final Identifier TYPE = id("touching");

        public static final MapCodec<Touching> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("blocks").forGetter(Touching::blocks),
                Codec.BOOL.optionalFieldOf("below", true).forGetter(Touching::below))
                .apply(i, Touching::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(ServerLevel level, Mob mob) {
            BlockPos feet = mob.blockPosition();
            if (matches(level, feet) || matches(level, feet.above())) {
                return true;
            }
            return below && matches(level, feet.below());
        }

        private boolean matches(ServerLevel level, BlockPos pos) {
            return level.getBlockState(pos).is(blocks);
        }
    }

    // ---------------------------------------------------------------- borrowed and combined

    /**
     * Any spawn condition, asked about where the mob is standing.
     *
     * <p>This is the type that makes the whole set worth having: "tougher in the Nether", "only in a
     * swamp", "only in daylight" all already exist as spawn conditions, and re-implementing them
     * against a Mob would be duplicating tested code to get a worse copy of it.
     */
    public record Where(SpawnCondition condition) implements MutationTrigger {

        public static final Identifier TYPE = id("where");

        public static final MapCodec<Where> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                SpawnCondition.CODEC.fieldOf("condition").forGetter(Where::condition))
                .apply(i, Where::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(ServerLevel level, Mob mob) {
            return condition.test(level, mob.blockPosition());
        }
    }

    /** All of them, so a mutation can want two things at once. */
    public record AllOf(List<MutationTrigger> triggers) implements MutationTrigger {

        public static final Identifier TYPE = id("all_of");

        public static final MapCodec<AllOf> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                MutationTrigger.CODEC.listOf().fieldOf("triggers").forGetter(AllOf::triggers))
                .apply(i, AllOf::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(ServerLevel level, Mob mob) {
            return triggers.stream().allMatch(t -> t.test(level, mob));
        }
    }

    /** Any of them. */
    public record AnyOf(List<MutationTrigger> triggers) implements MutationTrigger {

        public static final Identifier TYPE = id("any_of");

        public static final MapCodec<AnyOf> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                MutationTrigger.CODEC.listOf().fieldOf("triggers").forGetter(AnyOf::triggers))
                .apply(i, AnyOf::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(ServerLevel level, Mob mob) {
            return triggers.stream().anyMatch(t -> t.test(level, mob));
        }
    }

    /** The opposite. */
    public record Not(MutationTrigger trigger) implements MutationTrigger {

        public static final Identifier TYPE = id("not");

        public static final MapCodec<Not> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                MutationTrigger.CODEC.fieldOf("trigger").forGetter(Not::trigger))
                .apply(i, Not::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(ServerLevel level, Mob mob) {
            return !trigger.test(level, mob);
        }
    }

    private MutationTriggers() {}
}
