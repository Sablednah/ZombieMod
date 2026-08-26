# Status

What works, what's untested, what's left. Kept honest — "verified" means someone watched it happen
in game, not that it compiled.

Last updated 2026-08-26 (3.1.1, plus the 26.x branches).

**Counts here are now taken off the source, not off prose.** They had drifted — this file said 56
genera, 12 goal types, 22 abilities, 12 conditions and 3 hordes, and every one of those was wrong.
Re-derive them from `data/zombiemod/zombiemod/genus/*.json`, `.../horde/*.json` and the `register(`
lines in `GoalSpecTypes` / `AbilityTypes` / `SpawnConditionTypes` rather than editing the numbers by
hand.

## Built and verified in game

| | |
|---|---|
| **Genera as datapacks** | 59 shipped; hot-reload with `/reload` |
| **AI from JSON** | 12 goal types, recombined per genus |
| **Abilities** | 21 types |
| **Spawn conditions** | 14 types (11 general + 3 CityWorld), composable with `any_of` / `not` |
| **Weighted spawning** | Per base mob, with a configurable vanilla share. `vanillaWeight = 40` settled by play (2026-08-16) — measured at ~26% plain zombies on the surface, ~13% deep underground |
| **Behaviours** | Goal sets that switch on a condition (day/night) |
| **Bosses** | Boss bars, phases, loot tables, summon rituals with block patterns |
| **Player zombies** | Corpse wearing the player's real skin, carrying their items, with an admin recovery ledger |
| **Equipment** | Six slots, bare ids or full stacks with components |
| **Player-head faces** | Custom embedded textures on 52 of 59 genera, plus name-resolved profiles and `ghost` borrowing a real player's |
| **Climbing** | Navigation swap *plus* the goal that performs it |
| **Guardian beam** | By parenting an invisible Guardian to the caster |
| **Particle rays** | Hitscan with an audible, abortable charge-up |
| **Adaptive resistance** | Learns damage types, remembers across restart |
| **Block breaking** | Tag-gated, griefing-hook aware, long target memory |
| **Land claims** | FTB Chunks, by reflection, inert without it. Griefing veto verified in both directions: refuses inside a claim, breaks again the instant the claim is removed. |
| **XP** | Per genus |
| **Bounty** | Per genus, with a pluggable payer and a scoreboard fallback. **Paying into a real economy confirmed in play** (2026-08-25) through SableCraft Standards — the last thing outstanding from the 1.8 plugin, now closed end to end |
| **Horde events** | Wave director with a boss bar, four shipped hordes, off by default |
| **CityWorld districts** | 3 conditions on district, lot and wildness, reflective and inert without it. Verified against a generated city: 289 lots, 7 districts, 4 lot styles — and **in play**: Commuters in a highrise district and Harvesters in a farm one, at roughly the rate their weights predict. Weights since raised to 45/40; the new rate is being checked |
| **Mutation** | Genus becomes another genus on a trigger. `health_below`, `on_fire` and `where` (dimension) all watched in game |
| **Horde payoff** | Victory line, sound and XP on the last kill |
| **Straggler glow** | Both paths: a bell ring lights them up, and a horde that goes a minute without a kill lights them up itself |
| **Conversion** | What a genus kills rises as one of them, with an undead-counterpart mapping and four guards |
| **Infection** | Bite now, turn later, whatever kills you — and milk cures it. Bite, timer and the infected-player double-raise verified. Confirmed emergent in play: an infected flock wandered into a sweet berry bush and rose from it. |
| **Proximity spawning** | Zombies out of sight around each player, off by default |
| **ZombieDex** | Seen/killed per player, in chat, in a written book and on scoreboards. Confirmed in play by an ordinary non-op player: the book spawns and tracks met and kills |
| **Vanilla clients** | Verified twice with a genuinely unmodded client (the Mojang launcher, not a stripped profile) against a dedicated server carrying the client half: joined, stayed, no `may not be sent to the client`, no `Couldn't place player in world`, no exceptions. Re-run 2026-08-16 after the dex book, the payload guard and the server-side interaction cancel all landed — **and the book opens as a book**, which is the case that had to be watched rather than reasoned about. See CLAUDE.md for the procedure |
| **Commands** | `list`, `spawn`, `status`, `observe`, `corpse …`, `bestiary`, `config`, and `/zm`; plus the client-side `/zmdex render` |
| **Roster image export** | `/zmdex render` writes a transparent PNG per genus, two-pass alpha, reusing the dex doll. Confirmed in play: 58 images, every head correct. Renders whatever roster the server sent, so a third-party pack gets its own set |
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
- **The faces themselves** — **read correctly in play** (2026-08-24): "faces and styles seem to read
  ok". Held as *provisional* rather than closed, and deliberately so — this is the one item on the
  list that a single pair of eyes cannot settle, because it is a question about whether a stranger
  recognises a genus at a glance. It wants **player feedback at volume**, which only the public
  release can supply. Every hash resolves at Mojang and every genus wears the head on its head slot,
  both checked headlessly, so what was ever in doubt was legibility, not loading. Already one correction from
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
- ~~**Colossus and Rusted Warden's dolls**~~ — **confirmed in play** (2026-08-17): both render fine,
  and the Colossus "fills the box perfectly", which is the case that had the most room to go wrong.
  Worth recording *why* they were in doubt: both were added in `ba1222b`, later than the doll probe
  and the stock-take, so the "all 54" and "all 56" counts in this section are accurate history rather
  than stale numbers — and those two genuinely sat outside them. The doll half is closed by eyes, and
  **the stock-take is now done too** (2026-08-24) — measured against the roster rather than guessed
  at. `follow_range`, `look_at` distance, xp/bounty and ability shape all checked out on both; one
  number was wrong, the Rusted Warden's shockwave cadence, which fired half as often as its weight
  predicts. Fixed and written up in [BALANCE.md](BALANCE.md).
