package com.sablednah.zombiemod.platform;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Declaring a chunk of world save data.
 *
 * <p><b>Why this exists.</b> {@code SavedDataType}'s name parameter became an {@code Identifier}
 * rather than a {@code String} in 26.1, and the several convenience constructors collapsed into one.
 * Three call sites — the corpse ledger, the bestiary, the known-players list — and all three store
 * things a player would be upset to lose, so this is a seam worth being careful in.
 *
 * <p>The name is kept as a plain string here and namespaced at the boundary, so callers go on
 * reading the way they did and the version branch decides how a name is spelled.
 *
 * <p>{@code dataFix} is passed through and is {@code null} at every call site today: none of this
 * data has needed a datafixer yet. It stays in the signature rather than being dropped, because the
 * day one of these formats changes shape is the day it is wanted, and a seam that quietly removed
 * the parameter would hide that option.
 */
public final class Saves {

    private Saves() {}

    /**
     * A saved-data type keyed by {@code name}, which must stay stable across versions — it is the
     * file the data lives in, and changing it silently orphans every existing world's copy.
     *
     * <p><b>Differs per version.</b> On 26.1+ the first argument becomes
     * {@code Identifier.fromNamespaceAndPath("zombiemod", name)} rather than the bare string.
     */
    public static <T extends SavedData> SavedDataType<T> of(String name, Supplier<T> factory,
            Codec<T> codec, net.minecraft.util.datafix.DataFixTypes dataFix) {
        return new SavedDataType<>(name, factory, codec, dataFix);
    }
}
