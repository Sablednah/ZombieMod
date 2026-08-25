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
        return ChatFormatting.COLOR_CODEC;
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
        return colour.getName();
    }

    /**
     * Paint a scoreboard team in this colour, which is what actually tints the outline.
     *
     * <p><b>Differs per version.</b> On 26.2 this becomes
     * {@code team.setColor(Optional.of(TeamColor...))}.
     */
    public static void paint(PlayerTeam team, ChatFormatting colour) {
        team.setColor(colour);
    }
}