- **The stock-take.** All 54 genera had `follow_range` set by sensory tier, the xp gaps filled and
  trims and arrows added where they say something. Every value was read back off a *built* mob
  rather than out of the JSON — 54 checked, 0 mismatched — but whether the tiers make a crowd read
  the way they should is a thing only play can answer.
- **The Ghost face pool** — seed names, login memory, ageing and ban filtering. All four were proven
  headlessly and in both directions, including banning a probe player and watching it vanish from
  200 draws, then un-banning and watching it return. What is unverified is only how it *looks*.
- **The six new appearance fields** — `invisible`, `baby`, `burning`, `arrows`, `glow`, `villager`.
  Each was built for real through `GenusApplier.assign` and read back headlessly, so the state is
  right — and they **read correctly in play** (2026-08-24), on the same provisional footing as the
  faces above. ~~Ghost in particular~~ — **Ghost confirmed
  in play** (2026-08-16): they spawn wearing faces. The lone-chestplate reading noted here was the
  dev server having no seed list *and* no player who had ever joined; the seed list was added three
  commits later precisely because of it, and it works.
- ~~The full-moon Siege~~ — **confirmed in play** (2026-08-16): one fired by itself on a full moon,
  unforced. `moon`, `depth` and `see_sky` had each been proven headlessly in both directions;
  what was missing was the world being on the right moon, and now it has been.
  `/zombiemod horde start zombiemod:the_siege` still forces it regardless of the moon.
- ~~**`summon`'s `max_nearby` cap** under real pressure~~ — **confirmed in play** (2026-08-24). A
  Breeder left running beside an AFK player in observer mode "didn't get out of hand - max
  respected", which is the exact scenario the cap exists for: not a burst, but an unattended spawner
  with an indefinite amount of time to overrun a loaded chunk. Observer mode is what made the test
  clean — the player takes no damage, so the Breeder was never interrupted and never had a reason to
  stop.
