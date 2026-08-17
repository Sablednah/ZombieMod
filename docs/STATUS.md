# Status

What works, what's untested, what's left. Kept honest — "verified" means someone watched it happen
in game, not that it compiled.

Last updated 2026-08-17.

## Built and verified in game

| | |
|---|---|
| **Genera as datapacks** | 56 shipped; hot-reload with `/reload` |
| **AI from JSON** | 12 goal types, recombined per genus |
| **Abilities** | 22 types |
| **Spawn conditions** | 12 types, composable with `any_of` / `not` |
| **Weighted spawning** | Per base mob, with a configurable vanilla share. `vanillaWeight = 40` settled by play (2026-08-16) — measured at ~26% plain zombies on the surface, ~13% deep underground |
| **Behaviours** | Goal sets that switch on a condition (day/night) |
| **Bosses** | Boss bars, phases, loot tables, summon rituals with block patterns |
| **Player zombies** | Corpse wearing the player's real skin, carrying their items, with an admin recovery ledger |
| **Equipment** | Six slots, bare ids or full stacks with components |
| **Player-head faces** | Custom embedded textures on 51 of 56 genera, plus name-resolved profiles and `ghost` borrowing a real player's |
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
| **Vanilla clients** | Verified twice with a genuinely unmodded client (the Mojang launcher, not a stripped profile) against a dedicated server carrying the client half: joined, stayed, no `may not be sent to the client`, no `Couldn't place player in world`, no exceptions. Re-run 2026-08-16 after the dex book, the payload guard and the server-side interaction cancel all landed — **and the book opens as a book**, which is the case that had to be watched rather than reasoned about. See CLAUDE.md for the procedure |
| **Commands** | `list`, `spawn`, `status`, `observe`, `corpse …`, `bestiary`, `config`, and `/zm` |
| **Herd infection** | A Biter bites, loses interest, and moves on; the flock sickens and spreads it; milk cures an animal. Confirmed in play, including an infected flock dying to a sweet berry bush and rising from it |
| **Bramble & Blight** | The mirror pair, in play: one lays moss as it walks, the other seeks moss out and eats it, and they hunt each other. Blight usually wins |
| **`seek_blocks`** | A genus walking to blocks it has an opinion about. Confirmed in play: three Blights stripped a terraced CityWorld roof of moss and vines |

## Built, not yet verified in game

- ~~`NO_SPAWNS` claim mode~~ — **confirmed in play** (2026-08-17), in both directions, which is the
  only version of this test that means anything: a claimed nine-chunk grid stayed empty while
  unclaimed land beside it kept spawning. Confirmed too that mobs spawn outside a claim and then walk
  in and aggro — correct, but not a safe zone, and now said so in the README because it is the first
  thing that reads as a bug. `/zombiemod status` counts cancellations, since the feature's whole
  effect is an absence.
- ~~Non-zombie bases beyond husk/drowned/skeleton~~ — **all confirmed in play** (2026-08-16).
  `zombie_villager` met in the wild; `giant` and `iron_golem` given a genus each to try them with,
  `colossus` and `warden_golem`, and both work. Both weight 0, so they are deliberate rather than
  ambient — note that proximity spawning ignores the base match, so a weight above 0 would put
  giants on the landscape whatever vanilla does. The golem cannot wear anything: see TROPES.
- **The faces themselves.** Every hash resolves at Mojang and every genus wears the head on its head
  slot, both checked headlessly — but nobody has stood in front of one and looked at it. What is
  unproven is whether they *read* at a distance, not whether they load. Already one correction from
  play: Nightstalker's head was "Masked Zombie", whose mask turns out to be a *surgical* one, which
  said nothing about hunting in the dark. Picking by catalogue name is how that happened; picks are
  now screened by rendering the face pixels and looking at them, dimmed as well as lit.
- ~~Mutation's two damp triggers~~ — **confirmed in play** (2026-08-16): ice and water both fire.
- ~~`alert`~~ — **confirmed in play** (2026-08-17), and reported "subtle", which is about right for a
  genus whose whole job is to make the fight someone else's problem.
- ~~The Borg Hive~~ — **confirmed in play** ("borg works"). Still untuned by feel: whether horde
  weight 1 is rare enough, and the queen's numbers.
- ~~The Vault Dweller~~ — **confirmed in play** (2026-08-16): met underground, blue suit and all.
- ~~The dex book as a key~~ — **confirmed in play** (2026-08-16). The first build opened the book
  behind it a tick later, because the client cancel was not enough: `ServerPlayer.openItemGui` sends
  a packet, so the server opens the book itself. Cancelled on both sides now, the server half gated
  on the player speaking our channel — and that guard is now verified from the other side too: an
  unmodded client on the Mojang launcher right-clicks the same item and gets the written pages.
  **Sneak-right-click** falls through to those pages on a modded client as well, so you can check
  what a vanilla player sees without a second instance. An anvil-renamed book counts as a dex. The
  sneak decision is taken entirely on the client, because the crouch flag reaches the server a tick
  behind the click and two sides answering it separately would give both windows or neither on the
  frame shift changes.
