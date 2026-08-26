# ZombieMod — settings reference

Everything in `zombiemod-server.toml`. The zombie types themselves are **not** here — they are
datapack files at `data/<pack>/zombiemod/genus/<name>.json`. See the genus reference for those.

Source of truth: `src/main/java/com/sablednah/zombiemod/ZombieModConfig.java`. Keep this file in
step with it.

## Where the file lives

It is a **server** config. There is one file, and an optional per-world override:

| | |
|---|---|
| The settings | `config/zombiemod-server.toml` — edit this one |
| One world only | copy it to `saves/<world>/serverconfig/zombiemod-server.toml` |

That is NeoForge's arrangement, described in the `readme.txt` it leaves in every world's
`serverconfig/` folder: a file placed there **overrides** the one in `config/` for that world. The
override does not exist until you put it there — an empty `serverconfig/` folder is normal and means
the world is using the settings in `config/`.

*(Earlier versions of this page had it backwards, claiming the `config/` copy did nothing in
singleplayer. It sent people to edit a file that was never there.)*

This catches everyone at least once. If a setting appears to do nothing, run **`/zombiemod status`**
first — it reports what the mod actually believes, which is the fastest way to find out you have been
editing the wrong file.

Nine switches can be flipped in-game without touching the file at all — see `/zombiemod config` in
the commands reference.

---

## `[spawning]`

| Setting | Default | What it does |
|---|---|---|
| `enabled` | `true` | Master switch. Off means every mob spawns exactly as vanilla would. |
| `vanillaWeight` | `40` | How strongly to leave a mob alone, weighed against the genera that could claim it. |
| `logSpawns` | `false` | Log every genus spawn to the console. Noisy; for tuning weights. |
| `builtinGenera` | `true` | Let the genera shipped with the mod claim spawns. |

### `vanillaWeight`, the one that matters

"Leave it as a plain zombie" is an ordinary entry in the same weighted draw as every genus. If your
genera total 200 and this is 200, roughly half of all zombies stay vanilla. Set it to `0` and a
genus claims every eligible spawn.

What the shipped genera add up to depends on **where you are standing**, because most of them carry
spawn conditions. Measured in a fresh world at the default of 40:

| Where | Genera eligible | Their weight | Stay vanilla |
|---|---|---|---|
| Surface, at night | 9 | 114 | 26% |
| 20 blocks down | 31 | 254 | 14% |
| 45 blocks down | 35 | 266 | 13% |

The default is deliberately low. If this mod is installed you should notice, and a plain zombie is
what you get when nothing more interesting turned up — not the house style. It is not zero either:
some ordinary dead is what makes the rest read as unusual.

The table also *understates* how ordinary a crowd looks, because Walker is itself a near-vanilla
shambler at weight 35. Between the two, about half of what you meet on the surface is still just a
zombie.

### `builtinGenera`

Turn this off to run a pack of your own and nothing else. It affects the **three weighted draws** —
natural spawns, horde waves that do not name their genera, and what a conversion raises — and
nothing else. The shipped genera stay whole and usable by `/zombiemod spawn`, spawners, rituals and
anything that names one explicitly. Your own datapack's genera are unaffected either way.

It is deliberately a switch rather than a "reset" datapack setting every shipped genus to weight 0.
Such a pack has to list every genus by name, so it goes quietly out of date the moment a new one
ships — and starts letting through exactly the thing it was installed to stop.

To silence a *single* genus instead, override that one file from a higher-priority datapack with
`"weight": 0`.

---

## `[playerZombies]`

When a player dies, their corpse gets up. **Off by default** — this takes a player's dropped items
and puts them inside a mob, which is a real gameplay decision and not one to make on someone's
behalf just because they installed a mob pack.

| Setting | Default | What it does |
|---|---|---|
| `enabled` | `false` | Raise a zombie wearing the dead player's skin at their death spot. |
| `takeItems` | `true` | The corpse carries what the player dropped; kill it to get your things back. Off and the items drop normally, leaving the corpse as a monument. |
| `genus` | `zombiemod:player_zombie` | Which genus a corpse uses as its template. |
| `name` | `Corpse %P` | Corpse name. `%P` is the player's name. |
| `infectionAlsoRaises` | `true` | A player who dies infected raises **both**: the corpse with their face and belongings, and a second zombie from the infection itself. |

`infectionAlsoRaises` is a decoy mechanic — only the corpse has the loot, so killing the wrong one
gets you nothing. Set it false if you would rather one death meant one zombie.

Admins can recover a corpse's items with `/zombiemod corpse` when it falls in lava or the void; the
ledger records why an entry is still outstanding.

---

## `[claims]` — FTB Chunks

**All of this is inert if FTB Chunks is not installed.** Linked by reflection, so there is no
version pin and no hard dependency.

| Setting | Default | What it does |
|---|---|---|
| `respectClaims` | `true` | Respect FTB Chunks claims at all. |
| `noGriefingInClaims` | `true` | Stop ZombieMod's mobs breaking or placing blocks inside claims. |
| `inClaims` | `VANILLA_ONLY` | What happens to spawns inside a claim. |