- ~~Corpse recovery~~ — **confirmed in play**, face and all (2026-08-16, re-confirmed 17th after the
  fix below). `respawn` rebuilt the corpse with the items, but it came back *bald* — `rebuild` wrote the ledger tag and the pockets but never the
  face, so a rebuilt corpse was not recognisably anybody. Fixed, along with two things sitting beside
  it: the armour was not re-worn either, and the name went through `Component.literal` so a
  colour-coded `Corpse %P` would have shown its codes. The **lava and void cases** are now handled and
  proven headlessly in all three directions: a lava death and a void death leave the entry
  outstanding with a reason recorded, a clean kill still settles it, and an entry written before the
  field existed still decodes. **Ordinary recovery is now confirmed by repeated play**
  (2026-08-24) — killed himself several times and got his things back each time, which is the loop
  most players will actually use. Note this is the *plain* corpse and **not** the decoy scene below:
  an uninfected death raises one zombie, and there is nothing to pick the wrong one from. A
  **grinder** is deliberately left undecidable — a hopper may have taken the items, so re-issuing could duplicate an
  inventory, and that stays an admin's call.
- **Loot tables** — resolution is proven; nobody has watched Patient Zero drop his netherite scrap.
- **Conversion in play.** The guards are tested (a listed victim rises, an unlisted one doesn't, an
  undead one never does, and 40 kills in one tick produce exactly one). Nobody has watched a Carrier
  work through a village, which is the case that matters.
- ~~Infection's two remaining paths~~ — **both confirmed in play** (2026-08-16): bitten then killed
  by something unrelated, and milk clearing it before death.
- **Corpse recovery after killing only the decoy** — the inventory-losing half of this is now
  **closed by construction**, and the rest is cosmetic.

  **First, what the decoy is not.** It is *not* the player corpse — that is the real one, and it is
  the thing you are meant to kill. This entry's old one-line phrasing read as though it were, and did
  in fact mislead once (2026-08-24), so it is spelled out here.

  The scene: die while *infected* and two things get up. Your **corpse**, wearing your face and
  carrying your belongings, and a second zombie raised by the infection itself
  (`infectionAlsoRaises`, on by default). The second one copies your **armour** so it looks the part,
  but at drop chance 0 and with none of your inventory — so killing the wrong one gets you nothing.
  That is the decoy.

  The fear was that killing the decoy might *settle the ledger*, leaving an admin reading "already
  recovered" about an inventory still walking around, and refusing to re-issue it. It cannot: the
  ledger is settled only from `LEDGER_TAG` in the dead mob's persistent data
  (`PlayerZombies.dropCarried`), and the decoy is a fresh mob from `Conversions.raiseInfected` that is
  never given that tag. No tag, no `claim()` and no `lost()` — the entry stays outstanding and keeps
  listing. Read off the code rather than watched in play, but it is a structural argument, not a
  probabilistic one.

  **The pair has now been seen in play** (2026-08-24) and read correctly — "different enough, I
  wasn't confused". Held *provisional* for a reason Sable raised himself and which is worth keeping:
  he knows the mod, so he is the **worst available witness** for a question about whether a stranger
  can tell the two apart. Someone without that frame of reference may simply read it as one zombie
  duplicated. Revisit once people are playing; do not close it on the author's own read.
- ~~Proximity spawning~~ — **confirmed in play**. `nearbyCap = 8` settled 2026-08-16, and the "no
  spot" flood fixed 2026-08-17: "less no spots, mostly at cap, which is fine", which is the counter
  you want to be dominant — it means the ground is full rather than unusable. Two causes were behind
  it: the standing-room test was stricter than vanilla's (a tuft of grass or a snow layer
  disqualified a spot), and the search only ever aimed at the surface heightmap, so a player in a
  cave or a cellar had company placed on the roof of the world above them. Worth recording that the
  probe for this was *inconclusive* — 1681 surface spots in the dev world, old and new rules agreeing
  exactly, because that terrain has none of the cases the fix is for. Play settled what the probe
  could not.
