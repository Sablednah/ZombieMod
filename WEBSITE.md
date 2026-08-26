# sablecraft.co.uk — what the site needs from this repo

The docs site section lives at **<https://sablecraft.co.uk/zombiemod-reforged/>** and is maintained
by a separate session, so this file is the handover: what to build, where the source text lives, and
what must stay in sync with the mod.

This follows the shape already proven for CityWorld (`../CityWorld-ReForged/WEBSITE.md`). Read that
one first if you are the site session — the conventions below are its conventions.

## Status — ✅ BUILT AND DEPLOYED (2026-08-19) by the site session

**ZombieMod 3.0.0 shipped 2026-08-18/19.** The site section was built from this file the same day.

All six pages below are live at the proposed URLs, plus a card on `/game-plugins/`. What the site
session did, for the record:

- `/zombiemod-reforged/` and its five children exist as real WP pages (ids 129–134, children on
  `post_parent = 129`), each on its own theme template, sharing a `zombiemod-subnav.php` tab strip —
  the same shape as CityWorld's.
- **Version facts are on the landing page only.** The hero tags are version-agnostic ("59 genera",
  "NeoForge", "MIT", "No client mod required"), as CityWorld's now are, so a release that does not
  change the support matrix needs no landing-page edit at all.
- The three "careful to get right" points are all honoured: the page leads with *"your players do not
  need to install anything"* rather than "server-side only"; `/settings/` carries a callout that a
  claim is not a safe zone; and both `/settings/` and `/commands/` say the server config is per-world
  in singleplayer and point at `/zombiemod status`.
- Counts were taken off the source, not off prose: 59 genera, 52 with a `head`, 12 goals, 21
  abilities, 14 conditions, 4 hordes.
- The roster write-ups are each genus's own `description` field, so that page cannot drift from the
  game. Genus/hordes/settings/commands were built from `README.md` and the CURSEFORGE docs; nothing
  was copied into a new markdown file in this repo.
- Artwork used: `docs/slime-logo-850.png` as the landing wordmark, `docs/main-logo.png` as the
  `/game-plugins/` card logo, and ten of `screenshots/` resized to 1200px JPEG.

**One drift risk to know about:** the `/game-plugins/` card carries a version line
("Minecraft 1.21.11, NeoForge 21.11.42+, Java 21"), matching the other NeoForge cards on that page.
That is the one place outside the landing page where a version number lives, and it is exactly the
line that went stale for CityWorld once. Check it on any release that moves the matrix.

### The section URL is already fixed: `/zombiemod-reforged/`

Not a choice left to make. That exact URL is **already published and in the wild** — in the GitHub
release notes people are reading now, in the repo's homepage field, and in the CurseForge project
description. Anything else breaks links that already exist. Trailing slash included, as CityWorld's.

### Canonical outbound links

| | |
|---|---|
| CurseForge | `https://www.curseforge.com/minecraft/mc-mods/zombiemod-reforged` — **live** |
| GitHub | `https://github.com/Sablednah/ZombieMod` |
| Latest release | `https://github.com/Sablednah/ZombieMod/releases/latest` |
| Direct jar | the `zombiemod-3.1.1.jar` asset on that release |
| Modrinth | not created yet — leave the link out rather than pointing at a 404 |

**Note for a future release, not for the site:** the jar's own `displayURL` points at GitHub rather
than at this site, so the Homepage button in the mods list goes to the repo. Worth pointing at
`/zombiemod-reforged/` in 3.0.1 once the pages exist; it cannot be changed in a jar already
published.

## Where the site's facts come from

| Site page | Source in this repo |
|---|---|
| Landing — what it is, requirements, feature overview | `CURSEFORGE.md` |
| Settings reference | `CURSEFORGE-CONFIGURATION.md` |
| Commands reference | `CURSEFORGE-COMMANDS.md` |
| The roster — all 59 genera | `docs/ROSTER.md`, plus the `description` field in each `src/main/resources/data/zombiemod/zombiemod/genus/*.json` |
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

## The page tree — as built

Six pages. The split is by *what the reader is trying to do*, which is why "the roster" and "writing
your own" are separate — almost nobody wants both on the same visit.

| Path | For | Source |
|---|---|---|
| `/zombiemod-reforged/` | Landing. What it is, requirements, install, the highlights, links out. | `CURSEFORGE.md` |
| `/zombiemod-reforged/roster/` | All 59 genera, grouped by family, with what each one does. | `docs/ROSTER.md` |
| `/zombiemod-reforged/genus/` | **Build your own zombie.** Every field, all 12 goal types, all 21 abilities, all 14 spawn conditions, with worked examples. | `README.md` |
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

3. **The server config is `config/zombiemod-server.toml`**, and a copy under a world's
   `serverconfig/` folder overrides it for that world alone — that is what NeoForge's own
   `serverconfig/readme.txt` says. **The site previously had this backwards** (claiming the config/
   copy did nothing in singleplayer), which sent people to edit a file that does not exist and caused
   a real "feature doesn't work" report. Both the settings and commands pages should carry the
   corrected wording and point at `/zombiemod status`, which prints both paths.

## Two good hooks for the landing page

Worth leading with, because they are the things nothing else does:

- **Herobrine.** Weight 1, zero damage, no attack goal. He only moves while you are facing away, he
  leaves if you get within six blocks, he blinks the instant an arrow lands, and a quarter of the
  time he simply is not there any more. He cannot hurt you. That is the point.
- **The faces.** 52 of the 59 wear a real player-head texture on an unmodified client. It is what
  makes a crowd read as a cast rather than a palette swap.

## The numbers, and where to get them

Correct as of 2026-08-17, counted off the source:

| | | Where it comes from |
|---|---:|---|
| Genera | **59** | `data/zombiemod/zombiemod/genus/*.json` |
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
`player_zombie` is in the 59 but is the player-corpse template rather than something you meet in the
wild, and several genera are weight 0 (bosses and summon-only), so "59 you can encounter" would
overstate it. `any_of` and `not` are among the 14 conditions but are combinators, not places.

## Checklist for future releases

1. Update the requirements block **only if** the supported version matrix changed.
2. Add anything genuinely new and player-visible.
3. If a genus was added, update `/roster/` — that page is the one that goes stale first.
4. If a goal type, ability or spawn condition was added, update `/genus/` from `README.md`.
5. Leave the other child pages alone unless a command or setting actually changed.

**⚠ Deployment is not visibility: Cloudflare must be purged** before changes reach real visitors.
Do not conclude from a fetch that a deploy failed.
