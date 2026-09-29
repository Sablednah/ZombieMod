package com.sablednah.zombiemod.platform;

import com.mojang.serialization.Codec;

import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.ResourceKey;

/**
 * Codecs for "a set of registry entries" - {@code "minecraft:stone"}, {@code "#minecraft:logs"} or a
 * list of either - which genus, ritual and ability files use for blocks, items, biomes and victims.
 *
 * <p><b>Why this exists.</b> {@code net.minecraft.core.RegistryCodecs.homogeneousList} moved to
 * {@code net.minecraft.core.registries.codec.RegistryCodecs.holderSet} on 26.3. Seven call sites across
 * five files, all of them in shared code that must stay identical on every branch.
 */
public final class Codecs {

    private Codecs() {}

    /**
     * A holder set of {@code registry}'s entries, by id, tag or list.
     *
     * <p><b>Differs per version.</b> On 26.3 this is
     * {@code net.minecraft.core.registries.codec.RegistryCodecs.holderSet(registry)}.
     */
    public static <E> Codec<HolderSet<E>> holderSet(ResourceKey<? extends Registry<E>> registry) {
        return RegistryCodecs.homogeneousList(registry);
    }
}
