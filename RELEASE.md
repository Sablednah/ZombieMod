# Release copy — every field, in one place

Copy the strings below **verbatim** into each platform. They live here so the same sentence cannot
end up three different shapes across GitHub, CurseForge and Modrinth, which is how store copy always
rots.

If you change a tagline, change it *here first*, then re-copy everywhere it appears.

---

## The canonical strings

### One-liner (≤120 chars)

Used for: GitHub repo description, CurseForge summary, Modrinth summary.

> 61 zombie types with hand-built AI, and the JSON to write your own. Your players join with a vanilla client.

*(119 characters. Modrinth's limit is 256, GitHub's 350, so it fits everywhere with room.)*

### The hook (one paragraph)

Used for: the top of any long description, forum posts, Reddit.

> **Build your own undead.** ZombieMod turns zombie types into datapack files — health, size, colour,
> face, and above all **AI** — so a coward that flees on sight, a stalker that watches you from across
> the valley, and a climber that comes over the wall are all just JSON. It ships with 61 of them
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
| Tag | `v3.4.0` |
| Title | `3.4.0 — Whose land is it` |
| Body | The `## 3.4.0` section of [`CHANGELOG.md`](CHANGELOG.md), plus the requirements table below |
| Attach | every `build/libs/zombiemod-<ver>+mc<mc>.jar`, one per supported version — the CurseForge upload reads the `+mc` suffix to label each |

Requirements block to append to the release body:

> **Requirements** — there is a jar per Minecraft version, named for the one it was built against.
> Take the one that matches your server.
>
> | Minecraft | NeoForge | Java |
> |---|---|---|
> | 1.21.11 | 21.11.42+ | 21 |
> | 26.1.2 | 26.1.2.95+ | 25 |
> | 26.2 | 26.2.0.59+ | 25 |
>
> Install on the server. Your players do not need the mod — a stock client can join and meet every
> genus. Installing it client-side too is optional and adds the ZombieDex screen.

Keep this table, `CURSEFORGE.md`'s and [`docs/MULTIVERSION.md`](docs/MULTIVERSION.md)'s in step —
MULTIVERSION.md is the measured one, so it is the source. All three said 1.21.11 alone for two
releases after the branches appeared.

---

## CurseForge

| Field | Value |
|---|---|
| Summary | The one-liner |
| Description | [`CURSEFORGE.md`](CURSEFORGE.md) |
| Changelog | The `## 3.4.0` section of [`CHANGELOG.md`](CHANGELOG.md) |
| Project icon | `docs/main-logo.png` — 1035×1035, square |
| Header/banner | `docs/slime-logo-850.png` — 850px wide, the description limit |
| Licence | MIT |
| **Live at** | `https://www.curseforge.com/minecraft/mc-mods/zombiemod-reforged` |
| Source | `https://github.com/Sablednah/ZombieMod` |
| Issues | `https://github.com/Sablednah/ZombieMod/issues` |
| Wiki | `https://sablecraft.co.uk/zombiemod-reforged/` |

**Categories:** Mobs, Server Utility, Adventure and RPG.
**Modloader:** NeoForge · **Release type:** Release.
**Game version:** not typed in — both upload scripts read it from each jar's `+mc` filename suffix,
so a release carrying three jars is tagged for three Minecraft versions without anyone choosing.

---

## Modrinth

| Field | Value |
|---|---|
| Summary | The one-liner (limit 256) |
| Description | [`CURSEFORGE.md`](CURSEFORGE.md) |
| Icon | **`docs/modrinth-icon.png`** — 512×512, 44 KB, the slime banner padded square. *Not* the shield lockup; see below |
| Licence | MIT |
| Source / Issues / Wiki | as CurseForge above |

