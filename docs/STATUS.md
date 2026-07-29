# Status

What works, what's untested, what's left. Kept honest — "verified" means someone watched it happen
in game, not that it compiled.

Last updated 2026-07-29.

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
| **Land claims** | FTB Chunks, by reflection, inert without it |
| **XP** | Per genus |
| **Commands** | `list`, `spawn`, `status`, `observe`, `corpse …` |

## Built, not yet verified in game

- **`NO_SPAWNS` claim mode** — only `VANILLA_ONLY` (the default) has been exercised.
- **Non-zombie bases beyond husk/drowned/skeleton** — `giant`, `zombie_villager`, `iron_golem`.
- **`alert`** — Screamer handing its target to a horde.
- **`summon`'s `max_nearby` cap** under real pressure, i.e. a Breeder left alone in a loaded chunk.
- **Corpse recovery edge cases** — a corpse lost to lava or a mob grinder, then `give`/`respawn`.
- **Loot tables** — resolution is proven; nobody has watched Patient Zero drop his netherite scrap.

## Left from the 1.8 plugin

Every ability is done. Two things aren't:

- **Proximity spawning.** `ProximitySystems` put zombies just out of sight around each player,
  ignoring vanilla's rules and scaling with distance from world spawn. Genera currently only claim
  spawns vanilla was already making, so a world has vanilla's zombie *count* with more variety.
  This is the last real gap in feel.
- **Bounty.** Waiting on an economy decision. There is no Vault equivalent on NeoForge; Impactor is
  the nearest thing to a common API and isn't on 1.21.11. `xp` covers most of the intent meanwhile.

## Next, in the order I'd do it

1. **`convert`** — turn what you kill into one of them. The biggest idea in the genre and the mod has
   no answer to it. Also where "kill it before it turns" lives.
2. **Proximity spawning** — see above.
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
