# Permissions

**ZombieMod registers six permission nodes, and every one of them defaults to the op level the
command always needed.** With no permissions mod installed nothing changes: op your staff, or map a
group to op level 2, and everything below follows. With one installed — LuckPerms, or
[SableCraft Standards](https://github.com/Sablednah/SableCraft-Standards) with its handler selected —
you can hand one branch of the tree to somebody who is not an op: a storyteller who can call a horde
and spawn a genus without also being handed `/stop`.

The genus list, the whole bestiary, and turning your own observer mode off need no permission at
all, because they are player features rather than staff tools.

Correct for **3.5.0**, and identical on all three supported Minecraft versions.

## The nodes

| Node | Grants | Default |
|---|---|---|
| `zombiemod.spawn` | `/zombiemod spawn` | op level 2 |
| `zombiemod.horde` | `/zombiemod horde list`, `start`, `stop` | op level 2 |
| `zombiemod.corpse` | the whole corpse ledger — `list`, `give`, `respawn`, `forget` | op level 2 |
| `zombiemod.observe` | turning observer mode **on**, for yourself or anyone, and off for *someone else* | op level 2 |
| `zombiemod.status` | `/zombiemod status` | op level 2 |
| `zombiemod.config` | `/zombiemod config` | op level **3** |

Six rather than one per command, because the useful split is by what an owner delegates, not by
what a command is called. Spawning things and running hordes are what a game master does; the
corpse ledger and observer mode are staff tools; `config` changes what the server does for
everyone. `zombiemod.*` is your permissions mod's wildcard, not a node of ours — LuckPerms and
Standards both expand it.

All six are **booleans**. There is nothing here that takes a number.

### The default is the level, and the level still works

A node's default resolver is "does this player hold op level 2" (3 for `config`). NeoForge's own
default handler, which is what answers when no permissions mod is installed, returns exactly that.
So:

- **No permissions mod:** identical to every earlier release. A plain op can run all of it.
- **A permissions mod that grants nothing:** identical to every earlier release, because the
  fallthrough for an unset node is its default. Installing a manager changes nothing until you
  grant or deny something.
- **A grant:** the player runs that branch whether or not they are an op.
- **A deny on an op:** the branch is refused to them. A permissions mod's explicit answer beats
  the level, in both directions.

### The storyteller example

The case this was built for. A storyteller runs sessions with
[LegendQuest StoryTeller](https://github.com/Sablednah/LegendQuest-StoryTeller), should be able to
call hordes and spawn genera into the story, and should not be an op, because op brings `/stop`,
`/ban` and every block on the server with it. With Standards' handler selected:

```
/rank group storyteller create
/rank group storyteller set zombiemod.spawn true
/rank group storyteller set zombiemod.horde true
/rank group storyteller set zombiemod.observe true
/rank user <player> group add storyteller
```

or, for all of it in one line, `set zombiemod.* true` — which includes `config` and the corpse
ledger, so decide whether you mean that. `/rank check <player> zombiemod.horde` tells you which
rule answered. LuckPerms is the same shape with its own commands.

Note that `/zombiemod horde start` directs a horde at whoever typed it, and `spawn` with no
position spawns where they are looking. A storyteller with these nodes is running the encounter
from inside it, which is the point.

## Every command, and what it needs

`/zm` is an alias for `/zombiemod` and carries exactly the same gates — it is a Brigadier redirect
onto the same node tree, not a second registration, so the two cannot drift apart.

### Open to everyone

| Command | What it does |
|---|---|
| `/zombiemod list` | Lists every genus loaded |
| `/zombiemod bestiary` | Your ZombieDex, in chat |
| `/zombiemod bestiary book` | The same dex as a written book — works on a vanilla client |
| `/zombiemod bestiary info <genus>` | One genus's entry |
| `/zombiemod observe off` | **Turns your own observer mode off.** Always available — see below |

### `zombiemod.spawn` — default op level 2

| Command | What it does |
|---|---|
| `/zombiemod spawn <genus> [pos]` | Spawns one where you are looking, or at a position |

### `zombiemod.horde` — default op level 2

| Command | What it does |
|---|---|
| `/zombiemod horde list` | Lists the defined hordes |
| `/zombiemod horde start <horde>` | Calls a horde in |
| `/zombiemod horde stop` | Stops the running horde |

### `zombiemod.corpse` — default op level 2

| Command | What it does |
|---|---|
| `/zombiemod corpse list [player]` | The corpse ledger |
| `/zombiemod corpse give <player> [index]` | Hands a corpse's contents back |
| `/zombiemod corpse respawn <player> [index] [here]` | Rebuilds the corpse, optionally where you are looking |
| `/zombiemod corpse forget <player>` | Drops a corpse from the ledger |

### `zombiemod.observe` — default op level 2

| Command | What it does |
|---|---|
| `/zombiemod observe` | Toggles your own observer mode — **the "on" direction only** |
| `/zombiemod observe on [player]` | Turns observer mode on, for you or someone else |
| `/zombiemod observe off <player>` | Turns *someone else's* observer mode off |

### `zombiemod.status` — default op level 2

| Command | What it does |
|---|---|
| `/zombiemod status` | What the mod believes its settings are |

### `zombiemod.config` — default op level 3

| Command | What it does |
|---|---|
| `/zombiemod config` | Lists the runtime toggles |
| `/zombiemod config <toggle> [on\|off]` | Sets one. With no `on`/`off`, flips it |

The toggles are `enabled`, `hordes`, `playerZombies`, `proximity`, `bestiary`, `perGenus`,
`hideUnspawnable`, `unspawnableRevealedWhenMet`, `logSpawns`.

## Two deliberate holes, and why they are there

**`/zombiemod observe off` is available to everybody, permanently.** Turning your own invulnerability
*off* is not a power, and gating it caused a real incident: observer mode was switched on for a
player, that player was later deopped, and the only command that could switch it back off now needed
the permission they had just lost. They were invulnerable, could not fix it, and could not ask an op
to fix it either, because the command only ever acted on whoever typed it.

So the bar sits on the things that *grant* something and never on the way out:

| | Needs |
|---|---|
| `observe` (toggle self) | `zombiemod.observe` — but only when turning **on**; the off direction is checked in code, because one node cannot bar a single direction |
| `observe on`, `observe on <player>` | `zombiemod.observe` |
| `observe off` | **nothing — anyone, always** |
| `observe off <player>` | `zombiemod.observe` |

**Neither `/zombiemod` nor `/zm` carries a bar at its root.** Brigadier ANDs a child's requirement
with its parent's, so a restrictive root cannot be relaxed by a permissive child — a gated root
would put the bestiary permanently out of a normal player's reach. Every subcommand carries its own
gate instead, which is why the alias is exactly as restricted as the full name and no more.

## Console and command blocks

A node is a question about a *player*. The console, a command block and an `/execute` with no
player behind it have no identity a permissions mod could grant to, so they pass on **op level**
exactly as before: level 2 for everything, level 3 for `config`. A command block runs at level 2
and always could.

Permission is not the whole story there either: several commands **act on whoever typed them**, so
they need a player behind them and fail from the console with *"a player is required to run this
command"* — however high the permission. That is a separate axis from the tables above.

| Runs from the console | Needs a player |
|---|---|
| `/zombiemod list` | `/zombiemod bestiary`, `bestiary book`, `bestiary info` — it is *your* dex |
| `/zombiemod status` | `/zombiemod spawn <genus>` with no position — it spawns where you are *looking* |
| `/zombiemod config …` | `/zombiemod horde start`, `horde stop` — a horde is directed at a player |
| `/zombiemod corpse list`, `corpse forget` | `/zombiemod observe`, `observe on`, `observe off` with no player named |
| `/zombiemod spawn <genus> <pos>` | `/zombiemod corpse respawn … here` — "here" is where you are looking |
| `/zombiemod observe on\|off <player>` | `/zombiemod corpse give` **only when the owner is offline**, since the items then drop at the admin's feet |

`/zombiemod spawn` is the one worth knowing: give it an explicit position and it works from a command
block or the console, and `~ ~ ~` and `^ ^ ^5` both work, so "five blocks in front of me" needs no
special handling.

## Client commands — no permission at any level

These are registered on the **client's** dispatcher and run entirely on the player's own machine, so
they change nothing on the server and are unavailable to anyone who has not installed the mod
client-side.

| Command | What it does |
|---|---|
| `/zmdex render [size]` | Writes one PNG per genus to `screenshots/zombiemod/<namespace>/<genus>.png` (64–1024px, default 256) |
| `/zmdex cancel` | Stops a render in progress |

There is also a keybind, **`Z`** by default, that opens the ZombieDex screen. Rebindable in
Controls → Miscellaneous.

## How it fits together

The nodes go through NeoForge's own `PermissionAPI`. LuckPerms is a *handler* for that API and so
is Standards; the server owner picks which one answers in `neoforge-server.toml`, and ZombieMod
does not know or care which. There is no ZombieMod-side integration with either mod and nothing to
configure on ZombieMod's side.

If you think the split is wrong — that `horde` and `spawn` should be one node, or that `corpse give`
deserves to be separable from `corpse forget` — say so at
<https://github.com/Sablednah/ZombieMod/issues>. The useful shape depends on how a server actually
delegates.

## What is *not* permission-gated

- **Being attacked.** Genera do not check anything about the player they hunt. Ops are not exempt;
  that is what `/zombiemod observe` is for.
- **Infection, conversion and mutation.** They apply to anyone.
- **Reloading genera.** There is deliberately no `/zombiemod reload` — genera are a datapack registry,
  so vanilla's own `/reload` picks up changes, and that command's permission is vanilla's business.
- **The bestiary unlocking.** Entries unlock by meeting things, per player, with no permission
  involved.
