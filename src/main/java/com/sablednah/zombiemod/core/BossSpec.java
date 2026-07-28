package com.sablednah.zombiemod.core;

import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.BossEvent;

/**
 * Turns a genus into a named encounter with a bar at the top of the screen.
 *
 * <p>Presence of this block is what makes something a boss; there is no separate flag. Pair it with
 * {@code "weight": 0} so it never turns up in the wild, and summon it deliberately.
 *
 * @param color      bar colour
 * @param overlay    plain bar or notched
 * @param range      how close a player must be to see the bar
 * @param darkenSky  the Wither's sky-darkening
 * @param bossMusic  play the boss music track
 * @param fog        the Ender Dragon's fog
 * @param title      bar text; falls back to the genus name, then to the mob's type
 */
public record BossSpec(
        BossEvent.BossBarColor color,
        BossEvent.BossBarOverlay overlay,
        double range,
        boolean darkenSky,
        boolean bossMusic,
        boolean fog,
        Optional<String> title) {

    private static <E extends Enum<E>> Codec<E> enumCodec(Class<E> type) {
        return Codec.STRING.xmap(
                v -> Enum.valueOf(type, v.toUpperCase(Locale.ROOT)),
                v -> v.name().toLowerCase(Locale.ROOT));
    }

    public static final MapCodec<BossSpec> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            enumCodec(BossEvent.BossBarColor.class).optionalFieldOf("color", BossEvent.BossBarColor.RED)
                    .forGetter(BossSpec::color),
            enumCodec(BossEvent.BossBarOverlay.class).optionalFieldOf("overlay", BossEvent.BossBarOverlay.PROGRESS)
                    .forGetter(BossSpec::overlay),
            Codec.DOUBLE.optionalFieldOf("range", 64.0D).forGetter(BossSpec::range),
            Codec.BOOL.optionalFieldOf("darken_sky", false).forGetter(BossSpec::darkenSky),
            Codec.BOOL.optionalFieldOf("boss_music", false).forGetter(BossSpec::bossMusic),
            Codec.BOOL.optionalFieldOf("fog", false).forGetter(BossSpec::fog),
            Codec.STRING.optionalFieldOf("title").forGetter(BossSpec::title))
            .apply(i, BossSpec::new));

    public static final Codec<BossSpec> CODEC = MAP_CODEC.codec();
}
