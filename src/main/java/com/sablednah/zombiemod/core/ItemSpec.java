package com.sablednah.zombiemod.core;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.slf4j.Logger;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * An item a genus asks for, described rather than built.
 *
 * <p><b>Why this is not just an {@code ItemStack}.</b> A stack cannot be constructed while a
 * datapack registry is loading: genus files are parsed on a worker thread before item data
 * components are bound, and from Minecraft 26.x that is fatal — <em>"Item minecraft:bow does not
 * have components yet"</em>. So a genus file is read into a *description*, and the stack is built at
 * the moment a mob is equipped, which is long after bootstrap. See {@code docs/MULTIVERSION.md}.
 *
 * <p>It is a better shape regardless of version, and it buys the behaviour below.
 *
 * <p><b>A wrong item line does not cost you the mob.</b> The id is resolved when the mob is
 * equipped, not when the file is read, so a typo is reported and that one slot is skipped while
 * everything else about the genus still spawns. Losing a whole mob — or a whole world load — over a
 * misspelled helmet would tell a datapack author less than seeing the zombie arrive bare-headed with
 * a line in the log saying which slot and which id.
 *
 * <p>Reported <b>once</b> per mistake. Equipment is applied on every single spawn, so a warning per
 * spawn would bury the thing it is trying to tell you.
 *
 * @param id         the item, resolved late
 * @param components enchantments, trims, dyes, custom names — {@code EMPTY} for a bare id
 */
public record ItemSpec(Identifier id, DataComponentPatch components) {

    private static final Logger LOG = LogUtils.getLogger();

    /** Keyed by genus + slot + id, so the same mistake is only ever reported once. */
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    /** The bare form: {@code "minecraft:iron_sword"}. */
    private static final Codec<ItemSpec> BARE =
            Identifier.CODEC.xmap(id -> new ItemSpec(id, DataComponentPatch.EMPTY), ItemSpec::id);

    /** The full form: {@code {"id": "...", "components": {...}}}. */
    private static final Codec<ItemSpec> FULL = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("id").forGetter(ItemSpec::id),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                    .forGetter(ItemSpec::components))
            .apply(i, ItemSpec::new));

    /**
     * Most entries are a bare id, so demanding the object form everywhere would make every genus
     * file noisier for no gain. Both spellings must keep working — every shipped genus and every
     * third-party datapack depends on it.
     */
    public static final Codec<ItemSpec> CODEC = Codec.withAlternative(FULL, BARE);

    /**
     * Build the stack, or report why not and hand back nothing.
     *
     * @param genus which genus asked, so a log line names the file to go and fix
     * @param slot  which slot, for the same reason
     */
    public Optional<ItemStack> stack(String genus, String slot) {
        Optional<Holder.Reference<Item>> item = BuiltInRegistries.ITEM.get(id);
        if (item.isEmpty()) {
            report(genus, slot, "no such item");
            return Optional.empty();
        }
        try {
            return Optional.of(new ItemStack(item.get(), 1, components));
        } catch (RuntimeException e) {
            // A component the item cannot carry, or one built wrong. The mob still spawns.
            report(genus, slot, e.getMessage());
            return Optional.empty();
        }
    }

    private void report(String genus, String slot, String why) {
        if (REPORTED.add(genus + "|" + slot + "|" + id)) {
            LOG.warn("ZombieMod: genus '{}' asks for '{}' in its {} slot, which failed: {}. "
                    + "That slot is left empty and the mob spawns without it.",
                    genus, id, slot.toLowerCase(java.util.Locale.ROOT), why);
        }
    }
}