- ~~The dex concealment configs~~ — **confirmed in play** (2026-08-16), both ladders and both
  outcomes: `hideUnspawnable` works; a Herobrine on the `hidden` list was killed and still left no
  record; a Duststalker on `hiddenRevealedWhenMet` appeared the moment it was wounded.
- ~~`sound_target`~~ — **confirmed in play** ("clicker works well"), eyeless face and all.
- ~~The dex batch~~ — **confirmed in play** (2026-08-16), all of it. The mirror dolls read the way
  they were meant to: the Ghost is the viewer's own face over an invisible body, the Corpse is
  wearing the viewer's current gear. Play chips sound. The **doll geometry** took three passes —
  feet wandering, then heads clipped, then right — and only the third started from measurement: a
  probe built all 56 dolls and checked the box against the renderer's real placement. Every one
  stands on the same line (feet at 175-176 of 180) with nothing clipped, Tank drawing 128px against
  Walker's 58 and Swarmling's 29. The bug it found was that setting `Attributes.SCALE` on a doll
  *shrank* the geometry the renderer computed — see CLAUDE.md.
- ~~The dex info page~~ — **confirmed in play** (2026-08-16): clicking through, the back button, the
  ability expansion and the entity preview, which was the one piece with no headless equivalent at
  all. The vanilla half of the same feature is confirmed too — `/zm bestiary info <genus>` reads well
  in chat, and the written book's pages hold up.
- **The balance pass.** xp and bounty were re-derived from a threat score across all 54, and tools
  given to the genera whose identity is a tool. Every one was read back off a built mob — but whether
  the numbers *feel* right is a thing only play decides.

  **Drops were the part play found wanting** ("a little sparse", 2026-08-16), so the sixteen tables
  became fifty-six: every genus now leaves something that says what it was, except Walker, which
  stays ordinary on purpose, and the player corpse, whose drop is your own inventory. Each is a
  signature item in quantity plus one rarer thing — sugar and a rabbit's foot from a Runner, an echo
  shard from the blind Clicker, a saddle from the Outrider that rode in, a poppy from the golem. All
  fifty-six load clean, which validates every item id, but none are farmed yet.
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
- ~~Corpse recovery~~ — **confirmed in play**, face and all (2026-08-16, re-confirmed 17th after the
  fix below). `respawn` rebuilt the corpse with the items, but it came back *bald* — `rebuild` wrote the ledger tag and the pockets but never the
  face, so a rebuilt corpse was not recognisably anybody. Fixed, along with two things sitting beside
  it: the armour was not re-worn either, and the name went through `Component.literal` so a
  colour-coded `Corpse %P` would have shown its codes. The **lava and void cases** are now handled and
  proven headlessly in all three directions: a lava death and a void death leave the entry
  outstanding with a reason recorded, a clean kill still settles it, and an entry written before the
  field existed still decodes. What nobody has watched is any of it in play, and a **grinder** is
  deliberately left undecidable — a hopper may have taken the items, so re-issuing could duplicate an
  inventory, and that stays an admin's call.
- **Loot tables** — resolution is proven; nobody has watched Patient Zero drop his netherite scrap.
- **Conversion in play.** The guards are tested (a listed victim rises, an unlisted one doesn't, an
  undead one never does, and 40 kills in one tick produce exactly one). Nobody has watched a Carrier
  work through a village, which is the case that matters.
- ~~Infection's two remaining paths~~ — **both confirmed in play** (2026-08-16): bitten then killed
  by something unrelated, and milk clearing it before death.
- **Corpse recovery after killing only the decoy.** The one path where a bug would genuinely cost
  someone their inventory.
- ~~Proximity spawning~~ — **confirmed in play**. `nearbyCap = 8` settled 2026-08-16, and the "no
  spot" flood fixed 2026-08-17: "less no spots, mostly at cap, which is fine", which is the counter
  you want to be dominant — it means the ground is full rather than unusable. Two causes were behind
  it: the standing-room test was stricter than vanilla's (a tuft of grass or a snow layer
  disqualified a spot), and the search only ever aimed at the surface heightmap, so a player in a
  cave or a cellar had company placed on the roof of the world above them. Worth recording that the
  probe for this was *inconclusive* — 1681 surface spots in the dev world, old and new rules agreeing
  exactly, because that terrain has none of the cases the fix is for. Play settled what the probe
  could not.
- **Bounty payouts** — the scoreboard tally, and whether the numbers feel proportionate.
- **Hordes.** One Siege survived, which found both of the gaps now closed. `cap = 40` **settled by
  play** (2026-08-16). "Whether it builds or just arrives" turned out to be a **bug, not a tuning
  question**: every wave inherited the *previous* wave's delay, so a `delay: 0` first wave made the
  second arrive on the next tick and the third one tick after that, and the last wave's delay was
  never read at all. Measured with a FakePlayer: waves at ticks 21 and 22, then nothing for 1378
  ticks. Three waves in three ticks — it never built, and the numbers in every shipped horde described
  something nobody had seen. Fixed and re-measured at +1/+401/+901, matching the JSON.
  **`on_clear` (new)** makes a wave arrive the moment the previous is dead, with the delay as a
  ceiling; proven both ways — wave 3 came at tick 501 when the field was wiped at 500, and at 921 when
  it was not. The retuned numbers are now a genuine first guess rather than a wrong one, and nobody
  has played a chained horde yet.
  `glowAfter = 1200` is settled: in play it fired just as the player was giving up and heading for a
  bell, which is exactly where that threshold wants to sit.
