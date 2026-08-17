# The shipped roster

58 genera, all in `src/main/resources/data/zombiemod/zombiemod/genus/`. They're ordinary datapack
files — override any of them by putting a file with the same name in a higher-priority datapack, or
delete the lot by shipping an empty override.

Weights are relative, drawn against `vanillaWeight` in the config (default **40**) — and crucially
the draw is **per base mob**, so a husk genus only competes with other husk genera. Archer at weight 7
gets essentially every eligible skeleton spawn, because nothing else wants skeletons.

**The balance was settled deliberately in August.** The default is low on purpose — if the mod is
installed you should notice — and the vanilla share is *depth-dependent*, because most genera carry
spawn conditions: measured at roughly 26% plain zombies on the surface and 13% deep underground. The
measured table and the reasoning live in the README's spawning section; `builtinGenera = false`
switches the whole shipped roster off for servers running their own.

## Speed horror — *28 Days Later*, *Dying Light*

| Genus | Weight | Idea |
|-------|-------:|------|
| **Runner** | 25 | Fast, fragile, dark only. Occasional Speed bursts. The one that made zombies scary again. |
| **Volatile** | 4 | Night-time surface only. Very fast, leaps, roars, trails soul particles. Dying Light's night terror. |

## The classic — *Night of the Living Dead*

| Genus | Weight | Idea |
|-------|-------:|------|
| **Walker** | 35 | Slow, tough, knockback-resistant, groans at low pitch. The shambling default. |
| **Swarmling** | 12 | Half-size, fast, weak, no abilities. Individually trivial, unpleasant in numbers. |

## Special infected — *Left 4 Dead*

| Genus | Weight | Idea |
|-------|-------:|------|
| **Boomer** | 8 | Bloated, slow, swells and bursts, blinds everything nearby. |
| **Smoker** | 6 | Keeps its distance and drags you in. Heavy smoke, wheezing. |
| **Hunter** | 7 | Climbs walls, pounces, and hunts pets as well as players. |
| **Charger** | 6 | Low, heavy, launches itself with big knockback. |
| **Spitter** | 7 | Won't melee. Poisons from range. |
| **Tank** | 2 | 2.2× scale, 120 HP, immovable, shockwaves that launch you. Deep underground only. |

## Fungal — *The Last of Us*

| Genus | Weight | Idea |
|-------|-------:|------|
| **Clicker** | 9 | Nearly blind (10-block follow range) but hits like a truck. Clicks. Underground. |
| **Bloater** | 8 | Huge, poisonous, regenerates, detonates on approach. |
| **Stalker** | 10 | Stands dead still and watches from up to 48 blocks. Never approaches. |

## The 1.8 plugin's own

| Genus | Weight | Idea | Old ability |
|-------|-------:|------|-------------|
| **Crawler** | 10 | Tiny, climbs walls, survives falls. | `SPIDER` |
| **Stormcaller** | 2 | Calls lightning on its victim from 20 blocks. Open sky only. | `LIGHTNING` |
| **Breeder** | 3 | Summons more zombies, capped at 6 nearby. | `BREEDER` |
| **Juggernaut** | 4 | 16 armour, 6 toughness, self-heals. | `BORG` |
| **Coward** | 30 | Runs away from players. | *(the original oddity)* |

## Elemental and biome-flavoured

| Genus | Weight | Idea |
|-------|-------:|------|
| **Ember** | 5 | Nether or deep underground. Immune to burning, trails flame. |
| **Frost** | 8 | Snowy biomes. Chills you with Slowness. |
| **Bogman** | 10 | A *drowned*, amphibious, swamp only. Inflicts Hunger. |
| **Dust Stalker** | 10 | A *husk*, desert/badlands/savanna. Kicks up blinding dust. |

## Armed and organised

| Genus | Weight | Idea |
|-------|-------:|------|
| **Screamer** | 5 | Won't fight you. Acquires you as a target, keeps its distance, and screams — handing you to every monster within 24 blocks. The horde does the work. |
| **Rioter** | 6 | Iron helmet, chainmail, shield, sword. Knockback-resistant, and alerts its mates at short range. |
| **Sapper** | 4 | A husk with a Sharpness II iron axe named *Pry Bar*. Demonstrates the full item-stack form with components. |

## Blinkers

