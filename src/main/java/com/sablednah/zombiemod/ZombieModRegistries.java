package com.sablednah.zombiemod;

import com.sablednah.zombiemod.core.Genus;
import com.sablednah.zombiemod.core.SummonRitual;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Registry keys owned by ZombieMod.
 *
 * <p>{@link #GENUS} is a <em>datapack</em> registry, which is the whole delivery mechanism: server
 * owners drop JSON into a datapack and get new zombie types, no code and no restart. Files live at
 * {@code data/<namespace>/zombiemod/genus/<name>.json}.
 */
public final class ZombieModRegistries {

    public static final ResourceKey<Registry<Genus>> GENUS =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(ZombieMod.MOD_ID, "genus"));

    /** Summon rituals: use item X on block Y, get genus Z. Files at {@code data/<pack>/zombiemod/ritual/}. */
    public static final ResourceKey<Registry<SummonRitual>> RITUAL =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(ZombieMod.MOD_ID, "ritual"));

    /**
     * Registered on the mod event bus. The network codec is supplied so genera sync to clients —
     * harmless today (nothing client-side reads them yet) and required the moment the optional
     * client mod wants to know what it is looking at.
     */
    static void register(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(GENUS, Genus.CODEC, Genus.CODEC);
        event.dataPackRegistry(RITUAL, SummonRitual.CODEC, SummonRitual.CODEC);
    }

    private ZombieModRegistries() {}
}
