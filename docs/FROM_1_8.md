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
| XP and bounty on kill | ⚠️ | `xp` shipped. Bounty still needs an economy mod, and there is no Vault equivalent — Impactor is the closest thing to a common API. Deferred. |
| DaySpawner | — | Already dead in the original: commented out at its scheduling site. |
| BeardStat, Spout, LegendQuest, Factions | — | Gone. FTB Chunks/Teams and CityWorld are the modern replacements, planned as spawn conditions. |

## What's actually left

Every ability from the original is now covered. Two systems and one dependency remain:

- **Proximity spawning** — `ProximitySystems` put zombies just out of sight around each player,
  ignoring vanilla's rules and scaling with distance from world spawn. "There are more of them than
  there should be, and they're already close" is a different feeling from riding the vanilla spawn
  table, and it's the last real gap.
- **Angry iron golems** — probably just `base: minecraft:iron_golem` on a genus. Untested.
- **Bounty** — waiting on an economy decision. There is no Vault equivalent on NeoForge; Impactor is
  the closest thing to a common API, and it isn't on 1.21.11 yet.
