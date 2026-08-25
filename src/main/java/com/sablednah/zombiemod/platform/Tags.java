package com.sablednah.zombiemod.platform;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * Asking whether an entity type is in a tag.
 *
 * <p><b>Why this exists.</b> {@code EntityType.is(TagKey)} is gone on 26.2; the type's registry
 * holder answers instead. Two call sites, both asking the same question — <em>is this thing already
 * undead?</em> — which is the guard that stops the conversion and infection systems raising a zombie
 * from a zombie.
 */
public final class Tags {

    private Tags() {}

    /**
     * <b>Differs per version.</b> On 26.2 this becomes
     * {@code type.builtInRegistryHolder().is(tag)}.
     */
    public static boolean is(EntityType<?> type, TagKey<EntityType<?>> tag) {
        return type.is(tag);
    }
}