`inClaims` takes one of:

| Value | Meaning |
|---|---|
| `ALLOW` | No special treatment. |
| `VANILLA_ONLY` | Genera never claim a spawn there, so you get ordinary mobs. |
| `NO_SPAWNS` | Cancel the spawn entirely. |

`VANILLA_ONLY` is the default because keeping ZombieMod out of someone's base is this mod's
business, while emptying it of vanilla mobs is not.

**A claim is not a safe zone**, and this reads as a bug the first time. Mobs spawn outside a claim,
walk in, and aggro normally — which is correct. Only *spawning* is affected.

Worth knowing why the griefing setting exists at all: FTB Chunks protects explosions inside claims
but does not cover general mob block-breaking, so NeoForge's griefing hook gets no answer from it and
a claim does nothing against a Breaker. ZombieMod closes that from its own side.

---

## `[proximity]`

Put zombies just out of sight around each player, instead of only riding vanilla's spawn table. This
is the 1.8 plugin's `ProximitySystems`, and it is what made that world feel *occupied* rather than
merely populated.

**Off by default.** It is the one feature here that adds mobs beyond what vanilla would have made,
and installing a mob pack should not silently change how many things are hunting you.

| Setting | Default | Range | What it does |
|---|---|---|---|
| `enabled` | `false` | | Spawn genera near players regardless of vanilla's spawn table. |
| `interval` | `100` | 20–12000 | Ticks between attempts, per player. 100 is five seconds. |
| `chance` | `0.5` | 0–1 | Chance an attempt spawns anything at all. |
| `minDistance` | `16` | 4–128 | Closest it will spawn to a player. |
| `maxDistance` | `32` | 8–128 | Furthest it will spawn from a player. |
| `nearbyCap` | `8` | 1–200 | Stop once this many ZombieMod mobs are already near the player. |
| `outOfSightOnly` | `true` | | Only spawn where the player cannot currently see the spot. |

`nearbyCap` is the one number that decides whether this is atmosphere or a siege. `outOfSightOnly`
matters more than it looks: the point is that they were always there, not that they appeared.

Note that proximity spawning **ignores a genus's base-mob match**, so a giant or golem genus given a
weight above 0 will turn up on the landscape whatever vanilla would have done.

---

## `[bounty]`

What killing a genus is worth. A genus carries a number; who pays it is a separate question.

| Setting | Default | What it does |
|---|---|---|
| `enabled` | `true` | Pay out bounties at all. |
| `objective` | `zombiemod.bounty` | Scoreboard objective to tally into. Blank to disable. |
| `announce` | `true` | Show the payout on the killer's action bar. |

There is **no Vault on NeoForge** — no abstraction every economy mod implements — so ZombieMod does
not pick one for you. An economy adapter registers itself and gets called.

