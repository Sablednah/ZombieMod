# ZombieMod — reforged for NeoForge

**Build your own undead.** ZombieMod turns zombie types into **datapack files** — health, size,
colour, face, and above all **AI** — so a coward that flees on sight, a stalker that watches you from
across the valley, and a climber that comes over the wall are all just JSON. It ships with **61 of
them** already written, drawn from the genre canon.

A full **NeoForge rewrite** of the 2013 ZombieMod Bukkit plugin, rebuilt from the ground up for
modern Minecraft.

**Your players do not need to install anything.** ZombieMod registers no entity types of its own, so
every genus is an ordinary vanilla mob wearing changes a vanilla client already knows how to draw.
Tested for real, twice, with an unmodified client off the Mojang launcher.

---

## What arrives in your world

Not a mob pack — a **cast**. Every one of these is in the box:

- **Speed horror** — the **Runner**, fast and fragile and dark-only. The **Volatile**, which owns the
  night surface and leaps.
- **The classic shamble** — the **Walker**, slow and tough and knockback-resistant. The
  **Swarmling**, half-size and trivial alone.
- **Special infected** — a **Boomer** that swells and bursts and blinds the room, a **Smoker** that
  keeps its distance and drags you in, a **Hunter** that climbs walls and pounces, a **Spitter**
  that will not melee you at all, and a 120 HP **Tank** that only lives deep underground.
- **Fungal** — the **Clicker**, near-blind at ten blocks and hits like a truck. The **Stalker**,
  which stands dead still and watches from forty-eight blocks and never once approaches.
- **Herobrine.** Weight 1, rare on purpose, zero damage, no attack goal. He watches. He only moves
  while you are facing away, he leaves if you get within six blocks, he blinks the instant an arrow
  lands, and a quarter of the time he simply is not there any more. He cannot hurt you. That is the
  point.
- **The wall-eaters** — a **Breaker** that chews through dirt, glass, doors and planks when it cannot
  reach you, and a **Big Breaker** that eats deepslate too, so hiding in the deep dark stops being
  hiding.
- **Zombies that were people** — **Townsfolk**, **Commuters** still holding the shovel someone handed
  them at the end, **Field Hands**, an **Apothecary** still carrying what they were mixing, and a
  **Vault Dweller** whose suit still fits and whose smile never left.
- **The Ghost**, wearing the name *and the face* of a real player who has actually played on your
  server.

…and thirty-odd more: Ender Zombies that blink in behind you already facing you, a **Weaver** that
cobwebs the ground under your feet, a **Zomborg** that learns what hurt it and stops taking it, an
**Archer** built on a skeleton so you can see it draw, a **Lazer** that burns you down a red hitscan
beam, a **Howler** that fires the Warden's sonic boom, and a **Nightstalker** that runs from you in
daylight and hunts you after dark.

## Faces

Fifty-one of the sixty-one wear a **real face**, rendered on an unmodified client, because a genus
can carry a player-head texture in its JSON. It is the single thing that makes a crowd read as a
cast rather than a palette swap — you recognise a Clicker before you are close enough to be in
trouble.

## The systems underneath

| | |
|---|---|
| **Bosses** | Boss bars, health phases, loot tables, and summon rituals — build a soul sand cross, put a skull on it, right-click the skull with rotten flesh, and meet Patient Zero. |
| **Hordes** | Four wave events with a director and a boss bar. Ring a bell and the survivors glow. Go a minute without a kill and they glow anyway, because hunting one straggler across a dark forest is a problem Minecraft has never solved well. |
| **Infection** | A bite marks you. Die within the minute to *anything* — including a fall — and you get up as one of them. It spreads through livestock on its own. Milk cures it, for you and for the cow. |
| **Conversion** | What a Carrier kills gets up as one of them. A villager rises as a zombie villager, keeping its name and its armour. |
| **Mutation** | A genus becomes a different genus on a trigger — wounded, on fire, in water, in the wrong dimension. |
| **Player corpses** | Die and your corpse gets up wearing your real skin and carrying your things. Kill it to get them back. Admins get a recovery ledger for when it goes in the lava. Off by default. |
| **ZombieDex** | A per-player bestiary — in chat, as a written book that works on a vanilla client, on scoreboards, and on a proper screen for players who do have the mod. Entries unlock by meeting things. |
| **Proximity spawning** | Zombies placed just out of sight around each player, ignoring vanilla's spawn table entirely. The thing that made the 1.8 plugin's world feel *occupied*. Off by default. |
| **Bounties** | Per genus. Paid into the server's economy via [SableCraft Standards](https://github.com/Sablednah/SableCraft-Standards) if it is installed, and tallied to a scoreboard everywhere else — so the reward is real on a server with nothing else added. |