- **Bounty payouts** — the payment itself is confirmed (2026-08-25). What is still open is only the
  scoreboard tally, and whether the *numbers* feel proportionate, which is a matter of feel.
- **Hordes.** One Siege survived, which found both of the gaps now closed. `cap = 40` **settled by
  play** (2026-08-16). "Whether it builds or just arrives" turned out to be a **bug, not a tuning
  question**: every wave inherited the *previous* wave's delay, so a `delay: 0` first wave made the
  second arrive on the next tick and the third one tick after that, and the last wave's delay was
  never read at all. Measured with a FakePlayer: waves at ticks 21 and 22, then nothing for 1378
  ticks. Three waves in three ticks — it never built, and the numbers in every shipped horde described
  something nobody had seen. Fixed and re-measured at +1/+401/+901, matching the JSON.
  **`on_clear` (new)** makes a wave arrive the moment the previous is dead, with the delay as a
  ceiling; proven both ways — wave 3 came at tick 501 when the field was wiped at 500, and at 921 when
  it was not. The retuned numbers are now a genuine first guess rather than a wrong one — and a
  **horde has now been played through end to end** (2026-08-24, "worked great"), which closes the
  last thing the wave-delay fix left open. The measured timings were already proven; what was missing
  was somebody fighting one from the first wave to the victory line.
  `glowAfter = 1200` is settled: in play it fired just as the player was giving up and heading for a
  bell, which is exactly where that threshold wants to sit.
- **Horde counting and chunk unloads.** Survivors are counted by identity now, so distance no longer
  loses them, but a mob in an unloaded chunk still reads as gone and would end the horde early.
  Unlikely at these radii; not impossible if a player runs.
- **The Undertow** (new in 3.1.0). **It swims** — confirmed in play 2026-08-25, but only after
  3.1.1 fixed it; see *Fixed, worth remembering* below, because the first build bobbed at the surface
  and the cause is worth carrying forward. Still unproven: whether blindness plus `pull` in water is
  a good fight or an unfair drowning, and **weight 25**, which is the number most likely to be wrong
  — roughly 38% of drowned spawns once it competes with `vanillaWeight`, chosen deliberately so the
  update is noticeable. The containment is that drowned only spawn in water, so nothing on land
  changes whatever the number turns out to be.
- ~~**Bounties paid through Standards**~~ (new in 3.1.0) — **confirmed in play** (2026-08-25):
  "bountys are paying out". Money reaches a real account through the facade, which closes the last
  unproven step of the feature. The reflective link had already been verified against the shipped
  Standards jar — all three handles resolve, and `isAvailable()` with no provider returns a plain
  `false` rather than throwing — but a reflective call that resolves is not the same as a payment
  that arrives, and only the second one is worth anything to a player.

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

- **`navigation: swim` planned a route and nothing followed it** (shipped in 3.1.0, fixed in 3.1.1).
  The Undertow bobbed at the surface instead of swimming, and there were two independent causes plus
  a structural one.

  The visible cause was **`FloatGoal`**, which calls `JumpControl.jump()` every tick it is in water
  — it pins a mob to the surface and bounces it. The goal list had been copied from the Bogman, which
  is *amphibious* and walks the bottom, where floating is exactly right. **A goal that is correct on
  one genus can be actively wrong on another**, and nothing about the JSON says so.

  The quieter cause was **`random_stroll`**, which picks destinations on land via `LandRandomPos`, so
  a swim navigator was handed places it could not path to and had nowhere to go when idle.

  The structural one is the lesson: **a navigator is only half a movement mode.** `GenusApplier`
  already says so for `climb` — "the navigator plans the climb; ClimbGoal performs it. Neither works
  alone" — and `swim` had only the planning half, with no goal to perform it. It went unnoticed
  because no shipped genus used `swim` until the Undertow. `random_swim` is the missing half.

  Worth knowing for any future swimmer: vanilla `Drowned.wantsToSwim()` is
  `searchingForLand || (target != null && target.isInWater())`, and `searchingForLand` is set only by
  `DrownedGoToBeachGoal`, which `clear_goals: true` removes. So `DrownedMoveControl` does real
  swimming movement while **chasing something in water**, and idle movement is a separate problem —
  which is why the two had to be fixed separately. (Checked and discarded along the way: `Drowned`
  does *not* reassign `navigation` in `updateSwimming()` on 1.21.11, so the `swim` assignment does
  stick.)

