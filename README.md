# ZombieMod ReForged

*28 Dead Resident Shaun's of Evil Later.*

Build your own undead. ZombieMod turns zombie types into **datapack files** — health, size, colour,
and above all **AI** — so a cowardly zombie that flees on sight, a stalker that watches you from
across the valley, or a climber that comes over the wall are all just JSON.

A modern **NeoForge** rewrite of the 2013 [ZombieMod](https://github.com/Sablednah/ZombieMod) Bukkit
plugin.

| | |
|---|---|
| Minecraft | 1.21.11 |
| Loader | NeoForge 21.11.42+ |
| Java | 21 |
| License | MIT |
| Side | **Server-side only** — players do not need the mod installed |

> **Alpha.** The genus format, AI, spawning and abilities all work. See [Status](#status) for what
> is still missing.

## Install

Drop `zombiemod-<version>.jar` into `mods/` on the server. That's it — no dependencies, and nothing
to install on the client.

## Why vanilla clients work

ZombieMod registers **no entity types of its own**. A vanilla client can't draw a mob it has never
heard of, so instead a *genus* is a set of changes applied to an ordinary vanilla mob the moment it
spawns. Everything a player sees — size, colour, name, behaviour — is expressed through things the
vanilla client already knows how to render.

That's not a compromise; it's the same trick the 2013 plugin used, which swapped its classes in under
vanilla's entity ids precisely so clients saw a normal zombie. This just does it without reflection.

## Writing a genus

Drop a JSON file in a datapack at `data/<your_pack>/zombiemod/genus/<name>.json`. Run `/reload` and
it's live — genera are a datapack registry, so vanilla's own reload picks them up.

Here is the coward, in full:

```json
{
  "name": "Coward",
  "base": "minecraft:zombie",
  "weight": 30,

  "health": 14.0,
  "damage": 1.0,
  "speed": 1.25,
  "follow_range": 24.0,
  "scale": 0.9,
  "armor_color": 11003600,

  "clear_goals": true,
  "goals": [
    { "type": "zombiemod:float",         "priority": 0 },
    { "type": "zombiemod:avoid_entity",  "priority": 1, "target": "player", "distance": 12.0, "sprint_speed": 1.5 },
    { "type": "zombiemod:random_stroll", "priority": 7, "speed": 0.9 },
    { "type": "zombiemod:look_at",       "priority": 8, "target": "player", "distance": 16.0, "probability": 0.35 },
    { "type": "zombiemod:random_look",   "priority": 9 }
  ]
}
```

### Genus fields

| Field | Default | Meaning |
|-------|---------|---------|
| `name` | *(none)* | Display name, shown when you look at it. Omit for an anonymous mob. |
| `base` | `minecraft:zombie` | Which vanilla mob to dress up. Any entity type that is a `Mob` — see [what a base can be](#what-a-base-can-be). |
| `weight` | `0` | Relative spawn frequency against other genera on the same base mob. `0` = never spawns naturally, command only. |
| `health` | *vanilla* | Max health. |
| `damage` | *vanilla* | Attack damage. |
| `speed` | `1.0` | Movement speed **multiplier** on the base mob, as in the old configs — `1.25` is 25% quicker than a zombie. |
| `follow_range` | *vanilla* | How far away it notices you (the old `agro`). Vanilla is 16; **this is the knob for a monster that ignores you until you are close**. |
| `scale` | `1.0` | Body size multiplier, `0.0625`–`16`. Giants and tiddlers cost nothing. |
| `armor_color` | *(none)* | Dyes a full set of leather armour this RGB colour — the cheapest way to tell genera apart. |
| `clear_goals` | `true` | Throw away the vanilla AI before adding yours. Set `false` to *add* behaviour to a normal zombie. |
| `goals` | `[]` | What it does. |
| `target_goals` | `[]` | Who it picks a fight with. |
| `spawn` | *(anywhere)* | Where and when it may appear — see below. |
| `abilities` | `[]` | Things it does repeatedly while alive — see below. |
| `attributes` | `{}` | Any other attribute by id, e.g. `{"minecraft:armor": 8.0}`. Covers everything the named fields don't, including other mods' attributes. |
| `head` | *(none)* | A player head to wear — `"head": "Notch"`, or the full profile form with an explicit texture. Beats `armor_color` for the head slot. |
| `boss` | *(none)* | Present makes this a boss — see below. |
| `phases` | `[]` | Stages that open up as it's worn down — see below. |
| `loot` | *(none)* | `{ "table": "<id>", "replace": false }` — genus-specific drops. |
| `bounty` | *(none)* | What killing it is worth — see below. |
| `xp` | *vanilla* | Experience dropped on death. A Tank worth the same 5xp as a stray zombie is a strange reward for a two-minute fight. |
| `behaviours` | `[]` | Goal sets that switch on and off with a condition — see below. |
| `ghost` | `false` | Take the name and face of a real player — a config seed list plus everyone who has logged in. See below. |
| `mount` | *(none)* | Something to ride in on — the old `jockey` field. |
| `navigation` | `default` | `climb` makes it scale walls like a spider, `swim` and `amphibious` the aquatic ones. |
| `equipment` | `{}` | Held and worn items — see below. Beats `armor_color` and `head` for any slot it names. |
| `invisible` | `false` | Render nothing but the equipment — armour walking around on its own. |
| `baby` | `false` | The vanilla baby variant: half size, quicker, and its own proportions rather than a shrunken adult. |
| `burning` | `false` | Permanently alight, harmlessly. |
| `arrows` | `0` | Arrows left sticking out of it. |
| `glow` | *(none)* | An outline colour — any of the 16 — visible through walls. |
| `villager` | *(none)* | `{ "profession": ..., "type": ... }` for a `zombie_villager` base. |

### Equipment

```json
"equipment": {
  "mainhand": "minecraft:iron_sword",
  "offhand": "minecraft:shield",
  "head": "minecraft:iron_helmet",
  "chest": "minecraft:chainmail_chestplate",
  "legs": "minecraft:chainmail_leggings",
  "feet": "minecraft:iron_boots",
  "drop_chance": 0.0
}
```

Each slot takes either a bare item id or a full stack with components, so a genus can carry
something enchanted, renamed, trimmed or dyed:

```json
"mainhand": {
  "id": "minecraft:iron_axe",
  "components": {
    "minecraft:enchantments": { "minecraft:sharpness": 2 },
    "minecraft:custom_name": "\"Pry Bar\""
  }
}
```

`drop_chance` defaults to **0** for every slot. Kitting a genus out shouldn't turn it into a loot
piñata, and a farmable diamond-armour zombie is an economy bug rather than a feature.

Appearance cascades broad → specific: `armor_color` dresses all four armour slots, `head` replaces
the helmet, `equipment` overrides whichever slots it names.

Note that a mob wearing *any* helmet doesn't burn in daylight — vanilla behaviour, and it applies to
the dyed leather set too, so most genera survive the morning without asking.

**Trims are the largest untapped space here** and cost nothing but JSON — 18 patterns × 16 materials
on any trimmable piece, layering over the dye:

```json
"chest": { "id": "minecraft:iron_chestplate",
           "components": { "minecraft:trim": { "pattern": "minecraft:rib", "material": "minecraft:copper" } } }
```

Outrider wears rib-on-copper, so a skeleton seems to show through the plate; Juggernaut has
sentry-on-netherite. `"minecraft:enchantment_glint_override": true` shimmers a weapon with no
enchantment behind it, which is a warning a player reads instantly.

### Two traps worth knowing about

Both of these look like the obvious way to do the thing, and both kill the mob wearing them.

**Burning does not use `remainingFireTicks`.** That sets the mob genuinely on fire, and a zombie is
not fire-immune, so a permanently-burning genus built that way dies of its own costume. `burning`
sets `hasVisualFire` instead — display-only, and saved by vanilla, so it needs no upkeep either.
That field is private, hence the access-transformer line.

**There is no `frozen`, and that is deliberate.** `setTicksFrozen` past the threshold buys a shiver
animation and a speed penalty — *not* the ice-blue skin it sounds like; the only blue tint vanilla
draws is the player's own frost vignette. It also charges a point of freeze damage every forty ticks.
An effect nobody would notice, for a damage-cancelling hook and a slow death.

### Which villager it used to be

The best variety-per-line in the mod. A `zombie_villager` base plus one field picks from seven biome
styles and a dozen-odd professions — roughly ninety looks, every one a texture a vanilla client
already ships:

```json
"base": "minecraft:zombie_villager",
"villager": { "profession": "minecraft:cleric", "type": "minecraft:swamp" }
```

Townsfolk, Apothecary and Field Hand use it. None of them sets `head` — the helmet slot would cover
the very face being chosen. They ride vanilla's own 5% zombie-villager spawn chance, so they stay
uncommon without needing a low weight.

### Glow

`glow` puts the mob on a scoreboard team and turns on its `Glowing` tag, because a team colour is
the only thing vanilla consults for an outline. Glowing One is `green` — a radioactive thing that
announces itself from across a cave.

Use it sparingly: an outline is visible **through walls**, so every one of these is a monster the
player can never be surprised by. One genus out of fifty is about right.

### Spawning

A genus with a `weight` above zero can claim a spawn that its `base` mob was going to make anyway.
Which genus gets it is a weighted draw — and **"leave it as a plain zombie" is an entry in that same
draw**, weighted by `vanillaWeight` in the config. Without that, the moment you shipped one genus it
would claim every zombie in the world and plain zombies would quietly cease to exist.

So with `vanillaWeight = 200` and genera weighted 30 and 10, any eligible zombie spawn is a draw from
200 + 30 + 10.

**What the shipped genera add up to depends on where you are standing**, because most of them carry
spawn conditions. Measured in a fresh world rather than added up by hand:

| Where | Genera eligible | Their weight | Stay vanilla |
|---|---|---|---|
| Surface, at night | 9 | 114 | 26% |
| 20 blocks down | 31 | 254 | 14% |
| 45 blocks down | 35 | 266 | 13% |

**The default of 40 is deliberately low.** If this mod is installed you should notice; a plain zombie
is what you get when nothing more interesting turned up, not the house style. It isn't zero either —
some ordinary dead is what makes the rest read as unusual.

That table also *understates* how ordinary a crowd looks, because Walker is itself a near-vanilla
shambler at weight 35. Between the two, about half of what you meet on the surface is still just a
zombie. Raise `vanillaWeight` for a mostly-vanilla world, drop it to 0 and a genus claims every
eligible spawn.

The optional `spawn` block narrows where a genus is eligible. Omit it and the genus can appear
anywhere its base mob does.

```json
"spawn": {
  "reasons": ["natural", "spawner"],
  "conditions": [
    { "type": "zombiemod:dimension", "dimensions": ["minecraft:overworld"] },
    { "type": "zombiemod:biome", "biomes": "#minecraft:is_forest" },
    { "type": "zombiemod:light", "max": 7 },
    { "type": "zombiemod:height", "max": 62 }
  ]
}
```

**Conditions are ANDed** — every one must pass. `reasons` defaults to `natural`,
`chunk_generation` and `spawner`; that's deliberately narrow, because a genus riding `conversion`
would replace drowning zombies and cured villagers, and one riding `reinforcement` could summon a
horde of itself.

| Condition | Options |
|-----------|---------|
| `zombiemod:biome` | `biomes` — a tag (`"#minecraft:is_swamp"`) or a list of ids |
| `zombiemod:dimension` | `dimensions` — list of dimension ids |
| `zombiemod:height` | `min`, `max` — either may be omitted |
| `zombiemod:light` | `min`, `max` — light at the spawn point, so it follows day/night outdoors |
| `zombiemod:see_sky` | `value` (default `true`) — open sky above, or deliberately not |
| `zombiemod:depth` | `min`, `max` — blocks below the local surface. 0 in a field, a few under your own roof, hundreds in a cave |
| `zombiemod:time` | `phase` (`day`/`night`), or `min`/`max` on the 24000-tick cycle |
| `zombiemod:moon` | `phases` — any of vanilla's eight, e.g. `["full_moon"]` |
| `zombiemod:in_claim` | `value` (default `true`) — inside an FTB Chunks claim. Always `false` without FTB. |
| `zombiemod:any_of` | `conditions` — passes if any nested condition passes |
| `zombiemod:not` | `condition` — inverts one |

Conditions are a **registry**, not a fixed list, so another mod can contribute its own types via
`SpawnConditionTypes.register`. That's how CityWorld integration is planned to work — a
`cityworld:lot` condition letting a genus prefer the wilderness or a particular district, as an
optional dependency that ZombieMod itself never links against.

### Behaviours — day/night and other switching

A genus's plain `goals` are always active. `behaviours` add goals that switch on and off underneath
them, gated by the **same conditions that gate spawning**:

```json
"behaviours": [
  {
    "when": { "type": "zombiemod:time", "phase": "day" },
    "goals": [
      { "type": "zombiemod:avoid_entity", "priority": 2, "target": "player", "distance": 8.0 }
    ]
  },
  {
    "when": { "type": "zombiemod:time", "phase": "night" },
    "goals": [ { "type": "zombiemod:melee_attack", "priority": 3, "speed": 1.15 } ],
    "target_goals": [ { "type": "zombiemod:nearest_target", "priority": 2, "target": "player" } ]
  }
]
```

That's the Nightstalker: it flees from you in daylight and hunts you after dark. Any condition works,
so "hostile only in the wilderness" or "docile in this biome" are the same shape.

There's a new `zombiemod:time` condition for this — `{"phase": "day"}` or `{"phase": "night"}`, or an
explicit `min`/`max` on the 24000-tick cycle. It's deliberately distinct from `light`: light asks *is
it dark here*, which is true in a cave at noon; time asks *is it night in this world*, which is true
in a lit room at midnight. Spawning usually wants light; behaviour switching usually wants time.

Nothing is rebuilt when the condition flips — both sets are registered up front and each goal is
gated through `canUse`, which is what the goal system is already for.

### Goal types

Each goal takes a `priority` (**lower runs first**, as in vanilla) plus its own options.

| Type | Options | Does |
|------|---------|------|
| `zombiemod:avoid_entity` | `target`, `distance` (8.0), `walk_speed` (1.0), `sprint_speed` (1.35) | Runs away. **This is the coward.** |
| `zombiemod:melee_attack` | `speed` (1.0), `follow_unseen` (false) | Walks up and hits its target. |
| `zombiemod:bow_attack` | `speed`, `interval`, `range` | Draws and looses a bow, using vanilla's own goal. See the note below. |
| `zombiemod:look_at` | `target` (player), `distance` (8.0), `probability` (0.02) | Watches. At high range and probability, **this is Herobrine**. |
| `zombiemod:random_stroll` | `speed` (1.0) | Wanders. Without it, an idle mob stands perfectly still. |
| `zombiemod:seek_blocks` | `blocks`, `speed`, `range`, `vertical_range`, `only_when_idle` — walk to blocks it has an opinion about |
| `zombiemod:random_look` | — | Idle head movement. Cheap, but its absence reads as "broken". |
| `zombiemod:float` | — | Swims instead of sinking. |
| `zombiemod:nearest_target` † | `target`, `must_see` (true), `unseen_memory` (60) | Picks a victim. `unseen_memory` is how many ticks it holds a target it can't see. |
| `zombiemod:hurt_by_target` † | — | Fights back when struck. |

† Belongs in `target_goals`, not `goals`.

`target` is one of: `player`, `living`, `mob`, `monster`, `animal`, `villager`, `zombie`, `wolf`,
`ocelot`, `cat`.

### Climbing

`"navigation": "climb"` does two things, and it needs both. `WallClimberNavigation` lets the
pathfinder route straight up a wall — but *executing* that path needs `onClimbable()` to be true,
which is how the spider does it: it overrides `onClimbable` to return a climbing flag set from
`horizontalCollision` each tick. A vanilla zombie's `onClimbable` only answers for ladders and vines,
so the navigator alone plans a climb the mob can't perform and it stands at the bottom of the wall.

So a climbing genus also gets a goal that pushes it upward while it's pressed against something —
same result, no mixin. Like a spider, it climbs whenever it collides, target or not.

### Bows, and why the base mob matters

`bow_attack` gives real bow behaviour — nock, draw, hold, release — but whether it *looks* right is
a renderer question, and the answer is narrow:

```java
// AbstractSkeletonRenderer — the only humanoid renderer with this branch
state.isAggressive() && state.getMainHandItem().is(Items.BOW) ? ArmPose.BOW_AND_ARROW : ...
```

`AbstractZombieRenderer` has no bow case, so **a zombie with a bow will fire arrows perfectly and
never draw the string.** For the animation, give the genus a skeleton `base` — which is what the
shipped Archer does.

The goal also only applies to mobs vanilla considers ranged (`Monster & RangedAttackMob`):
skeletons, strays, bogged, drowned, witches, illusioners. On anything else it's skipped with a log
line, like any goal that doesn't fit.

For a ranged *zombie*, use the `projectile` ability instead — no draw animation, but it works on
anything and can fire whatever you like. That's what Spitfire does with fireballs.

### A note on goals

The point of the original mod was that its zombies borrowed Minecraft's *own* pathfinder goals and
recombined them into new creatures. That's what this format is for. A genus with only `look_at` and
no movement goals will stand dead still and track you with its head. One with `avoid_entity` on
`player` runs. One with `melee_attack` plus `nearest_target` behaves like a normal, angry zombie.

Same engine, entirely different monsters, no code.

### Abilities

Goals decide where a zombie *goes*. Abilities are what it *does* — the 1.8 plugin's per-tick tricks,
now declared per genus with their own timing.

```json
"abilities": [
  { "type": "zombiemod:particles", "interval": 20, "particle": "minecraft:sneeze" },
  { "type": "zombiemod:heal",      "interval": 60, "amount": 1.0 },
  { "type": "zombiemod:effect",    "interval": 60, "chance": 0.5, "target": "nearby_players",
    "effect": "minecraft:poison", "duration": 80, "radius": 4.0 },
  { "type": "zombiemod:explode",   "interval": 20, "power": 2.5, "trigger_radius": 2.5 }
]
```

Every ability takes `interval` (ticks between attempts, 20 = one second) and `chance` (0–1).
First firings are staggered per mob, so a horde that spawned together doesn't act in lockstep.

| Ability | Options |
|---------|---------|
| `zombiemod:effect` | `effect`, `target`, `duration`, `amplifier`, `radius` — apply a potion effect |
| `zombiemod:heal` | `amount` — regenerate |
| `zombiemod:lightning` | `target`, `radius`, `visual_only` — call down a bolt |
| `zombiemod:explode` | `power`, `destroy_blocks`, `kills_self`, `trigger_radius` — immediate |
| `zombiemod:fuse` | `fuse_ticks`, `trigger_radius`, `swell_to`, `power`, `destroy_blocks`, `kills_self`, `sound` — creeper-style |
| `zombiemod:shockwave` | `radius`, `damage`, `knockup` — launch and hurt everything nearby |
| `zombiemod:leap` | `range`, `power`, `lift` — pounce at the victim |
| `zombiemod:pull` | `range`, `power` — drag nearby players toward it |
| `zombiemod:summon` | `entity`, `count`, `max_nearby`, `radius` — spawn reinforcements |
| `zombiemod:alert` | `radius`, `who`, `max_alerted` — hand your target to everything nearby |
| `zombiemod:break_blocks` | `allowed`, `reach`, `infest` — chew through walls when the path is blocked |
| `zombiemod:projectile` | `projectile`, `range`, `power`, `inaccuracy` — fire something |
| `zombiemod:place_block` | `block`, `target`, `radius`, `air_only` — cobweb the victim |
| `zombiemod:infect` | `chance`, `duration`, `effect`, `genus`, `announce` — bite now, turn later |
| `zombiemod:convert` | `victims`, `genus`, `chance`, `max_nearby`, `radius`, `cooldown`, `inherit_equipment`, `inherit_name` — what it kills gets up as one of them |
| `zombiemod:beam` | `range`, `damage`, `duration`, `elder` — a real guardian beam, from anything |
| `zombiemod:ray` | `range`, `damage`, `particle`, `density`, `ignite` — a hitscan beam drawn with particles |
| `zombiemod:adapt` | `resistance`, `max_adaptations` — learn what hurt it and stop taking it |
| `zombiemod:teleport` | `mode`, `range`, `distance`, `only_when_unseen`, `min_distance`, `on_projectile`, `vanish_chance` — see below |
| `zombiemod:particles` | `particle`, `count`, `spread` |
| `zombiemod:sound` | `sound`, `volume`, `pitch` |

`target` is `self`, `victim` (whatever it's currently attacking) or `nearby_players`.

Two defaults worth knowing. `explode` has `destroy_blocks: false` — a zombie that eats your build is
a very different proposition from one that hurts, so griefing is opt-in. And it only fires when
something is actually within `trigger_radius`, or an interval-timed bomb is just a mob that deletes
itself in an empty field.

#### Conversion

`convert` is the defining idea of the genre: what a zombie kills gets up as one of them. It fires on a
kill, so it's a consequence of the fight rather than something that happens nearby.

```json
{ "type": "zombiemod:convert",
  "victims": ["minecraft:villager", "minecraft:cow", "minecraft:pig"],
  "max_nearby": 8, "cooldown": 20 }
```

Where vanilla has an undead counterpart the corpse rises as its own kind — a villager becomes a
**zombie villager**, a piglin a **zombified piglin**, a horse a **zombie horse**. "That used to be my
villager" lands considerably harder than a generic zombie standing where it fell. Armour and custom
names are inherited by default.

Four guards, because unchecked this is how a server ends:

- **`victims` has no default.** A genus must name what it can turn. Converting anything that dies
  near a zombie isn't a feature, it's an outage.
- **Nothing already undead is converted**, so zombies can't endlessly re-raise each other.
- **`max_nearby`** caps how many of the risen genus may exist within `radius`.
- **`cooldown`** limits how often one killer may convert at all, default once a second.

That last one exists because `max_nearby` is measured with an entity query, and **an entity query
can't see what was added earlier in the same tick** — so several kills landing together could each
pass a cap all of them should have tripped. A rate limit needs nothing from the world to be correct.
Tested: 40 kills in a single tick produce exactly one conversion.

#### Infection

`convert` raises a corpse the instant it dies, which is the effect but not the story. The trope is a
bite, a while of knowing, and then it doesn't matter what actually killed you.

```json
{ "type": "zombiemod:infect", "chance": 0.35, "duration": 1200, "effect": "minecraft:hunger" }
```

Get bitten and you're marked for a minute. **Die while marked — to anything at all, including a fall
or another player — and you get up.** Re-biting refreshes rather than stacks, so a long fight isn't a
death sentence measured in hits.

**Milk cures it.** The marker is a real potion effect as well as a stored timer, and the death check
requires both — so clearing the effect clears the infection. That's deliberate: a curable infection is
a far better mechanic than an inevitable one, and it costs one extra condition. `duration` is also the
effect's duration, so the HUD icon *is* the timer a player can read.

Players turn too, even with the player-zombie feature switched off, because they were bitten rather
than merely killed.

#### The beam

`beam` is the one thing here that cannot be faked. A guardian's beam is drawn entirely by
`GuardianRenderer` from that guardian's synced attack target, and no other renderer draws one — so
particles can't imitate it and a zombie can't be given one.

What *can* be done is give the zombie a guardian. The ability parks a real Guardian — invisible,
silent, inert, never persisted — inside the caster's head, points it at the victim, and moves it with
the caster each tick so the beam tracks. The client then renders a genuine beam between the two.
Damage is applied by us at the end, so the guardian is scenery rather than a second attacker.

`elder: true` uses an Elder Guardian for the wider beam. Patient Zero's final phase uses that.

**`ray` is the other way to do it**, and often the better one: it draws the line itself with
particles and applies the damage, so there's no second entity to manage and nothing to leak. The
trade is look — it won't be a guardian beam, but the colour is yours:

```json
{ "type": "zombiemod:ray", "range": 20.0, "damage": 4.0, "density": 6.0,
  "particle": { "type": "minecraft:dust", "color": 16711680, "scale": 1.0 } }
```

`minecraft:dust` takes any RGB and reads as a proper coloured laser; `minecraft:sonic_boom` borrows
the Warden's look; `end_rod` and `electric_spark` both work well. `density` is particles per block.

A `ray` **charges before it fires** — `charge` is the wind-up in ticks (default 30, i.e. 1.5s). The
wind-up is loud as well as bright, and the warning sound's volume is scaled to the weapon's range, so
a shot that reaches 24 blocks can be heard at 24 blocks. The point is that a player learns the noise
and moves, including when it's behind them.

Breaking line of sight or leaving range during the wind-up **aborts the shot**. That's what makes the
telegraph honest rather than decorative — set `charge: 0` for the old instant behaviour if you want
something genuinely unfair.

#### Breaking things

`break_blocks` is the one that changes what the mod *is*: right now a wall is a solution to zombies,
and with a Breaker in the world it's a delay. It only fires when the mob has a target it can't reach
*and* has stopped making progress, so it eats walls rather than scenery.

Two deliberate opt-ins before anything of yours gets damaged: the ability itself, and `allowed`,
which has **no default** — a genus must name every block it may break. It also asks NeoForge's `canEntityGrief` hook before touching anything — **not** the `mobGriefing`
gamerule directly. That matters: the hook fires `EntityMobGriefingEvent`, which is how a
land-protection mod (FTB Chunks, Open Parties and Claims, and friends) vetoes griefing per entity
and per position. Reading the gamerule instead would work perfectly and quietly ignore every claim
on the server. The hook still consults the gamerule when nothing objects, so the global off switch
is unaffected. Same for `place_block`.

`infest: true` turns broken blocks into infested stone instead of dropping them — the old `INFEST`.

**Give a breaker a long memory.** Vanilla forgets an unseen target after 60 ticks, which is right
for a mob that walks around obstacles and exactly wrong for one that digs through them — the instant
the wall blocks line of sight it forgets why it was digging. The shipped breakers use
`"must_see": false` with `"unseen_memory": 1200`, so they stay committed for a full minute.

The shipped breakers name a **tag** rather than a list, so there's one place to change what gets
eaten and server owners can override it without touching a genus:

| Tag | Contains |
|-----|----------|
| `#zombiemod:breakable/soft` | Dirt, sand, gravel, glass, doors, planks, torches, ladders… |
| `#zombiemod:breakable/stone` | Stone, cobblestone, mossy cobblestone, granite, diorite, andesite, tuff, calcite, dripstone |
| `#zombiemod:breakable/deepslate` | Deepslate, cobbled deepslate |
| `#zombiemod:breakable/breaker` | soft + stone |
| `#zombiemod:breakable/big_breaker` | breaker + deepslate |

**Nothing player-crafted is in any of them.** Stone bricks, deepslate bricks, polished variants,
concrete and metal blocks are all absent, so a properly built base still holds. That's a deliberate
line: breakers should make a hole in the hillside you hid in, not walk through your walls.

#### Teleport, and the 1.8 BACKSTAB

`mode: behind` is the old plugin's BACKSTAB rebuilt: it reads the *victim's* look direction and
lands 180° opposite — behind them, already facing their back. The tell isn't seeing it move, it's
the sound at your shoulder.

The rest of the options compose into stalking behaviour:

| Option | Effect |
|--------|--------|
| `only_when_unseen` | Won't blink while you're looking at it — so it closes every time you turn away. |
| `min_distance` | Always blinks if you get closer than this, overriding everything above. Cornering it is exactly when it shouldn't be standing there. |
| `on_projectile` | Blinks the instant it's shot, outside the interval. Melee deliberately doesn't trigger it — get close enough to swing and it has to wear the hit. |
| `vanish_chance` | Sometimes it just leaves instead. |

"Looking at it" is a ~53° cone plus line of sight, so it's about what's on your screen rather than
exact aim.

#### The fuse, and what a vanilla client can't do

`zombiemod:fuse` is a creeper in zombie form: come within `trigger_radius` and it hisses, freezes,
swells and detonates — and stands down (at double speed) if you back off.

The swell is real, not a trick of particles. A creeper's own swell can't be borrowed —
`DATA_SWELL_DIR` is defined against `Creeper.class` and it's `CreeperRenderer` that inflates the
model, so a vanilla client has no way to draw a swelling zombie. But `minecraft:scale` *is* a synced
attribute, so ramping it over the fuse genuinely inflates the mob on an unmodified client. It swells
relative to whatever size the genus already is, so a big genus gets bigger rather than snapping to a
fixed size.

That's the general shape of this mod's limits: anything bound to a specific mob's *renderer* is out
of reach, and anything expressed through synced attributes, equipment, effects, sounds or particles
is fair game.

Rather than one ability per 1.8 name, the set is compositional: `effect` + `particles` + `sound`
between them build most of the old flavour abilities, so you assemble a screamer or a plague carrier
out of parts instead of waiting for that exact ability to exist.

## What's included

**45 genera ship with the mod** — Runner, Walker, Tank, Clicker, Bloater, Stalker, Boomer, Smoker,
Hunter, Charger, Spitter, Volatile, Crawler, Stormcaller, Breeder, Juggernaut, Coward, Swarmling,
Ember, Frost, Bogman, Dust Stalker, Screamer, Rioter, Sapper, Ender Zombie, Weeping Zombie, Herobrine, Nightstalker, Patient Zero, The Butcher, Corpse, Breaker, Infester, Spitfire, Archer, Weaver, Zomborg, Ghost, Outrider, Big Breaker, Lazer, Howler, Carrier, Biter. They're ordinary datapack files, so override or delete any of
them from a higher-priority datapack.

See [`docs/ROSTER.md`](docs/ROSTER.md) for what each one is and which feature it demonstrates, and
[`docs/TROPES.md`](docs/TROPES.md) for what's still missing from the genre.

## Bosses

Add a `boss` block and the genus gets a bar at the top of the screen. There's no separate flag —
presence of the block is what makes it a boss. Pair it with `"weight": 0` so it never turns up in
the wild.

```json
"boss": {
  "color": "red",
  "overlay": "notched_10",
  "range": 64.0,
  "darken_sky": true,
  "boss_music": true,
  "title": "Patient Zero"
}
```

`color` takes any vanilla bar colour, `overlay` is `progress` or `notched_6/10/12/20`, and
`darken_sky`, `boss_music` and `fog` are the Wither's and Dragon's own effects. The bar appears for
players within `range` and follows them in and out of it.

### Phases

```json
"phases": [
  { "below_health": 0.33,
    "title": "Patient Zero comes apart.",
    "sound": "minecraft:entity.wither.spawn",
    "attributes": { "minecraft:attack_damage": 16.0 },
    "abilities": [ { "type": "zombiemod:summon", "count": 3, "max_nearby": 14 } ] }
]
```

Abilities and attributes behave differently on purpose. **Abilities are gated** on the threshold, so
they switch off again if the mob heals — a boss with a regeneration ability shouldn't keep its
enrage after clawing back to full. **Attributes apply once and stay**, because stats that yo-yo as
health crosses a line read as broken rather than escalating.

`title` is announced once on entry and supports `&` colour codes. `announce` picks how:

| Mode | |
|------|--|
| `action_bar` *(default)* | Above the hotbar. Brief — easy to miss mid-fight. |
| `chat` | Stays put, so nobody misses it. |
| `title` | Big text across the screen, like the Wither arriving. |
| `title_and_chat` | Both. What Patient Zero uses. |
| `none` | Silent. |

`announce_radius` defaults to 64 blocks. `sound` plays once on entry regardless.

### Loot

```json
"loot": { "table": "zombiemod:entities/patient_zero", "replace": false }
```

`replace` defaults to **false**, so genus drops are *added* to whatever the base mob gives. A horde
that stops dropping rotten flesh is a surprising thing to inflict on a server just by adding a mob
type. Set it true for a boss whose drops should be exactly its own.

### Summoning

Bosses are meant to be called up, and there are three ways in:

**A command**, which works from command blocks and other mods since it takes an explicit position
and needs no player:

```
/zombiemod spawn zombiemod:patient_zero ~ ~ ~
```

**A ritual** — use an item on a block. These are their own datapack registry at
`data/<pack>/zombiemod/ritual/<name>.json`, rather than living on the genus, so one boss can have
several summons and a pack can add a ritual for someone else's genus without overriding their file:

```json
{
  "block": ["minecraft:soul_sand"],
  "item": ["minecraft:rotten_flesh"],
  "genus": "zombiemod:patient_zero",
  "consume": true,
  "replace_block": false,
  "count": 1
}
```

Both `block` and `item` accept a tag as well as a list.

A ritual can also demand a **built structure**, offsets relative to the block you activate:

```json
"block": ["minecraft:wither_skeleton_skull", "minecraft:zombie_head"],
"pattern": [
  { "offset": [ 0, -1,  0], "block": ["minecraft:soul_sand"] },
  { "offset": [ 1, -1,  0], "block": ["minecraft:soul_sand"] },
  { "offset": [-1, -1,  0], "block": ["minecraft:soul_sand"] },
  { "offset": [ 0, -1,  1], "block": ["minecraft:soul_sand"] },
  { "offset": [ 0, -1, -1], "block": ["minecraft:soul_sand"] }
]
```

**Anchor on a block the player can actually reach.** Offsets are relative to the block you
right-click, so it must have an exposed face. The obvious first draft of the example above anchored
on the centre soul sand — which has neighbours on four sides and a skull on top, leaving no clickable
face and a ritual that could never be performed. Anchoring on the skull and describing the soul sand
*below* it fixes that, and matches how the Wither reads: you place the last skull.

All four horizontal rotations are tried, so the shape can be built facing any way. With
`replace_block: true` the structure is consumed along with the activated block — otherwise one build
summons bosses forever.

The shipped example: build a **soul sand cross**, put a **wither skeleton skull, zombie head or
skeleton skull** on the centre, then **right-click the skull holding rotten flesh** — which summons
Patient Zero — 250 HP, 2× scale, netherite sword, two phases, and
its own loot table.

**Or your own mod/plugin**, by running the command or spawning the mob and calling
`GenusApplier.assign`.

## Player zombies

When a player dies, their corpse gets up wearing **their actual skin** — a player head built from
their game profile, so it works on a completely vanilla client. The 1.8 version needed Spout
installed on every client to manage that.

**Off by default.** Turn it on in the config. It takes a player's dropped items and puts them inside
a mob, which is a real gameplay decision and not one to make on someone's behalf just because they
installed a mob pack.

```toml
[playerZombies]
    enabled = false
    takeItems = true                     # corpse carries your drops; kill it to get them back
    genus = "zombiemod:player_zombie"    # the template it's built from
    name = "Corpse %P"
```

The corpse is an ordinary genus, so edit `player_zombie.json` to change how it behaves. It doesn't
burn in daylight — a corpse that quietly evaporates at dawn takes your inventory with it.

### Recovery

The most common complaint about the 1.8 version was *"my player zombie went missing"* — despawned,
fell in a ravine, died in a mob grinder, dropped nothing. When that happened the items were simply
gone, because nowhere else knew about them.

So every corpse is **written down the moment it's raised**, items included, in a save-level ledger.
A corpse that dies normally settles its own entry. Anything else, and an admin can put it right:

| Command | |
|---------|--|
| `/zombiemod corpse list [player]` | Every recorded corpse, newest first, with position and stack count. |
| `/zombiemod corpse give <player> [n]` | Hand the items back — straight to the owner if they're online, otherwise to you. |
| `/zombiemod corpse respawn <player> [n]` | Rebuild the corpse where it fell, carrying its items again. |
| `/zombiemod corpse forget <player> [n]` | Drop the record. |

`n` is the index shown by `list`, defaulting to the most recent. All op-only, like every other
ZombieMod command.

## Faces

The 1.8 plugin reskinned its zombies with Spout, which meant a client mod, which meant almost nobody
ever saw them. A vanilla client in 1.21 will happily render a **player head with an embedded texture**
— the mechanism behind every decorative head on [minecraft-heads.com](https://minecraft-heads.com/player-heads)
— so the reskin the original wanted is now free, and it costs the player nothing.

```json
"armor_color": 3355443,
"head": { "properties": { "textures": ["eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6..."] } }
```

The value is base64 of `{"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/<hash>"}}}`.
A profile carrying `properties` is *static* — the client renders it from the embedded texture and
never asks the session server who this is, so it works offline-mode, on servers, and for players who
have never seen that skin.

`head` also still takes a plain player name (`"head": "Herobrine"`), which resolves through the
session server and is what the Herobrine genus uses.

**Layering**, broad to specific: `armor_color` dyes all four leather slots, `head` replaces the
helmet, and `equipment` overrides any slot it names. So the usual arrangement is a dyed body for the
silhouette and colour, with a face on top — which is why nearly every genus keeps its `armor_color`.
`ghost` is applied last of all and wins outright, because borrowing a real player's face is that
genus's whole point.

### Where the Ghost gets its faces

Two pools, added together:

```toml
[ghost]
    names = ["Notch", "jeb_", "Dinnerbone"]   # always available, resolved by name
    rememberLogins = true
    rememberDays = 90     # forget anyone unseen this long. 0 never forgets
    skipBanned = true
```

The seed list exists because the second pool starts empty, and a Ghost with nothing to wear is just
a zombie — on a fresh server it would be faceless until somebody had played. Seed entries resolve
**by name**, exactly as `"head": "Herobrine"` does; remembered players resolve by uuid.

The two are pooled rather than one preferred, so a server with three seeds and thirty players mostly
shows its own players. Add more seed names to weight them up.

**`rememberDays` is real days, not game days** — the thing being remembered is a person, not
anything that happens in the world. The list is pruned once per server start, and stays capped at
512 regardless, evicting least-recently-seen.

**Bans are filtered when a face is drawn, not when the ban lands.** There is no ban event in
NeoForge, but reacting to one would be worse even if there were: it would miss bans applied from the
console or while the server was down, it would keep its own copy of state the server already has,
and un-banning somebody would not put them back. Asking the live ban list at the moment of the draw
is one call and is always right. Pruning *also* drops banned entries, so the saved file does not
keep names the server has decided against; a later un-ban re-adds them on their next login.

**44 of the 47 genera now have a face**: the Bloater is bloated, the Ember is on fire, the Frost is
frozen, the Tank is a brute, the Charger wears a football helmet, the Weeping is an angel, the
Rioter is in riot gear and the Commuter is a professor. Every texture hash was checked against
`textures.minecraft.net` before it shipped.

> **Provenance.** The texture hashes came from the [MinecraftHeads
> database](https://github.com/TheLuca98/MinecraftHeads), a compiled index of community heads from
> minecraft-heads.com. ZombieMod ships **hashes, not artwork** — the textures stay on Mojang's own
> servers and are fetched by the client, exactly as any decorative head works. Worth a look before a
> public release if you care about attribution for individual skins.

## CityWorld districts

[CityWorld](https://github.com/Sablednah/CityWorld-ReForged) generates cities — highrise districts,
farmland, roads, whole named buildings — and it knows what it planned for every chunk before that
chunk is generated. This is the tie the two mods were always meant to have: the difference between
monsters *distributed across* a world and monsters that *belong to* the parts of it they're found in.

Three conditions, usable anywhere a spawn condition is — genus spawn rules, behaviours, horde
conditions, and (through `where`) mutation triggers:

| | |
|---|---|
| `city_district` | `districts` — `HIGHRISE`, `MIDRISE`, `LOWRISE`, `INDUSTRIAL`, `MUNICIPAL`, `NEIGHBORHOOD`, `CONSTRUCTION`, `FARM`, `PARK`, `NATURE`, `OUTLAND`, `ASTRAL`, `ROUNDABOUT` — and/or `classes` for the context class name |
| `city_lot` | `styles` — `NATURE`, `STRUCTURE`, `ROAD`, `ROUNDABOUT` — plus `classes` and named `schematics` |
| `city_nature` | `min`/`max` on the generator's own grading, 0.0 dense city … 1.0 wilderness |

```json
"spawn": { "conditions": [
  { "type": "zombiemod:city_district", "districts": ["HIGHRISE", "MIDRISE"] },
  { "type": "zombiemod:light", "max": 7 }
] }
```

Names are matched case-insensitively, so `"highrise"` works as well as shouting the enum constant.
An omitted list means "don't care", which is how one condition asks about a district, a class, or
both.

**Without CityWorld these never match.** Failing closed is the only defensible default: a genus that
says it belongs in a highrise district is opting into CityWorld, and treating an unanswerable
question as satisfied would scatter city-only monsters across a vanilla world — surprising, and much
harder to work out than their simply not turning up. The integration is reflective, so ZombieMod
neither builds nor runs against CityWorld; if it's absent, or its API ever changes, the conditions go
quiet and everything else carries on.

Two genera ship using it: the **Commuter** in the built-up districts, and the **Harvester** out on
the farms. Both are inert in an ordinary world.

## Mutation

Everything else a genus describes is what a monster **is**. Mutation is the one that lets it stop
being that — so "kill it before it turns" applies to the monsters as well as to you, and so a place
can change what walks around in it.

```json
"mutations": [
  {
    "into": "zombiemod:runner",
    "when": { "type": "zombiemod:health_below", "fraction": 0.35 },
    "for": 0, "chance": 0.35,
    "sound": "minecraft:entity.zombie.infect",
    "particle": "minecraft:crit"
  },
  {
    "into": "zombiemod:ember",
    "when": { "type": "zombiemod:on_fire" },
    "for": 60, "chance": 0.6
  }
]
```

`for` is how many ticks the trigger must hold **continuously**, and it defaults to a full second
rather than zero because the triggers people reach for first are the twitchy ones — a zombie crossing
a stream is in water for three ticks, and a mutation that fires on that reads as a bug even while
doing exactly what the JSON asked. Step out of the water and the count resets to zero rather than
decaying, or a mob could stutter in and out of a puddle and accumulate its way to a change that never
really happened.

### Triggers

| | |
|---|---|
| `health_below` | `fraction` (default) or an absolute `amount` |
| `on_fire` | burning, from any source including the sun |
| `in_water` / `in_lava` | standing in it |
| `touching` | a block tag or list, checked at its feet, its body and below it |
| `where` | **any spawn condition**, asked where the mob is standing |
| `all_of` / `any_of` / `not` | combined |

`where` is the one that matters. Dimension, biome, height, light, time of day, sky and claim already
exist as spawn conditions, so mutation gets all of them for free — and any condition an optional
integration adds later comes along too:

```json
{ "type": "zombiemod:where",
  "condition": { "type": "zombiemod:dimension", "dimensions": ["minecraft:the_nether"] } }
```

That's "the same zombie is a harder thing in the Nether", with no new trigger type needed.

### What ships using it

The **Walker** is the substrate — weight 35, the one you meet most, deliberately the plainest thing
in the roster. What it becomes depends on what happens to it: hurt below a third it may break into a
**Runner**; left burning it becomes an **Ember**; stood on ice long enough it turns **Frost**; and in
the Nether it thickens into a **Charger**. The **Ember** closes the loop — hold one under water and
it goes out, and what is left is a Walker again.

### How it works, and why it costs an entity

A mutation **replaces** the mob rather than re-dressing it. Re-applying a genus over a live one looks
cheaper and is wrong in ways that would be miserable to diagnose: `speed` is *multiplied* into the
existing value, so it would compound every time; equipment slots the new genus doesn't mention would
keep the old gear; a `scale` of 1.0 is skipped rather than applied, so a shrinking mutation would
silently keep the old size. Building a fresh mob and assigning the new genus once is the only version
that's obviously right.

Carried across: position, facing, **health as a fraction** (so mutating into something with triple
the health isn't a free heal), current target, and burning. The whole persistent-data tag comes too,
which is how a horde member stays a horde member — and the horde's roster is told about the swap
directly, since it tracks members by identity. A 60-tick floor between one mob's mutations stops two
genera that name each other from swapping entities forever.

## Horde events

Everything else in this mod is an **encounter** — one monster, met on its own terms. A horde is the
layer above: a director that decides when several arrive together, builds, peaks and subsides.

Hordes are a datapack registry too, at `data/<pack>/zombiemod/horde/<name>.json`:

```json
{
  "name": "&cThe Horde",
  "weight": 10,
  "radius": 40.0, "min_radius": 22.0,
  "bar_color": "red",
  "announce": "&4You hear them coming.",
  "sound": "minecraft:entity.wither.spawn",
  "conditions": [ { "type": "zombiemod:time", "phase": "night" } ],
  "waves": [
    { "count": 6,  "delay": 0 },
    { "count": 10, "delay": 400 },
    { "count": 16, "delay": 500 }
  ]
}
```

**Waves rather than a number, on purpose.** Twenty at once is a wall; six, then ten, then sixteen is a
story with a middle. A wave can name specific `genera` or leave it out and draw from the weighted
table as usual, and `conditions` are the same ones that gate spawning.

Three ship: **The Horde** (a general build), **The Swarm** (many weak things, a different problem to
solve) and **The Siege** (breakers first, so the way is open when the rest arrive).

### Rare, or rare and legible

The Siege is gated on a **full moon**, and on the player being **under a roof**:

```json
"conditions": [
  { "type": "zombiemod:time", "phase": "night" },
  { "type": "zombiemod:moon", "phases": ["full_moon"] },
  { "type": "zombiemod:see_sky", "value": false },
  { "type": "zombiemod:depth", "max": 12 }
]
```

A low `weight` and a moon phase both make a thing rare. Only one of them lets a player *see it
coming* — you look up, and you know tonight is the night to check the walls. So the moon does the
limiting and the weight is high (30), rather than the other way around.

The other two conditions are what make "The walls held" true rather than a slogan. `see_sky: false`
means the Siege finds you sheltered — there is something for breakers to break. But sheltered alone
is also true at the bottom of a ravine, so `depth` bounds how far below the local surface you are.

**`depth` exists because of a bug it exposed.** Hordes place their mobs at surface height in a ring
around the player, so a horde starting while you were deep in a cave would spawn its whole wave on
the roof of the world and never reach you — the bar would appear and nothing would arrive. All three
shipped hordes now carry `depth: {max: 12}`, which is deep enough for a basement and not deep enough
for that.

`moon` takes any of vanilla's eight phases — `full_moon`, `waning_gibbous`, `third_quarter`,
`waning_crescent`, `new_moon`, `waxing_crescent`, `first_quarter`, `waxing_gibbous` — and it reads
1.21.11's `MOON_PHASE` environment attribute rather than deriving the phase from the day count, so a
dimension that disagrees about the moon is handled for free. `depth` is measured against the column's
heightmap, not an absolute Y, so it means the same thing on a mountain as at sea level.

The bar counts **what's still alive**, not what's been spawned, because "twelve still out there" is
the number a player actually wants. A horde ends when its last one falls rather than on a timer —
which is what makes the quiet afterwards mean anything, and it ends with a line, a sound and the
`xp` the horde is worth, on top of whatever the mobs themselves dropped. A bar that simply vanishes
reads as the feature stopping rather than as you winning.

### Finding the last one

Which leaves the problem every wave-defence has, and that Minecraft itself has never solved: one
survivor, somewhere, in the dark. Two answers, both borrowed from vanilla's instincts rather than
invented:

**Ring a bell.** Exactly what vanilla does for a raid, so the gesture is already learned. Any bell
within `bellRadius` (48 by default, vanilla's number) makes the horde's survivors glow. It's hooked
on the ring itself, so an arrow or a redstone pulse works as well as your hand. None of vanilla's
code is reusable here — `BellBlockEntity.makeRaidersGlow` is private and filters on the
`#minecraft:raiders` entity tag, which a zombie must never be in, since that tag is what makes
something count towards a raid.

**Or wait.** Once the last wave is out, a horde that has gone `glowAfter` ticks without a kill lights
its own survivors up. Measured from the last kill rather than from the start, deliberately: a long
fight you're winning isn't the problem, and shouldn't be treated as one.

```toml
[hordes]
    enabled = false     # off by default, like proximity spawning
    chance = 0.08
    cooldown = 36000    # a day and a half; rarity is most of what makes one memorable
    cap = 40            # never more than this alive at once
    bellGlow = true     # ring a bell to light up the stragglers
    bellRadius = 48.0
    glowAfter = 1200    # ...or a minute without a kill and they light themselves up. 0 disables
    glowDuration = 200
```

### Flipping switches without a restart

```
/zombiemod config                    # what is on
/zombiemod config hordes on
/zombiemod config proximity          # no argument toggles it
```

`enabled`, `hordes`, `playerZombies`, `proximity`, `bestiary`, `perGenus` and `logSpawns`. **Admin
only** — permission level 3, a step above the rest of the tree, because these change what the server
does for everyone rather than what happens in front of whoever typed it.

Deliberately not every config key: a command that can set anything is a second, worse config editor.
These are the ones whose answer is yes or no and whose effect is immediate — the set you want to
change while standing in the world it affects.

Changes are written to disk, not just held in memory. NeoForge's `set()` explicitly does not save,
which would have given a setting that worked perfectly until the next restart.

It also saves you the trap that `/zombiemod status` exists for: `config/zombiemod-server.toml` is
only the **template** for new worlds, and the live copy is `saves/<world>/serverconfig/`. The command
always edits the one that is actually in force.

```
/zombiemod horde list
/zombiemod horde start zombiemod:the_siege
/zombiemod horde stop
```

**Everything above is per player.** The check, the cooldown, the conditions and the horde itself are
all keyed on one player's id, and conditions are evaluated where *that* player is standing. So two
people in different places get their own night rather than sharing one, and a horde asks about the
moon and the roof over the head of the person it is coming for. It scales with population by
construction — no server-wide event to schedule, and nobody's siege lands on someone who is
somewhere else entirely.

The one shared number is `cap`, which bounds horde mobs alive **per horde**, so a busy server is
still bounded per player rather than in total. Worth watching if a lot of people play at once.

### What a base can be

`base` takes **any** entity type, and more of them work than you would guess — measured by building
one of each and reading back what applied:

| | |
|---|---|
| **Full genus** | every humanoid monster: zombies, skeletons, piglins, **illagers** (pillager, vindicator, evoker, illusioner), witch, enderman, creeper, spider, blaze, guardian, ravager, zoglin, breeze, creaking, giant — and **warden** (500hp) and **wither** (300hp) |
| **Movement and looks, no bite** | squid, glow squid, villager, snow golem, shulker, cow, sheep — all `PathfinderMob`s that take goals, but they have **no `attack_damage` attribute**, so `damage` is silently ignored |
| **Looks and stats only** | ghast, slime, magma cube, phantom, **ender dragon** — not `PathfinderMob`s, so most goals cannot attach |

Two things to know before you write one.

**Attributes a type never declared are not defaults, they throw.** Vanilla's
`AttributeSupplier` raises `IllegalArgumentException` rather than returning zero. A `melee_attack`
goal on a squid was therefore not a squid that hits for nothing — it was a server crash the first
time it reached a target. ZombieMod now skips that goal on any mob without `attack_damage`, the same
way it skips goals that need a `PathfinderMob`. Check the log, not the corpse.

**A genus only claims spawns vanilla was already making.** So a wither or an ender dragon genus is
real, but nothing will ever spawn one — those are summoned, not spawned. Use `/zombiemod spawn` or a
spawner. Illagers are the interesting case here: they spawn in patrols, outposts and raids, so a
genus on one turns up on its own.

#### What a non-humanoid can actually wear

Nothing, and it is worth knowing why: **armour, held items and the `head` player-head are drawn by
model *layers*, and only humanoid models have them.** Checked against the renderers rather than
guessed:

| Layer | Which renderers add it |
|---|---|
| `CustomHeadLayer` — the `head` field | HumanoidMob (so every zombie, skeleton, husk, drowned, giant), Illager, Piglin, Villager, Wandering Trader |
| `HumanoidArmorLayer` — `armor_color`, worn `equipment` | Zombie, Skeleton, Zombified Piglin, Zombie Villager, Giant, Piglin |
| `ItemInHandLayer` — held `equipment` | HumanoidMob, Illager, Pillager, Vindicator, Evoker, Illusioner, Vex, Giant |

**Guardian has no layers at all.** Neither do Ravager, Blaze or Zoglin. Creaking and Warden have only
emissive glow layers, Enderman has eyes and a carried block, Spider has eyes, Creeper has the charge
overlay, and the Wither's one layer is its invulnerability shield. Set `equipment` on any of them and
the item is genuinely on the mob server-side — it just draws nothing.

**But the whole-entity effects still work on anything**, because they are not layers:
`scale` is an attribute, `glow` is an outline colour on the render state, `burning` goes through the
flame feature renderer, and `invisible`, `name`, particles and sounds are all model-agnostic. So a
Guardian genus is perfectly viable — resized, glowing, alight, renamed, trailing particles. It just
cannot wear a hat.

So: green aggressive squid, yes, but it will bump rather than bite — the aquatic idea is better
served by a `drowned` base, which is already a zombie with everything attached. Tall skinny ender
zombies and zombie illagers, wholeheartedly yes.

### Breaking as a means, or as the point

`break_blocks` defaults to the Breaker's behaviour: only when there is something to reach, and only
when actually stuck, so a mob still making progress does not demolish the countryside it walks past.

`needs_target: false` inverts that. No target, no stuck check, and it clears its own square and the
ring around its feet as it goes. That is the difference between breaking to reach you and breaking
being the point — Blight uses it, the Breakers do not.

### Bramble and Blight

Two genera built as mechanical opposites, tied to opposite districts.

**Bramble** is the only genus in the mod that *builds*. It lays moss carpet in its own footprint as it
wanders — reclaiming the city rather than wrecking it. Slow, sixty health, and `follow_range: 12`,
because nature is not hunting you, it is simply taking the ground back. Nature and park districts, and
forest or jungle biomes, so it exists in worlds with no city at all.

**Blight** is the mirror. It eats flowers, leaves, saplings, crops, grass, ferns and vines — and
`moss_carpet` is in its tag too, so it destroys exactly what Bramble makes. Trails smoke and gives
anyone standing close nausea. Industrial and construction districts.

Ground both have crossed visibly flickers between overgrown and blighted, and nothing coordinates
that — it falls out of two genera wandering the same block.

**Blight goes looking.** With nothing to fight it walks to the nearest greenery and eats it, rather
than standing on a bare block surrounded by the moss it hates:

```json
{ "type": "zombiemod:seek_blocks", "priority": 6,
  "blocks": "#zombiemod:breakable/blight", "range": 16, "vertical_range": 4 }
```

`seek_blocks` takes the same tag-or-list shape as `break_blocks`, so a genus can seek exactly what it
destroys. It stands down while the mob has a target — hunting something beats hunting moss — and it
looks about four times as often as vanilla's `MoveToBlockGoal` normally would, because for this genus
searching *is* the job rather than an errand.

**And they hunt each other.** `nearest_target` takes a `genera` list, which narrows a target class
down to particular genera — `TargetClass` can say "any zombie" and could never say "*that* zombie",
so a grudge between two genera was not expressible at all, since Bramble and Blight are the same
base mob:

```json
{ "type": "zombiemod:nearest_target", "priority": 3,
  "target": "zombie", "genera": ["zombiemod:blight"] }
```

Bramble's placement goes through `EventHooks.canEntityGrief`, so a land claim vetoes it, and moss
carpet was chosen over moss block deliberately: it is decorative, it needs no support removed, and it
comes up with one punch.

### Districts should feel like districts

Commuter (45) and Harvester (40) are weighted near the top of the table, above Coward and just under
Walker. That is deliberate and it is safe, because unlike every other heavy genus **they are gated on
a district** — they cannot leak into a world that has no city in it. A highrise block should read as
an office that died, not as a generic dark room that occasionally contains an office worker.

Against roughly 114 of eligible competition plus `vanillaWeight` 40, that puts Commuter at about one
in four of what spawns in a dark highrise district.

Worth knowing that **`SPAWNER` is one of the default spawn reasons**, so CityWorld's apocalypse mode
— which seeds spawners through basements, caves and sewers — produces genera rather than plain
zombies. A sewer under a highrise pumps out Commuters without either mod being told to co-operate.

### How far it notices you

`follow_range` turned out to be the most expressive field in the mod, because vanilla's targeting
goal sizes its search from `Attributes.FOLLOW_RANGE` — so it is not a difficulty dial, it is *what
kind of creature this is*. The 52 shipped genera sit in five tiers:

| Tier | Range | Who |
|---|---|---|
| Oblivious | 6–10 | Sleeper, Clicker |
| Shambler | 16–20 | Walker, Harvester, Commuter, Crawler, Bloater, Boomer |
| Ordinary dead | 22–34 | most of them |
| Hunter | 36–48 | Runner, Hunter, Ender, Volatile, Nightstalker |
| Watcher | 48–64 | Stalker, Weeping, Screamer, **Coward**, Herobrine |

**The Coward is in the top tier, not the bottom**, which reads wrong for a second and then obviously
right: it needs to spot you from further away than anything else, because it has to be *running* by
the time you arrive. A coward that notices at 24 blocks is just a slow zombie with a bad plan.

The tiers are also why a crowd reads as a crowd. Shamblers turn late and hunters commit early, so a
mixed group arrives in waves without anything coordinating it.

### Leave him alone and he leaves you alone

`follow_range` is the whole of it, because vanilla's targeting goal sizes its search from
`Attributes.FOLLOW_RANGE` — so a low value means the mob genuinely does not know you are there.

The **Sleeper** is built on it: `follow_range: 6.0`, ninety health, fourteen damage, barely moves,
twelve arrows still stuck in it from people who tried. It keeps `hurt_by_target`, and that pairing is
the contract — retaliation is *not* range-limited, so leaving it alone works and hitting it once is a
decision you do not get to take back at any distance.

### A tainted herd

Infection was always cross-species — `Infect.onAttack` never checked its victim was a player, so a
Biter could always sicken a cow, and a marked animal always rose when it died whatever killed it.
What it could not do was **carry on**: an infected cow has no genus, so it has no abilities, so the
chain stopped at one animal.

```toml
[infection]
    spread = true
    interval = 200      # ticks between one infected mob's attempts
    chance = 0.25       # roughly one new case every 40s per infected animal
    radius = 4.0
    toPlayers = true    # standing too close is a mistake
    milkCure = true
```

**What rises is filtered by where it died.** The conversion draw applies the dying genus's spawn
*conditions* — not its reasons, since the default reason set excludes `CONVERSION` on purpose — so a
sheep dying in a wheat field cannot get up as a Commuter. It had to: without it, any genus with a
district or a depth behind it could appear anywhere something happened to die, and the gap only
became visible once a district genus was weighted heavily enough to win most of the draws.

**The Biter walks past what it has already bitten.** Its animal target goal carries
`skip_infected: true`, which is the difference between a wood-chipper and a vector — without it a
Biter simply kills each sheep in three hits and they get straight back up, and the infection never
gets to be the mechanic. With it, it moves on to fresh livestock and leaves a field of sick, curable
animals behind it.

**Tuning a bite for a sheep is not tuning it for a player.** The Biter's original 35%-per-hit was
set with a 20hp armoured player in mind, where it reads as dread across a long fight. Against an 8hp
animal that killed better than one in four outright before the infection ever landed. It bites for 2
now, at 75% — four hits to kill a sheep, and almost always infected by the first or second. Measured
over 500 sheep: 490 left sick and alive, 10 killed, 1.3 hits on average.

`duration` had the same problem. At 1200 ticks an infected animal expected barely **1.5**
transmissions before it cleared, so a herd fizzled rather than turned. At 3600 it expects about 4.5,
which is a contagion. A bitten *player* now carries it for three minutes rather than one, which is
long enough that waiting it out is no longer the obvious play — milk is.

That flag alone was not enough, and the reason is worth knowing: **a target already held is never
re-tested.** `TargetGoal.canContinueToUse` checks reach, line of sight and team, and never consults
the targeting conditions again — so the bite landed, the sheep became infected, and the Biter
finished it off regardless. `infect` now clears the attacker's target the moment a bite takes, for
any victim that is not a player. Having infected it, it moves on.

Deliberately per-goal and **not** on the goal that targets players: "be bitten once and zombies
ignore you" is an exploit, not a mechanic.

**Something has to bite the livestock first.** Biter carries the infection and now targets `animal`
below `player` and `villager`, so it goes for a herd only when nobody better is about. Carrier hunts
animals too but uses `convert`, which raises them on the spot — instant, and no chain. If you want a
field to turn slowly, it is Biter you let in.

**Right-click an infected animal with a milk bucket to cure it.** Milk already cured a *player* for
free, because the cure is "the marker effect is gone" and drinking milk clears effects — this is the
same cure aimed at something that cannot drink it itself. Safe to hang on the vanilla interaction,
because using a *milk* bucket on an entity is not a vanilla interaction at all; milking a cow takes
an empty one.

Three things keep it from running away:

- **One neighbour per attempt.** A herd should turn over minutes — something you can watch happen and
  still do something about — rather than in a single tick, which is just an event you are told about.
- **Already-infected animals are skipped**, so one sick cow cannot re-mark its neighbours forever.
- **The timer is passed on, not refreshed**, so a chain cannot outlive its source indefinitely.

Undead things and anything already carrying a genus are immune, exactly as they are to a bite.

Because the marker is a real potion effect, an infected animal **shows it** — pick `minecraft:poison`
for unmistakable green, or `minecraft:hunger` (the Biter's default) for something subtler. The horror
is that the sheep still looks like a sheep.

## ZombieDex

*Gotta slay 'em all.* Who has met what, and who has killed what — a completionist checklist that
gives you a reason to chase down a Coward.

```
/zombiemod bestiary        # the list, in chat
/zombiemod bestiary book   # the same list as a written book you can carry
```

**The record lives in the world's saved data and is always complete.** The scoreboard is a *view* of
it. That ordering is the whole design: turning the per-genus view on later shows a history that was
being kept all along rather than starting from zero.

Two objectives are always kept, both created on demand:

| Objective | Holds |
|---|---|
| `zombiemod.slain` | total kills |
| `zombiemod.genera` | how many **distinct** genera you have killed |

```
/scoreboard objectives setdisplay sidebar zombiemod.genera
```

### Per-genus tracking

```toml
[bestiary]
    enabled = true
    perGenus = false
```

`perGenus` adds one objective per genus, `zombiemod.<genus>`, holding your kill count for it — which
is what makes the checklist queryable by anything that speaks scoreboard:

```
execute if score @s zombiemod.coward matches 1.. run ...
```

Off by default because it costs a scoreboard row per genus **per player**, and every row syncs to
every client — fifty genera on a busy server is a lot of packets for a checklist. On a small server
it is nothing, which is exactly who the switch is for. Since the saved record is kept either way,
switching it on is never too late.

**Why a scoreboard rather than a private format.** It is already a public read-write API that every
admin tool, command block and datapack speaks, so a leaderboard, an advancement gate and a
below-name counter are all things a server owner can build without this mod knowing about them.

**"Met" is weaker than seen** — it means damage passed between you, in either direction. A real
did-you-lay-eyes-on-it test would be a visibility check per mob per tick, which is a lot of work to
tick a box for something glimpsed across a valley.

A companion client mod could draw this far more nicely, and that stays entirely optional: the book
and the sidebar are the feature, and both work on a vanilla client.

### What they leave behind

Sixteen genera carry a loot table, all with `replace: false`, so rotten flesh drops as normal and
these are additions. The rule is that a genus should leave behind **what it was**: Harvester drops
wheat and seeds and occasionally its hoe, Commuter paper and the odd emerald, Blight coal and
charcoal, Bramble moss and vines, Breaker cobble and iron nuggets, Frost snowballs, Bogman clay and
lily pads, Sleeper a serious pile of iron and sometimes a golden apple.

## Bounties

A genus can carry a `bounty` — what killing it is worth.

Mostly it tracks how much trouble the thing is, with one deliberate exception: **the Coward pays 6,
far above its threat, while giving only 2xp.** It is trivial to kill and very hard to catch, so the
money is for catching it at all. The reward is the chase, not the fight. Every shipped genus has one, roughly
proportional to how much trouble it is: a Walker is 1, a Tank 15, Patient Zero 150.

**Who pays it is a separate question.** There's no Vault on NeoForge — no abstraction every economy
mod implements — so ZombieMod doesn't pick one for you. An economy adapter registers itself:

```java
Bounties.register((player, amount) -> myEconomy.deposit(player, amount));
```

Payers are additive, not exclusive, so an adapter registering doesn't stop the built-in tally — a
server may reasonably want both the money and the count.

### With no economy mod

The fallback is the **scoreboard**, which is a real reward on a vanilla server rather than a number
waiting for a dependency. Opt in with one command:

```
/scoreboard objectives add zombiemod.bounty dummy
/scoreboard objectives setdisplay sidebar zombiemod.bounty
```

ZombieMod only tallies into an objective that **already exists**, so nothing appears on a server that
never asked for it. Scores are integers, so fractional bounties accumulate as their floor — paying
less than the genus is worth being the better error of the two.

```toml
[bounty]
    enabled = true
    objective = "zombiemod.bounty"   # blank to disable
    announce = true                  # action-bar payout on the kill
```

Shooting one from a distance counts — the payout reads the damage source's owner, so a bounty that
only paid for melee would quietly punish the players being careful.

## Proximity spawning

Genera otherwise only ever claim spawns vanilla was already going to make, so the mod can change *what*
you meet but never *how much* is out there. This is the 1.8 plugin's `ProximitySystems`, and it's the
difference between a world that's populated and one that feels occupied.

```toml
[proximity]
    enabled = false          # off by default - see below
    interval = 100           # ticks between attempts, per player
    chance = 0.5
    minDistance = 16
    maxDistance = 32
    nearbyCap = 8            # the number that decides atmosphere vs siege
    outOfSightOnly = true    # only spawn where the player can't watch it appear
```

**Off by default.** It's the only feature here that adds mobs beyond vanilla's own rules, and
installing a mob pack shouldn't quietly change how many things are hunting you.

Every candidate still has to pass the genus's own `spawn` conditions at the chosen position, so a
Frost still only appears in snow and a Stalker still only in the dark. This adds *opportunities*; it
doesn't bypass the rules. It also respects claims, refuses unloaded chunks, and asks NeoForge's
spawn-position hook so spawn-control mods still get a say.

`outOfSightOnly` is the one that matters for feel: the point is that they were always there, not that
you watched them appear.

Which makes it **impossible to tell whether it's working by looking** — so `/zombiemod status` reports
what it's been doing:

```
proximity: true   240 attempts, 12 spawned (no spot 31, in view 190, at cap 7, claimed 0, no genus 0)
```

The breakdown matters more than the total. 240 attempts producing 12 tells you little; 190 of them
rejected *for being in view* tells you exactly which knob to turn — lower `outOfSightOnly`, or push
`minDistance` out so there's more cover between you and the spot. `logSpawns = true` logs each one
with its distance.

## Land claims (FTB Chunks)

If [FTB Chunks](https://www.curseforge.com/minecraft/mc-mods/ftb-chunks) is installed, ZombieMod
respects claims: its mobs won't break or place blocks inside one, and genera don't claim spawns there.

```toml
[claims]
    respectClaims = true
    noGriefingInClaims = true
    inClaims = "VANILLA_ONLY"   # ALLOW | VANILLA_ONLY | NO_SPAWNS
```

Worth knowing **why this is needed**, because it looks like it shouldn't be: `break_blocks` already
asks NeoForge's `canEntityGrief` hook, which is how a protection mod vetoes griefing. But FTB Chunks
protects *explosions* in claims and doesn't cover general mob block-breaking — there's an [open
feature request for Wither protection](https://github.com/FTBTeam/FTB-Mods-Issues/issues/1144) for
exactly that reason. So the hook fires, FTB declines to answer, and the claim does nothing. ZombieMod
closes the gap from its own side.

`inClaims` defaults to `VANILLA_ONLY` rather than `NO_SPAWNS`: keeping *our* mobs out of someone's
base is this mod's business, while emptying it of vanilla mobs isn't. Set `NO_SPAWNS` if you want
claims genuinely quiet.

The griefing veto is scoped to ZombieMod's own mobs, for the same reason — silently vetoing griefing
for every mob in the game would be doing a land-protection mod's job for it, from a mob pack.

There's also a per-genus condition, so one genus can be claim-aware without changing the config:

```json
{ "type": "zombiemod:in_claim", "value": false }
```

**All of it is inert without FTB Chunks.** The bridge is reflection over three methods rather than a
build dependency — no extra Maven repository, no pinned FTB version, and no way for an optional
integration to break the thing it's optional to. Every failure path answers "not claimed", so the
worst case is protection quietly doing nothing rather than zombies quietly breaking.

`/zombiemod status` shows whether the link came up.

## Configuration

`zombiemod-server.toml` is a **server** config, so where it lives depends on how you're playing:

| | |
|---|---|
| Dedicated server | `config/zombiemod-server.toml` |
| Singleplayer / LAN | `saves/<world>/serverconfig/zombiemod-server.toml` |

Per-world in singleplayer is deliberate on NeoForge's part — these settings change how a world plays,
so they travel with the save. **The file doesn't exist until you've loaded the world once.**

| Option | Default | Purpose |
|--------|---------|---------|
| `enabled` | `true` | Master switch. Off means everything spawns exactly as vanilla would. |
| `vanillaWeight` | `40` | How strongly to leave a mob alone, weighed against the genera that could claim it. `0` means a genus claims every eligible spawn. |
| `logSpawns` | `false` | Log every genus spawn to the console. Noisy; for tuning weights. |

## Commands

| Command | |
|---------|--|
| `/zombiemod list` | Every genus the loaded datapacks define. |
| `/zombiemod spawn <genus>` | Spawn one where you're **looking**, up to 48 blocks. |
| `/zombiemod spawn <genus> <x> <y> <z>` | Spawn one at a position. Accepts `~ ~ ~` relative and `^ ^ ^5` local — so `^ ^ ^5` is "five blocks in front of me". Works from the console and command blocks. |
| `/zombiemod horde list\|start <horde>\|stop` | Wave events. |
| `/zm …` | Alias for everything below — a redirect onto the same node tree, so subcommands and suggestions are identical. |
| `/zombiemod bestiary [book]` | Your ZombieDex checklist, in chat or as a written book. |
| `/zombiemod config` | List the live toggles. **Admin only** (permission level 3). |
| `/zombiemod config <name> [on\|off]` | Flip one, and write it to disk. Omit `on`/`off` to toggle. |
| `/zombiemod status` | What the mod believes its settings are, whether the corpse genus resolved, and whether FTB Chunks linked. **Start here when something seems not to work.** |
| `/zombiemod observe [on\|off]` | Take no damage while staying a completely normal target. |
| `/zombiemod corpse list [player]` | Every recorded player corpse, newest first. |
| `/zombiemod corpse give <player> [n]` | Hand a corpse's items back. |
| `/zombiemod corpse respawn <player> [n]` | Rebuild a corpse where it fell. |
| `/zombiemod corpse forget <player> [n]` | Drop the record. |

All require permission level `LEVEL_GAMEMASTERS` (op 2).

`observe` exists because the usual ways to survive a test don't work here. Creative mode and every
god-mode command set vanilla's invulnerable flag, and `LivingEntity.canBeSeenAsEnemy()` is
`!isInvulnerable() && canBeSeenByAnyone()` — so an invulnerable player is one no zombie will ever walk
towards. Useless when the thing you're testing *is* what zombies do. Observer mode changes nothing
about the player: you're targeted, chased, swung at, knocked back and teleported behind exactly as
before, and the damage is cancelled on arrival. Knockback still lands.

There is deliberately **no** `/zombiemod reload` — genera are datapack data, so vanilla `/reload`
already does the job.

## Status

**Alpha, but broadly working.** 43 genera, every ability from the 1.8 plugin rebuilt, and most of it
confirmed in play. See [`docs/STATUS.md`](docs/STATUS.md) for what's verified, what's built but
untested, and what's left — including the two things still missing from the original (proximity
spawning, and bounty pending an economy decision).

Known limitations worth knowing before you write a genus:

- **Goal targets** come from a fixed name→class list, because vanilla's targeting goals are typed on a
  Java class rather than an entity id. "Avoid wolves" works; "avoid a modded mob" doesn't.
- **A malformed genus file stops the world loading** rather than being skipped — standard
  datapack-registry behaviour, harsher here because these files are hand-written. The log names the
  file and the field.
- **Renderer-bound effects** can't simply be handed to any mob. Sometimes there's a way round (the
  guardian beam, the creeper-style swell, the bow draw) and sometimes there isn't. See
  [`docs/TROPES.md`](docs/TROPES.md).

## Building from source

Requires JDK 21. Standard NeoForge ModDevGradle setup:

```
export JAVA_HOME=/path/to/jdk21

./gradlew compileJava   # fast inner loop
./gradlew build         # jar in build/libs/zombiemod-<version>.jar
./gradlew runServer     # dev dedicated server on port 25567 (needs run/eula.txt)
./deploy.sh             # build and copy into a CurseForge test instance
```

The first build after touching `src/main/resources/META-INF/accesstransformer.cfg` takes **10+
minutes** — ModDevGradle recompiles Minecraft with the transformer applied. It looks like a hang and
isn't.

### How it fits together

```
data/<pack>/zombiemod/genus/*.json      datapack registry `zombiemod:genus`
        ↓  Genus.CODEC
core/Genus                              the parsed type
        ↓  GoalSpec.CODEC, dispatching on "type"
core/goal/GoalSpecs                     one record per vanilla goal
        ↓  spec.build(mob)
neoforge/GenusApplier                   mutates a live vanilla Mob
        ↑
neoforge/ZombieModEvents                FinalizeSpawnEvent + EntityJoinLevelEvent
```

| Path | Role |
|------|------|
| `core/Genus.java` | The genus record and its codec. **Start here.** |
| `core/goal/GoalSpecs.java` | One record per goal type; fields mirror the vanilla constructor. |
| `core/goal/GoalSpecTypes.java` | Registry the `"type"` field dispatches through. |
| `core/goal/TargetClass.java` | The `target` name → Java class map. |
| `core/spawn/SpawnRules.java` | Reasons plus a list of conditions. |
| `core/spawn/SpawnConditions.java` | One record per condition type. |
| `core/spawn/SpawnConditionTypes.java` | Condition registry; `register` is public for other mods. |
| `neoforge/GenusApplier.java` | Applies a genus to a live mob. |
| `neoforge/ZombieModEvents.java` | Spawn and level-join hooks; weighted genus selection. |

Adding a goal type is a record in `GoalSpecs` plus one line in `GoalSpecTypes`.

The split that matters: **attributes, equipment, name and scale persist** in entity NBT and are
applied once at spawn, while **goals are never serialised** and must be rebuilt every time the
entity joins a level. The genus id lives in the entity's persistent data so a reloaded mob knows what
to rebuild — which also fixes a bug the 1.8 plugin's README shipped with, where player-corpse zombies
were lost on restart.

Because the mod mutates vanilla mobs from outside, it needs an access transformer for
`Mob.goalSelector`, `targetSelector` and `navigation`. See
`src/main/resources/META-INF/accesstransformer.cfg`.

## Credits & licence

Originally a Bukkit plugin by **Sablednah**
([Sablednah/ZombieMod](https://github.com/Sablednah/ZombieMod)), rewritten from the ground up for
NeoForge. Released under the **MIT** licence — the original's CC BY-NC-ND terms were relicensed by
the same author.

The 2013 plugin's source is retained in this repository under `src/me/sablednah/` as a reference
while the port is completed. It contains third-party contributions and is **not** covered by the MIT
grant; see `LICENSE`. It will be removed at release.