- **Horde counting and chunk unloads.** Survivors are counted by identity now, so distance no longer
  loses them, but a mob in an unloaded chunk still reads as gone and would end the horde early.
  Unlikely at these radii; not impossible if a player runs.

## Fixed, worth remembering

- **World creation deadlocked with CityWorld installed** (found and diagnosed by the LegendQuest
  session from a thread dump, 2026-08-16; fixed same day). CityWorld populates chunks from inside
  generation and routes through `EventHooks.finalizeMobSpawn` so other mods get their say —
  correctly. ZombieMod took that say and called `level.getHeight` from a worldgen thread, which asks
  the chunk system for a chunk from inside chunk generation: the worker parked on a future the
  server thread was itself waiting to fulfil, and world creation hung at "Preparing spawn area"
  forever with no crash and no log line.

  Two layers of fix. `onFinalizeSpawn` now returns immediately for `CHUNK_GENERATION`, which is also
  right on its own merits — those are pre-population mobs that mostly despawn, and rolling a genus
  for each one put a registry scan on the worldgen hot path. And the four terrain conditions
  (`biome`, `light`, `depth`, `see_sky`) now check `hasChunkAt` first and fail closed, so no other
  mod can reproduce it from a different direction. Verified by generating a CityWorld world
  headlessly with both mods present: spawn area 100%, 3858 ms, `Done (5.031s)`.

  The lesson is in the javadoc that licensed it — "every caller here is at a player or a live spawn
  attempt, so it always is". A comment asserting an invariant about *all callers* is a claim you
  cannot make about a mod nobody has written yet.

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

## Where this stands, 2026-08-17

**Everything testable has been tested.** The "built, not yet verified" list is down to items that
need a *situation* to arise rather than a session to run (a Carrier working through a village, a
Breeder left alone under real pressure), items that are matters of feel rather than function (drop
rates, the Sleeper's six-block range, the Borg Queen's numbers, the new chained wave timings), and
one thing that is blocked outright (bounty payouts).

The mod is feature-complete for a first release. **What remains is release work**, below.

## Next, in the order I'd do it

1. **Proximity in survival.** Enabled in Sable's instance; the cap semantics are settled ("quiet
   place top up is perfect" — 2026-08-15). What remains is simply a survival session on quiet ground
   watching it fire, and whether `nearbyCap = 8` feels like atmosphere.
2. **Spawn density** via `neoforge:add_spawns` biome modifiers. Example in
   [`examples/add_spawns_biome_modifier.json`](examples/add_spawns_biome_modifier.json), deliberately
   not enabled.

## Before release

- **Remove the 1.8 source tree** (`src/me/sablednah/`) and the LICENSE carve-out that exists for it.
- **Rewrite `README.md`'s opening** for a store page; move the technical detail down or out.
- **Balance pass.** Radii, damage and phase thresholds are still first guesses. Weights are not:
  see [ROSTER.md](ROSTER.md).
- **Root-folder leftovers from the Bukkit plugin** — `config.yml`, `lang.yml`, `plugin.yml` are still
  in the repo root and mean nothing to a NeoForge mod. They go with the 1.8 source tree.
- **Version number.** Still `3.0.0-alpha.1` in `gradle.properties`; a release wants a real one.
- **`mod_description`** in `gradle.properties` is what the mods screen shows beside the new logo. It
  already reads as store prose; worth one re-read against the final feature list rather than a
  rewrite.
- `docs/curseforge-description.md` — the WoodDye release shape. **Artwork is in hand** (2026-08-17):
  `docs/main-logo.png` is the square lockup for the CurseForge project icon, `docs/slime-logo.png` the
  banner, and `night-`/`Stone-`/`survival-logo.png` are variants held back for updates and themed
  events. All five arrived with a magenta chroma-key background rather than alpha, which would have
  shown as a solid magenta square wherever they were used; keyed out on the magenta-ness axis
  (`min(R,B) - G`, since the key colour was not uniform) with a despill on the ramp, then trimmed.

## Known limitations

- **Goal targets** come from a fixed name→class map (`TargetClass`), because vanilla's targeting goals
  are typed on a Java class rather than an entity id. "Avoid wolves" works; "avoid a modded mob" does
  not.
- **A malformed genus stops the world loading** rather than being skipped. Standard datapack-registry
  behaviour, harsher here because these files are hand-written. The log names the file and field.
- **Renderer-bound effects can't be given to arbitrary mobs.** Sometimes there's a way round — the
  beam parents a guardian, the creeper swell is approximated with `scale`, the bow draw needs a
  skeleton base — and sometimes there isn't. See [TROPES.md](TROPES.md).