- **Command output was unreadable outside the game** (2026-08-24, found by the LegendQuest session).
  `/zombiemod status` and `/zombiemod corpse list` were built with legacy section codes inside the
  string. The client rendered them correctly, so nothing *inside* the game could reveal that a server
  console, the log and RCON were printing the codes as literal text.

  The structural lesson is the useful part, and it is why this was the **third of four ports** to
  have it: you cannot see a representation error while looking through the thing that interprets the
  representation. Standards (`38cb7a0`) and LegendQuest (`dda06b6`) each found their own copy the
  same way — from outside. The rule and the two conversion traps are in CLAUDE.md; the cheap audit is
  `grep -rn '§' src/main/java` and confirming every hit is a comment or client font rendering.

- **`/zm` was op-only, including the subcommands that are deliberately open.** The alias was a
  Brigadier redirect carrying its own `LEVEL_GAMEMASTERS` requirement, and Brigadier checks the
  redirect node's requirement *before* following it — so that bar ANDed with every child, including
  `bestiary` and `list`, which the root node leaves open on purpose. The bar is gone; each subcommand
  keeps its own, so the alias is now exactly as restricted as the full name.

## Left from the 1.8 plugin

Every ability is done, and so is proximity spawning — it is built, configurable and off by default,
waiting only for somebody to turn it on and judge it. **Nothing is outstanding.**

- ~~**Bounty.**~~ **Closed 2026-08-25** — wired on the 24th, confirmed paying in play on the 25th.
  It waited on an economy decision that could not be made: no Vault on NeoForge, and no leader among
  the economy mods to build against. Sable's
  **SableCraft Standards** closes it by shipping a ledger *behind an interface*, so paying through it
  is not choosing its ledger — a dedicated economy mod registers a higher-priority provider and takes
  over, and neither side needs to know the other exists. `compat/StandardsEconomy` is a
  `Bounties.Payer` doing exactly that, reflective and inert without Standards, like FTB Chunks and
  CityWorld. The scoreboard tally still runs alongside it, because payers are additive.

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

## Where this stands, 2026-08-25

**Released and in players' hands.** `3.0.0` shipped 2026-08-18 and `3.1.0` on 2026-08-24; both are
live on CurseForge, and the sablecraft.co.uk pages are up.

Everything that can be settled without an audience has been settled. What is left falls into four
kinds, and none of it blocks anything:

- **Wants an audience, not a session.** Whether the faces read at a glance to somebody who has never
  seen them, and whether the infected pair reads as a decoy rather than a duplication bug. Both look
  right to Sable, and both are held open deliberately — the author knowing what he is looking at is
  exactly what disqualifies him as the witness. Only the public release answers these.
- **Needs a situation to arise.** A Carrier working through a village. Somebody meeting the Undertow
  without knowing what it does.
- **Matters of feel.** Drop rates, the Sleeper's six-block range, the Borg Queen's numbers, and the
  Undertow's weight.
- **Known gap.** A horde survivor in an unloaded chunk still reads as dead and would end the horde
  early. Unlikely at these radii; not impossible if a player runs.

**The 1.8 plugin is now fully accounted for.** Bounty payouts — the last thing outstanding, and the
only item ever on this list that was *stuck* rather than merely unbuilt — pay through SableCraft
Standards as of 3.1.0, and were **confirmed paying in play on 2026-08-25**. Nothing from the original
plugin is missing, unbuilt or unverified. See *Left from the 1.8 plugin* above for why it was stuck
and how Standards resolves it without ZombieMod having to pick an economy.

