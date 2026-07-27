# Tropes not yet covered, and what each would need

A survey of the classic zombie-fiction roster against what ZombieMod can currently express. Sorted
by how much work they'd be, because several are free and a few are impossible.

## Free — buildable today, just needs a JSON file

No code required. Listed because they're the obvious next genera rather than the obvious next
features.

| Trope | Source | Build it from |
|-------|--------|---------------|
| **Screamer / Witch** | L4D, Dead Rising | `summon` on a short interval + a loud `sound`; passive until hurt (`hurt_by_target` only, no `nearest_target`) — that's the Witch exactly: harmless until you disturb her. |
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
| **Boomer bile / horde magnet** | `attract` ability | Pull nearby hostiles' targets onto one player. The genuinely missing L4D mechanic — bile is scarier than the explosion. |
| **Smoker tongue (real)** | `tether` ability | Persistent pull with line-of-sight break, rather than one shove. |
| **Screamer (real)** | `alert` ability | Wake and target-set every hostile in a radius. Distinct from `summon` — uses the mobs already there. |
| **Infector / patient zero** | `convert` ability | Turn a killed villager/player-corpse into a genus. The 1.8 mod's player-corpse feature lives here. |
| **Armoured / riot** | `equipment` field | Real armour and weapons per genus, not just dyed leather. Also unlocks weapon-wielding zombies. |
| **Blinker / phaser** | `teleport` ability | Short-range blink toward the victim, enderman-style. |
| **Spitter (projectile)** | `projectile` ability | Actually fire something — arrow, snowball, small fireball — instead of an aura. |
| **Necromancer** | `resurrect` ability | Re-raise nearby zombie corpses. Needs death tracking. |

## Moderate — needs new machinery

| Trope | Needs |
|-------|-------|
| **Day/night behaviour switch** | Conditional goal sets — "docile by day, hunts at night". Currently a genus has one fixed AI. Wants goals gated on a condition, reusing the `SpawnCondition` registry. |
| **Hive mind / pack tactics** | Goals that read other ZombieMod mobs nearby — flanking, surrounding, holding back until numbers are up. |
| **Mutation / stages** | A genus that becomes another genus on a trigger (damage taken, time alive). "Kill it before it turns." |
| **Boss / named encounter** | Boss bar, phases, loot. Would pair with your own MobHealth-NeoForge. |
| **Horde events** | A timed wave director — the L4D crescendo. Server-level, not per-mob. |
| **Sound-driven aggro** | Clicker done properly: blind, but hears sprinting and blocks breaking. Needs vibration/`GameEvent` listening. |

## Hard or impossible

Worth being explicit so nobody spends a weekend on them.

| Trope | Why |
|-------|-----|
| **Custom models** — lickers, crawling torsos, distended jaws | Needs a client mod. Vanilla clients can only render vanilla mob shapes. This is the wall the whole design is built against. |
| **Creeper-style swell on a non-creeper** | Renderer-bound. *Approximated* by ramping the `scale` attribute — see `zombiemod:fuse`. |
| **Grabs and pins** — the Jockey riding you, Smoker constricting | Vanilla has no player-restraint primitive. Could fake it with a mount, but the camera is wrong and players hate losing control. |
| **Dismemberment / partial damage** | No vanilla concept of limb state on a mob. |
| **Gore, blood decals** | Client rendering. |
| **Infection spreading to players** | Doable as effects, but "you turn into a zombie on death" is a separate gameplay mod, not a mob type. |

## The one I'd build next

**`equipment`.** It's cheap, it's the biggest visual jump left after player heads, and it unlocks a
whole family at once — riot police, soldiers, miners, the armoured horde. Right now a genus can be
any colour you like as long as it's leather.

After that, **`alert`** — because the scariest thing in zombie fiction isn't any single monster, it's
one of them noticing you and everything else turning around.
