# Permissions

**ZombieMod defines no permission nodes of its own.** Every command is gated by Minecraft's own
command permission levels, and there is nothing to add to a permissions file to make ZombieMod work.
If you have gone looking for a `zombiemod.*` node, that is why you did not find one.

What that means in practice: **op your staff, or map a group to op level 2, and everything below
follows.** The genus list, the whole bestiary, and turning your own observer mode off need no
permission at all, because they are player features rather than staff tools.

Correct for **3.4.0**, and identical on all three supported Minecraft versions.

## The levels ZombieMod uses

Minecraft has five levels, 0 to 4. ZombieMod uses three of them.

| Level | Vanilla name | Who has it | What ZombieMod puts here |
|---|---|---|---|
| **0** | `all` | everybody, including a brand-new player | The bestiary and the genus list |
| **2** | `gamemasters` | an op, by default | Almost everything else — spawning, hordes, corpses, status |
| **3** | `admins` | an op, by default | `/zombiemod config` alone |

An op gets level **4** by default, so **a plain op can run all of it**. The 2/3 split only becomes
visible on a server that assigns levels more finely than "op or not" — there, level 3 is the line
between *changing what happens in front of you* and *changing what the server does for everyone*.

Level 2 is also what a **command block** runs at, and what `/execute` gives a non-player source —
but permission is not the only thing that decides whether a command works there. See *Console and
command blocks* below.

## Every command, and the level it needs

`/zm` is an alias for `/zombiemod` and carries exactly the same levels — it is a Brigadier redirect
onto the same node tree, not a second registration, so the two cannot drift apart.

### Open to everyone — level 0

| Command | What it does |
|---|---|
| `/zombiemod list` | Lists every genus loaded |
| `/zombiemod bestiary` | Your ZombieDex, in chat |
| `/zombiemod bestiary book` | The same dex as a written book — works on a vanilla client |
| `/zombiemod bestiary info <genus>` | One genus's entry |
| `/zombiemod observe off` | **Turns your own observer mode off.** Always available — see below |

### Op — level 2 (`gamemasters`)

| Command | What it does |
|---|---|
| `/zombiemod spawn <genus> [pos]` | Spawns one where you are looking, or at a position |
| `/zombiemod status` | What the mod believes its settings are |
| `/zombiemod horde list` | Lists the defined hordes |
| `/zombiemod horde start <horde>` | Calls a horde in |
| `/zombiemod horde stop` | Stops the running horde |
| `/zombiemod corpse list [player]` | The corpse ledger |
| `/zombiemod corpse give <player> [index]` | Hands a corpse's contents back |
| `/zombiemod corpse respawn <player> [index] [here]` | Rebuilds the corpse, optionally where you are looking |
| `/zombiemod corpse forget <player>` | Drops a corpse from the ledger |
| `/zombiemod observe` | Toggles your own observer mode — **the "on" direction only** |
| `/zombiemod observe on [player]` | Turns observer mode on, for you or someone else |
| `/zombiemod observe off <player>` | Turns *someone else's* observer mode off |

### Admin — level 3 (`admins`)

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

| | Level |
|---|---|
| `observe` (toggle self) | 2 — but only when turning **on**; the off direction is checked in code, because one node cannot bar a single direction |
| `observe on`, `observe on <player>` | 2 |
| `observe off` | **0 — anyone, always** |
| `observe off <player>` | 2 |

**Neither `/zombiemod` nor `/zm` carries a bar at its root.** Brigadier ANDs a child's requirement
with its parent's, so a restrictive root cannot be relaxed by a permissive child — a level-2 root
would put the bestiary permanently out of a normal player's reach. Every subcommand carries its own
bar instead, which is why the alias is exactly as restricted as the full name and no more.

## Console and command blocks

Permission level is not the whole story: several commands **act on whoever typed them**, so they need
a player behind them and fail from the console with *"a player is required to run this command"* —
however high the permission. That is a separate axis from the tables above.

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

There is also a keybind, **`J`** by default, that opens the ZombieDex screen. Rebindable in
Controls → Miscellaneous.

## For server owners running a permissions mod

Because ZombieMod registers no named nodes, a permissions mod can only reach these commands the way
it reaches vanilla's own: by **granting a command permission level**. Give your staff group level 2
(and level 3 if you want them changing config), and the whole tree above resolves.

A permissions mod that works *only* by named nodes has nothing of ZombieMod's to bind, because there
is nothing to name. This is worth stating plainly, since the alternative is an admin adding
`zombiemod.*` to a config, seeing nothing happen, and reasonably concluding something is broken.

Minecraft 1.21.11 did add a named-permission concept — `Permission.Atom`, an id like
`minecraft:commands/entity_selectors` — alongside the old level checks. ZombieMod does not yet
define any atoms of its own. **If per-command nodes would be useful on your server, that is a
reasonable feature request**; open an issue at
<https://github.com/Sablednah/ZombieMod/issues> saying which commands you want to split apart, since
the useful shape depends on how you actually delegate.

## What is *not* permission-gated

- **Being attacked.** Genera do not check anything about the player they hunt. Ops are not exempt;
  that is what `/zombiemod observe` is for.
- **Infection, conversion and mutation.** They apply to anyone.
- **Reloading genera.** There is deliberately no `/zombiemod reload` — genera are a datapack registry,
  so vanilla's own `/reload` picks up changes, and that command's permission is vanilla's business.
- **The bestiary unlocking.** Entries unlock by meeting things, per player, with no permission
  involved.
