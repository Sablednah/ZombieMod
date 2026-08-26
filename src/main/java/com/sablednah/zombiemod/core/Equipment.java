package com.sablednah.zombiemod.core;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
        Optional<ItemSpec> mainHand,
        Optional<ItemSpec> offHand,
        Optional<ItemSpec> head,
        Optional<ItemSpec> chest,
        Optional<ItemSpec> legs,
        Optional<ItemSpec> feet,
        float dropChance) {

    public static final Equipment NONE = new Equipment(Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0.0F);

    /**
     * Accepts {@code "minecraft:iron_sword"} or the full
     * {@code {"id": "minecraft:iron_sword", "components": {...}}} — see {@link ItemSpec}, which also
     * explains why a slot holds a description rather than a built stack.
     */
    private static final Codec<ItemSpec> ITEM_CODEC = ItemSpec.CODEC;

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

    /**
     * Slot/stack pairs that were specified <em>and</em> could be built.
     *
     * <p>A slot whose item is wrong is reported once and skipped rather than throwing, so one bad
     * line costs that slot and nothing else — see {@link ItemSpec}.
     *
     * @param genus names the genus in any warning, so the log points at the file to fix
     */
    public void forEach(String genus, java.util.function.BiConsumer<EquipmentSlot, ItemStack> action) {
        build(genus, EquipmentSlot.MAINHAND, mainHand, action);
        build(genus, EquipmentSlot.OFFHAND, offHand, action);
        build(genus, EquipmentSlot.HEAD, head, action);
        build(genus, EquipmentSlot.CHEST, chest, action);
        build(genus, EquipmentSlot.LEGS, legs, action);
        build(genus, EquipmentSlot.FEET, feet, action);
    }

    private static void build(String genus, EquipmentSlot slot, Optional<ItemSpec> spec,
            java.util.function.BiConsumer<EquipmentSlot, ItemStack> action) {
        spec.flatMap(s -> s.stack(genus, slot.name())).ifPresent(stack -> action.accept(slot, stack));
    }
}
