# Status

What works, what's untested, what's left. Kept honest — "verified" means someone watched it happen
in game, not that it compiled.

Last updated 2026-08-16.

## Built and verified in game

| | |
|---|---|
| **Genera as datapacks** | 43 shipped; hot-reload with `/reload` |
| **AI from JSON** | 9 goal types, recombined per genus |
| **Abilities** | 19 types |
| **Spawn conditions** | 9 types, composable with `any_of` / `not` |
| **Weighted spawning** | Per base mob, with a configurable vanilla share. `vanillaWeight = 40` settled by play (2026-08-16) — measured at ~26% plain zombies on the surface, ~13% deep underground |
| **Behaviours** | Goal sets that switch on a condition (day/night) |
| **Bosses** | Boss bars, phases, loot tables, summon rituals with block patterns |
| **Player zombies** | Corpse wearing the player's real skin, carrying their items, with an admin recovery ledger |
| **Equipment** | Six slots, bare ids or full stacks with components |
| **Player-head faces** | Custom embedded textures on 44 of 47 genera, plus name-resolved profiles and `ghost` borrowing a real player's |
| **Climbing** | Navigation swap *plus* the goal that performs it |
| **Guardian beam** | By parenting an invisible Guardian to the caster |
| **Particle rays** | Hitscan with an audible, abortable charge-up |
| **Adaptive resistance** | Learns damage types, remembers across restart |
| **Block breaking** | Tag-gated, griefing-hook aware, long target memory |
| **Land claims** | FTB Chunks, by reflection, inert without it. Griefing veto verified in both directions: refuses inside a claim, breaks again the instant the claim is removed. |
| **XP** | Per genus |
| **Bounty** | Per genus, with a pluggable payer and a scoreboard fallback |
| **Horde events** | Wave director with a boss bar, three shipped hordes, off by default |
| **CityWorld districts** | 3 conditions on district, lot and wildness, reflective and inert without it. Verified against a generated city: 289 lots, 7 districts, 4 lot styles — and **in play**: Commuters in a highrise district and Harvesters in a farm one, at roughly the rate their weights predict. Weights since raised to 45/40; the new rate is being checked |
| **Mutation** | Genus becomes another genus on a trigger. `health_below`, `on_fire` and `where` (dimension) all watched in game |
| **Horde payoff** | Victory line, sound and XP on the last kill |
| **Straggler glow** | Both paths: a bell ring lights them up, and a horde that goes a minute without a kill lights them up itself |
| **Conversion** | What a genus kills rises as one of them, with an undead-counterpart mapping and four guards |
| **Infection** | Bite now, turn later, whatever kills you — and milk cures it. Bite, timer and the infected-player double-raise verified. Confirmed emergent in play: an infected flock wandered into a sweet berry bush and rose from it. |
| **Proximity spawning** | Zombies out of sight around each player, off by default |
| **ZombieDex** | Seen/killed per player, in chat, in a written book and on scoreboards. Confirmed in play by an ordinary non-op player: the book spawns and tracks met and kills |
| **Vanilla clients** | Verified with a genuinely unmodded client against a dedicated server carrying the client half: joined, stayed, no `may not be sent to the client`, no exceptions. See CLAUDE.md for the procedure |
| **Commands** | `list`, `spawn`, `status`, `observe`, `corpse …`, `bestiary`, `config`, and `/zm` |
| **Herd infection** | A Biter bites, loses interest, and moves on; the flock sickens and spreads it; milk cures an animal. Confirmed in play, including an infected flock dying to a sweet berry bush and rising from it |
| **Bramble & Blight** | The mirror pair, in play: one lays moss as it walks, the other seeks moss out and eats it, and they hunt each other. Blight usually wins |
| **`seek_blocks`** | A genus walking to blocks it has an opinion about. Confirmed in play: three Blights stripped a terraced CityWorld roof of moss and vines |

## Built, not yet verified in game

