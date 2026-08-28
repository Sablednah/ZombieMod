# Changelog

All notable changes to the NeoForge rewrite of ZombieMod.

Zombie types are **datapack files**, so `/reload` picks up changes to them without a restart. The
settings in `zombiemod-server.toml` are a **server** config, and it lives at
`config/zombiemod-server.toml`. A copy under `saves/<world>/serverconfig/` overrides it for that
world alone.

## 3.3.0

*2026-08-28.* Zombies that only turn up at certain times of year.

### Added

- **Seasonal genera.** A new `zombiemod:date` spawn condition gates a genus to a range of the real
  calendar, month-day and inclusive, recurring every year. It composes with the other conditions, so
  "late October **and** at night" costs nothing extra.

  Two ship with it:

  - **Jack** (24 Oct – 2 Nov) — a carved pumpkin for a head, a gold outline, trailing flame, and
    **Darkness** on anyone who comes close. Drops pumpkin pie.
  - **Krampus** (18 Dec – 2 Jan) — red and white, and almost harmless: two damage. But he rings, and
    every ring calls everything within 22 blocks to exactly where you are. The danger is what
    arrives, not him. Kill him for a gift — or for coal.

- **`dateOverride` in the server config**, which pretends today is some other day. A seasonal genus
  is invisible for fifty-one weeks of the year, which looks exactly like a broken one; set this to
  `10-31` to see Halloween in June. **`/zombiemod status`** reports the date in force and which
  seasonal genera are in season.

### Notes

- **The date comes from the server, not the player.** Everyone in a session meets the same October,
  whatever timezone they are in.
- **Ranges may cross the new year.** `12-18` to `01-02` is a fortnight over Christmas, not an empty
  set.

## 3.2.0

*2026-08-26.* Minecraft 26.1 and 26.2, and a genus file that gets an item wrong no longer costs you
the world.

### Added

- **Minecraft 26.1.2 and 26.2 are supported**, alongside 1.21.11. Each has its own jar — check the
  `+mc` in the filename — and all three are built from the same source, so a fix reaches every
  version. Both new versions have been played, not merely compiled.
- **An icon in 26.2's mod list.** 26.2 gives each mod a small square beside its name; without one,
  ZombieMod was the only row in the list without an icon.
- **`/zombiemod status` now explains conversions.** A genus that converts what it kills declines for
  six different reasons, and every one of them looks the same from in the world — a mob dies and
  nothing gets up. Status now counts them, so "the Carrier is broken" can be answered with "the
  Carrier is at its nearby cap".

### Fixed

- **A wrong item in a genus file no longer stops the world loading.** Previously a misspelled item id
  was a parse failure, and a malformed genus refuses to load the world — so one typo in one armour
  slot took everything with it. The mistake is now reported once, naming the genus, the item and the
  slot, that slot is left empty, and the mob spawns without it. A bare-headed zombie and a line in
  the log tell a datapack author far more than a world that will not start.
- **The documentation named the wrong config file.** `/zombiemod status`, the changelog and both
  store pages said the settings were per-world in singleplayer and that the copy in `config/` did
  nothing. That is the opposite of how NeoForge works: `config/zombiemod-server.toml` holds the
  settings, and a copy under a world's `serverconfig/` overrides it for that world alone. This sent
  people to edit a file that never existed.
- **Three player-facing messages carried raw formatting codes** — the infection warning, the observer
  notice and the corpse-returned message.
- **On 26.2, opening a ZombieDex entry for a genus holding an item crashed the game.**

### Changed

- **Equipment is read as a description and built when a mob is equipped**, rather than when the genus
  file is parsed. Required by 26.x, where an item cannot be constructed that early — and it is what
  makes a wrong item line survivable.
- **Entity types are looked up in the registry** rather than read from constants, which 26.2 removed.
  It is also more correct for a mod whose genera come from datapacks.

## 3.1.1

*2026-08-25.* The Undertow swims. In 3.1.0 it did not.

### Fixed

- **The Undertow bobbed at the surface instead of swimming.** It shipped with a `float` goal, and
  `FloatGoal` calls `JumpControl.jump()` on every tick it spends in water — which pins a mob to the
  surface and bounces it there. Harmless on the genus the goal list was copied from (the Bogman is
  *amphibious* and walks the bottom, where floating is correct) and completely wrong on a swimmer.

  Its idle wandering was broken too, for a separate reason: `random_stroll` picks destinations on
  land, so a swim navigator was being handed places it could not path to.

### Added

- **A `random_swim` goal type**, which is what `navigation: swim` had been missing. The navigator
  plans a route through water but nothing was choosing anywhere to go — the same shape as `climb`,
  where the spider navigator plans the climb and `ClimbGoal` performs it, and neither works alone.
  `swim` had the planning half only, and no genus had exercised it until the Undertow.

  Datapack authors: give a swimmer `random_swim` rather than `random_stroll`, and **do not give it
  `float`**.

## 3.1.0

*2026-08-24.* A new genus, an economy, and the
command output made readable from a console.

### Added

- **A new genus: the Undertow.** A drowned that swims properly instead of walking along the bottom,
  outlined in dark aqua, trailing ink, wearing the face of Dagon. It blinds you and then drags you under — which in water is a
  different kind of problem from being hit.

  It fills the emptiest part of the roster. Of the fifty-eight genera before it, exactly one used the
  drowned base and one used `pull`, so open water was the place the mod had least to say. It spawns
  ambiently wherever drowned do, at weight 25 — the draw is per base mob, so this changes what you
  meet while swimming and touches nothing on land.

- **Bounties can be paid into a real economy.** If
  [SableCraft Standards](https://github.com/Sablednah/SableCraft-Standards) is installed, ZombieMod
  registers a bounty payer against its economy facade and money lands in players' accounts — no
  configuration, and nothing to install if you do not want it.

  This was the last thing outstanding from the 1.8 plugin, and it was blocked rather than unbuilt:
  NeoForge has no Vault, no abstraction every economy mod implements, so picking one would have
  picked a side on the server owner's behalf. Standards resolves it by keeping the money *behind an
  interface* — a dedicated economy mod registers a higher-priority provider and takes over the
  payments — so paying through Standards is not a vote for Standards' ledger.

  The scoreboard tally still runs alongside it; payers are additive on purpose. `/zombiemod status`
  now reports how many payers answered, which is the first thing to check when a bounty pays nobody.

### Changed

- **The Rusted Warden's shockwave fires at the right rate now.** It was set to a 9-second average
  gap — the slowest of the four genera that have a shockwave, despite being the lightest of them,
  and slower than a Patient Zero at two and a half times its health. It now matches the Tank, which
  is the genus it is balanced against. Its radius, damage and knockup are unchanged: a small blast
  with a hard throw is the Warden's character.

### Fixed

- **Command output was unreadable outside the game.** `/zombiemod status` and `/zombiemod corpse
  list` were built with legacy section codes inside the string, so while the in-game client rendered
  them correctly, everything that is not a client — the server console, the log, and RCON — printed
  the codes as literal text (`§eZombieMod status`). Both commands now build a component tree
  with explicit styles: identical in chat, and a clean sentence everywhere else.

  It matters most on exactly the output written to be read out-of-game. `status` is an admin's
  command, its config-path line exists to be **copied**, and `corpse list` is read on a console
  before deciding whether to re-issue a dead player's inventory — where the note explaining that the
  items went into lava was the part wrapped in codes.

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
