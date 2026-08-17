# Release copy — every field, in one place

Copy the strings below **verbatim** into each platform. They live here so the same sentence cannot
end up three different shapes across GitHub, CurseForge and Modrinth, which is how store copy always
rots.

If you change a tagline, change it *here first*, then re-copy everywhere it appears.

---

## The canonical strings

### One-liner (≤120 chars)

Used for: GitHub repo description, CurseForge summary, Modrinth summary.

> 58 zombie types with hand-built AI, and the JSON to write your own. Your players join with a vanilla client.

*(119 characters. Modrinth's limit is 256, GitHub's 350, so it fits everywhere with room.)*

### The hook (one paragraph)

Used for: the top of any long description, forum posts, Reddit.

> **Build your own undead.** ZombieMod turns zombie types into datapack files — health, size, colour,
> face, and above all **AI** — so a coward that flees on sight, a stalker that watches you from across
> the valley, and a climber that comes over the wall are all just JSON. It ships with 58 of them
> already written. Install it on the server; your players need nothing.

### The promise (one sentence, never soften it)

> ZombieMod registers no entity types of its own, so an unmodified client sees all of it.

This is the mod's single most important claim and the one most likely to be diluted into something
weaker and vaguer ("mostly server-side", "lightweight"). Do not write **"server-side only"** — the jar
now contains a client half behind `@Mod(dist = Dist.CLIENT)` that adds the ZombieDex screen. The true
and testable statement is *players do not need it*.

### Long description

**`CURSEFORGE.md`** — use it for **both** CurseForge and Modrinth. It is plain Markdown and needs no
per-platform edit. Do not fork it into a second file.

---

## GitHub

| Field | Value |
|---|---|
| Repo description | The one-liner, above |
| Website | `https://sablecraft.co.uk/zombiemod-reforged/` |
| Topics | `minecraft`, `neoforge`, `minecraft-mod`, `zombies`, `datapack`, `mobs`, `minecraft-1-21` |

### Release

| Field | Value |
|---|---|
| Tag | `v3.0.0` |
| Title | `3.0.0 — Build your own undead` |
| Body | The `## 3.0.0` section of [`CHANGELOG.md`](CHANGELOG.md), plus the requirements table below |
| Attach | `build/libs/zombiemod-3.0.0.jar` |

Requirements block to append to the release body:

> **Requirements:** Minecraft 1.21.11, NeoForge 21.11.42+, Java 21.
> Install on the server. Your players do not need the mod — a stock client can join and meet every
> genus. Installing it client-side too is optional and adds the ZombieDex screen.

---

## CurseForge

| Field | Value |
|---|---|
| Summary | The one-liner |
| Description | [`CURSEFORGE.md`](CURSEFORGE.md) |
| Changelog | The `## 3.0.0` section of [`CHANGELOG.md`](CHANGELOG.md) |
| Project icon | `docs/main-logo.png` (square lockup) |
| Header/banner | `docs/slime-logo.png` |
| Licence | MIT |
| Source | `https://github.com/Sablednah/ZombieMod` |
| Issues | `https://github.com/Sablednah/ZombieMod/issues` |
| Wiki | `https://sablecraft.co.uk/zombiemod-reforged/` |

**Categories:** Mobs, Server Utility, Adventure and RPG.
**Game version:** 1.21.11 · **Modloader:** NeoForge · **Release type:** Release.

---

## Modrinth

| Field | Value |
|---|---|
| Summary | The one-liner (limit 256) |
| Description | [`CURSEFORGE.md`](CURSEFORGE.md) |
| Icon | `docs/main-logo.png` |
| Licence | MIT |
| Source / Issues / Wiki | as CurseForge above |

**Categories:** `mobs`, `adventure`, `game-mechanics`.
**Environment — get this right, it is the field people filter on:**

| | |
|---|---|
| Server | **Required** |
| Client | **Optional** |

That pair is the exact truth: the mod does its work on the server, and a client that has it gets the
ZombieDex screen. Marking the client *Unsupported* would be wrong now the client half exists, and
marking it *Required* would send away the people the mod was built for.

---

## Before you publish

- [ ] `./gradlew build` and confirm the jar is `zombiemod-3.0.0.jar`
- [ ] Screenshots — the ZombieDex screen is the best single image the mod has; `/zm bestiary` with
      `bestiary.info = ALWAYS` and `hideUnspawnable = off` fills it out for photography
- [ ] Push `master` and the `v3.0.0` tag
- [ ] GitHub release first — CurseForge and Modrinth descriptions link back to it
- [ ] Hand `WEBSITE.md` to the sablecraft.co.uk session; **Cloudflare must be purged** before the
      pages are visible

## After

Anything player-visible that changes goes in `CHANGELOG.md` first, then into the CurseForge and
Modrinth changelog fields from there. The version lives in `gradle.properties` and nowhere else —
`neoforge.mods.toml` is generated from it at build time, so never edit the generated file.
