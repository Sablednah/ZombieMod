# What's left from the 1.8 plugin

An inventory of the original against the port. Kept honest — "covered" means the behaviour is
reachable, not that the code was translated.

## Abilities

| 1.8 | Status | Now |
|-----|--------|-----|
| `BACKSTAB` | ✅ | `teleport` with `mode: behind` — reads the victim's look and lands 180° from it |
| `BREEDER` | ✅ | `summon`, with the `max_nearby` cap the original lacked |
| `EXPLODE` | ✅ | `explode`, plus `fuse` for the creeper-style wind-up |
| `HEAL` | ✅ | `heal` |
| `HEROBRINE` | ✅ | `look_at` at range + `teleport`; see the Herobrine genus |
| `HUNTER` | ✅ | `nearest_target` on `wolf`/`cat`; see the Hunter genus |
| `INK` | ✅ | `particles` with `squid_ink` |
| `LIGHTNING` | ✅ | `lightning` |
| `SHOCKWAVE` / `STOMP` | ✅ | `shockwave` |
| `SPIDER` | ✅ | `navigation: climb` |
| `GHOST` | ✅ | `ghost: true`. Wears their face as well as their name now, which the original couldn't without Spout. Needs a record of who has played here, since the profile cache answers questions but won't enumerate — hence `KnownPlayers`. |
| `BORG` | ✅ | `adapt` — learns damage types rather than materials, and remembers across a restart. See Zomborg. |
| `WEB` | ✅ | `place_block`. See Weaver. |
| `INFEST` | ✅ | `break_blocks` with `infest: true`. See Infester. |
| `LAZER` | ✅ | `projectile`, which unlike the original actually lets a genus pick what it fires. *(In the original both branches of the `LAZER` check called the same arrow goal, so it never differed from an archer.)* |

## Systems

| 1.8 | Status | Notes |
|-----|--------|-------|
| Genus files, weighted spawning | ✅ | Datapack registry, plus spawn conditions the original never had |
| Player corpses | ✅ | And they survive a restart now, plus a recovery ledger |
| Skins | ✅ | Player heads on a vanilla client, where the original needed Spout |
| Giants | ✅ | `base: minecraft:giant`, or just `scale` |
| Equipment and drop rates | ✅ | `equipment` + loot tables |
| Sounds, particles, potions | ✅ | `sound`, `particles`, `effect` |
| `agro`, `coward`, `passive`, `noBurn` | ✅ | `follow_range`, goals, `burning_time` |
| **Block breaking** | ✅ | `break_blocks`, gated on the mob being stuck, an explicit `allowed` list, and NeoForge's `canEntityGrief` hook so claim mods can veto. See Breaker and Big Breaker. |
| **Proximity spawning** | ❌ | `ProximitySystems`: spawned zombies just out of sight around each player, ignoring vanilla's rules, scaled by distance from world spawn. Different in feel from riding vanilla's spawn table — it's what made the world feel occupied. |
| **Jockeys / mounts** | ✅ | A `mount` field taking an entity id. See Outrider. |
| Angry iron golems | ⚠️ | The original registered a hostile `AngryGolem`. A genus with `base: minecraft:iron_golem` gets most of the way there — untested. |
| XP and bounty on kill | ✅ | Both shipped. `xp` is vanilla; `bounty` is a number on the genus with a pluggable payer, falling back to a scoreboard objective so it works with no economy mod at all. |
| DaySpawner | — | Already dead in the original: commented out at its scheduling site. |
| BeardStat, Spout, LegendQuest, Factions | — | Gone. FTB Chunks/Teams and CityWorld are the modern replacements, planned as spawn conditions. |

## What's actually left

**Every ability from the original is covered.** What remains:

- **Proximity spawning** — `ProximitySystems` put zombies just out of sight around each player,
  ignoring vanilla's rules and scaling with distance from world spawn. "There are more of them than
  there should be, and they're already close" is a different feeling from riding the vanilla spawn
  table, and it's the last real gap in how the mod *feels*.
- **Angry iron golems** — probably just `base: minecraft:iron_golem` on a genus. Untested.
- **Nothing else.** Bounty was the last, and it shipped without committing to an economy mod.

Live status, including what's built but unverified, is in [STATUS.md](STATUS.md).

## Things the port does that the original didn't

Worth recording, because "port" undersells some of it:

| | |
|---|---|
| Spawn conditions | biome, dimension, height, light, time, sky, claims — the original had none |
| Behaviours | the same genus docile by day and hunting at night |
| Bosses | boss bars, health phases, loot tables, summon rituals with block patterns |
| Corpse recovery | a durable ledger, so a lost player zombie is recoverable rather than gone |
| Charged rays | an audible, abortable wind-up |
| Land claims | FTB Chunks awareness, which didn't exist to integrate with |
| Survives restart | the original lost every player zombie on shutdown — its own README said so |
| Skins on vanilla clients | player heads, where the original needed Spout on every client |