## Build your own

A genus is one JSON file in a datapack at `data/<your_pack>/zombiemod/genus/<name>.json`. Run
`/reload` and it is live — no restart, because genera are a datapack registry and vanilla's own
reload picks them up.

```json
{
  "name": "Coward",
  "base": "minecraft:zombie",
  "weight": 30,
  "health": 14.0,
  "speed": 1.25,
  "scale": 0.9,
  "clear_goals": true,
  "goals": [
    { "type": "zombiemod:avoid_entity",  "priority": 1, "target": "player", "distance": 12.0 },
    { "type": "zombiemod:random_stroll", "priority": 7, "speed": 0.9 }
  ]
}
```

That is the whole coward. What you have to work with:

- **12 goal types** — vanilla's own pathfinder goals, pulled apart and recombined. Avoid, melee,
  bow, watch, stroll, float, and targeting by ear or by block.
- **21 abilities** — effects, healing, lightning, explosions, creeper-style fuses, shockwaves,
  leaps, drags, summons, block-breaking, projectiles, cobwebs, infection, conversion, a real
  guardian beam, particle hitscan rays, adaptive resistance and teleport.
- **15 spawn conditions** — biome, dimension, height, light, sky, depth below the local surface,
  time of day, moon phase, land claims and CityWorld districts — composable with `any_of` and `not`.
- **Any vanilla mob as a base.** Husks, drowned, skeletons, zombie villagers, giants, iron golems.
- **Attributes, equipment, faces, navigation swaps** — climbing, swimming, amphibious.

Goals, abilities and conditions are all **registries**, so another mod can add its own types without
ZombieMod knowing it exists.

**→ Full genus reference, every field, and guides to building your own roster:
[sablecraft.co.uk/zombiemod-reforged](https://sablecraft.co.uk/zombiemod-reforged/)**

## Balance, deliberately

The shipped roster does not take over your world, and it does not politely hide either. "Leave it as
a plain zombie" is an ordinary entry in the same weighted draw as every genus, weighted by
`vanillaWeight` — default **40**, settled by play rather than guessed.

Because most genera carry spawn conditions, the mix is depth-dependent, measured in a fresh world:

| Where | Genera eligible | Stay vanilla |
|---|---|---|
| Surface, at night | 9 | 26% |
| 20 blocks down | 31 | 14% |
| 45 blocks down | 35 | 13% |

Raise it for a mostly-vanilla world, drop it to 0 and a genus claims every eligible spawn, or set
`builtinGenera = false` and run nothing but your own.

## Plays well with others

- **FTB Chunks** — claims are respected. Genera stay out of them, and ZombieMod's mobs will not
  break blocks inside one. (Worth knowing: FTB Chunks itself does not cover general mob
  block-breaking, so a claim does nothing against a Breaker until ZombieMod closes it from this
  side.) Linked by reflection, completely inert without it.
- **CityWorld** — genera that key off districts, lots and wildness, so Commuters haunt the high
  streets and Harvesters work the farms. Also optional, also inert without it.

## Commands

`/zombiemod list` and `/zombiemod bestiary` are open to everyone; the rest is op-only. `/zm` is an
alias for all of it. `/zombiemod spawn <genus>` puts one where you are looking, `/zombiemod status`
tells you what the mod believes its settings are, `/zombiemod observe` lets you stand in a fight and
take no damage while remaining a completely normal target, and `/zombiemod horde start` calls one in.

**→ Every command and every setting:
[sablecraft.co.uk/zombiemod-reforged](https://sablecraft.co.uk/zombiemod-reforged/)**

## Requirements

| Minecraft | NeoForge | Java |
|---|---|---|
| 1.21.11 | 21.11.42+ | 21 |
| 26.1.2 | 26.1.2.95+ | 25 |
| 26.2 | 26.2.0.59+ | 25 |

There is **a jar per Minecraft version**, named for the one it was built against — take the one that
matches your server.

**Install on the server. That is all.** No dependencies, and nothing your players have to do —
they can join on a stock client from the Mojang launcher and meet every one of the sixty-one.
Installing it client-side as well is optional and adds the ZombieDex screen; people with and without
it play together on the same server.

## Credits and licence

ZombieMod is licensed under **MIT**.

- Original **ZombieMod** Bukkit plugin by **Sablednah**, 2013.
- This NeoForge rewrite by **Sablednah**.

Full docs, genus reference and guides:
**[sablecraft.co.uk/zombiemod-reforged](https://sablecraft.co.uk/zombiemod-reforged/)**
Source, issue tracker and full port history: see the GitHub repository.
