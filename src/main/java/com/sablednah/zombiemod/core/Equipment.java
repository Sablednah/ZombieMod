package com.sablednah.zombiemod.core;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.sablednah.zombiemod.platform.Items;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * What a genus is carrying and wearing.
 *
 * <p>The 1.8 config had {@code handItem} / {@code helmetItem} / … as numeric item ids. This is the
 * same idea with two upgrades: numeric ids are long gone, and each slot accepts either a bare item
 * id or a full stack with components — so a genus can carry an enchanted, named, trimmed or dyed
 * item, not just a plain one.
 *
 * <p>Equipment is the biggest visual lever available to a vanilla client after {@code scale} and a
 * player head: armour is drawn on the mob, held items are drawn in its hands, and none of it needs
 * anything installed.
 *
 * @param mainHand   weapon; also determines melee damage the way it does for any mob
 * @param offHand    shield, torch, banner…
 * @param head       overrides both {@code armor_color} and {@code head} for that slot
 * @param dropChance 0–1, applied to every slot. Defaults to 0: kitting a genus out should not turn
 *                   it into a loot piñata, and a farmable diamond-armour zombie is an economy bug
 */
public record Equipment(
        Optional<ItemStack> mainHand,
        Optional<ItemStack> offHand,
        Optional<ItemStack> head,
        Optional<ItemStack> chest,
        Optional<ItemStack> legs,
        Optional<ItemStack> feet,
        float dropChance) {

    public static final Equipment NONE = new Equipment(Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0.0F);

    /**
     * Accepts {@code "minecraft:iron_sword"} or the full
     * {@code {"id": "minecraft:iron_sword", "components": {...}}}. Most entries are the former, so
     * demanding the object form everywhere would make genus files noisy for no gain.
     */
    private static final Codec<ItemStack> ITEM_CODEC = Items.stackCodec();

    public static final MapCodec<Equipment> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ITEM_CODEC.optionalFieldOf("mainhand").forGetter(Equipment::mainHand),
            ITEM_CODEC.optionalFieldOf("offhand").forGetter(Equipment::offHand),
            ITEM_CODEC.optionalFieldOf("head").forGetter(Equipment::head),
            ITEM_CODEC.optionalFieldOf("chest").forGetter(Equipment::chest),
            ITEM_CODEC.optionalFieldOf("legs").forGetter(Equipment::legs),
            ITEM_CODEC.optionalFieldOf("feet").forGetter(Equipment::feet),
            Codec.FLOAT.optionalFieldOf("drop_chance", 0.0F).forGetter(Equipment::dropChance))
            .apply(i, Equipment::new));

    /** Nested under an {@code "equipment"} key, unlike Appearance which is flattened. */
    public static final Codec<Equipment> CODEC = MAP_CODEC.codec();

    /** Slot/stack pairs that were actually specified. */
    public void forEach(java.util.function.BiConsumer<EquipmentSlot, ItemStack> action) {
        mainHand.ifPresent(s -> action.accept(EquipmentSlot.MAINHAND, s));
        offHand.ifPresent(s -> action.accept(EquipmentSlot.OFFHAND, s));
        head.ifPresent(s -> action.accept(EquipmentSlot.HEAD, s));
        chest.ifPresent(s -> action.accept(EquipmentSlot.CHEST, s));
        legs.ifPresent(s -> action.accept(EquipmentSlot.LEGS, s));
        feet.ifPresent(s -> action.accept(EquipmentSlot.FEET, s));
    }
}