**Modrinth's icon is not the same artwork as CurseForge's, and the reason matters.**
Modrinth runs a **no-generative-AI policy** over uploaded art. The shield lockup —
`docs/main-logo-icon.png`, the zombie head with spikes and chains — **tripped it**. The slime banner
`docs/slime-logo.png` did not. So Modrinth gets `docs/modrinth-icon.png`: that banner scaled to fit
and padded to a transparent 512×512 square, 44 KB. Do not "fix" it back to the shield to match
CurseForge — it will be rejected again. Regenerate it with `scripts/make-modrinth-icon.py`.

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

## Screenshots

Fifteen in `screenshots/`, all 1597x1075. Suggested gallery order — the first two do the persuading,
so lead with them:

| # | File | Caption to use |
|---|---|---|
| 1 | `giant.png` | **The Colossus.** A giant zombie in the middle of a ruined high street, scaled against the tower blocks behind it. Clean daylight shot with no HUD — the best hero image. |
| 2 | `boss.png` | **Patient Zero.** Boss bar, darkened sky, a burning zombie beside him — and the tooltip reading `minecraft:zombie`, because that is all he ever was. |
| 3 | `ZombieDex.png` | **The ZombieDex.** A field guide to the dead: slain, met, and not yet found. |
| 4 | `ZombieDex3.png` | **Every entry earns itself.** The Corpse, wearing your own face and your own gear. |
| 5 | `ZombieDex_book.png` | **The same dex on a vanilla client**, as a written book. Players who install the mod get the illustrated edition; players who don't get this. |
| 6 | `Corpse.png` | **Your corpse gets up.** Wearing your real skin, carrying your netherite. Kill it to get it back. |
| 7 | `runners.png` | **Runners.** Fast, fragile, and never alone. |
| 8 | `swarmlings.png` | **Swarmlings.** Half-size, trivial one at a time. |
| 9 | `peekaboo.png` | **Climbers get close.** |
| — | `ZombieDex1/2/4/5/6.png`, `zombiedex_chat.png` | Further dex entries and the chat view. Hold back as spares — a gallery of one screen repeated sells nothing. |

**Two things to know before uploading them.**

The boss and corpse shots carry a **Jade tooltip** showing `minecraft:zombie`, and that is worth
keeping rather than cropping out: it is visual proof of the mod's central claim, which no amount of
description text can make as well.

Several shots include **CityWorld** scenery, a **LegendQuest** XP bar, and one has the "Saved
screenshot as…" toast. That is honest gameplay and fine — but do not caption a CityWorld street in a
way that implies ZombieMod builds cities. The integration is real and optional; the city is not part
of this mod.

---

## Image sizes — the two limits that bite

Both were found the hard way rather than in advance. Sizes here are correct as of 3.0.0.

| Where | Limit | Use |
|---|---|---|
| CurseForge **description** images | **850px wide** | `docs/slime-logo-850.png` |
| Modrinth **project icon** | **256 KiB**, square, **and no generative AI** | `docs/modrinth-icon.png` (512×512, 44 KB) |
| CurseForge project icon | square | `docs/main-logo.png` (1035×1035) |
| Gallery screenshots | no practical limit | `screenshots/*.png` at 1597×1075 |

**The icon has to be square.** `main-logo.png` is a shield lockup that was wider than it was tall, so
it was padded to 1035×1035. A non-square icon gets cropped or letterboxed by both stores.

**Modrinth rejects an icon over 256 KiB**, and the padded logo is 1.4 MB — five times over, so it
would simply fail. `main-logo-icon.png` is the compliant one: 512×512, palette-quantised to 82 KB
with alpha intact and no visible banding. Regenerate it with Pillow if the artwork changes:

```python
from PIL import Image
src = Image.open('docs/main-logo.png').convert('RGBA')
src.resize((512, 512), Image.LANCZOS) \
   .quantize(colors=256, method=Image.FASTOCTREE) \
   .save('docs/main-logo-icon.png', 'PNG', optimize=True)
```

`FASTOCTREE` specifically, because it is the quantiser that preserves alpha — the default drops it
and the icon gains a black box. Plain RGBA resizing does not get under the limit until about 384px,
so quantising buys a sharper icon than shrinking does.

