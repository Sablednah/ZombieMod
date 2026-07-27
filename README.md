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

> **Alpha.** The genus format and AI system work and are in use; natural spawning is not wired up
> yet, so genera currently only appear via command. See [Status](#status).

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
- Weighted selection between genera sharing a base mob

Not yet:

- **Natural spawning.** `weight` currently only decides *which* genus rides a spawn vanilla was
  already going to make — genera don't yet add spawns of their own, per-biome. Next up.
- The 1.8 plugin's per-tick abilities: `EXPLODE`, `HEAL`, `STOMP`, `LIGHTNING`, `WEB`, `SPIDER`,
  `BORG`, `HUNTER`, and the rest.
- Navigation swapping (spider climbing), mounts/jockeys.
- Richer appearance: player-head faces via the `minecraft:profile` component, which would give each
  genus a distinct face on a vanilla client.
- Optional client mod for real models and animation.

Known limitation: goal targets come from a fixed list of classes, because vanilla's targeting goals
are typed on a Java class rather than an entity id. So "avoid wolves" works; "avoid a modded mob"
does not, yet.

## Building from source

Requires JDK 21. Standard NeoForge ModDevGradle setup:

```
export JAVA_HOME=/path/to/jdk21

./gradlew compileJava   # fast inner loop
./gradlew build         # jar in build/libs/zombiemod-<version>.jar
./gradlew runServer     # dev dedicated server (needs run/eula.txt)
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
