# ZombieMod — commands reference

Source of truth: `src/main/java/com/sablednah/zombiemod/neoforge/ZombieModCommands.java`.

There is deliberately **no `/zombiemod reload`**. Genera are datapack data, so vanilla's own
`/reload` already does the job — edit a genus file, `/reload`, and it is live.

## Permissions

| Level | Commands |
|---|---|
| Everyone | `list`, `bestiary` |
| Op (level 2) | `spawn`, `status`, `observe`, `corpse`, `horde` |
| Admin (level 3) | `config` |

`bestiary` and `list` are open on purpose — the checklist is a player feature, and a companion
client mod should only ever be a nicer window onto it. `/zm` carries no bar of its own, so the alias
is exactly as restricted as the full name and no more.

---

## Finding out what is loaded

### `/zombiemod list`

Every genus the loaded datapacks define, by id. Open to everyone.

If this says **"No genera loaded"** the datapack did not load — that is the first thing to check when
nothing seems to be happening.

### `/zombiemod status`

**Start here when something seems not to work.** Reports what the mod actually believes: its
settings, whether the corpse genus resolved, whether FTB Chunks linked, and running counters for
proximity spawning, claim cancellations and spawn-rule rejections.

Those counters exist because several of this mod's features produce an **absence** — a claim that
stays empty, a spawn that does not happen — and an absence looks identical whether it is working or
broken. A number tells you which.

It is also the fastest way to discover you have been editing the wrong config file. A server config
is **per-world in singleplayer**, so the copy in `config/` does nothing for a single-player world.

---

## Spawning things

### `/zombiemod spawn <genus>`

Spawns one where you are **looking**, up to 48 blocks away.

`<genus>` accepts the full id (`zombiemod:coward`) or the bare name (`coward`). Bare names resolve by
path across *every* namespace, so a third-party `mypack:runner` is reachable as `runner`. If two
packs define the same name it reports the ambiguity rather than guessing.

### `/zombiemod spawn <genus> <x> <y> <z>`

Spawns one at a position. Takes `~ ~ ~` relative and `^ ^ ^5` local coordinates, so `^ ^ ^5` means
"five blocks in front of me".

Because it needs no player, this works **from the console and from command blocks** — which is how
you summon a boss from another mod, a map, or a redstone contraption. Combined with `"weight": 0`,
which keeps a genus off every spawn table, that is the whole toolkit for a one-off encounter.

---

## Hordes

### `/zombiemod horde list`

Every horde event the loaded datapacks define.

### `/zombiemod horde start <horde>`

Starts one on you now, regardless of config, cooldown, time of day or moon phase. This is how you
see `zombiemod:the_siege` without waiting for a full moon.

### `/zombiemod horde stop`

Ends the horde running on you.

---

## The ZombieDex

### `/zombiemod bestiary`

Your checklist in chat: what you have met, what you have killed, and what is left. Open to everyone.

### `/zombiemod bestiary book`

The same thing as a **written book** you can carry. This works on a completely unmodified client,
which is the point — the book is the vanilla half of the feature, not a fallback.

Right-clicking the book opens the proper screen for players who have the mod installed, and the
written pages for players who do not. **Sneak-right-click** always gives you the written pages, so
you can check what a vanilla player sees without launching a second instance. An anvil-renamed book
still counts as a dex.

### `/zombiemod bestiary info <genus>`

The full write-up for one genus — what it is, what it does, its abilities. How much you can read
depends on the `info` setting: by default you must have met it.

---

## Player corpses

All op-only. Worth having even when player zombies are working perfectly: *"my corpse went missing"*
was the single most common complaint about the 1.8 version, and an admin with no way to check had to
guess.

### `/zombiemod corpse list [player]`

Every recorded corpse, newest first. Names the player, where and when they died, and whether the
items are still outstanding.

A corpse that burned in lava or fell into the void is recorded as **outstanding with a reason**
rather than quietly settled, so you can tell the difference between "they got their things back" and
"there was nothing left to get".

A **grinder** is deliberately left undecidable — a hopper may already have taken the items, and
re-issuing them would duplicate an inventory. That stays an admin's call.

### `/zombiemod corpse give <player> [index]`

Hand a corpse's items straight back. `index` defaults to 1, the most recent; higher numbers reach
older corpses.

### `/zombiemod corpse respawn <player> [index] [here]`

Rebuild the corpse — face, armour and pockets — where it fell, so the player can go and kill it
properly.

Add **`here`** to rebuild it where *you* are looking instead. That is what you actually want when the
death spot is the problem: lava, the void, a grinder, or simply the bottom of a ravine nobody wants
to visit twice.

### `/zombiemod corpse forget <player> [index]`

Drop the record. Use it once a case is settled by hand.

---

## Live configuration

### `/zombiemod config`

Lists the nine switches that can be flipped without touching the file, and whether each is on.
**Admin only** (level 3), unlike the rest of the tree, because these change what the server does for
everyone rather than what happens in front of the person typing.

### `/zombiemod config <name> [on|off]`

Flips one and **writes it to disk**, so it survives a restart. Omit `on`/`off` to toggle.

| Switch | Default |
|---|---|
| `enabled` | on |
| `hordes` | off |
| `playerZombies` | off |
| `proximity` | off |
| `bestiary` | on |
| `perGenus` | off |
| `hideUnspawnable` | off |
| `unspawnableRevealedWhenMet` | on |
| `logSpawns` | off |

Everything else lives in `zombiemod-server.toml` — see the settings reference.

---

## Testing

### `/zombiemod observe [on|off]`

Take no damage while staying a **completely normal target**.

This exists because the usual ways to survive a test do not work here. Creative mode and every
god-mode command set vanilla's invulnerable flag, and `LivingEntity.canBeSeenAsEnemy()` is
`!isInvulnerable() && canBeSeenByAnyone()` — so an invulnerable player is one no zombie will ever
walk towards. Useless when the thing you are testing *is* what zombies do.

Observer mode changes nothing about the player. You are targeted, chased, swung at, knocked back and
teleported behind exactly as before; the damage is cancelled on arrival. **Knockback still lands**,
which is deliberate — being shoved off a ledge is part of what a Charger is.

---

## `/zm`

An alias for the whole tree, implemented as a Brigadier **redirect** rather than a second
registration, so every subcommand, argument type and suggestion is the same object and the two
cannot drift.

It carries no permission requirement of its own — each subcommand keeps its own — so `/zm bestiary`
and `/zm list` are open to everyone exactly as their full-length forms are.
