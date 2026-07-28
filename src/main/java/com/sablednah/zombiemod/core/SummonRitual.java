package com.sablednah.zombiemod.core;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Right-click block X holding item Y, get a zombie.
 *
 * <p>The cheap half of the Wither pattern, and it covers most of what a summon ritual is for: a
 * deliberate, discoverable, craftable way to call something up. A full multi-block structure is a
 * separate job; this needs no pattern matching and reads as one line of JSON.
 *
 * <p>Lives in its own datapack registry ({@code zombiemod:ritual}) rather than on the genus, because
 * one genus may have several ways to summon it, and because a pack author adding a ritual for
 * someone else's genus shouldn't have to override their file.
 *
 * @param block        which block to use the item on; accepts a tag
 * @param item         what to be holding; accepts a tag
 * @param genus        what to spawn
 * @param consume      take the item (never in creative)
 * @param replaceBlock clear the block, Wither-style
 * @param count        how many to spawn
 */
public record SummonRitual(
        HolderSet<Block> block,
        HolderSet<Item> item,
        ResourceKey<Genus> genus,
        boolean consume,
        boolean replaceBlock,
        int count) {

    private static final Codec<ResourceKey<Genus>> GENUS_KEY = Identifier.CODEC.xmap(
            id -> ResourceKey.create(
                    ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("zombiemod", "genus")), id),
            ResourceKey::identifier);

    public static final Codec<SummonRitual> CODEC = RecordCodecBuilder.create(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("block").forGetter(SummonRitual::block),
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("item").forGetter(SummonRitual::item),
            GENUS_KEY.fieldOf("genus").forGetter(SummonRitual::genus),
            Codec.BOOL.optionalFieldOf("consume", true).forGetter(SummonRitual::consume),
            Codec.BOOL.optionalFieldOf("replace_block", false).forGetter(SummonRitual::replaceBlock),
            Codec.INT.optionalFieldOf("count", 1).forGetter(SummonRitual::count))
            .apply(i, SummonRitual::new));

    public boolean matches(Holder<Block> usedOn, Holder<Item> held) {
        return block.contains(usedOn) && item.contains(held);
    }

    /** Unused today; kept so a future structure variant has an obvious home. */
    public Optional<Identifier> pattern() {
        return Optional.empty();
    }
}