| Genus | Weight | Idea |
|-------|-------:|------|
| **Ender Zombie** | 4 | Teleports **behind you, already facing you**, every few seconds. The tell isn't seeing it move — it's the sound at your back. A 1.8 favourite, rebuilt. |
| **Weeping Zombie** | *0* | The same trick with `only_when_unseen`: it will not blink while you're looking at it, so it closes every time you turn around. **Weight 0 — summon it deliberately**, it's too mean to meet by accident. |

### Herobrine

Weight 1 — rare on purpose. He is the whole teleport feature set pointed at one idea:

- **Watches.** No melee goal, no stroll, 0 damage, 64-block sight. He only ever stands and looks.
- **Moves when you don't.** `only_when_unseen` means he blinks only while you're facing away.
- **Won't be approached.** `min_distance: 6` — get within six blocks and he's gone.
- **Won't be shot.** `on_projectile` blinks him the instant an arrow lands.
- **Sometimes just leaves.** `vanish_chance: 0.25` — a quarter of the time he despawns instead.
- Wears a `head` of `"Herobrine"`, so on a vanilla client he has the face.

He cannot hurt you. That's the point.

### Nightstalker

| Genus | Weight | Idea |
|-------|-------:|------|
| **Nightstalker** | 12 | Runs from you in daylight and hunts you after dark. Same mob, two personalities, switched on the clock. |

## Bosses

| Genus | Weight | Idea |
|-------|-------:|------|
| **Patient Zero** | *0* | 250 HP, 2× scale, netherite sword, red notched boss bar, darkened sky and boss music. Calls in zombies, shockwaves, inflicts Mining Fatigue, regenerates. Two phases — poisons at 66%, then at 33% speeds up, hits for 16 and starts blinking behind you. Drops netherite scrap and a notch apple. Summon by building **a soul sand cross with a skull on the centre** and **right-clicking the skull with rotten flesh**, or `/zombiemod spawn patient_zero`. |

| **The Butcher** | *0* | The mid-tier fight. 120 HP, 1.5× scale, Sharpness II iron axe, chainmail, yellow notched bar — but **no darkened sky and no boss music**, because a mid-boss shouldn't announce itself like the end of the world. Leaps at 60%, shockwaves and inflicts Weakness at 30%. Its phase changes use `action_bar` where Patient Zero uses `title_and_chat`. Summon with **a bone on a zombie head sat on an iron block**. |

## The wall-eaters and the rest of the 1.8 set

| Genus | Weight | Idea |
|-------|-------:|------|
| **Breaker** | 8 | Chews through dirt, glass, doors and planks when it can't reach you. The 1.8 `Break`/`BreakRunner`, and the reason bases needed defending. |
| **Big Breaker** | 2 | Rare, 70 HP, 1.7× scale, slow and armoured. Eats everything the Breaker does *plus deepslate and cobbled deepslate* — so hiding in the deep dark is no longer hiding. |
| **Infester** | 4 | The same, except what it breaks becomes infested stone. The old `INFEST`. |
| **Archer** | 7 | A *skeleton*, so it visibly draws the bow — the pose only exists on the skeleton renderer. Uses vanilla's own bow goal. |
| **Lazer** | 2 | Keeps its distance and burns you with a **red hitscan beam** drawn in `dust` particles. No melee at all. |
| **Howler** | 2 | Deep underground. Fires the Warden's `sonic_boom` down a line. |
| **Spitfire** | 5 | Fires small fireballs. The old `LAZER`, finally doing something an archer doesn't. |
| **Weaver** | 6 | Climbs, and cobwebs the ground you're standing on. The old `WEB`. |
| **Zomborg** | 3 | Learns what hurt it. Take four swings with a sword and the fifth barely registers — switch weapons. The old `BORG`, and the memory survives a restart. |

| **Ghost** | 3 | Wears the name *and face* of a real player who has played on your server, trails soul particles, occasionally blinks behind you and sometimes just leaves. The old `GHOST` — which could only borrow the name. |
| **Outrider** | 3 | Rides in on a zombie horse with an iron sword. The old `jockey` field. |

## The infected

| Genus | Weight | Idea |
|-------|-------:|------|
| **Biter** | 6 | Hits softly and **infects**. Die within the minute — to anything, including a fall — and you get up as one of them. Milk cures it. The bite is the threat, not the damage. |

## The carrier

