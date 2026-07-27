package com.sablednah.zombiemod;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side settings. Genera themselves are datapack data — this is only the handful of knobs
 * that govern how aggressively the mod takes over.
 */
public final class ZombieModConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue VANILLA_WEIGHT;
    public static final ModConfigSpec.BooleanValue LOG_SPAWNS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("ZombieMod - datapack-defined zombie types.",
                "The zombie types themselves live in datapacks, not here:",
                "  data/<pack>/zombiemod/genus/<name>.json").push("spawning");

        ENABLED = b.comment("Master switch. Off means every mob spawns exactly as vanilla would.")
                .define("enabled", true);

        VANILLA_WEIGHT = b.comment(
                        "How strongly to leave a mob alone, weighed against the genera that could claim it.",
                        "This is an ordinary entry in the same weighted draw as every genus, so if your",
                        "genera total 200 and this is 200, roughly half of all zombies stay vanilla.",
                        "The genera shipped with the mod total around 190 in a typical dark overworld",
                        "spot, so the default of 200 leaves you a little over half plain zombies.",
                        "Raise it for a mostly-vanilla world; lower it for an infested one; set it to 0",
                        "and a genus claims every eligible spawn.")
                .defineInRange("vanillaWeight", 200, 0, Integer.MAX_VALUE);

        LOG_SPAWNS = b.comment("Log every genus spawn to the server console. Noisy; for tuning weights.")
                .define("logSpawns", false);

        b.pop();
        SPEC = b.build();
    }

    private ZombieModConfig() {}
}
