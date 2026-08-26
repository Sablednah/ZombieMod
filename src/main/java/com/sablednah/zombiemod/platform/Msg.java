package com.sablednah.zombiemod.platform;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Sending a player a line of text, in the two places it can go.
 *
 * <p><b>Why this exists.</b> {@code Player.displayClientMessage(Component, boolean)} does not exist
 * from Minecraft 26.1 onward. The boolean meant "action bar rather than chat", and the replacement
 * says it in the method name instead: {@code sendSystemMessage} and {@code sendOverlayMessage}. It is
 * a rename with a decision attached rather than a redesign, and it accounts for 20 of the compile
 * errors on both new versions. See {@code docs/MULTIVERSION.md}.
 *
 * <p><b>Both methods differ per version</b>, so both bodies are what a version branch edits — but
 * that is two lines in one file rather than twenty calls across seven. Note {@code sendSystemMessage}
 * exists on {@code ServerPlayer} on 1.21.11 and is tempting to reach for; it is not on {@code Player},
 * and several callers here only have a {@code Player}, so it is not the shared spelling it appears to
 * be.
 *
 * <p>Prefer these over calling either vanilla method directly, so the next version's rename lands
 * here rather than in twenty files.
 */
public final class Msg {

    private Msg() {}

    /**
     * Ordinary chat.
     *
     * <p><b>Differs per version.</b> On 26.1+ this becomes {@code player.sendSystemMessage(text)}.
     */
    public static void chat(Player player, Component text) {
        player.sendSystemMessage(text);
    }

    /**
     * The bar above the hotbar, for things that should not scroll chat away — a bounty landing, a
     * boss changing phase.
     *
     * <p><b>The one line that differs per version.</b> On 26.1+ this becomes
     * {@code player.sendOverlayMessage(text)}.
     */
    public static void actionBar(Player player, Component text) {
        player.sendOverlayMessage(text);
    }
}