- **`NO_SPAWNS` claim mode** — the griefing veto is verified, but only the default `VANILLA_ONLY`
  spawn behaviour has been exercised; nobody has watched `NO_SPAWNS` cancel a spawn.
- **Non-zombie bases beyond husk/drowned/skeleton** — `giant`, `zombie_villager`, `iron_golem`.
- **The faces themselves.** Every hash resolves at Mojang and every genus wears the head on its head
  slot, both checked headlessly — but nobody has stood in front of one and looked at it. What is
  unproven is whether they *read* at a distance, not whether they load. Already one correction from
  play: Nightstalker's head was "Masked Zombie", whose mask turns out to be a *surgical* one, which
  said nothing about hunting in the dark. Picking by catalogue name is how that happened; picks are
  now screened by rendering the face pixels and looking at them, dimmed as well as lit.
- **Mutation's two damp triggers** — `touching` (Walker on ice turning Frost) and `in_water` (an
  Ember doused back into a Walker). Same machinery as the three that are confirmed, and both were
  proven headlessly in both directions, but nobody has watched either happen.
- **`alert`** — Screamer handing its target to a horde.
- ~~The Borg Hive~~ — **confirmed in play** ("borg works"). Still untuned by feel: whether horde
  weight 1 is rare enough, and the queen's numbers.
- **The Vault Dweller** — blue suit, gold trim, Vault Boy grin, underground only. Built, never met.
- **The dex book as a key.** ~~Right-clicking~~ — **opens in play** (2026-08-16), but the first build
  opened the book behind it a tick later: the client cancel was not enough, because
  `ServerPlayer.openItemGui` sends a packet and the server opens the book itself. Now cancelled on
  both sides, the server half gated on the player speaking our channel so a vanilla client keeps its
  book. Re-test is just: right-click, and nothing should follow the screen.
- ~~The dex concealment configs~~ — **confirmed in play** (2026-08-16), both ladders and both
  outcomes: `hideUnspawnable` works; a Herobrine on the `hidden` list was killed and still left no
  record; a Duststalker on `hiddenRevealedWhenMet` appeared the moment it was wounded.
- ~~`sound_target`~~ — **confirmed in play** ("clicker works well"), eyeless face and all.
- **The dex batch.** Drops enumeration is proven for all 16 tables; the mirror-Ghost, play chips and
  drop icons are client rendering, provable only by eye. The **doll geometry** is no longer among
  them: a probe built all 56 dolls and checked the box against the renderer's real placement, and
  every one now stands on the same line (feet at 175-176 of 180) with nothing clipped, Tank drawing
  128px against Walker's 58 and Swarmling's 29. The bug it found was that setting `Attributes.SCALE`
  on a doll *shrank* the geometry the renderer computed — see CLAUDE.md.
- **The dex info page.** All 54 descriptions load, the write-up renders, and the gate was proven in
  all three modes against a player who had met one thing and killed nothing. Untested in game: the
  clicking, the back button, the ability expansion, and above all the entity preview — that is the
  one piece with no headless equivalent at all.
- **The balance pass.** xp and bounty were re-derived from a threat score across all 54, sixteen loot
  tables added, tools given to the genera whose identity is a tool. Every one was read back off a
  built mob and every table was rolled 40 times for real — but whether the numbers *feel* right is a
  thing only play decides, and the ones most likely to be wrong are the drops, which nobody has
  farmed yet.
- **The Sleeper** — the low-`follow_range` genus. Whether six blocks is menacing or merely broken.
- **The stock-take.** All 54 genera had `follow_range` set by sensory tier, the xp gaps filled and
  trims and arrows added where they say something. Every value was read back off a *built* mob
  rather than out of the JSON — 54 checked, 0 mismatched — but whether the tiers make a crowd read
  the way they should is a thing only play can answer.
