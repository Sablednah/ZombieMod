# Changelog

All notable changes to the NeoForge rewrite of ZombieMod.

Zombie types are **datapack files**, so `/reload` picks up changes to them without a restart. The
settings in `zombiemod-server.toml` are a **server** config — per-world in singleplayer, at
`saves/<world>/serverconfig/`, not `config/`.

## 3.0.0

The first release of the NeoForge rewrite. The 2013 Bukkit plugin was version 2.x; this is a complete
rewrite sharing no code with it, so it starts at 3.0.0.

### The idea

A **genus** is a set of changes applied to an ordinary vanilla mob at the moment it spawns — health,
size, colour, face, equipment, and above all **AI**, assembled from vanilla's own pathfinder goals.
It is written as one JSON file in a datapack. ZombieMod registers **no entity types of its own**,
which is what lets an unmodified client see all of it.

### Added

- **58 genera**, drawn from the genre canon — speed horror, Romero shamblers, special infected,
  fungal, elemental, the 1.8 plugin's own set, and the people a village leaves behind. Full list in
  `docs/ROSTER.md`.
- **AI from JSON** — 11 goal types, recombined per genus. A Coward is `avoid_entity`; Herobrine is
  `look_at` at 64 blocks with no attack goal at all.
- **21 abilities** — effects, healing, lightning, explosions, creeper-style fuses, shockwaves, leaps,
  drags, summons, block-breaking, projectiles, cobwebs, infection, conversion, a real guardian beam,
  particle hitscan rays, adaptive resistance and teleport.
- **14 spawn conditions** — biome, dimension, height, light, sky, depth below the local surface, time
  of day, moon phase, land claims and CityWorld districts, composable with `any_of` and `not`.
- **Bosses** — boss bars, health phases, loot tables, and summon rituals built from block patterns.
- **Hordes** — four wave events with a director and a boss bar. Ring a bell and survivors glow; a
  horde that stalls lights them up itself, because hunting one straggler across a dark forest is a
  problem Minecraft has never solved well.
- **Infection** — a bite marks you, and dying within the minute to *anything* raises you. It spreads
  through livestock on its own. Milk cures it, for you and for the cow.
- **Conversion** — what a Carrier kills gets up as one of them, a villager rising as a zombie villager
  keeping its name and armour.
- **Mutation** — a genus becomes another on a trigger: wounded, on fire, in water, in the wrong
  dimension.
- **Player corpses** — your corpse gets up wearing your real skin and carrying your things, with an
  admin recovery ledger for when it lands in lava. Off by default.
- **ZombieDex** — a per-player bestiary in chat, as a written book that works on a vanilla client, on
  scoreboards, and on a full screen for players who do install the mod.
- **Player-head faces** on 51 of the 58, rendered on an unmodified client.
- **Proximity spawning** — zombies placed just out of sight around each player, ignoring vanilla's
  spawn table. Off by default.
- **Bounties**, with a scoreboard fallback where there is no economy mod.
- **FTB Chunks** and **CityWorld** integrations, both reflective and both completely inert when the
  other mod is absent.
- **Commands** — `list`, `spawn`, `status`, `observe`, `corpse`, `horde`, `bestiary`, `config`, and
  `/zm` for all of it.

### Notes

- **Your players do not need this installed.** Verified twice against a genuinely unmodified client
  from the Mojang launcher, joining a dedicated server carrying the mod.
- **A land claim is not a safe zone.** `inClaims` governs *spawning*; mobs spawn outside a claim, walk
  in, and aggro normally.
- **A malformed genus file stops the world loading** rather than being skipped — standard
  datapack-registry behaviour, harsher here because these files are hand-written. The log names the
  file and the field.
- Proximity spawning and hordes are **off by default**: each adds mobs beyond what vanilla would have
  made, and installing a mob pack should not silently change how many things are hunting you.
