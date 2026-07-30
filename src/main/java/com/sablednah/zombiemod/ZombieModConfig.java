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
    public static final ModConfigSpec.BooleanValue PLAYER_ZOMBIES;
    public static final ModConfigSpec.BooleanValue PLAYER_ZOMBIE_TAKES_ITEMS;
    public static final ModConfigSpec.ConfigValue<String> PLAYER_ZOMBIE_GENUS;
    public static final ModConfigSpec.ConfigValue<String> PLAYER_ZOMBIE_NAME;
    public static final ModConfigSpec.BooleanValue PLAYER_ZOMBIE_INFECTED_TOO;
    public static final ModConfigSpec.BooleanValue CLAIM_PROTECTION;
    public static final ModConfigSpec.BooleanValue CLAIM_NO_GRIEFING;
    public static final ModConfigSpec.EnumValue<ClaimSpawns> CLAIM_SPAWNS;
    public static final ModConfigSpec.BooleanValue PROXIMITY;
    public static final ModConfigSpec.IntValue PROXIMITY_INTERVAL;
    public static final ModConfigSpec.DoubleValue PROXIMITY_CHANCE;
    public static final ModConfigSpec.IntValue PROXIMITY_MIN;
    public static final ModConfigSpec.IntValue PROXIMITY_MAX;
    public static final ModConfigSpec.IntValue PROXIMITY_CAP;
    public static final ModConfigSpec.BooleanValue PROXIMITY_UNSEEN;
    public static final ModConfigSpec.BooleanValue BOUNTY;
    public static final ModConfigSpec.ConfigValue<String> BOUNTY_OBJECTIVE;
    public static final ModConfigSpec.BooleanValue BOUNTY_ANNOUNCE;
    public static final ModConfigSpec.BooleanValue HORDES;
    public static final ModConfigSpec.IntValue HORDE_CHECK;
    public static final ModConfigSpec.DoubleValue HORDE_CHANCE;
    public static final ModConfigSpec.IntValue HORDE_COOLDOWN;
    public static final ModConfigSpec.IntValue HORDE_CAP;
    public static final ModConfigSpec.BooleanValue HORDE_BELL;
    public static final ModConfigSpec.DoubleValue HORDE_BELL_RADIUS;
    public static final ModConfigSpec.IntValue HORDE_GLOW_STALL;
    public static final ModConfigSpec.IntValue HORDE_GLOW_DURATION;

    /** How spawns inside a land claim are treated. */
    public enum ClaimSpawns {
        ALLOW, VANILLA_ONLY, NO_SPAWNS
    }

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

        b.comment("Player zombies: when a player dies, their corpse gets up.",
                "Off by default - this takes a player's dropped items and puts them inside a mob,",
                "which is a real gameplay decision and not one to make on someone's behalf just",
                "because they installed a mob pack.").push("playerZombies");

        PLAYER_ZOMBIES = b.comment("Raise a zombie wearing the dead player's skin at their death spot.")
                .define("enabled", false);

        PLAYER_ZOMBIE_TAKES_ITEMS = b.comment(
                        "The corpse carries what the player dropped; kill it to get your things back.",
                        "With this off the items drop normally and the corpse is only a monument.")
                .define("takeItems", true);

        PLAYER_ZOMBIE_GENUS = b.comment("Which genus a corpse uses as its template.")
                .define("genus", "zombiemod:player_zombie");

        PLAYER_ZOMBIE_NAME = b.comment("Corpse name. %P is the player's name.")
                .define("name", "Corpse %P");

        PLAYER_ZOMBIE_INFECTED_TOO = b.comment(
                        "A player who dies infected raises BOTH: the corpse, wearing their face and",
                        "carrying their belongings, and a second zombie from the infection itself.",
                        "",
                        "Only the corpse has the loot, so the pair works as a decoy - killing the",
                        "wrong one gets you nothing. Set false if you would rather one death meant",
                        "one zombie.")
                .define("infectionAlsoRaises", true);

        b.pop();

        b.comment("Land claims (FTB Chunks). All of this is inert if FTB Chunks is not installed.",
                "",
                "Worth knowing why this exists: FTB Chunks protects explosions inside claims but does",
                "not cover general mob block-breaking, so asking NeoForge's griefing hook gets no",
                "answer from it and a claim does nothing against a Breaker. ZombieMod closes that",
                "from its own side.").push("claims");

        CLAIM_PROTECTION = b.comment("Respect FTB Chunks claims at all.")
                .define("respectClaims", true);

        CLAIM_NO_GRIEFING = b.comment("Stop ZombieMod's mobs breaking or placing blocks inside claims.")
                .define("noGriefingInClaims", true);

        CLAIM_SPAWNS = b.comment("What happens to spawns inside a claim:",
                        "  ALLOW        - no special treatment",
                        "  VANILLA_ONLY - genera never claim a spawn there, so you get ordinary mobs",
                        "  NO_SPAWNS    - cancel the spawn entirely",
                        "VANILLA_ONLY is the default: keeping ZombieMod out of someone's base is this",
                        "mod's business, while emptying it of vanilla mobs is not.")
                .defineEnum("inClaims", ClaimSpawns.VANILLA_ONLY);

        b.pop();

        b.comment("Proximity spawning: put zombies just out of sight around each player, instead of",
                "only riding vanilla's spawn table.",
                "",
                "This is the 1.8 plugin's ProximitySystems, and it is what made that world feel",
                "occupied rather than merely populated. It is also the one feature here that adds mobs",
                "beyond what vanilla would have made, so it is OFF by default - installing a mob pack",
                "should not silently change how many things are hunting you.").push("proximity");

        PROXIMITY = b.comment("Spawn genera near players regardless of vanilla's spawn table.")
                .define("enabled", false);

        PROXIMITY_INTERVAL = b.comment("Ticks between attempts, per player. 100 is five seconds.")
                .defineInRange("interval", 100, 20, 12000);

        PROXIMITY_CHANCE = b.comment("Chance an attempt spawns anything at all.")
                .defineInRange("chance", 0.5D, 0.0D, 1.0D);

        PROXIMITY_MIN = b.comment("Closest it will spawn to a player.")
                .defineInRange("minDistance", 16, 4, 128);

        PROXIMITY_MAX = b.comment("Furthest it will spawn from a player.")
                .defineInRange("maxDistance", 32, 8, 128);

        PROXIMITY_CAP = b.comment("Stop once this many ZombieMod mobs are already near the player.",
                        "The one number that decides whether this is atmosphere or a siege.")
                .defineInRange("nearbyCap", 8, 1, 200);

        PROXIMITY_UNSEEN = b.comment("Only spawn where the player cannot currently see the spot.",
                        "The point is that they were always there, not that they appeared.")
                .define("outOfSightOnly", true);

        b.pop();

        b.comment("Bounties: what killing a genus is worth.",
                "",
                "A genus carries a number; who pays it is a separate question. There is no Vault on",
                "NeoForge - no abstraction every economy mod implements - so ZombieMod does not pick",
                "one for you. An economy adapter registers itself and gets called.",
                "",
                "With no economy mod at all the scoreboard is the fallback, which is a real reward on",
                "a vanilla server rather than a number waiting for a dependency. Opt in with:",
                "  /scoreboard objectives add zombiemod.bounty dummy",
                "  /scoreboard objectives setdisplay sidebar zombiemod.bounty").push("bounty");

        BOUNTY = b.comment("Pay out bounties at all.").define("enabled", true);

        BOUNTY_OBJECTIVE = b.comment(
                        "Scoreboard objective to tally into. Only used if it already exists, so",
                        "nothing appears on a server that never asked for it. Blank to disable.")
                .define("objective", "zombiemod.bounty");

        BOUNTY_ANNOUNCE = b.comment("Show the payout on the killer's action bar.")
                .define("announce", true);

        b.pop();

        b.comment("Horde events: nights when several of them arrive together.",
                "",
                "Everything else in this mod is an encounter - one monster, met on its own terms. A",
                "horde is the layer above, and it is waves rather than a number on purpose: twenty at",
                "once is a wall, while eight then twelve then twenty is a story with a middle.",
                "",
                "Off by default, for the same reason as proximity spawning.",
                "Start one by hand with /zombiemod horde start.").push("hordes");

        HORDES = b.comment("Let hordes start on their own.").define("enabled", false);

        HORDE_CHECK = b.comment("Ticks between checks, per player.")
                .defineInRange("checkInterval", 600, 100, 24000);

        HORDE_CHANCE = b.comment("Chance a check starts one.")
                .defineInRange("chance", 0.08D, 0.0D, 1.0D);

        HORDE_COOLDOWN = b.comment("Minimum ticks between hordes for the same player.",
                        "24000 is a full day. Rarity is most of what makes one memorable.")
                .defineInRange("cooldown", 36000, 0, 1000000);

        HORDE_CAP = b.comment("Never have more than this many horde mobs alive at once.",
                        "The safety rail: a wave that cannot place its full count simply places fewer.")
                .defineInRange("cap", 40, 4, 300);

        HORDE_BELL = b.comment("Ringing a bell makes nearby horde mobs glow.",
                        "Vanilla does exactly this for raids, so the gesture is already learned - but its",
                        "version is hard-gated on the #minecraft:raiders entity tag and every method in the",
                        "path is private, so this is our own implementation of the same idea.")
                .define("bellGlow", true);

        HORDE_BELL_RADIUS = b.comment("How far a bell reaches. 48 is what vanilla uses for raiders.")
                .defineInRange("bellRadius", 48.0D, 8.0D, 256.0D);

        HORDE_GLOW_STALL = b.comment("Once the last wave is out, make survivors glow after this many",
                        "ticks without a kill. 1200 is a minute of finding nothing.",
                        "",
                        "Deliberately measured from the last kill rather than from the start: a long fight",
                        "you are winning is not the problem. Hunting one straggler across a dark forest is,",
                        "and it is a problem Minecraft has never solved well. 0 disables it.")
                .defineInRange("glowAfter", 1200, 0, 24000);

        HORDE_GLOW_DURATION = b.comment("How long the glow lasts, in ticks. Refreshed while stalled.")
                .defineInRange("glowDuration", 200, 20, 6000);

        b.pop();
        SPEC = b.build();
    }

    private ZombieModConfig() {}
}
