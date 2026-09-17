# Changelog

All notable changes to the NeoForge rewrite of ZombieMod.

Zombie types are **datapack files**, so `/reload` picks up changes to them without a restart. The
settings in `zombiemod-server.toml` are a **server** config, and it lives at
`config/zombiemod-server.toml`. A copy under `saves/<world>/serverconfig/` overrides it for that
world alone.

## Unreleased

### Added

- **Advancements.** ZombieMod has its own tab in the advancements screen, 25 of them: filling the
  ZombieDex by meeting and by killing, the three bosses and *Apex Predator* for all of them, and the
  niche ones — *Bomb Disposal* for killing a Boomer or Bloater after its fuse is lit and before it
  goes off, *It Was Bait* for running down a Coward, *Pull Yourself Together* for killing your own
  corpse, *Got Milk?*, *Last One Standing*, two seasonal ones, and a few that stay hidden until you
  earn them. **They are plain vanilla advancements**, so they work on an unmodded client and show up
  in Better Advancements. The tab appears when you meet your first genus, and progress already in
  your ZombieDex is caught up at login. Datapacks can add their own for any genus with no code — see
  *Advancements* in the README. `advancements.enabled = false` switches the lot off.

- **Works with the Corpse mod.** If [Corpse](https://modrinth.com/mod/corpse) is installed alongside
  player zombies, the two become stages of one death. Your body gets up and walks off with your
  things, so Corpse no longer leaves an empty body lying at the death spot; and when somebody kills
  the zombie it goes down as a Corpse body holding everything it carried, rather than scattering
  loose items. Armour and shield go back in their slots, so Corpse's transfer button re-equips them.
  A body does not despawn after five minutes, cannot be emptied by a hopper, honours Corpse's
  `only_owner` setting, and floats in lava — so a corpse killed in lava is no longer a lost
  inventory. On by default and does nothing without Corpse; `playerZombies.corpseMod = false`
  switches it off. `/zombiemod status` shows the link and counts bodies laid.

### Fixed

- **Corpse and player zombies used to collide.** With both installed, Corpse left an empty body
  wearing your armour where you died while the zombie carried the real items away — which looked
  like a body you could loot and was not. Covered by the change above.

## 3.5.1

*2026-09-14.* Nobody on the other end: another mod's automation can no longer crash the server
through ZombieMod.

### Fixed

- **A fake player could crash the server.** Mods that act through a fake player — mob grinders,
  deployers, automated weapons — hand the game a player with no real network connection behind it.
  When one of them killed a genus, ZombieMod went to update that "player's" ZombieDex, asked the
  missing connection whether it could receive the update, and the question itself threw. Fake
  players are now recognised and simply skipped, since there is no screen on the other end to
  update. Real players, modded or vanilla clients, are unaffected. Reported by the Chronicler, whose
  own mod had the same fault.

## 3.5.0

*2026-09-14.* Handing over the keys: staff commands can be granted without op, and the Undertow
stays in the water.

### Added

- **Permission nodes.** Six of them, through NeoForge's own permission API, so LuckPerms and
  [SableCraft Standards](https://github.com/Sablednah/SableCraft-Standards) can both grant them:
  `zombiemod.spawn`, `zombiemod.horde`, `zombiemod.corpse`, `zombiemod.observe`, `zombiemod.status`
  and `zombiemod.config`. **Every one defaults to the op level the command always needed**, so a
  server with no permissions mod — or one that grants nothing — behaves exactly as before. What they
  add is delegation: a storyteller can now be given hordes and spawning for their sessions without
  being made an op, which would hand them `/stop` as well. The console and command blocks pass on op
  level as they always did. [`NODES.md`](NODES.md) is the full statement.

- **A `zombiemod:in_water` spawn condition.** `value` defaults to `true`; `false` means dry land.

### Fixed

- **The Undertow turned up on dry land.** A drowned base is not a spawn rule: vanilla only puts a
  drowned in water, but proximity spawning picks a patch of ground before it picks a genus, and a
  glowing, ink-trailing thing that drags people under was appearing in fields with nothing to drag
  anyone into. It now carries `in_water`, so it only ever spawns in the sea, a river, or an
  underground lake — every spawn path, not only vanilla's. A drowned that has already chased you
  onto the beach is still a drowned, and follows you out exactly as vanilla's do.

## 3.4.1

*2026-09-11.* Room to breathe: zombies leave again instead of piling up, a boss lets you walk
away, and nothing reacts to a player who isn't there.

### Fixed

- **A boss could crash the server.** If a player moved out of range of a boss's health bar while
  the boss was still alive, the server stopped with a *"Ticking entity"* crash naming
  `BossBars.update`. It went unnoticed because it never happens during a fight: you stay near a boss
  until one of you is dead. It took a flight, a mass teleport and a Borg Queen dropped far below the
  player to find it. Running, flying or teleporting away from a living boss would all have done the
  same, as would the boss falling or being knocked far away.

  Fixed, and a boss bar that fails for any other reason now logs once and lets the fight carry on
  without its bar, instead of taking the world down with it.

- **Zombies piled up without limit, and the server slowed to a crawl.** One test world held over two
  thousand when it was finally cleared. The mod's own spawning was capped; vanilla's was being
  defeated.

  Every ZombieMod zombie was marked *persistent* the moment it spawned, so it would never despawn.
  But vanilla's mob cap **does not count persistent mobs** — it deliberately ignores them, because a
  persistent mob is normally one a player has chosen to keep. So each zombie that became one of ours
  vanished from the cap's count, vanilla saw room and spawned a replacement, that one became one of
  ours as well, and none of them ever left. The world filled indefinitely.

  They now despawn the way vanilla zombies do, and count toward the cap like vanilla zombies do.
  **Still kept, deliberately:** player corpses, which carry someone's inventory; bosses; and a horde's
  zombies while the horde is still running, since one despawning would end it early. Name-tag one to
  keep it, exactly as in vanilla.

  **Upgrading a world that already has them.** The old flag is saved into each zombie, so the ones
  already out there stay until you clear them. Run these once as an op — they remove persistent mobs
  of the five kinds ZombieMod zombies are built on:

  ```
  /kill @e[type=minecraft:zombie,nbt={PersistenceRequired:1b}]
  /kill @e[type=minecraft:husk,nbt={PersistenceRequired:1b}]
  /kill @e[type=minecraft:drowned,nbt={PersistenceRequired:1b}]
  /kill @e[type=minecraft:zombie_villager,nbt={PersistenceRequired:1b}]
  /kill @e[type=minecraft:skeleton,nbt={PersistenceRequired:1b}]
  ```

  A line that answers **"No entity was found"** is not an error — there were simply none of that
  kind to clear. On a test world, zombies, drowned and skeletons all had some; husks and zombie
  villagers had none.

  Things to know before you do:

  - **It only reaches loaded chunks.** Zombies in parts of the world nobody is near are untouched,
    and will be back when those areas load. Run it again somewhere else, or ask players to.
  - **It also catches player corpses.** A corpse killed this way drops what it was carrying where it
    stands, rather than losing it — but that may be somewhere nobody is. Check
    `/zombiemod corpse list` first; anything outstanding can be re-issued from the ledger.
  - **It catches anything of those types that a player name-tagged**, and any boss a player summoned.
    If your server has pets like that, add `distance=..128` and run it somewhere they are not.

- **Zombies no longer react to vanished players.** A Boomer was detonating beside staff who had
  vanished through [SableCraft Standards](https://github.com/Sablednah/SableCraft-Standards) — and a
  crater with no visible cause gives a hidden player away as completely as being seen would.

  Standards already stops mobs *targeting* someone vanished, which is why this only showed up on a
  few abilities. The Boomer's fuse never consults a target: it asks who is standing within its
  trigger radius, and a vanished player was answering. Every ability that sweeps an area — the fuse,
  and anything aimed at `nearby_players` — now treats a vanished player the way it already treated
  spectators and creative-mode players: present in the world, but not someone to react to.

  **Proximity spawning and hordes do the same.** Neither will pick a vanished player: no crowd
  quietly accumulates around staff standing in an empty field, and no horde starts on somebody
  nobody can see. A horde already running when its player vanishes ends the way it does when they
  log out — the bar comes down and nothing more is sent, while whatever had already spawned stays
  in the world. Vanishing walks away from a fight rather than rewinding it.

  Nothing to configure, and nothing changes on a server without Standards installed.

### Added

- **Every jar says which build it is, and the mod says so at startup.** A version number answers
  "which release"; during development that is a different question from "which bytes", and it is a
  sharper one here than in most mods because a release ships three jars that differ only in a `+mc`
  suffix. The startup line and `/zombiemod status` now both read like this:

  ```
  ZombieMod ReForged 3.4.0+mc1.21.11 (build 1946c37a on master, 2026-09-10T07:49:34Z)
  ```

  A `-dirty` suffix on the commit means that jar was built from uncommitted changes. The same four
  values are on the jar manifest as `Build-Commit`, `Build-Branch` and `Build-Time`, so a jar can be
  identified from a shell without loading it:

  ```bash
  unzip -p zombiemod-3.4.0+mc1.21.11.jar META-INF/MANIFEST.MF | grep Build
  ```

  **What it is for is bug reports.** A stamp inside a jar says what is on disk; the startup line
  says what actually *ran*, which is the question a report needs answered and the one that could not
  be answered before. The format is shared with the other SableCraft mods, so a server owner running
  several of them reads the same line from each.

  The stamp can never stop the mod loading: a missing or corrupt one degrades to `unknown`, and the
  build tolerates git being absent, as in a source zip.

### Changed

- **The ZombieDex opens with Z, not J.** J is JourneyMap's full-screen map, and a mod that common
  should not have to be rebound to make room for a zombie pack. Vanilla binds nothing to Z.

  **If you already had ZombieMod installed, you will still be on J.** Minecraft saves every key
  binding, defaults included, so an existing install keeps what it had. Change it under
  *Options → Controls → Key Binds → Open ZombieDex*.

## 3.4.0

*2026-08-30.* Zombies respect whoever owns the land, and nobody gets stuck invulnerable.

### Fixed

- **A player switched to observer mode and then deopped was stranded.** The only command that could
  switch it off needed the permission they had just lost — invulnerable, unable to fix it, and unable
  to ask an op to fix it either, because the command only acted on whoever typed it.

  **`/zombiemod observe off` now needs no permission at all.** Turning your own invulnerability off is
  giving something up, not taking something. Ops can also switch it off for someone else with
  `/zombiemod observe off <player>`, and the player is told when they do. Turning it *on* still needs
  op, for yourself or anyone else.

- **The mod refused to install on perfectly good NeoForge builds.** It demanded the exact build it was
  compiled against or newer, so a server one patch behind was locked out over a change the mod does
  not touch — while simultaneously claiming to work on far-future NeoForge versions that will not be
  compatible at all. Each jar now accepts any NeoForge on the line it was built for, and nothing off
  it. The 1.21.11 jar also accepts future 1.21.x releases rather than that one exact version.

### Added

- **Griefing zombies respect any land-claim mod, not just FTB Chunks.** ZombieMod asked FTB directly,
  which quietly made it the only claims mod that could protect anything — a server running
  [SableCraft Standards](https://github.com/Sablednah/SableCraft-Standards) with a faction mod behind
  it got no protection, and nothing said so. Both are now asked, and either answering "claimed" is
  enough. `/zombiemod status` names which provider answered.

- **Being blinded can count as being in combat**, through Standards, so a player cannot teleport out
  of a fight they cannot see. A Jack does no damage at all, so nothing else on a server has any reason
  to think anything is happening.

  On by default, and it costs nothing unless you have asked for it: blindness is reported as **PvE**
  combat, and Standards ships with PvE combat not blocking teleports. It only does anything on a
  server that has already decided a fight with the world stops you leaving. Turn `blindnessIsCombat`
  off to keep "being hit stops you leaving" while dropping "standing in the dark stops you leaving".

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
