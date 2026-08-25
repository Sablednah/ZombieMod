package com.sablednah.zombiemod.platform;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;

/**
 * The codec behind a genus's {@code glow} field.
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
}
