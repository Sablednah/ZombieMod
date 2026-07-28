package com.sablednah.zombiemod.core;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** How a phase announces itself. */
public enum Announce {
    /** Above the hotbar. Brief, unobtrusive, easy to miss mid-fight. */
    ACTION_BAR,
    /** In chat. Stays put, so nobody misses it. */
    CHAT,
    /** Big text across the middle of the screen, like the Wither arriving. */
    TITLE,
    /** Both chat and the big title. */
    TITLE_AND_CHAT,
    NONE;

    public static final Codec<Announce> CODEC = Codec.STRING.xmap(
            v -> valueOf(v.toUpperCase(Locale.ROOT)),
            v -> v.name().toLowerCase(Locale.ROOT));

    /**
     * Translate {@code &}-prefixed colour codes, the convention the 1.8 plugin and WoodDye both use.
     *
     * <p>Kept because a phase line reading "&cPatient Zero comes apart." with a literal ampersand is
     * a worse failure than not supporting colour at all.
     */
    public static Component format(String text) {
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && ChatFormatting.getByCode(Character.toLowerCase(chars[i + 1])) != null) {
                chars[i] = ChatFormatting.PREFIX_CODE;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return Component.literal(new String(chars));
    }
}