## Minecraft 26.1 and 26.2

**Both build the whole mod and run**, on branches `mc26.1` and `mc26.2`. **26.2 is confirmed in
play** (2026-08-26): zombies spawning across the roster, the **ZombieDex screen** open and clicking
through entries, and a **player corpse raised wearing its gear**. Then the four highest-risk rewrites, all confirmed the same day:
**saved data survives a restart** (the dex remembers), **glow works** (the Glowing One glows),
**boss bars work** (Patient Zero and a horde bar, which are separate call sites), and
**item components render** (trims, plus the Vault Dweller's yellow-and-blue, which is `armor_color`
rather than a component and so a path of its own). That last one matters most — a
corpse carries the dead player's equipment, so it is the sharpest available test of the deferred
item build, which is the change 26.x forced. 26.1 has been built and run headlessly but not played.

Two bugs surfaced on 26.2 and both are fixed: the dex crashed to desktop on any genus holding an
item (a doll is never added to a level, so it had no entity id, and 26.2's `Entity.getId` throws
where 1.21.11 tolerated it), and the corpse that "did not spawn" was `playerZombies` being off by
default — which the mod's own documentation made hard to check, because it named the wrong config
file. See *Releases* for that correction; it was wrong on every version.

Everything that moved between versions is behind `com.sablednah.zombiemod.platform`, so a version
branch edits one small class per concern rather than the call sites. The full account — the seam
table, the two changes that were not renames, the 26.x GUI rename table, and how to add the next
version in about an hour — is in [MULTIVERSION.md](MULTIVERSION.md). Read that before touching any
of it.

Two things from it worth knowing even if you never build for 26.x, because both changed `master`:

- **Equipment is now deferred.** A genus slot holds a description rather than a built stack, because
  an `ItemStack` cannot be constructed while a datapack registry is loading. A wrong item id is now
  reported once, naming genus, item and slot, and the mob spawns without that slot instead of the
  world refusing to load.
- **Entity types are named through the registry**, not `EntityType`'s constants — which 26.2 removed
  outright. That is the more correct code for a mod whose genera are datapack-defined.

Not done: the client GUI code diverges in three places on 26.2 only, sitting in the two largest
client files, so a `platform/Screens` seam is worth adding before the next dex change. Nothing is
pushed; all three branches are local.

## Releases

**The version lives in `gradle.properties` and nowhere else** — `neoforge.mods.toml` is generated
from it at build time, so never edit the generated file.

| Version | Shipped | What it was |
|---|---|---|
| `3.0.0` | 2026-08-18 | First release of the NeoForge rewrite. 58 genera. |
| `3.1.0` | 2026-08-24 | The Undertow (59 genera), bounties through Standards, the Rusted Warden's shockwave cadence, and section codes gone from command output. |

**Publishing to GitHub publishes to CurseForge**, via `.github/workflows/curseforge.yml`. Proven on
both releases.

**Three things the automation does not do.** Each is manual, and each is invisible when forgotten:

- **The store description.** The workflow pushes *jars only*. When a release changes a number in the
  copy — 3.1.0 moved 58 genera to 59 — the live page goes on describing the previous version until
  `CURSEFORGE.md` is pasted in by hand.
- **The gallery.** Fifteen screenshots, uploaded 2026-08-24, with a captioned order in
  [`../RELEASE.md`](../RELEASE.md).
- **Moderation.** HTTP 200 means *accepted*, not published. CurseForge dedupes by file content, so a
  duplicate is rejected while looking like it never arrived — rejected files are hidden from the
  authors list by default. The authoritative view is
  `https://authors.curseforge.com/#/projects/1658560/files`, not the public Files tab.

**The one store still missing is Modrinth** — checked 2026-08-24; the project does not exist (API
404, no search hits). The recipe and its two expensive traps are in [`../RELEASE.md`](../RELEASE.md):
the icon must be `docs/main-logo-icon.png`, because Modrinth caps icons at 256 KiB and the lockup is
1.4 MB; and `client_side`/`server_side` are marked deprecated in favour of an `environment` field
that **does not exist on v2**, so the deprecated pair is still what you must send. Environment is
Server **Required**, Client **Optional** — the field people filter on, and the costliest to get
wrong.

