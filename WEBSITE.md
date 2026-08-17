# sablecraft.co.uk — what the site needs from this repo

The docs site section lives at **<https://sablecraft.co.uk/zombiemod-reforged/>** and is maintained
by a separate session, so this file is the handover: what to build, where the source text lives, and
what must stay in sync with the mod.

This follows the shape already proven for CityWorld (`../CityWorld-ReForged/WEBSITE.md`). Read that
one first if you are the site session — the conventions below are its conventions.

## Status: the section does not exist yet

This is a **first build**, not a release update. Everything below is new.

## Where the site's facts come from

| Site page | Source in this repo |
|---|---|
| Landing — what it is, requirements, feature overview | `CURSEFORGE.md` |
| Settings reference | `CURSEFORGE-CONFIGURATION.md` |
| Commands reference | `CURSEFORGE-COMMANDS.md` |
| The roster — all 58 genera | `docs/ROSTER.md`, plus the `description` field in each `src/main/resources/data/zombiemod/zombiemod/genus/*.json` |
| Writing a genus — fields, goals, abilities, conditions | `README.md` §§ *Writing a genus* → *Abilities* (lines ~42–566) |
| Bosses, phases, loot, summon rituals | `README.md` §§ *Bosses* → *Summoning* |
| Horde events | `README.md` § *Horde events* |
| Balance rules — sensory tiers, how `xp`/`bounty` are derived | `docs/BALANCE.md` |
| What is and is not finished | `docs/STATUS.md` |
| Artwork | `docs/main-logo.png` (square lockup), `docs/slime-logo.png` (banner) |

**Do not copy the genus reference into a new markdown file in this repo.** It is ~900 lines of
`README.md`, it is current, and a second copy would start drifting on the first new ability. Build
the site pages from those sections and treat `README.md` as the upstream. This is the one place
ZombieMod deliberately departs from the CityWorld pattern, where the equivalent material was small
enough to live in its own file.

## Proposed page tree

Six pages. The split is by *what the reader is trying to do*, which is why "the roster" and "writing
your own" are separate — almost nobody wants both on the same visit.

| Path | For | Source |
|---|---|---|
| `/zombiemod-reforged/` | Landing. What it is, requirements, install, the highlights, links out. | `CURSEFORGE.md` |
| `/zombiemod-reforged/roster/` | All 58 genera, grouped by family, with what each one does. | `docs/ROSTER.md` |
| `/zombiemod-reforged/genus/` | **Build your own zombie.** Every field, all 11 goal types, all 21 abilities, all 14 spawn conditions, with worked examples. | `README.md` |
| `/zombiemod-reforged/hordes/` | **Build your own horde.** Wave events, and the boss layer — phases, boss bars, loot, summon rituals. | `README.md` |
| `/zombiemod-reforged/settings/` | Every setting in `zombiemod-server.toml`. | `CURSEFORGE-CONFIGURATION.md` |
| `/zombiemod-reforged/commands/` | Every command. | `CURSEFORGE-COMMANDS.md` |

### Keep version facts on the landing page only

As with CityWorld: `/roster/`, `/genus/`, `/hordes/`, `/settings/` and `/commands/` should be
**version-agnostic**, so a release does not invalidate them. CityWorld's session went further and
made even the landing-page hero tags version-agnostic, which is why its releases now often need no
landing-page edit at all. Worth doing the same here from the start.

Requirements for the landing page:

> **Requirements**
>
> | Minecraft | NeoForge | Java |
> |---|---|---|
> | 1.21.11 | 21.11.42+ | 21 |
>
> Install on the server. Your players do not need the mod — they can join on a stock client from the
> Mojang launcher. Installing it client-side too is optional and adds the ZombieDex screen; people
> with and without it play together on the same server.

## Three things the site should be careful to get right

These are the points where a casual summary would say something false.

1. **"Server-side only" is now not quite the phrase.** The jar contains a client half behind
   `@Mod(dist = Dist.CLIENT)`. The promise that survives — and has been tested twice with a genuinely
   unmodified client from the Mojang launcher against a dedicated server — is that **players do not
   need it**. Say that, rather than "server-side only".

2. **A land claim is not a safe zone.** `inClaims` affects *spawning*. Mobs spawn outside a claim,
   walk in, and aggro normally. This reads as a bug the first time, and it is the first support
   question the settings page can pre-empt.

3. **The server config is per-world in singleplayer.** `saves/<world>/serverconfig/`, not `config/`,
   and the file does not exist until the world has been loaded once. This was the cause of a real
   "feature doesn't work" report during development. Both the settings and commands pages should say
   it, and point at `/zombiemod status`.

## Two good hooks for the landing page

Worth leading with, because they are the things nothing else does:

- **Herobrine.** Weight 1, zero damage, no attack goal. He only moves while you are facing away, he
  leaves if you get within six blocks, he blinks the instant an arrow lands, and a quarter of the
  time he simply is not there any more. He cannot hurt you. That is the point.
- **The faces.** 51 of the 58 wear a real player-head texture on an unmodified client. It is what
  makes a crowd read as a cast rather than a palette swap.

## The numbers, and where to get them

Correct as of 2026-08-17, counted off the source:

| | | Where it comes from |
|---|---:|---|
| Genera | **58** | `data/zombiemod/zombiemod/genus/*.json` |
| Genera wearing a face | **51** | those files with a `head` field |
| Goal types | **11** | `register(` lines in `GoalSpecTypes.java` |
| Abilities | **21** | `AbilityTypes.java` |
| Spawn conditions | **14** (11 general + 3 CityWorld) | `SpawnConditionTypes.java` |
| Horde events | **4** | `data/zombiemod/zombiemod/horde/*.json` |

**Count these off the source, never off prose.** Every one of them had drifted in `docs/` — ROSTER
and STATUS said 56 genera, and STATUS said 12 goal types, 22 abilities, 12 conditions and 3 hordes.
All are corrected now, but the failure mode is the point: a hand-maintained number goes wrong
silently, and nothing about a wrong count looks wrong.

Two caveats when writing the roster page:
`player_zombie` is in the 58 but is the player-corpse template rather than something you meet in the
wild, and several genera are weight 0 (bosses and summon-only), so "58 you can encounter" would
overstate it. `any_of` and `not` are among the 14 conditions but are combinators, not places.

## Checklist for future releases

1. Update the requirements block **only if** the supported version matrix changed.
2. Add anything genuinely new and player-visible.
3. If a genus was added, update `/roster/` — that page is the one that goes stale first.
4. If a goal type, ability or spawn condition was added, update `/genus/` from `README.md`.
5. Leave the other child pages alone unless a command or setting actually changed.

**⚠ Deployment is not visibility: Cloudflare must be purged** before changes reach real visitors.
Do not conclude from a fetch that a deploy failed.
