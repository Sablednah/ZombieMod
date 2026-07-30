package com.sablednah.zombiemod.core;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sablednah.zombiemod.core.spawn.SpawnCondition;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;

/**
 * A night worth remembering.
 *
 * <p>Every other thing in this mod is an <em>encounter</em> — one monster, met on its own terms. A
 * horde is the layer above: a director that decides when several of them arrive together, builds,
 * peaks and subsides. That shape is the whole point, and it is why a horde is waves rather than a
 * number: twenty at once is a wall, whereas eight then twelve then twenty is a story with a middle.
 *
 * @param name      shown on the bar and in the warning
 * @param weight    relative chance of being chosen when one fires at random; 0 means command-only
 * @param waves     in order; each one's {@code delay} is counted from the previous wave spawning
 * @param radius    how far out the ring of spawns sits
 * @param minRadius and how close it may come — a wave that spawns on top of you is a mugging
 * @param barColor  boss bar colour, or absent for no bar
 * @param announce  warning text, with {@code &} colour codes
 * @param sound     played to everyone nearby when it begins
 * @param victory   shown when the last one falls; absent for a line built from the name
 * @param xp        experience for surviving it, on top of whatever the mobs themselves dropped
 * @param conditions where and when it may start at all
 */
public record HordeSpec(
        String name,
        int weight,
        List<Wave> waves,
        double radius,
        double minRadius,
        Optional<BossEvent.BossBarColor> barColor,
        Optional<String> announce,
        Optional<Holder<SoundEvent>> sound,
        Optional<String> victory,
        int xp,
        List<SpawnCondition> conditions) {

    /**
     * One push.
     *
     * @param count  how many to try to place
     * @param delay  ticks after the previous wave before this one arrives
     * @param genera explicit genus ids, or empty to draw from the weighted table as normal
     */
    public record Wave(int count, int delay, List<Identifier> genera) {
        public static final Codec<Wave> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("count").forGetter(Wave::count),
                Codec.INT.optionalFieldOf("delay", 200).forGetter(Wave::delay),
                Identifier.CODEC.listOf().optionalFieldOf("genera", List.of()).forGetter(Wave::genera))
                .apply(i, Wave::new));
    }

    private static final Codec<BossEvent.BossBarColor> COLOR = Codec.STRING.xmap(
            v -> BossEvent.BossBarColor.valueOf(v.toUpperCase(Locale.ROOT)),
            v -> v.name().toLowerCase(Locale.ROOT));

    public static final Codec<HordeSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(HordeSpec::name),
            Codec.INT.optionalFieldOf("weight", 0).forGetter(HordeSpec::weight),
            Wave.CODEC.listOf().fieldOf("waves").forGetter(HordeSpec::waves),
            Codec.DOUBLE.optionalFieldOf("radius", 40.0D).forGetter(HordeSpec::radius),
            Codec.DOUBLE.optionalFieldOf("min_radius", 20.0D).forGetter(HordeSpec::minRadius),
            COLOR.optionalFieldOf("bar_color").forGetter(HordeSpec::barColor),
            Codec.STRING.optionalFieldOf("announce").forGetter(HordeSpec::announce),
            BuiltInRegistries.SOUND_EVENT.holderByNameCodec().optionalFieldOf("sound")
                    .forGetter(HordeSpec::sound),
            Codec.STRING.optionalFieldOf("victory").forGetter(HordeSpec::victory),
            Codec.INT.optionalFieldOf("xp", 0).forGetter(HordeSpec::xp),
            SpawnCondition.CODEC.listOf().optionalFieldOf("conditions", List.of())
                    .forGetter(HordeSpec::conditions))
            .apply(i, HordeSpec::new));

    /** Everything it will try to place, for the bar's denominator. */
    public int totalCount() {
        return waves.stream().mapToInt(Wave::count).sum();
    }

    /**
     * What to say when it is over.
     *
     * <p>There is a default rather than an optional message because the bar simply vanishing on the
     * last kill reads as the feature ending rather than the player winning — the one thing every
     * horde needs is a full stop.
     */
    public String victoryText() {
        return victory.orElse("&a" + name + "&a is broken.");
    }
}
