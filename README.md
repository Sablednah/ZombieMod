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

### Spawning

A genus with a `weight` above zero can claim a spawn that its `base` mob was going to make anyway.
Which genus gets it is a weighted draw — and **"leave it as a plain zombie" is an entry in that same
draw**, weighted by `vanillaWeight` in the config. Without that, the moment you shipped one genus it
would claim every zombie in the world and plain zombies would quietly cease to exist.

So with `vanillaWeight = 100` and genera weighted 30 and 10, any eligible zombie spawn comes out
roughly 71% vanilla, 21% the first genus, 7% the second.

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
| `zombiemod:any_of` | `conditions` — passes if any nested condition passes |
| `zombiemod:not` | `condition` — inverts one |

Conditions are a **registry**, not a fixed list, so another mod can contribute its own types via
`SpawnConditionTypes.register`. That's how CityWorld integration is planned to work — a
`cityworld:lot` condition letting a genus prefer the wilderness or a particular district, as an
optional dependency that ZombieMod itself never links against.

### Goal types

Each goal takes a `priority` (**lower runs first**, as in vanilla) plus its own options.

| Type | Options | Does |
|------|---------|------|
| `zombiemod:avoid_entity` | `target`, `distance` (8.0), `walk_speed` (1.0), `sprint_speed` (1.35) | Runs away. **This is the coward.** |
| `zombiemod:melee_attack` | `speed` (1.0), `follow_unseen` (false) | Walks up and hits its target. |
| `zombiemod:look_at` | `target` (player), `distance` (8.0), `probability` (0.02) | Watches. At high range and probability, **this is Herobrine**. |
| `zombiemod:random_stroll` | `speed` (1.0) | Wanders. Without it, an idle mob stands perfectly still. |
| `zombiemod:random_look` | — | Idle head movement. Cheap, but its absence reads as "broken". |
| `zombiemod:float` | — | Swims instead of sinking. |
| `zombiemod:nearest_target` † | `target`, `must_see` (true) | Picks a victim. |
| `zombiemod:hurt_by_target` † | — | Fights back when struck. |

† Belongs in `target_goals`, not `goals`.

`target` is one of: `player`, `living`, `mob`, `monster`, `animal`, `villager`, `zombie`, `wolf`,
`ocelot`, `cat`.

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
| `zombiemod:explode` | `power`, `destroy_blocks`, `kills_self`, `trigger_radius` |
| `zombiemod:shockwave` | `radius`, `damage`, `knockup` — launch and hurt everything nearby |
| `zombiemod:particles` | `particle`, `count`, `spread` |
| `zombiemod:sound` | `sound`, `volume`, `pitch` |

`target` is `self`, `victim` (whatever it's currently attacking) or `nearby_players`.

Two defaults worth knowing. `explode` has `destroy_blocks: false` — a zombie that eats your build is
a very different proposition from one that hurts, so griefing is opt-in. And it only fires when
something is actually within `trigger_radius`, or an interval-timed bomb is just a mob that deletes
itself in an empty field.

Rather than one ability per 1.8 name, the set is compositional: `effect` + `particles` + `sound`
between them build most of the old flavour abilities, so you assemble a screamer or a plague carrier
out of parts instead of waiting for that exact ability to exist.

## Configuration (`config/zombiemod-server.toml`)

| Option | Default | Purpose |
|--------|---------|---------|
| `enabled` | `true` | Master switch. Off means everything spawns exactly as vanilla would. |
| `vanillaWeight` | `100` | How strongly to leave a mob alone, weighed against the genera that could claim it. `0` means a genus claims every eligible spawn. |
| `logSpawns` | `false` | Log every genus spawn to the console. Noisy; for tuning weights. |

## Commands

| Command | |
|---------|--|
| `/zombiemod list` | What genera the loaded datapacks define. |
| `/zombiemod spawn <genus>` | Spawn one where you're standing. |

Both require permission level `LEVEL_GAMEMASTERS` (op 2). `spawn` accepts either the full id
(`zombiemod:coward`) or just the name (`coward`), and tab-completes both.

There is deliberately **no** `/zombiemod reload` — genera are datapack data, so vanilla `/reload`
already does the job.

## Status

Working:

- Genera as a datapack registry, hot-reloadable
- AI assembled from JSON, with vanilla goals as the building blocks
- Attributes, scale, dyed armour, custom names
- Genus survives world save/reload (goals are rebuilt, attributes persist in NBT)
- Weighted spawning with per-genus conditions (biome, dimension, height, light, spawn reason),
  and a configurable share of spawns left vanilla
- Abilities: potion effects, healing, lightning, explosions, shockwaves, particles, sounds

Not yet:

- **Genera don't add spawns of their own.** They claim spawns the base mob was already making, so a
  world has the same number of zombies as vanilla — just more varied ones. Choosing *which* genus
  appears where already works (`spawn.biomes` takes a biome tag, so swamp- or ice-flavoured zombies
  are a datapack away); making a swamp spawn *more* zombies than vanilla needs
  `neoforge:add_spawns` biome modifiers. Planned.
- **No CityWorld integration yet.** The condition registry is in place for it; the adapter itself —
  "only in the wilderness", "only on a city lot" — isn't written.
- More abilities: `WEB`, `SPIDER` (climbing), `BORG`, `HUNTER`, `BREEDER`, `INFEST` from the 1.8 set.
- Navigation swapping (spider climbing), mounts/jockeys.
- Richer appearance: player-head faces via the `minecraft:profile` component, which would give each
  genus a distinct face on a vanilla client.
- Optional client mod for real models and animation.

**A malformed genus file stops the world loading**, rather than being skipped — that's how vanilla
treats every datapack registry, but it bites harder here because these files are meant to be
hand-written. The server log names the file and the field; check it before assuming a world is
corrupt.

Known limitation: goal targets come from a fixed list of classes, because vanilla's targeting goals
are typed on a Java class rather than an entity id. So "avoid wolves" works; "avoid a modded mob"
does not, yet.

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
