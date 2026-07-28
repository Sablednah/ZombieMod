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
| `GHOST` | ❌ | Named itself after a random offline player. Would be *better* now — a `head` from their profile means it could wear their face too. |
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
| **Block breaking** | ✅ | `break_blocks`, gated on the mob being stuck, an explicit `allowed` list, and the `mobGriefing` gamerule. See Breaker. |
| **Proximity spawning** | ❌ | `ProximitySystems`: spawned zombies just out of sight around each player, ignoring vanilla's rules, scaled by distance from world spawn. Different in feel from riding vanilla's spawn table — it's what made the world feel occupied. |
| **Jockeys / mounts** | ❌ | A pipe-delimited `jockey` field put the zombie on a horse, chicken, spider… |
| Angry iron golems | ⚠️ | The original registered a hostile `AngryGolem`. A genus with `base: minecraft:iron_golem` gets most of the way there — untested. |
| XP and bounty on kill | ⚠️ | `xp` shipped. Bounty still needs an economy mod, and there is no Vault equivalent — Impactor is the closest thing to a common API. Deferred. |
| DaySpawner | — | Already dead in the original: commented out at its scheduling site. |
| BeardStat, Spout, LegendQuest, Factions | — | Gone. FTB Chunks/Teams and CityWorld are the modern replacements, planned as spawn conditions. |

## What's actually left

- **`GHOST`** — named itself after a random offline player. Needs a record of who has played here,
  since there's no public "all known profiles" lookup. Cheap, and it lands better now: it can wear
  their face as well as borrow their name.
- **Proximity spawning** — "there are more of them than there should be, and they're already close".
- **Jockeys / mounts** — the old pipe-delimited `jockey` field.
- **Bounty** — waiting on an economy decision.
