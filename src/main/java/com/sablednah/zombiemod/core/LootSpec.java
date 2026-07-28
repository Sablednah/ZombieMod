package com.sablednah.zombiemod.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * What a genus drops.
 *
 * <p>{@code replace} defaults to <b>false</b>, so a genus's drops are added to whatever its base mob
 * already gives. That keeps rotten flesh dropping from zombies — a horde that stops supplying the
 * one thing zombies are farmed for is a surprising thing to inflict on a server by adding a mob
 * type. Set it true for a boss whose drops should be exactly its own.
 *
 * @param table   loot table id, e.g. {@code zombiemod:entities/patient_zero}
 * @param replace drop only this table rather than adding to the base mob's
 */
public record LootSpec(ResourceKey<LootTable> table, boolean replace) {

    private static final Codec<ResourceKey<LootTable>> TABLE_KEY =
            Identifier.CODEC.xmap(id -> ResourceKey.create(Registries.LOOT_TABLE, id), ResourceKey::identifier);

    public static final Codec<LootSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            TABLE_KEY.fieldOf("table").forGetter(LootSpec::table),
            Codec.BOOL.optionalFieldOf("replace", false).forGetter(LootSpec::replace))
            .apply(i, LootSpec::new));
}
