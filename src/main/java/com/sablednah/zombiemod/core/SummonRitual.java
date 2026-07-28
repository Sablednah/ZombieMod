package com.sablednah.zombiemod.core;

import java.util.List;

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
        int count,
        List<PatternBlock> pattern) {

    /**
     * One block the structure must contain, positioned relative to the block you activate.
     *
     * <p>Offsets rather than vanilla's {@code BlockPattern} aisles: the aisle format is fiddly to
     * author and worse to read in JSON, and a list of "there must be soul sand here" is obvious at a
     * glance. The cost is that we do the rotation ourselves — see {@code RitualHandler}, which tries
     * all four horizontal orientations so a player can build the shape facing any way.
     *
     * @param offset [x, y, z] from the activated block
     * @param block  what must be there; accepts a tag
     */
    public record PatternBlock(List<Integer> offset, HolderSet<Block> block) {

        public static final Codec<PatternBlock> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.listOf(3, 3).fieldOf("offset").forGetter(PatternBlock::offset),
                RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("block").forGetter(PatternBlock::block))
                .apply(i, PatternBlock::new));

        public int x() {
            return offset.get(0);
        }

        public int y() {
            return offset.get(1);
        }

        public int z() {
            return offset.get(2);
        }
    }

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
            Codec.INT.optionalFieldOf("count", 1).forGetter(SummonRitual::count),
            PatternBlock.CODEC.listOf().optionalFieldOf("pattern", List.of()).forGetter(SummonRitual::pattern))
            .apply(i, SummonRitual::new));

    public boolean matches(Holder<Block> usedOn, Holder<Item> held) {
        return block.contains(usedOn) && item.contains(held);
    }


}
