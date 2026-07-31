# Status

What works, what's untested, what's left. Kept honest — "verified" means someone watched it happen
in game, not that it compiled.

Last updated 2026-07-30.

## Built and verified in game

| | |
|---|---|
| **Genera as datapacks** | 43 shipped; hot-reload with `/reload` |
| **AI from JSON** | 9 goal types, recombined per genus |
| **Abilities** | 19 types |
| **Spawn conditions** | 9 types, composable with `any_of` / `not` |
| **Weighted spawning** | Per base mob, with a configurable vanilla share |
| **Behaviours** | Goal sets that switch on a condition (day/night) |
| **Bosses** | Boss bars, phases, loot tables, summon rituals with block patterns |
| **Player zombies** | Corpse wearing the player's real skin, carrying their items, with an admin recovery ledger |
| **Equipment** | Six slots, bare ids or full stacks with components |
| **Player-head faces** | Including `ghost` borrowing a real player's |
| **Climbing** | Navigation swap *plus* the goal that performs it |
| **Guardian beam** | By parenting an invisible Guardian to the caster |
| **Particle rays** | Hitscan with an audible, abortable charge-up |
| **Adaptive resistance** | Learns damage types, remembers across restart |
| **Block breaking** | Tag-gated, griefing-hook aware, long target memory |
| **Land claims** | FTB Chunks, by reflection, inert without it. Griefing veto verified in both directions: refuses inside a claim, breaks again the instant the claim is removed. |
| **XP** | Per genus |
| **Bounty** | Per genus, with a pluggable payer and a scoreboard fallback |
| **Horde events** | Wave director with a boss bar, three shipped hordes, off by default |
| **CityWorld districts** | 3 conditions on district, lot and wildness, reflective and inert without it. Verified against a generated city: 289 lots, 7 districts, 4 lot styles |
| **Mutation** | Genus becomes another genus on a trigger. `health_below`, `on_fire` and `where` (dimension) all watched in game |
| **Horde payoff** | Victory line, sound and XP on the last kill |
| **Straggler glow** | Both paths: a bell ring lights them up, and a horde that goes a minute without a kill lights them up itself |
| **Conversion** | What a genus kills rises as one of them, with an undead-counterpart mapping and four guards |
| **Infection** | Bite now, turn later, whatever kills you — and milk cures it. Bite, timer and the infected-player double-raise verified. |
| **Proximity spawning** | Zombies out of sight around each player, off by default |
| **Commands** | `list`, `spawn`, `status`, `observe`, `corpse …` |

## Built, not yet verified in game

- **`NO_SPAWNS` claim mode** — the griefing veto is verified, but only the default `VANILLA_ONLY`
  spawn behaviour has been exercised; nobody has watched `NO_SPAWNS` cancel a spawn.
- **Non-zombie bases beyond husk/drowned/skeleton** — `giant`, `zombie_villager`, `iron_golem`.
- **The two city genera** — Commuter and Harvester. The conditions underneath them are verified
  against a real generated city, but nobody has met either monster in one.
- **Mutation's two damp triggers** — `touching` (Walker on ice turning Frost) and `in_water` (an
  Ember doused back into a Walker). Same machinery as the three that are confirmed, and both were
  proven headlessly in both directions, but nobody has watched either happen.
- **`alert`** — Screamer handing its target to a horde.
- **`summon`'s `max_nearby` cap** under real pressure, i.e. a Breeder left alone in a loaded chunk.
- **Corpse recovery edge cases** — a corpse lost to lava or a mob grinder, then `give`/`respawn`.
- **Loot tables** — resolution is proven; nobody has watched Patient Zero drop his netherite scrap.
- **Conversion in play.** The guards are tested (a listed victim rises, an unlisted one doesn't, an
  undead one never does, and 40 kills in one tick produce exactly one). Nobody has watched a Carrier
  work through a village, which is the case that matters.
- **Infection's two remaining paths.** Being bitten and then dying to something *unrelated* — a fall,
  drowning — which is the whole point of the delay. And the cure: milk clearing it before you die.
- **Corpse recovery after killing only the decoy.** The one path where a bug would genuinely cost
  someone their inventory.
- **Proximity spawning** — needs `enabled = true`. Whether `outOfSightOnly` actually prevents you
  watching them appear, and whether `nearbyCap = 8` is atmosphere or a siege. `/zombiemod status`
  reports the counters.
- **Bounty payouts** — the scoreboard tally, and whether the numbers feel proportionate.
- **Hordes.** One Siege survived, which found both of the gaps now closed. The numbers most likely
  to be wrong are still `cap = 40` and the wave delays — whether it builds or just arrives.
  `glowAfter = 1200` is settled: in play it fired just as the player was giving up and heading for a
  bell, which is exactly where that threshold wants to sit.
- **Horde counting and chunk unloads.** Survivors are counted by identity now, so distance no longer
  loses them, but a mob in an unloaded chunk still reads as gone and would end the horde early.
  Unlikely at these radii; not impossible if a player runs.

## Left from the 1.8 plugin

Every ability is done. Two things aren't:

- **Proximity spawning.** `ProximitySystems` put zombies just out of sight around each player,
  ignoring vanilla's rules and scaling with distance from world spawn. Genera currently only claim
  spawns vanilla was already making, so a world has vanilla's zombie *count* with more variety.
  This is the last real gap in feel.
- **Bounty.** Waiting on an economy decision. There is no Vault equivalent on NeoForge; Impactor is
  the nearest thing to a common API and isn't on 1.21.11. `xp` covers most of the intent meanwhile.

## Next, in the order I'd do it

1. **Proximity spawning** — see above.
3. **Spawn density** via `neoforge:add_spawns` biome modifiers. Example in
   [`examples/add_spawns_biome_modifier.json`](examples/add_spawns_biome_modifier.json), deliberately
   not enabled.
4. **CityWorld conditions** — `CityWorldAPI.lotAt` gives lot/context/schematic, so "more in the
   wilderness, different genera per district" is a condition type in the slot FTB Chunks now occupies.
5. **Sound-driven aggro** — a Clicker that is genuinely blind but hears you sprint.

## Before release

- **Remove the 1.8 source tree** (`src/me/sablednah/`) and the LICENSE carve-out that exists for it.
- **Rewrite `README.md`'s opening** for a store page; move the technical detail down or out.
- **Balance pass.** Nothing has been tuned by playing — weights, radii, damage and phase thresholds
  are all first guesses. See the note in [ROSTER.md](ROSTER.md) about the vanilla share drifting as
  the roster grew.
- **Decide `vanillaWeight`.** At 200 with 43 genera, roughly half of all eligible zombie spawns are
  now a genus. That may be right, but it was set when the roster was a third the size.
- `docs/curseforge-description.md`, mod icon, banner — the WoodDye release shape.

## Known limitations

- **Goal targets** come from a fixed name→class map (`TargetClass`), because vanilla's targeting goals
  are typed on a Java class rather than an entity id. "Avoid wolves" works; "avoid a modded mob" does
  not.
- **A malformed genus stops the world loading** rather than being skipped. Standard datapack-registry
  behaviour, harsher here because these files are hand-written. The log names the file and field.
- **Renderer-bound effects can't be given to arbitrary mobs.** Sometimes there's a way round — the
  beam parents a guardian, the creeper swell is approximated with `scale`, the bow draw needs a
  skeleton base — and sometimes there isn't. See [TROPES.md](TROPES.md).
