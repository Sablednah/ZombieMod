package com.sablednah.zombiemod.core;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

/**
 * Is this the ZombieDex?
 *
 * <p>Shared rather than client-only, because both halves have to agree about it and for opposite
 * reasons: the client opens the screen, and the server has to <em>not</em> open the book. Two copies
 * of "what counts as the dex" would eventually disagree, and the symptom of that disagreement is
 * both windows opening at once.
 *
 * <p>Nothing here touches a client class, so a dedicated server loads it happily.
 */
public final class DexBook {

    /** The title, as written by {@code /zombiemod bestiary book} and as read by an anvil. */
    public static final String TITLE = "ZombieDex";

    private DexBook() {}

    /**
     * A book called ZombieDex — by its written title, or by an anvil rename.
     *
     * <p>Both, because the second is free and lets a map maker hand one out as loot without needing
     * the command. Restricted to book items so that naming a pickaxe ZombieDex does nothing
     * surprising.
     */
    public static boolean is(ItemStack stack) {
        if (!stack.is(Items.WRITTEN_BOOK) && !stack.is(Items.WRITABLE_BOOK) && !stack.is(Items.BOOK)) {
            return false;
        }
        WrittenBookContent written = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (written != null && named(written.title().raw())) {
            return true;
        }
        var custom = stack.get(DataComponents.CUSTOM_NAME);
        return custom != null && named(custom.getString());
    }

    private static boolean named(String name) {
        return name != null && name.trim().equalsIgnoreCase(TITLE);
    }
}