- **The Ghost face pool** — seed names, login memory, ageing and ban filtering. All four were proven
  headlessly and in both directions, including banning a probe player and watching it vanish from
  200 draws, then un-banning and watching it return. What is unverified is only how it *looks*.
- **The six new appearance fields** — `invisible`, `baby`, `burning`, `arrows`, `glow`, `villager`.
  Each was built for real through `GenusApplier.assign` and read back headlessly, so the state is
  right; what nobody has done is stand in front of one. ~~Ghost in particular~~ — **Ghost confirmed
  in play** (2026-08-16): they spawn wearing faces. The lone-chestplate reading noted here was the
  dev server having no seed list *and* no player who had ever joined; the seed list was added three
  commits later precisely because of it, and it works.
- ~~The full-moon Siege~~ — **confirmed in play** (2026-08-16): one fired by itself on a full moon,
  unforced. `moon`, `depth` and `see_sky` had each been proven headlessly in both directions;
  what was missing was the world being on the right moon, and now it has been.
  `/zombiemod horde start zombiemod:the_siege` still forces it regardless of the moon.
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

Every ability is done, and so is proximity spawning — it is built, configurable and off by default,
waiting only for somebody to turn it on and judge it. One thing is genuinely outstanding:

- **Bounty.** Waiting on an economy decision. There is no Vault equivalent on NeoForge; Impactor is
  the nearest thing to a common API and isn't on 1.21.11. `xp` covers most of the intent meanwhile.

## Parked ideas

- **An aquatic genus - a Drowned, but squiddier.** Sable's, 2026-08-13. Worth noting that it looks
  like pure JSON: `base: minecraft:drowned` (Bogman already uses it), `navigation: swim` or
  `amphibious`, `glow` for the glow-squid outline, `zombiemod:effect` with blindness plus
  `zombiemod:particles` with squid ink for an ink cloud, and `zombiemod:pull` to drag someone under.
  The 1.8 plugin's `INK` ability has no direct port - `effect` + `particles` is the replacement, and
  is more flexible than the original was.

## Parked: translatable / server-editable strings

LegendQuest shipped a full string-externalisation layer (2026-08-15): a ~280-key `messages.yml`
catalogue with `{term}` cross-refs and placeholders, `&` colours, live on `/reload`, and a small
vocab payload syncing resolved terms to modded clients with English fallbacks. If ZombieDex text
ever needs to be translatable or server-editable, the pattern is `Lang.java` +
`VocabPayload`/`ClientVocab` in `Sablednah/LegendQuest-ReForged` — with one non-obvious trap their
session flagged: GUI screens built from `§` literals bypass the `&`→`§` conversion at the send
layer, so screen-facing lookups need their own converting wrapper.

Not adopted yet, deliberately: ZombieMod's player-facing text is small (dex prose lives in genus
JSON, which datapacks already override per-server), and a second string system is only worth its
upkeep once someone actually asks for a translation.

## Next, in the order I'd do it

1. **Proximity in survival.** Enabled in Sable's instance; the cap semantics are settled ("quiet
   place top up is perfect" — 2026-08-15). What remains is simply a survival session on quiet ground
   watching it fire, and whether `nearbyCap = 8` feels like atmosphere.
2. **The dex, clicked through.** The info page, the back button, the ability expansion and the entity
   preview are the largest block of code in the mod with no headless proof at all. One session with
   the book in hand settles most of it.
3. **Spawn density** via `neoforge:add_spawns` biome modifiers. Example in
   [`examples/add_spawns_biome_modifier.json`](examples/add_spawns_biome_modifier.json), deliberately
   not enabled.

## Before release

- **Remove the 1.8 source tree** (`src/me/sablednah/`) and the LICENSE carve-out that exists for it.
- **Rewrite `README.md`'s opening** for a store page; move the technical detail down or out.
- **Balance pass.** Radii, damage and phase thresholds are still first guesses. Weights are not:
  see [ROSTER.md](ROSTER.md).
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
