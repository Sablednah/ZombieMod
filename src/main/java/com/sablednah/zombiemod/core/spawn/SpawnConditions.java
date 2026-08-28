package com.sablednah.zombiemod.core.spawn;

import java.time.MonthDay;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.sablednah.zombiemod.platform.Times;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/** The spawn conditions ZombieMod ships. Adding one is a record plus a line in {@link SpawnConditionTypes}. */
public final class SpawnConditions {

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("zombiemod", path);
    }

    /** Biome allow-list. Takes a tag (`#minecraft:is_forest`) or a list of ids. */
    public record InBiome(HolderSet<Biome> biomes) implements SpawnCondition {

        public static final Identifier TYPE = id("biome");

        public static final MapCodec<InBiome> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(InBiome::biomes))
                .apply(i, InBiome::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(Level level, BlockPos pos) {
            return answerable(level, pos) && biomes.contains(level.getBiome(pos));
        }
    }

    /** Dimension allow-list. */
    public record InDimension(List<ResourceKey<Level>> dimensions) implements SpawnCondition {

        public static final Identifier TYPE = id("dimension");

        private static final Codec<ResourceKey<Level>> KEY_CODEC =
                Identifier.CODEC.xmap(i -> ResourceKey.create(Registries.DIMENSION, i), ResourceKey::identifier);

        public static final MapCodec<InDimension> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                KEY_CODEC.listOf().fieldOf("dimensions").forGetter(InDimension::dimensions))
                .apply(i, InDimension::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(Level level, BlockPos pos) {
            return dimensions.contains(level.dimension());
        }
    }

    /** Height band. Either bound may be omitted. */
    public record Height(Optional<Integer> min, Optional<Integer> max) implements SpawnCondition {

        public static final Identifier TYPE = id("height");

        public static final MapCodec<Height> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("min").forGetter(Height::min),
                Codec.INT.optionalFieldOf("max").forGetter(Height::max))
                .apply(i, Height::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(Level level, BlockPos pos) {
            int y = pos.getY();
            return min.map(m -> y >= m).orElse(true) && max.map(m -> y <= m).orElse(true);
        }
    }

    /**
     * Light level at the spawn point.
     *
     * <p>Reads the effective local brightness, so it follows the day/night cycle outdoors — a
     * {@code max} of 7 means "dark", whether that is a cave or midnight in a field.
     */
    public record Light(Optional<Integer> min, Optional<Integer> max) implements SpawnCondition {

        public static final Identifier TYPE = id("light");

        public static final MapCodec<Light> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("min").forGetter(Light::min),
                Codec.INT.optionalFieldOf("max").forGetter(Light::max))
                .apply(i, Light::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(Level level, BlockPos pos) {
            if (!answerable(level, pos)) {
                return false;
            }
            int light = level.getMaxLocalRawBrightness(pos);
            return min.map(m -> light >= m).orElse(true) && max.map(m -> light <= m).orElse(true);
        }
    }

    /**
     * Which moon is up.
     *
     * <p>Eight phases, one per day, so a phase list is a way of saying "this happens on some nights
     * and not others" that a player can <em>see coming</em> by looking up. That is the whole appeal
     * over a low weight: both make a thing rare, but only one of them is legible.
     *
     * <p>Reads {@code EnvironmentAttributes.MOON_PHASE} rather than computing the phase from the day
     * count. It is position-dependent in 1.21.11, so a dimension or a region is free to disagree
     * about what the moon is doing, and asking the attribute system gets that right for free.
     */
    public record Moon(List<MoonPhase> phases) implements SpawnCondition {

        public static final Identifier TYPE = id("moon");

        public static final MapCodec<Moon> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                MoonPhase.CODEC.listOf().fieldOf("phases").forGetter(Moon::phases))
                .apply(i, Moon::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(Level level, BlockPos pos) {
            return phases.contains(level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos));
        }
    }

    /**
     * The real-world date, so a genus can belong to a season rather than to a place.
     *
     * <p>{@code {"type": "zombiemod:date", "from": "10-25", "to": "11-02"}} — month-day, inclusive,
     * recurring every year. Composes like any other condition, so "in late October <em>and</em> at
     * night" is an {@code any_of}/{@code not} away.
     *
     * <p><b>The server's date, not the player's.</b> Everyone in a session must meet the same
     * October: a genus that appears for one player and not the one standing beside them is a bug
     * report, not a feature. That means the server's timezone decides, which is the only clock all
     * players share.
     *
     * <p><b>The range wraps the year.</b> {@code "from": "12-20", "to": "01-05"} is a fortnight over
     * New Year, not an empty set — which is what a plain {@code from <= today <= to} would make it,
     * and that is the first range anyone writing a Christmas genus will reach for.
     *
     * <p><b>Testable out of season.</b> A date-gated genus is invisible for fifty-one weeks, which is
     * indistinguishable from broken, so {@code zombiemod-server.toml} has a {@code dateOverride} that
     * pretends today is some other day. {@code /zombiemod status} prints the date in force and which
     * date-gated genera are in season, because otherwise the only available conclusion is "the
     * Halloween zombies do not work".
     */
    public record OnDate(MonthDay from, MonthDay to) implements SpawnCondition {

        public static final Identifier TYPE = id("date");

        /** {@code MM-DD}. Rejected at load if it is not a real day, so a typo fails loudly. */
        private static final Codec<MonthDay> MONTH_DAY = Codec.STRING.comapFlatMap(
                text -> {
                    try {
                        return com.mojang.serialization.DataResult.success(
                                MonthDay.parse("--" + text));
                    } catch (RuntimeException e) {
                        return com.mojang.serialization.DataResult.error(
                                () -> "Not a MM-DD date: " + text);
                    }
                },
                md -> String.format("%02d-%02d", md.getMonthValue(), md.getDayOfMonth()));

        public static final MapCodec<OnDate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                MONTH_DAY.fieldOf("from").forGetter(OnDate::from),
                MONTH_DAY.fieldOf("to").forGetter(OnDate::to))
                .apply(i, OnDate::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        /** Today, or whatever {@code dateOverride} says it is. Public so status can report it. */
        public static MonthDay today() {
            String override = com.sablednah.zombiemod.ZombieModConfig.DATE_OVERRIDE.get();
            if (!override.isBlank()) {
                try {
                    return MonthDay.parse("--" + override);
                } catch (RuntimeException ignored) {
                    // A bad override must not take the world with it; fall through to the real date.
                }
            }
            return MonthDay.now();
        }

        public boolean inSeason() {
            MonthDay today = today();
            // Inclusive both ends. When from is after to the range crosses the year, and the test
            // inverts: in season if we are past the start OR before the end, rather than both.
            return from.compareTo(to) <= 0
                    ? today.compareTo(from) >= 0 && today.compareTo(to) <= 0
                    : today.compareTo(from) >= 0 || today.compareTo(to) <= 0;
        }

        @Override
        public boolean test(Level level, BlockPos pos) {
            return inSeason();
        }
    }

    /**
     * How far below the local surface this is.
     *
     * <p>Zero standing in a field, two or three under your own roof, hundreds in a deep cave — which
     * is the distinction {@link SeeSky} cannot draw on its own. {@code see_sky: false} is true of a
     * bedroom and of the bottom of a ravine alike, and for anything that arrives from outside, those
     * two are opposites.
     *
     * <p>Measured against the heightmap for the column rather than an absolute Y, so it means the
     * same thing on a mountain as it does at sea level. The chunk must be loaded for the heightmap
     * to be real — and it is <em>not</em> always, whatever this comment used to claim. It said
     * "every caller here is at a player or a live spawn attempt, so it always is", and that
     * assumption is what let a deadlock through: another mod populating a chunk during generation is
     * a caller too. See {@link #answerable}.
     */
    public record Depth(Optional<Integer> min, Optional<Integer> max) implements SpawnCondition {

        public static final Identifier TYPE = id("depth");

        public static final MapCodec<Depth> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("min").forGetter(Depth::min),
                Codec.INT.optionalFieldOf("max").forGetter(Depth::max))
                .apply(i, Depth::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(Level level, BlockPos pos) {
            if (!answerable(level, pos)) {
                return false;
            }
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
            int depth = surface - pos.getY();
            return min.map(m -> depth >= m).orElse(true) && max.map(m -> depth <= m).orElse(true);
        }
    }

    /** Open sky above, or deliberately not. */
    public record SeeSky(boolean value) implements SpawnCondition {

        public static final Identifier TYPE = id("see_sky");

        public static final MapCodec<SeeSky> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.BOOL.optionalFieldOf("value", true).forGetter(SeeSky::value))
                .apply(i, SeeSky::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(Level level, BlockPos pos) {
            return answerable(level, pos) && level.canSeeSky(pos) == value;
        }
    }

    /**
     * Passes when any nested condition passes.
     *
     * <p>The conditions on a genus are ANDed, which is the useful default; this is how you say
     * "swamps or mangroves" without needing a second genus.
     */
    public record AnyOf(List<SpawnCondition> conditions) implements SpawnCondition {

        public static final Identifier TYPE = id("any_of");

        public static final MapCodec<AnyOf> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                SpawnCondition.CODEC.listOf().fieldOf("conditions").forGetter(AnyOf::conditions))
                .apply(i, AnyOf::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(Level level, BlockPos pos) {
            for (SpawnCondition c : conditions) {
                if (c.test(level, pos)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Time of day, on the 24000-tick cycle.
     *
     * <p>Distinct from {@code light}, and both are useful: light asks "is it dark <em>here</em>",
     * which is true in a cave at noon; time asks "is it night in this world", which is true in a
     * lit room at midnight. Day/night behaviour switching wants time; spawn darkness wants light.
     */
    public record TimeOfDay(Optional<Phase> phase, Optional<Integer> min, Optional<Integer> max)
            implements SpawnCondition {

        public static final Identifier TYPE = id("time");

        /** Vanilla's own boundaries: night runs 13000–23000, day the rest. */
        public enum Phase {
            DAY(0, 12999), NIGHT(13000, 23999);

            final int from;
            final int to;

            Phase(int from, int to) {
                this.from = from;
                this.to = to;
            }

            public static final Codec<Phase> CODEC = Codec.STRING.xmap(
                    v -> valueOf(v.toUpperCase(Locale.ROOT)),
                    v -> v.name().toLowerCase(Locale.ROOT));
        }

        public static final MapCodec<TimeOfDay> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Phase.CODEC.optionalFieldOf("phase").forGetter(TimeOfDay::phase),
                Codec.INT.optionalFieldOf("min").forGetter(TimeOfDay::min),
                Codec.INT.optionalFieldOf("max").forGetter(TimeOfDay::max))
                .apply(i, TimeOfDay::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(Level level, BlockPos pos) {
            int t = (int) (Times.dayTime(level) % 24000L);
            if (phase.isPresent() && (t < phase.get().from || t > phase.get().to)) {
                return false;
            }
            return min.map(m -> t >= m).orElse(true) && max.map(m -> t <= m).orElse(true);
        }
    }

    /**
     * Inside (or outside) a land claim.
     *
     * <p>Registered whether or not FTB Chunks is installed, and answers "not claimed" when it isn't.
     * That is the point: a genus file naming this condition must still load on a server without the
     * mod, or a datapack becomes silently unportable and, worse, stops the world loading.
     */
    public record InClaim(boolean value) implements SpawnCondition {

        public static final Identifier TYPE = id("in_claim");

        public static final MapCodec<InClaim> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.BOOL.optionalFieldOf("value", true).forGetter(InClaim::value))
                .apply(i, InClaim::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(Level level, BlockPos pos) {
            return com.sablednah.zombiemod.compat.LandClaims.isClaimed(level, pos) == value;
        }
    }

    /**
     * Is this column safe to ask questions about, right now, on this thread?
     *
     * <p>Anything that reads terrain — height, biome, light, sky — goes through the chunk source,
     * and on a chunk that is not resident that means <em>blocking</em> until one is generated. From
     * the server thread that is merely slow. From a worldgen thread it is fatal: the worker parks on
     * a future the server thread is itself waiting to fulfil, and the game hangs at "Preparing spawn
     * area" with no crash and no log line. That is not hypothetical — CityWorld populates chunks
     * from inside generation, and it hung exactly like that.
     *
     * <p>{@code hasChunkAt} answers from what is already loaded and never blocks, so asking first
     * costs a lookup and removes the whole class of hang.
     *
     * <p>Callers <b>fail closed</b> when this is false: an unanswerable condition is treated as not
     * met, so the genus does not spawn and you get an ordinary mob. Failing open would let a genus
     * ignore its own rules precisely when we cannot check them, which is the worse of the two.
     */
    private static boolean answerable(Level level, BlockPos pos) {
        return level.hasChunkAt(pos);
    }

    // ---------------------------------------------------------------- CityWorld

    /*
     * The three below all answer "no" when CityWorld is absent, or when the level is an ordinary
     * one. Failing closed rather than open is the only defensible default: a genus that says it
     * belongs in a highrise district is opting into CityWorld, and the alternative — treating an
     * unanswerable question as satisfied — would put city-only monsters everywhere in a vanilla
     * world, which is both surprising and much harder to diagnose than their simply not appearing.
     */

    /**
     * Which district: {@code HIGHRISE}, {@code FARM}, {@code NATURE}, {@code INDUSTRIAL} and the
     * rest, or the context class name for finer grain.
     *
     * <p>This is the coarse "what kind of place is this", and the one most genera want.
     */
    public record CityDistrict(List<String> districts, List<String> classes) implements SpawnCondition {

        public static final Identifier TYPE = id("city_district");

        public static final MapCodec<CityDistrict> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.listOf().optionalFieldOf("districts", List.of()).forGetter(CityDistrict::districts),
                Codec.STRING.listOf().optionalFieldOf("classes", List.of()).forGetter(CityDistrict::classes))
                .apply(i, CityDistrict::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(Level level, BlockPos pos) {
            return com.sablednah.zombiemod.compat.CityWorld.lotAt(level, pos)
                    .filter(lot -> matches(districts, lot.context()) && matches(classes, lot.contextClass()))
                    .isPresent();
        }
    }

    /**
     * Which lot: {@code ROAD} for something that hunts the streets, {@code STRUCTURE} for something
     * that lives indoors, or a named schematic for something that haunts one specific building.
     */
    public record CityLot(List<String> styles, List<String> classes, List<String> schematics)
            implements SpawnCondition {

        public static final Identifier TYPE = id("city_lot");

        public static final MapCodec<CityLot> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.listOf().optionalFieldOf("styles", List.of()).forGetter(CityLot::styles),
                Codec.STRING.listOf().optionalFieldOf("classes", List.of()).forGetter(CityLot::classes),
                Codec.STRING.listOf().optionalFieldOf("schematics", List.of()).forGetter(CityLot::schematics))
                .apply(i, CityLot::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(Level level, BlockPos pos) {
            return com.sablednah.zombiemod.compat.CityWorld.lotAt(level, pos)
                    .filter(lot -> matches(styles, lot.style())
                            && matches(classes, lot.lotClass())
                            && matches(schematics, lot.schematic()))
                    .isPresent();
        }
    }

    /**
     * How wild the generator graded this place: 0.0 is dense city, 1.0 is wilderness.
     *
     * <p>The one worth reaching for when you want density rather than a category — "more of them the
     * further into town you get" is a range on this, not a list of district names.
     */
    public record CityNature(double min, double max) implements SpawnCondition {

        public static final Identifier TYPE = id("city_nature");

        public static final MapCodec<CityNature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("min", 0.0D).forGetter(CityNature::min),
                Codec.DOUBLE.optionalFieldOf("max", 1.0D).forGetter(CityNature::max))
                .apply(i, CityNature::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(Level level, BlockPos pos) {
            return com.sablednah.zombiemod.compat.CityWorld.lotAt(level, pos)
                    .filter(lot -> lot.nature() >= min && lot.nature() <= max)
                    .isPresent();
        }
    }

    /**
     * An empty list means "do not care", which is what lets one condition ask about a district, a
     * class, or both. Case-insensitive so a datapack can write {@code "highrise"} rather than
     * shouting the enum constant.
     */
    private static boolean matches(List<String> wanted, String actual) {
        if (wanted.isEmpty()) {
            return true;
        }
        return actual != null && wanted.stream().anyMatch(w -> w.equalsIgnoreCase(actual));
    }

    /** Inverts a condition — "anywhere but the Nether", "not in daylight". */
    public record Not(SpawnCondition condition) implements SpawnCondition {

        public static final Identifier TYPE = id("not");

        public static final MapCodec<Not> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                SpawnCondition.CODEC.fieldOf("condition").forGetter(Not::condition))
                .apply(i, Not::new));

        @Override
        public Identifier type() {
            return TYPE;
        }

        @Override
        public boolean test(Level level, BlockPos pos) {
            return !condition.test(level, pos);
        }
    }

    private SpawnConditions() {}
}
