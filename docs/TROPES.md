# Tropes not yet covered, and what each would need

A survey of the classic zombie-fiction roster against what ZombieMod can currently express. Sorted
by how much work they'd be, because several are free and a few are impossible.

## Free — buildable today, just needs a JSON file

No code required. Listed because they're the obvious next genera rather than the obvious next
features.

| Trope | Source | Build it from |
|-------|--------|---------------|
| **Witch** | L4D | Passive until hurt — `hurt_by_target` only, no `nearest_target` — then lethal. Harmless until you disturb her. |
| **Jockey** | L4D | `leap` with high `lift` and low `power`, small `scale`. Won't actually ride you, but reads as a leaper. |
| **Crawler (legless)** | CoD Zombies | `scale` 0.5 + `gravity` attribute raised + slow speed. |
| **Toxic barrel / Tarman** | Return of the Living Dead | `fuse` with a big `effect` payload instead of much `power`. |
| **Irradiated** | Fallout ghouls, STALKER | `effect` with `wither` on nearby players + `glowing` on self. |
| **Plague doctor** | folk-horror | A `zombie_villager` base with `head` and Hunger/Nausea `effect`. |
| **Deep one** | Lovecraft, *drowned* variants | `drowned` base, `amphibious`, ocean biome tags, `pull`. |
| **Frozen berserker** | *30 Days of Night* | `frost`-like but fast, in `#minecraft:is_mountain`. |
| **Hive drone / swarm carrier** | *World War Z* | `summon` with `swarmling` numbers and a low `max_nearby`. |

## Cheap — one small ability each

| Trope | Needs | Notes |
|-------|-------|-------|
| **Smoker tongue (real)** | `tether` ability | Persistent pull with line-of-sight break, rather than one shove. |
| **Spitter (projectile)** | `projectile` ability | Actually fire something — arrow, snowball, small fireball — instead of an aura. |
| **Necromancer** | `resurrect` ability | Re-raise nearby zombie corpses. Needs death tracking. |

## Moderate — needs new machinery

| Trope | Needs |
|-------|-------|
| **Hive mind / pack tactics** | Goals that read other ZombieMod mobs nearby — flanking, surrounding, holding back until numbers are up. |
| **Mutation / stages** | A genus that becomes *another genus* on a trigger. `phases` covers stages within one mob; this is the "kill it before it turns" version that swaps identity. |
| **Boss / named encounter** | See the dedicated section below — partly possible already. |
| ~~**Horde events**~~ | **Done** — a `zombiemod:horde` datapack registry with waves, a bar and conditions. |
| **Sound-driven aggro** | Clicker done properly: blind, but hears sprinting and blocks breaking. Needs vibration/`GameEvent` listening. |

## Boss zombies (owner's idea, 2026-07-28)

Named, summoned, one-off encounters rather than anything you meet in the wild.

**Already works today:**

- `"weight": 0` keeps a genus off every natural spawn table entirely. Weeping Zombie proves it.
- `/zombiemod spawn <genus> <x> <y> <z>` runs from **command blocks, the console and other mods** —
  it takes an explicit position and needs no player, so a datapack or plugin can summon one.
- Big stat blocks, equipment, several abilities at once — Tank is already 120 HP and 2.2× scale.

**What's missing:**

| Piece | Notes |
|-------|-------|
| ~~**Boss bar**~~ | **Done** — a `boss` block on the genus. See Patient Zero. |
| ~~**Use-item-on-block trigger**~~ | **Done** — the `zombiemod:ritual` datapack registry. |
| ~~**Summon structure**~~ | **Done** — a `pattern` on the ritual, offset-based, tried in all four rotations. |
| ~~**Phases**~~ | **Done** — a `phases` list, abilities gated on health and attributes applied once. |
| ~~**Loot**~~ | **Done** — a `loot` block naming a loot table. |

**The whole boss list is done.** What's left for bosses is content rather than machinery: more of
them, and better rituals.

## Hard or impossible

Worth being explicit so nobody spends a weekend on them.

| Trope | Why |
|-------|-----|
| **Custom models** — lickers, crawling torsos, distended jaws | Needs a client mod. Vanilla clients can only render vanilla mob shapes. This is the wall the whole design is built against. |
| **Renderer-bound effects** — the guardian beam, the skeleton's bow draw, the creeper swell | Each is drawn by one specific renderer. Sometimes there's a way round: the beam works by *parenting an invisible guardian* (see `beam`), the swell is approximated with the `scale` attribute, and the bow draw simply requires a skeleton `base`. Sometimes there isn't. |
| **Creeper-style swell on a non-creeper** | Renderer-bound. *Approximated* by ramping the `scale` attribute — see `zombiemod:fuse`. |
| **Grabs and pins** — the Jockey riding you, Smoker constricting | Vanilla has no player-restraint primitive. Could fake it with a mount, but the camera is wrong and players hate losing control. |
| **Dismemberment / partial damage** | No vanilla concept of limb state on a mob. |
| **Gore, blood decals** | Client rendering. |
| ~~**Infection spreading to players**~~ | **Done** — `infect`, and it turns out to fit a mob type fine: the bite is the mob's, the turning is a consequence. Milk cures it. |

## Done since this was written

**`convert`** shipped — see Carrier. **`behaviours`** shipped — conditional goal sets, with a new `zombiemod:time` condition. See
Nightstalker. **`teleport`** shipped — see Ender Zombie and Weeping Zombie in [ROSTER.md](ROSTER.md).
**`equipment`** and **`alert`** both shipped — see Rioter, Sapper and Screamer in
[ROSTER.md](ROSTER.md). Boomer bile is largely covered by `alert` too: the horde-magnet effect was
always the scary half.

## Done since this was written

Everything that was on this list as "the one I'd build next" has shipped: `equipment`, `alert`,
`teleport`, day/night `behaviours`, the whole boss list, `convert`, `infect`, and proximity spawning.

## The one I'd build next

**Mutation between genera** — a genus that becomes *another* on a trigger, so "kill it before it
turns" applies to the monsters as well as to you. A Bloater that becomes something worse at low
health, or a Walker that turns Runner when it sees you.

After that it's mostly content rather than machinery: more hordes, more genera, and the CityWorld
conditions so a city block spawns differently from the wilderness around it.

Full ordering, and what's already done, is in [STATUS.md](STATUS.md).
