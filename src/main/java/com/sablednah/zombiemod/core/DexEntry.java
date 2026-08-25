package com.sablednah.zombiemod.core;

import com.sablednah.zombiemod.platform.Colours;

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
        genus.glow().ifPresent(c -> rows.add(new Row("Glow", Colours.name(c))));
        return rows;
    }

    /**
     * What it does, in its own words.
     *
     * <p>Each ability writes its own sentence from its own fields, so "applies a potion effect" can
     * be "Applies Nausea to players nearby within 4 blocks for 6s". The fallback table below only
     * covers abilities that have not written one yet — it can say what the ability <em>is</em> and
     * never what this genus's copy of it does.
     */
    public static List<Row> abilities(Genus genus) {
        List<Row> rows = new ArrayList<>();
        for (var ability : genus.abilities()) {
            String id = ability.type().getPath();
            String label = ability.label().isEmpty() ? pretty(id) : ability.label();
            String detail = ability.describe().isEmpty() ? generic(id) : ability.describe();
            if (rows.stream().noneMatch(r -> r.label().equals(label))) {
                rows.add(new Row(label, detail));
            }
        }
        return rows;
    }

    private static String pretty(String id) {
        String t = id.replace('_', ' ');
        return t.isEmpty() ? t : Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    /** Last resort, for an ability with no {@code describe()} of its own. */
    private static String generic(String ability) {
        return switch (ability) {
            case "infect" -> "A bite that takes hold later, whatever finally kills you. Milk cures it.";
            case "convert" -> "What it kills gets back up as one of them.";
            case "break_blocks" -> "Breaks its way through.";
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
        List<Row> abilities = abilities(genus);
        if (!abilities.isEmpty()) {
            out.add(Component.literal(" Abilities").withStyle(ChatFormatting.DARK_GRAY));
            for (Row a : abilities) {
                MutableComponent line = Component.literal("  " + a.label()).withStyle(ChatFormatting.YELLOW);
                if (!a.detail().isEmpty()) {
                    line.append(Component.literal(" - " + a.detail()).withStyle(ChatFormatting.GRAY));
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
