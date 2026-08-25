package com.sablednah.zombiemod.platform;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Everything a genus's {@code glow} colour touches.
 *
 * <p><b>Why this exists.</b> 26.2 removed {@code ChatFormatting.COLOR_CODEC} — and
 * {@code ChatFormatting.CODEC} with it — while keeping the constants themselves. One call site, but
 * it is a *datapack-facing* one: every genus that glows names its colour as a string in JSON, and
 * that spelling is part of the mod's public format. Whatever a version branch does here, {@code
 * "dark_aqua"} must keep parsing, or shipped genera and third-party packs break at world load.
 *
 * <p>On a version without the constant, build it by name rather than by ordinal:
 * {@code Codec.STRING.xmap(ChatFormatting::getByName, ChatFormatting::getName)} — names are stable
 * and the numeric ids are not.
 *
 * <p><b>Glow is wider than a codec, which is why applying it lives here too.</b> A glowing genus is
 * put on a scoreboard team, because the outline colour comes from team colour and nothing else — and
 * on 26.2 {@code PlayerTeam.setColor} takes an {@code Optional<TeamColor>} rather than a
 * {@code ChatFormatting}, while {@code ChatFormatting.getName()} is gone as well. Three call sites
 * in three files, all of them the same feature, so they belong behind one seam rather than three.
 */
public final class Colours {

    private Colours() {}

    /**
     * Colours only, not the styling codes: a genus glowing "bold" is meaningless.
     *
     * <p><b>Differs per version.</b> Gone on 26.2 — see the class note for how to rebuild it.
     */
    public static Codec<ChatFormatting> glowCodec() {
        // Built by name, never by ordinal: names are stable across versions, ids are not.
        // getByName is gone on 26.2 as well, so go through the enum's own name - and reject
        // anything that is not a colour, or "bold" would parse and then glow nothing.
        return Codec.STRING.comapFlatMap(Colours::parse, Colours::name);
    }

    // ChatFormatting.isColor() is gone on 26.2 too, and TeamColor is the better test anyway: a glow
    // colour is valid exactly when the thing it feeds can take it. "bold" fails here rather than
    // parsing and then glowing nothing.
    private static com.mojang.serialization.DataResult<ChatFormatting> parse(String name) {
        String upper = name.toUpperCase(java.util.Locale.ROOT);
        try {
            net.minecraft.world.scores.TeamColor.valueOf(upper);
            return com.mojang.serialization.DataResult.success(ChatFormatting.valueOf(upper));
        } catch (IllegalArgumentException e) {
            return com.mojang.serialization.DataResult.error(() -> "Not a glow colour: " + name);
        }
    }

    /**
     * The colour's stable lowercase name — used for the team name and shown in the dex.
     *
     * <p><b>Differs per version.</b> {@code ChatFormatting.getName()} is gone on 26.2.
     *
     * <p>It is load-bearing beyond display: the team a glowing mob joins is named after it, so a
     * change of spelling here strands mobs on the old team and they stop sharing an outline.
     */
    public static String name(ChatFormatting colour) {
        return colour.name().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Paint a scoreboard team in this colour, which is what actually tints the outline.
     *
     * <p><b>Differs per version.</b> On 26.2 this becomes
     * {@code team.setColor(Optional.of(TeamColor...))}.
     */
    public static void paint(PlayerTeam team, ChatFormatting colour) {
        // ChatFormatting and TeamColor share their colour names, so the enum name is the
        // bridge. glowCodec admits colours only, so there is no styling constant to miss.
        team.setColor(java.util.Optional.of(
                net.minecraft.world.scores.TeamColor.valueOf(colour.name())));
    }
}
