package com.sablednah.zombiemod.platform;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

/**
 * The vanilla entity types this mod names, looked up from the registry rather than read off
 * {@code EntityType}'s constants.
 *
 * <p><b>Why this exists.</b> Minecraft 26.2 removed those constants: {@code EntityType} carried 159
 * {@code public static final} fields on 26.1 and carries two on 26.2, both of them codecs. So
 * {@code EntityType.ZOMBIE} and its fifteen siblings simply stop existing, and every one of them is
 * a compile error on the newest version. See {@code docs/MULTIVERSION.md} for the measurements.
 *
 * <p><b>Why a registry lookup rather than a per-version shim.</b> Because it is not a workaround —
 * it is the more correct code, and it compiles unchanged on 1.21.11, 26.1 and 26.2 alike. Entity
 * types live in an open registry that datapacks and other mods extend; reaching for a hardcoded
 * field was always the weaker way to name one. This is the pattern CityWorld arrived at for the same
 * problem, and the reason generalises: <em>a seam that is version-agnostic is one the version
 * branches never have to differ over.</em>
 *
 * <p><b>Note this is {@code platform}, not {@code compat}.</b> The {@code compat} package is for
 * <em>other mods</em> — FTB Chunks, CityWorld, Standards — and everything in it is reflective and
 * inert when the other mod is absent. This package is for <em>Minecraft itself</em> moving under us
 * between versions. Different problem, different failure mode: a missing mod is normal and must be
 * survived, whereas a missing vanilla type is a broken build and should be.
 *
 * <p>Methods rather than constants, deliberately. Several of these are read while a codec is being
 * built, which happens during class initialisation, and a static field would pin the lookup to
 * whenever this class first loaded. A method looks it up when asked. The cost is a hash lookup on a
 * frozen registry, against calls that happen once per codec or once per spawn.
 */
public final class Types {

    private Types() {}

    private static EntityType<?> of(String path) {
        return BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(path));
    }

    // The default base for a genus, and the yardstick every spawn check is made against.
    public static EntityType<?> zombie() {
        return of("zombie");
    }

    public static EntityType<?> arrow() {
        return of("arrow");
    }

    public static EntityType<?> lightningBolt() {
        return of("lightning_bolt");
    }

    /** The beam is drawn by GuardianRenderer and nothing else can produce one - see the AT. */
    public static EntityType<?> guardian() {
        return of("guardian");
    }

    public static EntityType<?> elderGuardian() {
        return of("elder_guardian");
    }

    // The undead-counterpart mapping in Convert: what a victim rises as.
    public static EntityType<?> villager() {
        return of("villager");
    }

    public static EntityType<?> wanderingTrader() {
        return of("wandering_trader");
    }

    public static EntityType<?> zombieVillager() {
        return of("zombie_villager");
    }

    public static EntityType<?> piglin() {
        return of("piglin");
    }

    public static EntityType<?> piglinBrute() {
        return of("piglin_brute");
    }

    public static EntityType<?> zombifiedPiglin() {
        return of("zombified_piglin");
    }

    public static EntityType<?> horse() {
        return of("horse");
    }

    public static EntityType<?> donkey() {
        return of("donkey");
    }

    public static EntityType<?> mule() {
        return of("mule");
    }

    public static EntityType<?> zombieHorse() {
        return of("zombie_horse");
    }
}