There is **no Pillow system-wide on this box and PEP 668 blocks pip**, so use the venv:
`python3 -m venv venv && ./venv/bin/pip install Pillow`. It is gitignored. Creating it takes a couple
of minutes and looks like a hang.

**The gallery screenshots do not need resizing** — the 850px limit is for images embedded in the
*description body*, not the gallery. `CURSEFORGE.md` currently embeds no images at all, so nothing in
it is affected; if you add one, it must be ≤850px and uploaded to the project first.

---

## Publishing by API

Both stores have an API. They are not equally useful.

### CurseForge — automated, and worth it

`scripts/curseforge-upload.sh` uploads a jar; `.github/workflows/curseforge.yml` runs it whenever a
GitHub release is **published**, so publishing to GitHub publishes to CurseForge too.

**One-time setup (owner only — the token must never be pasted into a chat or committed):**

1. Create a token at <https://legacy.curseforge.com/account/api-tokens>.
2. Repo **Settings → Secrets and variables → Actions → Secrets**: add `CURSEFORGE_TOKEN`.
3. Same screen, **Variables** tab: add `CURSEFORGE_PROJECT_ID` — the numeric ID on the project page.

Until both exist the workflow **skips rather than fails**, so it will not put a red cross on a
release. `workflow_dispatch` re-uploads an existing tag by hand.

**The project must already exist.** The CurseForge upload API can only add files to a project;
unlike Modrinth it has no create-project endpoint. Make it on the website first.

**Four gotchas, all of which bit CityWorld during its 5.1.0 upload:**

- **Game-version IDs are numeric and they change**, so the script resolves them from
  `/api/game/versions` on every run. If a Minecraft version is not listed yet it fails with the
  names CurseForge *does* know — the expected failure right after a Minecraft release.
- **An upload naming no environment is rejected** — *"You must select at least one version from the
  environment group of versions"*. The script always sends Client and Server.
- **`--form-string`, not `-F`, for the metadata.** curl gives `;`, a leading `@` and a leading `<`
  special meaning inside an `-F` value, so a changelog containing any of them silently mangles the
  JSON — and CurseForge answers *"Error in field `metadata`: Invalid JSON"*, which reads like a bug
  in the JSON you built. (Tested here: a changelog containing all three arrives intact.)
- **A 500 on upload is the changelog, not the project.** ZombieMod's first upload spent 30 seconds
  and returned `500 An unhandled exception occurred`. It was **not** the project being unapproved —
  the same jar with a one-line changelog uploaded instantly to the same unapproved project. The
  changelog contained a Markdown **angle-bracket autolink** (`<https://…>`), which CurseForge's HTML
  sanitiser reads as a malformed tag. The script now unwraps them before sending. If a 500 recurs,
  the next suspect is the **Markdown table** — strip it and retry.
- **HTTP 200 means accepted, not published.** Moderation runs afterwards. CurseForge **dedupes by
  file content**, so re-uploading a jar that is already up gets it *rejected as a duplicate* even
  though the API returned a file ID — and rejected files are hidden from the authors file list by
  default, so they do not look rejected, they look like they never arrived. The authoritative view is
  always `https://authors.curseforge.com/#/projects/<id>/files`; the public Files tab lags it.

### Modrinth — automated, like CurseForge

`.github/workflows/modrinth.yml` drives three scripts. **The token stays a GitHub secret** — it is
never needed on the dev box, which is the whole reason this is a workflow rather than a shell call.

| Script | Workflow action | What it does |
|---|---|---|
| `scripts/modrinth-create.sh` | `create-project` | Creates the project as a private **draft**, sets the icon, uploads the gallery |
| `scripts/modrinth-upload.sh` | `upload-versions` | Attaches the jars from a GitHub release, one Modrinth version each |
| `scripts/modrinth-submit.sh` | `submit-for-review` | **The step that makes it public.** Sends the draft to moderation |

