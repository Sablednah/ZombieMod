package com.sablednah.zombiemod.client;

import java.util.List;

import com.sablednah.zombiemod.net.DexPayload;

/**
 * The last dex the server sent us. Client-only, and only ever reached from inside an
 * {@code enqueueWork} lambda, so a dedicated server never loads this class.
 */
public final class DexState {

    private static volatile List<DexPayload.Entry> entries = List.of();
    /** Seasonal genera: starred, and outside every total. Empty against a server too old to say. */
    private static volatile java.util.Set<net.minecraft.resources.Identifier> bonus = java.util.Set.of();

    private DexState() {}

    public static void accept(DexPayload payload) {
        entries = List.copyOf(payload.entries());
    }

    /**
     * Forget it on disconnect.
     *
     * <p>Otherwise the last server's roster is still here when you join the next one, and until that
     * server (if it even runs the mod) sends its own, the dex would show another world's zombies
     * with another world's kill counts.
     */
    public static void clear() {
        entries = List.of();
        bonus = java.util.Set.of();
    }

    public static void acceptBonus(com.sablednah.zombiemod.net.DexBonusPayload payload) {
        bonus = java.util.Set.copyOf(payload.genera());
    }

    public static boolean isBonus(DexPayload.Entry e) {
        return bonus.contains(e.genus());
    }

    public static List<DexPayload.Entry> entries() {
        return entries;
    }

    /** Of the genera that count. A seasonal one is a bonus and is tallied on its own. */
    public static int slain() {
        return (int) entries.stream().filter(e -> !isBonus(e) && e.kills() > 0).count();
    }

    public static int total() {
        return (int) entries.stream().filter(e -> !isBonus(e)).count();
    }

    public static int bonus() {
        return (int) entries.stream().filter(DexState::isBonus).count();
    }

    /**
     * The line under the title. Here rather than in the screen because the screen is one of the few
     * files that differs per Minecraft version, and what the dex <em>counts</em> should not be
     * written three times.
     */
    public static String summary() {
        int bonus = bonus();
        return "§8✦ §7" + slain() + "§8/§7" + total() + " slain §8· §7"
                + met() + " met" + (bonus > 0 ? " §8· §d+" + bonus + " bonus" : "") + " §8✦";
    }

    /** A row's progress mark, with a star on a bonus entry. */
    public static String mark(DexPayload.Entry e) {
        String mark = e.kills() > 0 ? "§a✔ " : e.met() ? "§e? " : "§8✘ ";
        return isBonus(e) ? mark + "§d★ " : mark;
    }

    public static int met() {
        return (int) entries.stream().filter(e -> !isBonus(e) && e.met()).count();
    }
}
