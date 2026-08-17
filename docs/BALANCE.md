# Balance

The rules the shipped roster's numbers follow, and where they deliberately don't.

This file exists because the model didn't. A balance pass in August set `xp`, `bounty` and
`follow_range` across the roster from a threat score that was never written down, so when Colossus and
Rusted Warden were added afterwards there was nothing to check them against — and the next person to
look (2026-08-17) reached for the wrong comparator and nearly "fixed" two genera that were correct.
**Numbers that only exist as a habit go wrong silently.** Write the rule down, then measure against
it.

None of this is enforced in code. It is a convention for the shipped roster; your own datapack owes it
nothing.

## Weights

Covered in [ROSTER.md](ROSTER.md) — settled by play in August and the one part of the balance that is
*not* a first guess. Note the draw is **per base mob** and rides against `vanillaWeight` (default 40).

## `follow_range` — the sensory tiers

How far a genus notices you. Values sit on a **4-block grid**: 16, 20, 24, 28, 32, 36, 40, 48.

| Range | Who, and what it means |
|---|---|
| **16** | Barely aware. Walker, Commuter, Harvester, Crawler — the ambient dead, which you can walk past. |
| **20** | Ordinary people. Townsfolk, Field Hand, Swarmling, the player corpse. |
| **24** | Awake. Most of the elemental and villager genera. The default if nothing else applies. |
| **28** | Hunting. Carrier, Biter, Rioter, Smoker, Weaver, Spitter, Spitfire, Dust Stalker. |
| **32** | Purposeful. Breaker, Big Breaker, Zomborg, Glowing One, Rusted Warden. |
| **36** | Long sight. Runner, Archer, Ghost, Howler, Lazer, Outrider, Charger. |
| **40** | Predatory. Tank, Clicker, Ender, Nightstalker, Stormcaller, Volatile, and the Borg Queen and Butcher. |
| **48** | Sees you first. Hunter, Stalker, Screamer, Coward, Weeping, Colossus, Patient Zero. |

**Four deliberate exceptions**, each of which *is* the genus:

| Genus | Range | Why |
|---|---:|---|
| **Sleeper** | 6 | Entirely uninterested until you are close enough to touch. |
| **Bramble** | 12 | It is not hunting you; you are in the way. |
| **Bloater / Boomer** | 18 | Blind bombers — they need to be stumbled into, not to come and find you. |
| **Herobrine** | 64 | He watches from across the valley. It is the whole character. |

If you add a genus, take a tier. If you take a number off the grid, it should be because the number is
the point, and it belongs in the table above.

## `xp` — half the health, adjusted for role

The base rule across the roster is:

```
xp ≈ health / 2
```

It holds within ±25% for forty-odd of the fifty-eight, which is what makes it a rule rather than an
observation. Three deliberate departures:

**Ambient genera are discounted toward vanilla.** The things you kill constantly are pulled down
toward vanilla's 5, so a common kill doesn't inflate. Roughly `xp ≈ health/2 × 0.5` once weight is
high:

| Genus | Weight | health/2 | Actual xp |
|---|---:|---:|---:|
| Commuter | 45 | 12 | 6 |
| Harvester | 40 | 15 | 8 |
| Walker | 35 | 13 | 6 |
| Coward | 30 | 7 | 2 |
| Swarmling | 12 | 4 | 2 |

**Bosses carry a premium** of roughly 2.5–3.5×, because the fight is the content:

| Genus | health/2 | Actual xp |
|---|---:|---:|
| Patient Zero | 125 | 300 |
| The Borg Queen | 80 | 250 |
| The Butcher | 60 | 200 |

**Herobrine is a rarity premium**, not a threat one — 100 xp on a 40 HP mob that cannot hurt you at
all. You are being paid for the encounter, not the kill.

**Colossus and Rusted Warden are ordinary under this rule**, despite being weight 0 and enormous.
They are not bosses — no boss bar, no phases, no ritual — so they take the plain `health/2`: 200 → 100
and 100 → 50. They scale off **Tank** (120 → 60), the genus they actually belong beside. Compare
either to the Butcher and they look badly underpaid; the Butcher's 200 is a boss premium on 120 HP.
This is exactly the trap the top of this file describes.

## `bounty` — a quarter to a third of the xp

`xp / bounty` sits between **2.5 and 4.0** for nearly everything. Pick the xp first, then divide.

Two departures worth knowing:

- **Patient Zero** pays 150 on 300 xp (ratio 2.0) — a boss should be worth robbing.
- **Coward** pays 6 on 2 xp (ratio 0.3), by a distance the biggest outlier in the roster. The
  argument for it: bounty rewards *difficulty of the kill* where xp rewards threat, and a genus whose
  whole behaviour is fleeing at sprint speed 1.5 is genuinely hard to catch. The argument against: at
  weight 30 it is one of the commonest things in the world, and 6 is what a 60 HP Bramble pays.
  **Unsettled — it wants play on a server with an economy**, and it is the single number here most
  likely to be wrong.

## Ability numbers

Scaled with the genus rather than set per ability. `shockwave` is the clearest example, and the
pattern to copy:

| Genus | health | radius | damage | knockup |
|---|---:|---:|---:|---:|
| The Butcher (phase 2) | 120 | 4.5 | 3.0 | 0.7 |
| Rusted Warden | 100 | 4.0 | 5.0 | 1.2 |
| Tank | 120 | 5.0 | 4.0 | 1.0 |
| Patient Zero | 250 | 6.0 | 5.0 | 0.9 |
| Colossus | 200 | 7.0 | 7.0 | 1.4 |

Colossus is the hardest hitter in the roster — 18 melee and a 7-damage shockwave, above Patient
Zero's 12 and 5. That is deliberate: vanilla's own heavies sit there (Warden 30, Iron Golem up to 21,
Ravager 12), and a Giant that hits like a zombie would be a joke. It is weight 0, so nobody meets one
by accident.

`leap` range runs 10–14 and must stay below `follow_range` or the goal can never fire. `alert` radius
tracks intent rather than size: Rioter reaches 14 for 6 mates, Screamer 24 for 16, because handing
you to the whole neighbourhood is the Screamer's entire job.

## What is still a first guess

Everything above is *consistent*; consistency is not the same as *right*. These need play, not
arithmetic:

- **Coward's bounty**, above.
- **Phase thresholds** — Patient Zero flips at 66% and 33%, the Butcher at 60% and 30%. Nobody has
  fought either enough to say whether the second phase arrives too early to matter.
- **Drop rates.** Fifty-eight tables load clean, which validates every item id and nothing else. None
  are farmed yet.
- **The Sleeper's 6-block range** — menacing, or merely broken.
- **Colossus and Rusted Warden's stock-take.** Their dolls are confirmed (2026-08-17) and their
  xp/bounty follow the rule above, but neither went through the original sweep, so their ability
  intervals and radii are first guesses where the other fifty-six are derived.
