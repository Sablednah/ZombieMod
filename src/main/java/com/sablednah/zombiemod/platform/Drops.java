package com.sablednah.zombiemod.platform;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Putting an item on the floor at a player's feet, for when their inventory is full.
 *
 * <p><b>Why this exists.</b> {@code Player.drop(stack, bool)} gained a third {@code Prediction}
 * argument on 26.3 and the two-argument form is gone. Two call sites — the ZombieDex book and a
 * corpse claim — both handing a player something the server made.
 */
public final class Drops {

    private Drops() {}

    /**
     * Drop {@code stack} at {@code player}'s feet.
     *
     * <p><b>Differs per version.</b> On 26.3 this is {@code drop(stack, false, Prediction.SERVER_ONLY)}.
     * {@code SERVER_ONLY} because the client never drew this drop — it is what vanilla's own
     * server-initiated drops use ({@code AbstractContainerMenu}, {@code AdvancementRewards}).
     * {@code PREDICTED} compiles too, and is wrong.
     */
    public static void atFeet(Player player, ItemStack stack) {
        player.drop(stack, false);
    }
}