Publishing a GitHub release fires `upload-versions` automatically, so from 3.5.0 onwards a release
reaches both stores unattended. The other two are `workflow_dispatch` only — creating and publishing
are things you should have to mean.

**Setup, once:** a PAT at <https://modrinth.com/settings/pats> with **`PROJECT_CREATE`,
`PROJECT_WRITE`, `VERSION_CREATE`**, added as the repository secret `MODRINTH_TOKEN`. Until it
exists the workflow skips rather than fails. The slug defaults to `zombiemod-reforged`; override it
with the repository variable `MODRINTH_SLUG`.

**Create and upload are both re-runnable**, which is the property that makes this safe to iterate on.
`create-project` on an existing project `PATCH`es the description from `CURSEFORGE.md` instead of
failing, refreshes the icon, and skips gallery images already up — so fixing a typo is an edit and a
re-run, not hand-editing the website. `upload-versions` is the exception: Modrinth version numbers are
unique per project, so re-uploading the same one is a 400.

**One Modrinth version per jar, not three files on one.** The version number carries the jar's own
`+mc` suffix — `3.4.0+mc1.21.11`, `3.4.0+mc26.2` — which keeps them unique and lets the Minecraft
version be read off the filename. Same reasoning as CurseForge: the workflow only checks out the
tag's ref, so `gradle.properties` there describes one of the three and would mislabel the rest.

**Four things worth knowing about the v2 API**, all verified against the live spec
(<https://docs.modrinth.com/openapi.yaml>) on 2026-08-31:

- **`Authorization: <token>` with no `Bearer` prefix.** A `Bearer` prefix gives a 401 that reads
  like a wrong token.
- **`environment` exists, but on the *version*, not the project.** The project still needs the
  deprecated `client_side`/`server_side` pair, which are still required fields. So both go: the
  project says `server_side: required` / `client_side: optional`, and each version says
  `environment: server_only_client_optional`. *(This corrects an earlier note here that said
  `environment` does not exist in v2 at all.)*
- **`is_draft`, `initial_versions` and `gallery_items` on create are all deprecated.** Create the
  project bare, then upload the icon, gallery and versions through their own endpoints — which is
  also what makes the script re-runnable.
- **Gallery captions are query parameters**, so they must be percent-encoded; several contain
  colons and commas.

The same `--form-string`-not-`-F` rule as CurseForge applies to both multipart calls: curl gives
`;`, a leading `@` and a leading `<` special meaning inside an `-F` value, and both the description
body and the changelog contain all three.

**Minotaur** — the usual Gradle plugin — would have uploaded versions but could not create the
project, so it would not have removed the one manual step that mattered.

---

## Before you publish

- [ ] `./gradlew build` and confirm the jars are `zombiemod-3.4.0+mc<version>.jar`, one per supported Minecraft version
- [ ] Redeploy to the test instance if it still has the pre-balance jar
- [ ] Create the CurseForge project **on the website** and note its numeric project ID (its upload
      API cannot create one). Modrinth's can: run the `modrinth.yml` workflow, `create-project`
- [ ] Add `CURSEFORGE_TOKEN` (secret) and `CURSEFORGE_PROJECT_ID` (variable) to the repo
- [ ] Push `master`, `mc26.1`, `mc26.2` and the `v3.4.0` tag
- [ ] **GitHub release first** — it triggers the CurseForge upload, and the store pages link back to it
- [ ] Check `https://authors.curseforge.com/#/projects/<id>/files`, not the public Files tab
- [ ] Modrinth: check the draft page reads right, then run `modrinth.yml` → `submit-for-review`
- [ ] Upload the gallery in the order above
- [ ] Hand `WEBSITE.md` to the sablecraft.co.uk session; **Cloudflare must be purged** before the
      pages are visible

## After

Anything player-visible that changes goes in `CHANGELOG.md` first, then into the CurseForge and
Modrinth changelog fields from there. The version lives in `gradle.properties` and nowhere else —
`neoforge.mods.toml` is generated from it at build time, so never edit the generated file.
