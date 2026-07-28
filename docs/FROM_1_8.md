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
| `BORG` | ❌ | Adaptive resistance: it remembered the material that hurt it and stopped taking damage from that. Genuinely novel, and nothing in the port does it. |
| `WEB` | ❌ | Placed cobwebs on its target. Needs a block-placing ability. |
| `INFEST` | ❌ | Blocks it broke became silverfish blocks. Depends on block breaking below. |
| `LAZER` | ❌ | A ranged attack. Needs a `projectile` ability or a ranged goal. *(Worth knowing: in the original both branches of the `LAZER` check called the same arrow goal, so it never actually differed from an ordinary archer.)* |

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
| **Block breaking** | ❌ | `Break`/`BreakRunner`: when a zombie's path to you was blocked and it stopped moving, it chewed through the wall. The `allowedbreaks` list capped what it could take. **This is the big one** — it's what made them threatening to a base rather than an obstacle to walk around. |
| **Proximity spawning** | ❌ | `ProximitySystems`: spawned zombies just out of sight around each player, ignoring vanilla's rules, scaled by distance from world spawn. Different in feel from riding vanilla's spawn table — it's what made the world feel occupied. |
| **Jockeys / mounts** | ❌ | A pipe-delimited `jockey` field put the zombie on a horse, chicken, spider… |
| Angry iron golems | ⚠️ | The original registered a hostile `AngryGolem`. A genus with `base: minecraft:iron_golem` gets most of the way there — untested. |
| XP and bounty on kill | ❌ | `xp` and `bounty` (via Vault). No economy dependency now; XP would be easy, bounty needs a target mod. |
| DaySpawner | — | Already dead in the original: commented out at its scheduling site. |
| BeardStat, Spout, LegendQuest, Factions | — | Gone. FTB Chunks/Teams and CityWorld are the modern replacements, planned as spawn conditions. |

## If I were picking

**Block breaking**, comfortably. It's the one absence you'd actually feel: right now a wall is a
solution to zombies, and in the original it was a delay. Everything else on this list adds a monster;
that one changes what the mod *is*.

Then **proximity spawning**, because "there are more of them than there should be, and they're
already close" is the other half of that feeling.

`GHOST` is the cheap win — it's `head` plus a name lookup, and it lands better now than it did then.
