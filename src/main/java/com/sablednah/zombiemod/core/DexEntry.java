package com.sablednah.zombiemod.core;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * A genus's write-up, as facts rather than as a layout.
 *
 * <p>Lives in {@code core} and returns plain rows so both halves of the mod can render it their own
 * way: chat draws them as lines for a vanilla player, the client screen draws them as a page. One
 * definition of <em>what a genus is</em>, two presentations — which is the only way the two stay
 * saying the same thing as the mod grows.
 */
public final class DexEntry {

    /** @param detail null for a plain heading row */
    public record Row(String label, String detail) {}

    private DexEntry() {}

    /** The stat block. Only what the genus actually declares — an absent field is not a zero. */
    public static List<Row> stats(Genus genus) {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("Base", genus.base().builtInRegistryHolder().key().identifier().getPath()));
        genus.health().ifPresent(v -> rows.add(new Row("Health", trim(v))));
        genus.damage().ifPresent(v -> rows.add(new Row("Damage", trim(v))));
        if (genus.speed() != 1.0D) {
            // A multiplier on the base mob, not blocks per tick - saying "1.6" alone would read as
            // a speed rather than a comparison.
            rows.add(new Row("Speed", "x" + trim(genus.speed())));
        }
        if (genus.scale() != 1.0D) {
            rows.add(new Row("Size", "x" + trim(genus.scale())));
        }
        genus.followRange().ifPresent(v -> rows.add(new Row("Notices you at", trim(v) + " blocks")));
        genus.xp().ifPresent(v -> rows.add(new Row("Experience", String.valueOf(v))));
        genus.bounty().ifPresent(v -> rows.add(new Row("Bounty", trim(v))));
        if (genus.baby()) {
            rows.add(new Row("Build", "a child"));
        }
        if (genus.invisible()) {
            rows.add(new Row("Build", "unseen but for what it wears"));
        }
        if (genus.burning()) {
            rows.add(new Row("Build", "alight"));
        }
        genus.glow().ifPresent(c -> rows.add(new Row("Glow", c.getName())));
        return rows;
    }

    /** What it does, by ability id. The id is the honest name — a genus is data, not prose. */
    public static List<String> abilities(Genus genus) {
        List<String> out = new ArrayList<>();
        for (var ability : genus.abilities()) {
            String path = ability.type().getPath();
            if (!out.contains(path)) {
                out.add(path);
            }
        }
        return out;
    }

    /** One line of human explanation per ability, so the list is not just jargon. */
    public static String explain(String ability) {
        return switch (ability) {
            case "adapt" -> "Learns what hurt it and stops taking as much from that.";
            case "alert" -> "Calls the others to whatever it has found.";
            case "beam" -> "Fires a guardian's beam.";
            case "break_blocks" -> "Breaks its way through, or simply breaks what it walks past.";
            case "effect" -> "Applies a potion effect, to itself or to whoever is near.";
            case "explode" -> "Detonates.";
            case "fuse" -> "Counts down, then detonates.";
            case "heal" -> "Closes its own wounds.";
            case "infect" -> "A bite that takes hold later, whatever finally kills you. Milk cures it.";
            case "leap" -> "Jumps at you.";
            case "lightning" -> "Calls down a bolt.";
            case "particles" -> "Trails something visible.";
            case "place_block" -> "Leaves blocks behind it.";
            case "projectile" -> "Throws or fires something.";
            case "pull" -> "Drags you towards it.";
            case "ray" -> "A hitscan line, with an audible wind-up you can break.";
            case "shockwave" -> "Knocks everything nearby away from it.";
            case "sound" -> "Makes noise.";
            case "summon" -> "Brings more.";
            case "teleport" -> "Moves without crossing the ground between.";
            case "convert" -> "What it kills gets back up as one of them.";
            default -> "";
        };
    }

    /** Chat rendering, for a vanilla client. */
    public static List<Component> chat(Genus genus, String displayName) {
        List<Component> out = new ArrayList<>();
        out.add(Component.literal(displayName).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        genus.description().ifPresent(d ->
                out.add(Component.literal(d).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)));
        for (Row r : stats(genus)) {
            out.add(Component.literal(" " + r.label() + ": ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(r.detail()).withStyle(ChatFormatting.WHITE)));
        }
        List<String> abilities = abilities(genus);
        if (!abilities.isEmpty()) {
            out.add(Component.literal(" Abilities").withStyle(ChatFormatting.DARK_GRAY));
            for (String a : abilities) {
                MutableComponent line = Component.literal("  " + a).withStyle(ChatFormatting.YELLOW);
                String why = explain(a);
                if (!why.isEmpty()) {
                    line.append(Component.literal(" - " + why).withStyle(ChatFormatting.GRAY));
                }
                out.add(line);
            }
        }
        return out;
    }

    private static String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