**With [SableCraft Standards](https://github.com/Sablednah/SableCraft-Standards) installed there is
nothing to wire up:** bounties are paid into whichever economy that server is running, because
Standards holds the money behind an interface rather than insisting it *is* the economy. Inert
without it. `/zombiemod status` tells you how many payers answered, which is the first thing to check
if a bounty pays nobody.

With no economy mod at all the scoreboard is the fallback, which is a real reward on a vanilla server
rather than a number waiting for a dependency. The objective is only used if it already exists, so
nothing appears on a server that never asked for it. Opt in with:

```
/scoreboard objectives add zombiemod.bounty dummy
/scoreboard objectives setdisplay sidebar zombiemod.bounty
```

---

## `[hordes]`

Nights when several of them arrive together. Everything else in this mod is an encounter — one
monster, met on its own terms. A horde is the layer above, and it is **waves** rather than a number
on purpose: twenty at once is a wall, while eight then twelve then twenty is a story with a middle.

**Off by default**, for the same reason as proximity spawning. Start one by hand with
`/zombiemod horde start` regardless.

| Setting | Default | Range | What it does |
|---|---|---|---|
| `enabled` | `false` | | Let hordes start on their own. |
| `checkInterval` | `600` | 100–24000 | Ticks between checks, per player. |
| `chance` | `0.08` | 0–1 | Chance a check starts one. |
| `cooldown` | `36000` | 0–1000000 | Minimum ticks between hordes for the same player. 24000 is a full day. |
| `cap` | `40` | 4–300 | Never have more than this many horde mobs alive at once. |
| `bellGlow` | `true` | | Ringing a bell makes nearby horde mobs glow. |
| `bellRadius` | `48.0` | 8–256 | How far a bell reaches. 48 is what vanilla uses for raiders. |
| `glowAfter` | `1200` | 0–24000 | Once the last wave is out, make survivors glow after this many ticks without a kill. 0 disables. |
| `glowDuration` | `200` | 20–6000 | How long that glow lasts. Refreshed while stalled. |

`cooldown` is doing more work than it looks — rarity is most of what makes one memorable.

`cap` is the safety rail: a wave that cannot place its full count simply places fewer.

`glowAfter` is measured from the **last kill**, not from the start, deliberately. A long fight you
are winning is not the problem; hunting one straggler across a dark forest is. 1200 is tuned rather
than guessed — in play it fires at about the moment a player gives up searching and starts walking
back to a bell.

`bellGlow` is our own implementation of something vanilla already does for raids. The gesture is
already learned, but vanilla's version is hard-gated on the `#minecraft:raiders` entity tag and every
method in the path is private.

---

## `[infection]`

A bite from a genus with the `infect` ability marks whatever it hits, animals included, and a marked
thing rises when it dies whatever killed it. These settings are what lets it **carry on** — an
infected cow has no genus, so it has no abilities, so without them the chain stopped at one animal.

The intent is that letting one infected animal near a herd is a mistake you get to watch unfold and
still do something about — not an event you are simply told about. Milk is the something.

| Setting | Default | Range | What it does |
|---|---|---|---|
| `spread` | `true` | | Let infected mobs infect their neighbours. |
| `interval` | `200` | 20–24000 | Ticks between one infected mob's attempts to pass it on. |
| `chance` | `0.25` | 0–1 | Chance an attempt succeeds. |
| `radius` | `4.0` | 1–32 | How close is too close, in blocks. |
| `toPlayers` | `true` | | Whether standing too near an infected animal can infect you. |
| `milkCure` | `true` | | Right-click an infected animal with a milk bucket to cure it. |

With the defaults, roughly one new case every 40 seconds per infected animal.

`toPlayers` is what makes a tainted herd genuinely dangerous rather than merely a loss of livestock.
Milk cures you exactly as it does after a bite — the cure is "the marker effect is gone", and
drinking milk clears effects. `milkCure` is the same cure aimed at something that cannot drink it
itself.

---

## `[bestiary]` — the ZombieDex

Who has met what, and who has killed what. The record is kept in the world's saved data and is
complete whether or not any of it is mirrored to a scoreboard — so turning the per-genus view on
later shows a history that was being kept all along, rather than starting from zero.

| Setting | Default | What it does |
|---|---|---|
| `enabled` | `true` | Record kills and encounters at all. |
| `perGenus` | `false` | Also keep one objective per genus, `zombiemod.<genus>`, holding your kill count. |
| `info` | `MET` | How much you have to have done to read a genus's write-up. |
| `hideUnspawnable` | `false` | Leave weight-0 genera out of the roster entirely. |
| `unspawnableRevealedWhenMet` | `true` | …but let a hidden weight-0 genus appear once a player has met it. |
| `hidden` | `[]` | Genus ids left out of the roster regardless of weight. |
| `hiddenRevealedWhenMet` | `[]` | The subset of `hidden` that appears once met. |

`info` takes one of:

| Value | Meaning |
|---|---|
| `MET` | Anything you have traded damage with. |
| `KILLED` | Only what you have actually put down. |
| `ALWAYS` | Everything, from the first login. |

`MET` is the default because the entry is a reward for the encounter, and a checklist you can read
cover to cover before meeting anything is not much of a checklist.

Two objectives are always kept and both are created on demand:

```
zombiemod.slain    total kills
zombiemod.genera   how many DISTINCT genera you have killed

/scoreboard objectives setdisplay sidebar zombiemod.genera
```

`perGenus` is off by default because it costs a scoreboard row per genus per player, and every row
syncs to every client — fifty-eight genera on a busy server is a lot of packets for a checklist. On a
small server it is nothing, which is who it is for.

`hideUnspawnable` turns bosses and summon-only genera from spoilers in a checklist into discoveries.
With `unspawnableRevealedWhenMet` on, hiding them is a surprise rather than a hole — the dex grows
when the boss does.

`hidden` is for the ones a server wants to stay rumours, e.g. `["zombiemod:herobrine"]`. Anything
hidden and **not** listed in `hiddenRevealedWhenMet` never shows in the dex at all, met or not — a
true secret, which is a strong choice: a player can kill it and find no record.

---

## `[ghost]`

The genus that wears somebody else's face. It needs names, and there is no way to ask the server for
every profile it has ever seen — the profile cache answers questions but will not enumerate. So
there are **two sources, added together**: this seed list, and everyone who logs in.

| Setting | Default | What it does |
|---|---|---|
| `names` | `["jeb_", "Sablednah"]` | Always available, whether or not they have ever played here. |
| `rememberLogins` | `true` | Remember players as they log in. |
| `rememberDays` | `90` | Forget a player who has not logged in for this many days. 0 never forgets. |
| `skipBanned` | `true` | Never wear a banned player's face. |

A brand new server has nobody in the second list, so **without a seed list the Ghost is faceless
until somebody has played**. That is what `names` is for. Names are resolved against Mojang, so they
render for real on a vanilla client.

`rememberDays` is measured in real days, not game days, because the thing being remembered is a
person rather than anything that happens in the world.

`skipBanned` is checked against the live ban list every time a face is picked, rather than when the
ban happens — so it covers bans applied from the console or while the server was down, and
un-banning somebody quietly puts them back in the pool.

Two names are best avoided: vanilla renders any entity named **Dinnerbone** or **Grumm** upside-down,
and a Ghost wears the name as well as the face. The mod already declines to *name* a Ghost either of
those, so it keeps the face and stays the right way up.
