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
| `base` | `minecraft:zombie` | Which vanilla mob to dress up — `husk`, `drowned`, `zombie_villager`, `giant`, anything. |
| `weight` | `0` | Relative spawn frequency against other genera on the same base mob. `0` = never spawns naturally, command only. |
| `health` | *vanilla* | Max health. |
| `damage` | *vanilla* | Attack damage. |
| `speed` | `1.0` | Movement speed **multiplier** on the base mob, as in the old configs — `1.25` is 25% quicker than a zombie. |
| `follow_range` | *vanilla* | How far away it notices you (the old `agro`). |
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
| `xp` | *vanilla* | Experience dropped on death. A Tank worth the same 5xp as a stray zombie is a strange reward for a two-minute fight. |
| `behaviours` | `[]` | Goal sets that switch on and off with a condition — see below. |
| `ghost` | `false` | Take the name and face of a random player who has played on this server. |
| `mount` | *(none)* | Something to ride in on — the old `jockey` field. |
| `navigation` | `default` | `climb` makes it scale walls like a spider, `swim` and `amphibious` the aquatic ones. |
| `equipment` | `{}` | Held and worn items — see below. Beats `armor_color` and `head` for any slot it names. |

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

### Spawning

A genus with a `weight` above zero can claim a spawn that its `base` mob was going to make anyway.
Which genus gets it is a weighted draw — and **"leave it as a plain zombie" is an entry in that same
draw**, weighted by `vanillaWeight` in the config. Without that, the moment you shipped one genus it
would claim every zombie in the world and plain zombies would quietly cease to exist.

So with `vanillaWeight = 200` and genera weighted 30 and 10, any eligible zombie spawn comes out
roughly 83% vanilla, 12.5% the first genus, 4% the second. The 22 shipped genera total around 190 in
a typical dark overworld spot, which leaves you a little over half plain zombies.

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
| `zombiemod:time` | `phase` (`day`/`night`), or `min`/`max` on the 24000-tick cycle |
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
| `vanillaWeight` | `200` | How strongly to leave a mob alone, weighed against the genera that could claim it. `0` means a genus claims every eligible spawn. |
| `logSpawns` | `false` | Log every genus spawn to the console. Noisy; for tuning weights. |

## Commands

| Command | |
|---------|--|
| `/zombiemod list` | Every genus the loaded datapacks define. |
| `/zombiemod spawn <genus>` | Spawn one where you're **looking**, up to 48 blocks. |
| `/zombiemod spawn <genus> <x> <y> <z>` | Spawn one at a position. Accepts `~ ~ ~` relative and `^ ^ ^5` local — so `^ ^ ^5` is "five blocks in front of me". Works from the console and command blocks. |
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