| Genus | Weight | Idea |
|-------|-------:|------|
| **Carrier** | 5 | Hunts villagers and livestock as well as players, and **what it kills gets up as one of them**. A villager rises as a zombie villager, keeping its name and armour. Patient Zero does this too, which is how a boss fight becomes a horde. |

## What these demonstrate

Worth reading the files rather than just the table — between them they exercise every feature:

- **Different base mobs** — Bogman is a drowned, Dust Stalker a husk. Anything vanilla works.
- **Navigation swaps** — Hunter and Crawler climb; Bogman is amphibious.
- **Attributes beyond the named fields** — Tank sets `attack_knockback` and `step_height`, Ember zeroes
  `burning_time`, Bogman raises `oxygen_bonus`.
- **Composed conditions** — Ember uses `any_of` to mean "the Nether *or* deep underground".
- **Explicit biome lists where no tag exists** — there is no `#minecraft:is_swamp`, so Bogman names
  the two swamps directly.
- **Abilities as parts** — Boomer is `fuse` + `effect` + `particles`; nobody had to write a "boomer
  ability".
- **Equipment both ways** — Rioter uses bare item ids, Sapper the full stack form with enchantments
  and a custom name.
- **A genus that fights by not fighting** — Screamer has no `melee_attack` at all.
- **Weight 0** — Weeping Zombie never spawns naturally and exists only for `/zombiemod spawn`.
- **A reactive ability** — Herobrine's teleport fires on being shot, outside the tick schedule.
- **A mob with no attack at all** — Herobrine does 0 damage and has no melee goal.
- **Conditional behaviour** — Nightstalker's `avoid_entity` and `melee_attack` are gated on the clock.

## Since this survey — August additions

Descriptions as they appear in the ZombieDex (each genus now carries its own `description` field, so
this table cannot drift from the game the way the ones above once did).

| Genus | Weight | Idea |
|-------|-------:|------|
| **Apothecary** | 6 | A village healer, still carrying whatever they were mixing. Turned with the rest of them. |
| **Blight** | 10 | Industrial rot on legs. Eats flowers, leaves, grass and moss wherever it walks, and leaves nausea behind it. |
| **Bramble** | 8 | The city taking itself back. Slow, tough, and lays moss as it goes. It is not hunting you; you are simply in the way. |
| **Commuter** | 45 | Office worker, still holding the shovel someone gave them at the end. Haunts the high streets it used to walk to work. |
| **Field Hand** | 10 | Farm labour that never went home. Slow, patient, and used to long days. |
| **Glowing One** | 3 | Something went very wrong underground. Green from the inside out, and it poisons the air around it. |
| **Harvester** | 40 | Still working the fields, still holding the hoe. It does not know the harvest is over. |
| **Corpse** | 0 | A player who died here, wearing their own face and carrying their own things. Kill it to get them back. |
| **Sleeper** | 2 | Enormous, ancient, and entirely uninterested in you until you are close enough to touch. Arrows still in it from people who tried. |
| **Townsfolk** | 14 | Somebody's neighbour. No skills, no weapon, no idea what happened. |
| **Vault Dweller** | 6 | The sirens sounded, the door rolled shut, and the vault kept its promise about survival - after a fashion. The suit still fits. The smile never left. |
| **The Borg Queen** | 0 | The hive's one mind. What wounds the queen, every drone soon shrugs off. Summons her own kind and teaches them her immunities. |

## Not zombies at all

Two genera exist to prove that a base does not have to be undead. Both are **weight 0** — deliberate
encounters rather than ambient ones, which matters more than usual here: proximity spawning ignores
the base-mob match, so a weight above 0 would put giants on the landscape whatever vanilla does.

| Genus | Base | Weight | Idea |
|-------|------|-------:|------|
| **Colossus** | `minecraft:giant` | *0* | 200 HP. Something went very wrong in the making of it. It has to stoop to get through the trees, and it does not stoop for anything else. Vanilla's giant has no AI of its own, so all of this one's behaviour is ours. |
| **Rusted Warden** | `minecraft:iron_golem` | *0* | 100 HP. It was built to guard, and it still is. Nobody has told it the village is gone. **The golem cannot wear anything** — no head, no equipment — so it is the one genus whose identity is carried entirely by name, attributes and behaviour. See [TROPES.md](TROPES.md). |