### The materials, and what is deliberately not in them

Four files in the **repo root**, matching `../CityWorld-ReForged`:

| File | What it is |
|---|---|
| `CURSEFORGE.md` | The store description — covers everything, links out for depth. Used for Modrinth too; do not fork it. |
| `CURSEFORGE-CONFIGURATION.md` | Every setting in all nine config sections. |
| `CURSEFORGE-COMMANDS.md` | Every command. |
| `RELEASE.md` | Every store field, the gallery order, and the publishing traps. |
| `WEBSITE.md` | Handover to the sablecraft.co.uk session. |

**Two deliberate departures, both for the same reason — a second copy drifts.**

The genus reference (~900 lines of `README.md`) is **not** copied into a `CURSEFORGE-*.md`; the site
builds those pages from README sections instead. CityWorld could copy because its equivalent was
small.

The balance model lives in [BALANCE.md](BALANCE.md) and **not here**. It was duplicated in this file
and the copies had already begun diverging — the same failure that made every count in this document
wrong once. BALANCE.md carries the rules, the deliberate exceptions (the Coward's bounty is *bait*
and must not be normalised), and the near-miss where Colossus and Rusted Warden were nearly
"corrected" against the wrong comparator.

### Artwork

`docs/main-logo.png` is the square CurseForge icon (1035×1035), `docs/slime-logo-850.png` the banner
at CurseForge's 850px description-image limit, `docs/main-logo-icon.png` the 82 KB Modrinth-legal
icon, and `night-`/`Stone-`/`survival-logo.png` are variants held back for updates and themed events.

All of them arrived with a **magenta chroma-key background rather than alpha**, which would have
shown as a solid magenta square wherever they were used. Keyed out on the magenta-ness axis
(`min(R,B) - G`, since the key colour was not uniform) with a despill on the ramp, then trimmed.

### The 1.8 source tree

**Removed at the 3.0.0 release**, along with the Bukkit-era `config.yml`, `lang.yml` and
`plugin.yml`; `src/` now contains only `main/`. The LICENSE carve-out was **rewritten rather than
deleted**: the MIT grant now covers the whole repository, but the note stays on the record because
the removed tree was CC BY-NC-ND with third-party contributions and is still reachable in git
history, so anyone who recovers it needs those terms. CLAUDE.md carries the
`git log --diff-filter=D` recipe for reading it again.

## Next, in the order I'd do it

1. **Watch the Undertow meet somebody.** It is the headline of 3.1.0, it has never been played, and
   its weight is a first guess.
2. **Modrinth.** The last unticked box on the release list.
3. **Proximity in survival.** Enabled in Sable's instance; the cap semantics are settled ("quiet
   place top up is perfect" — 2026-08-15). What remains is a survival session on quiet ground
   watching it fire, and whether `nearbyCap = 8` feels like atmosphere.
4. **Spawn density** via `neoforge:add_spawns` biome modifiers. Example in
   [`examples/add_spawns_biome_modifier.json`](examples/add_spawns_biome_modifier.json), deliberately
   not enabled.


## Known limitations

- **Goal targets** come from a fixed name→class map (`TargetClass`), because vanilla's targeting goals
  are typed on a Java class rather than an entity id. "Avoid wolves" works; "avoid a modded mob" does
  not.
- **A malformed genus stops the world loading** rather than being skipped. Standard datapack-registry
  behaviour, harsher here because these files are hand-written. The log names the file and field.
- **Renderer-bound effects can't be given to arbitrary mobs.** Sometimes there's a way round — the
  beam parents a guardian, the creeper swell is approximated with `scale`, the bow draw needs a
  skeleton base — and sometimes there isn't. See [TROPES.md](TROPES.md).
