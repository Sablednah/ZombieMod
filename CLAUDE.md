# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

**ZombieMod ReForged** — a NeoForge rewrite of ZombieMod, a 2013 Bukkit/Spigot plugin that added
configurable custom zombie types. The port was built in place at the repo root. The port is
complete and the 1.8 reference tree has been removed — see *Reading the original Bukkit plugin*
below for how to get it back when you need it.

**Shipping as 3.5.0** (2026-09-14): 61 genera, on GitHub and CurseForge, and on Modrinth as
`zombiemod-reforged` (submitted 2026-08-31, still awaiting first moderation on 2026-09-14; the
3.5.0 versions uploaded into the draft without complaint). A jar per Minecraft version,
three of them.

This is the **fourth** Bukkit→NeoForge port in a series. `../MobHealth-Forge` is the canonical
template and `../CityWorld-ReForged/PORTING.md` is the richest source of verified 1.21.11 API notes.
Read those before inventing anything.

**`master`'s targets** — the other two branches differ, see [docs/MULTIVERSION.md](docs/MULTIVERSION.md):

| | |
|---|---|
| Minecraft | 1.21.11 |
| Loader | NeoForge 21.11.42 |
| Java | 21 |
| Build | Gradle + ModDevGradle (`net.neoforged.moddev`) |
| Licence | MIT (the 1.8 plugin was CC BY-NC-ND; same author relicensed, as with WoodDye) |
| Mod id | `zombiemod`, package `com.sablednah.zombiemod` |

### Where the documents are

Code guidance is this file. Everything else has a home, and the rule is that a fact lives in exactly
one of them:

| Doc | What only it knows |
|---|---|
| [docs/MULTIVERSION.md](docs/MULTIVERSION.md) | **Read first for anything version-related.** The measured three-version matrix; the source when a requirements table disagrees |
| [docs/STATUS.md](docs/STATUS.md) | Where the project actually is, and the backlog in the order to do it |
| [docs/BALANCE.md](docs/BALANCE.md) | The balance model and its deliberate exceptions |
| [RELEASE.md](RELEASE.md) | Every store field, the gallery order, and the publishing traps — CurseForge *and* Modrinth |
| [CURSEFORGE.md](CURSEFORGE.md) | The store description. Used verbatim for both stores; **do not fork it** |
| [NODES.md](NODES.md) | Permissions. See *Command permissions* below |
| [WEBSITE.md](WEBSITE.md) | Handover to the sablecraft.co.uk session |

**Publishing is automated from a GitHub release.** Publishing one uploads to CurseForge *and*
Modrinth, each reading a jar's Minecraft version from its `+mc` filename suffix. Creating a new store
project is the only manual part. See RELEASE.md before touching either workflow.

## Build & run

**This mod ships for three Minecraft versions, one per branch** — see
[docs/MULTIVERSION.md](docs/MULTIVERSION.md), which is the file to read before touching any of it.

| Branch | Minecraft | JDK |
|---|---|---|
| `master` | 1.21.11 | 21 — `/mnt/d/Repos/sable/MobHealth-Forge/tools/jdk21` |
| `mc26.1` | 26.1.2 | **25** — `/mnt/d/Repos/sable/CityWorld-ReForged/tools/jdk25` |
| `mc26.2` | 26.2 | **25** — same |

Fixes go on **`master` first** and cherry-pick forward; that is what the `platform/` seam layer is
for, and cherry-picks have been conflict-free apart from the seams themselves. 26.x needs Java 25
because Minecraft 26.1 ships `java-runtime-epsilon`, where the 1.21 line shipped delta.

No system Java. Borrow the portable JDK — this repo bundles none:

```bash
export JAVA_HOME=/mnt/d/Repos/sable/MobHealth-Forge/tools/jdk21   # or .../CityWorld-ReForged/tools/jdk25 on 26.x
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew compileJava   # fast inner loop
./gradlew build         # -> build/libs/zombiemod-<version>.jar
./gradlew runServer     # headless dedicated server; needs run/eula.txt, pass no --args
./deploy.sh             # build + copy into the CurseForge test instance
```

- **The dev server runs on port 25567**, set in `build.gradle`, so it cannot collide with a
  CityWorld `runServer` or one of Sable's real test servers on 25565. **Kill the previous
  `runServer` before starting another** — a lingering one still holds the port and the clash
  surfaces as `bind(..) failed: Address already in use` → `Failed to initialize server` → a crash
  report, which reads like a code fault and is not one. `pkill -f "[g]radlew runServer"`, then
  confirm with `ss -ltn | grep 25567`. **The bracket is not decoration.** A bare
  `pkill -f "gradlew runServer"` inside a longer Bash tool call matches the shell running that very
  call, because its command line contains the pattern, and kills it — the whole call dies with exit
  144 before anything after the `pkill` runs (2026-09-14). The `[g]` idiom matches the process and
  not the pattern's own text.

- **The first build after changing `accesstransformer.cfg` is slow (10+ minutes).** ModDevGradle
  re-runs the neoform runtime to recompile Minecraft with the AT applied, and that result is keyed
  on the AT, so it cannot reuse the sibling projects' cache. It is working, not hung — check with
  `ps aux | grep neoform`. Ordinary builds after that are fast.
- If Gradle genuinely hangs on `:compileJava` with no CPU and no class files, that's the known WSL2
  `/mnt/d` degradation: `wsl --shutdown` from Windows PowerShell, reopen, rebuild.
- **Close Minecraft before `./deploy.sh`** — a running instance holds the jar open, Windows refuses
  the replace, and you end up testing a stale jar. `deploy.sh` checks and fails loudly.
- Versions/metadata live in `gradle.properties` and expand into
  `src/main/templates/META-INF/neoforge.mods.toml` at build time. Never edit a generated mods.toml.

### Which build is this? The stamp, and the four things holding it up

Every jar records the commit it came from, and the mod says so in the line that always prints:

```
ZombieMod ReForged 3.4.0+mc1.21.11 (build 1946c37a on master, 2026-09-10T07:49:34Z) loaded - ...
```

The same string is a line of `/zombiemod status`, because that is what gets pasted into a bug
report and an admin in game has no log to hand. **A version number answers "which release"; during
development that is a different question from "which bytes"**, and it is sharper here than in the
sibling mods — a release is three jars that differ only in a `+mc` suffix.

Two carriers, for two readers. `/zombiemod/build.properties` is read by the running mod;
`Build-Commit`/`Build-Branch`/`Build-Time` on the manifest are for inspecting a jar from a shell:

```bash
unzip -p build/libs/zombiemod-3.4.0+mc1.21.11.jar META-INF/MANIFEST.MF | grep Build
```

`-dirty` on the commit means the jar was built from uncommitted changes — worth seeing in
somebody's log before spending an hour reproducing against a tag. The format is shared with the
other SableCraft mods; **copied, not a common plugin**, on purpose.

Four things are load-bearing and none of them announce themselves:

- **`time` is the *commit's* timestamp, not the wall clock, and this is a caching decision.**
  `new Date()` at configure time rewrites the generated resource on every invocation, so
  `processResources` and `jar` can never be up to date — measured here at 3 of 5 tasks re-executed
  and 42s for a rebuild with nothing changed, against 5 up-to-date in 11s after. Use `%ct` and not
  `%cI`: `%cI` carries the committer machine's UTC offset, so one commit stamps differently on two
  machines.
- **`version` carries the `+mc` tag; `branch` must not be asked to.** `mod_version` is identical on
  all three branches, so a bare version reads the same on 1.21.11 and 26.2. `branch` looks like it
  distinguishes them only because it correlates here — it is empty in a source zip or a detached CI
  checkout.
- **`BuildInfo`'s `RESOURCE` must stay a compile-time constant.** `MOD_ID` is a constant
  expression, so javac inlines it and `BuildInfo.class` ends up with no runtime reference to
  `ZombieMod` or NeoForge — which is what lets the real compiled class be driven from `jshell` to
  check its own fallbacks with no Minecraft at all. Reading the path from a field or the config
  reintroduces the dependency with **no compile error** and the check silently stops working.
- **The degrade is all-or-nothing because of where the assignments sit**, not because a comment says
  so. `Properties.load` completes or throws before the first `getProperty`, so a half-parsed file
  cannot leave a real-looking commit beside three `unknown`s — which would be worse than no stamp,
  because it looks like an answer. Keep the assignments after `load()`, never interleaved.

**Absence is the easy case; corruption is the one that bites.** A missing resource is a null
stream, but `Properties.load` throws **`IllegalArgumentException`** — not `IOException` — on a
malformed unicode escape, so the obvious `catch (IOException)` would compile, read correctly, pass
review, and take the mod down at class-init as an `ExceptionInInitializerError`. Failing to load
over a diagnostic. `catch (Exception)` is deliberate; it was right by luck in four repos before
anyone tested it.

Check the fallbacks by **running them**, against the real class rather than a copy:

```bash
run() { printf 'System.out.println(com.sablednah.zombiemod.BuildInfo.describe());\n/exit\n' \
        | jshell --class-path "$1" -s -; }
run build/libs/zombiemod-3.4.0+mc1.21.11.jar   # from a jar
run build/classes/java/main                    # no stamp resource at all
```

Then the three that actually exercise the catch, each a directory holding
`zombiemod/build.properties` appended to the classpath: a malformed unicode escape, 200 bytes of
`/dev/urandom`, and — the one worth keeping — **three valid lines followed by a throwing one**,
which is the partial-parse case. All must print `unknown (build unknown on unknown, unknown)`.

**Do not write a unicode escape literally in a comment.** javac decodes them inside comments too,
so the comment explaining this failed the build with `error: illegal unicode escape`. Say it in
words.

## The one architectural decision everything follows from

**ZombieMod registers no entity types of its own.** A vanilla client cannot render an entity type it
has never heard of, and supporting unmodified clients is the entire point of the mod. So a "genus"
is not a new mob — it is a set of changes applied to an ordinary **vanilla** mob instance at the
moment it spawns. Size, colour, name and behaviour are all expressed through things vanilla already
knows how to draw.

The 1.8 plugin reached the same conclusion by a much worse road: it reflected its own classes into
`EntityTypes`' private maps under vanilla's ids (54 = zombie) and rewrote every biome's mob list,
precisely so clients still saw a normal zombie. Same goal, no reflection.

Consequence: **anything a genus wants must be reachable on a vanilla mob instance.** That is why
there is an access transformer.

## Architecture

```
data/<pack>/zombiemod/genus/<name>.json     datapack registry `zombiemod:genus`
        ↓  Genus.CODEC
core/Genus                                  the parsed type (attributes, colour, goal lists)
        ↓  GoalSpec.CODEC dispatch on "type"
core/goal/GoalSpecs.*                       one record per vanilla goal, fields = ctor args
        ↓  spec.build(mob)
neoforge/GenusApplier                       mutates a live vanilla Mob
        ↑
neoforge/ZombieModEvents                    FinalizeSpawnEvent + EntityJoinLevelEvent
```

- **`core/`** is loader-light: `Genus`, the goal specs. **`neoforge/`** holds everything that
  touches events, commands and live entities. (Following MobHealth's split, which exists so a Fabric
  port would only re-implement the adapter. Not a hard rule — WoodDye relaxed it.)
- **Genera are a datapack registry**, registered in `ZombieModRegistries` via
  `DataPackRegistryEvent.NewRegistry`. So new zombie types are JSON in a datapack, they sync to
  clients, and vanilla `/reload` picks them up. There is deliberately no `/zombiemod reload`.
- **Persistent vs transient is the split that matters** in `GenusApplier`:
  - *Persistent* — attributes, equipment, name, scale. Applied once at spawn; Minecraft saves them
    in entity NBT, so they survive a restart unaided.
  - *Transient* — the AI. Goals are objects on a `GoalSelector`, never serialised, so they must be
    rebuilt on **every** `EntityJoinLevelEvent`, not just the first.
  - The genus id lives in `mob.getPersistentData()` under `zombiemod:genus`, which is how a reloaded
    entity knows what to rebuild. (This incidentally fixes the 1.8 README's known bug where player
    corpse zombies were lost on restart — that map was memory-only.)
- **Weighted spawning** (`ZombieModEvents.rollGenus`) replaces the old `WeightedProbMap`: on
  `FinalizeSpawnEvent`, roll among genera whose `base` matches the mob that vanilla was about to
  spawn. `weight: 0` opts a genus out of natural spawning entirely.

### Testing with a genuinely vanilla client

**Run this before shipping anything that touches networking.** The mod's promise is that a player
does not need it installed, and exactly one class of bug breaks that promise silently on your machine
and loudly on everyone else's.

`PayloadRegistrar.optional()` makes the *handshake* tolerant so a vanilla client can connect. It does
**not** make sends safe. `PacketDistributor.sendToPlayer` throws — synchronously, on the server
thread — for a payload the receiver never negotiated:

```
Payload zombiemod:dex may not be sent to the client!
```

That propagates out of whatever handler you were in. From a login handler it takes vanilla's own
login flow with it and the player is kicked with "Invalid player data": a cosmetic feature destroying
the ability to join. **Optional ≠ droppable.** Every clientbound send goes through
`Net.sendIfAble`, which checks `connection.hasChannel` first; expensive payloads are built inside the
guard, not before it.

It is not a negotiation race, either — channels are agreed during the configuration phase, before
`PlayerLoggedInEvent`. A vanilla client simply never has the channel, at any point. Guard
permanently; there is no later event that helps.

The procedure (from the LegendQuest session, which found this the hard way on this NeoForge version):

1. Dedicated dev server, `online-mode=false`, only this mod plus known-safe friends.
2. Join from a genuinely unmodded launcher profile, **Direct Connect to `127.0.0.1`** — not
   `localhost`, which Windows resolves IPv6-first while WSL2's relay only forwards IPv4.
3. The kill window is the first second, because login-time sends fire immediately. Then exercise
   *every* server→client path: login syncs, periodic syncs, and event-driven sends.
4. Grep the server log for `may not be sent to the client` and `Couldn't place player in world`.
   Silence plus a player who stays connected is a pass.
5. Play the feature through its vanilla fallbacks — for ZombieDex that is `/zm bestiary`, the book
   and the scoreboard — to confirm nothing else quietly assumed a modded client.

Nothing was needed in `neoforge.mods.toml` for the connection itself: no `displayTest`, no `side`
change. (Untested: how the server renders in the multiplayer *list* ping, since that test used Direct
Connect.)

### Anything vanilla that moves between versions goes behind `platform/`

`com.sablednah.zombiemod.platform` is the seam layer: one small class per thing Minecraft renamed or
reshaped, so a version branch edits one documented method instead of twenty call sites. The full
table of what moved is in [docs/MULTIVERSION.md](docs/MULTIVERSION.md). Three rules:

- **`platform` is not `compat`.** `compat` is *other mods*, reflective and inert when they are
  absent. `platform` is *Minecraft itself*. A missing mod is normal and must be survived; a missing
  vanilla type is a broken build and should be.
- **Never name a seam after a common vanilla class.** `BlockTypes` and `ItemTypes` are named that way
  because `...block.Blocks` and `...item.Items` are imported all over this codebase, and the
  single-type-import clash is a baffling error from a class meant to reduce confusion.
- **Prefer the seam that needs no per-version body.** `Types` looks entity types up in the registry
  and compiles unchanged everywhere — and is the more correct code anyway, because entity types live
  in an open registry that datapacks extend. That shape costs the branches nothing.

### Moving work between the version branches: sync files, do not cherry-pick

Fixes go on `master` and forward to `mc26.1`/`mc26.2`. **`git cherry-pick -n` is a trap here.** It
leaves the result staged, and switching branches with staged work silently discards it — the pick
reports success, the branch looks fine, and the change is simply absent. That happened moving Jack
and Krampus across (2026-08-28): `mc26.1` was left at 59 genera while claiming to have picked them,
and nothing in the output said so.

**Prefer an explicit file sync**, because the seam layer means you already know exactly which files
are allowed to differ:

```bash
git checkout master -- <the shared paths>
```

Everything except `build.gradle`, `gradle.properties`, `platform/*` and the client GUI files should
be **identical to master** on every branch. So the reliable move is to take the shared files from
master outright rather than replay a commit and hope.

**Verify with something countable, not with the exit code.** Counting genus files across branches is
what actually caught it:

```bash
for b in master mc26.1 mc26.2; do
  echo "$b $(git ls-tree -r --name-only $b -- src/main/resources/data/zombiemod/zombiemod/genus | wc -l)"
done
git diff --name-only master mc26.2      # every line should be a file that is *meant* to differ
```

That second command is the real health check for the branches: if anything outside the version-specific
set appears, the branches have drifted and one of them is missing work.

**They had, and it was the docs** (found 2026-09-14). The branches were in step on code and genera
after 3.4.1 but three releases behind on `CHANGELOG.md`, `README.md`, `CLAUDE.md`, `NODES.md`, the
Modrinth scripts and the Modrinth workflow — nobody had counted docs, because nothing player-facing
depended on them. Before syncing, prove each differing file is merely *behind* rather than
*deliberately different*, or the sync will overwrite a branch-specific edit:

```bash
h=$(git rev-parse mc26.2:CHANGELOG.md)
git log master --format=%h --find-object=$h -- CHANGELOG.md   # non-empty: an old master version, safe to overwrite
```

A file absent from the branch is new on master and also safe. Anything that prints nothing is a
real divergence and wants reading, not overwriting. The sync itself is the `git checkout master --`
form above, run with the seam-layer exclusions as a `grep -v` on `git diff --name-only`, then a
version bump in the branch's own `gradle.properties` — that file is *not* shared, so the version
has to be bumped three times.

### An ItemStack cannot be built while a datapack registry is loading

Genus files are parsed on a worker thread before item data components are bound. On 26.x every
spelling of "read an item from JSON" fails with *"Item minecraft:bow does not have components yet"*,
and `Item.CODEC_WITH_BOUND_COMPONENTS` is the constant that *requires* them, so it does not help.

So `core/ItemSpec` holds a **description** — an id plus a component patch — and builds the stack when
a mob is actually equipped. If you add another place that reads an item out of a genus file, do the
same; do not reach for `ItemStack.CODEC`.

It also makes a wrong item line cheap: the id resolves late, so a typo is **reported once** naming
genus, item and slot, that slot is left empty, and the mob still spawns. Deduplicated deliberately —
equipment applies on *every* spawn, so a warning per spawn would bury what it is saying. Note this is
a deliberate exception to "a malformed genus stops the world loading": one misspelled helmet should
not cost the world.

### The 26.x GUI is a rename, not a redesign — and the sibling mods know it

Judged wrong once, at the cost of treating the dex screen as design work. `GuiGraphics` became
`GuiGraphicsExtractor`, `render` became `extractRenderState`, `drawString` became `text`, and the
doll helper is a pure rename with an identical signature. The table, including the two rows that are
**26.2-only** because 26.1 sits between the two states, is in
[docs/MULTIVERSION.md](docs/MULTIVERSION.md).

The general lesson is the useful one: **CityWorld and LegendQuest carry the same three-branch layout
and have hit these APIs already.** One diff from a sibling that has done the port beats an afternoon
of guessing at `javap` output — that is exactly how the rename table was found, after the wrong
conclusion had already been drawn from two missing methods.

### Command output: styles, never section codes

**Anything that is not a client reads a component through `getString()`, which hands legacy section
codes straight back as literal text.** A server console, the log, and RCON therefore render
`Component.literal("§eZombieMod status")` as exactly that, section sign and all, while the
in-game client shows it correctly. That asymmetry is the whole trap: you cannot see a representation
error while looking through the one thing that interprets the representation.

Build command output as a component tree instead — `Component.literal("ZombieMod status")
.withStyle(ChatFormatting.YELLOW)`. It renders identically in chat and flattens to a clean sentence
everywhere else.

Two details when converting:

- **A coloured span must not be the root.** Children inherit their parent's style, so making the
  coloured part the root colours everything appended after it. Where a line was `§a...§r...`,
  use an unstyled `Component.empty()` root with both spans as children — that is what `§r` meant.
- The same inheritance rule is what made the dex book's first page bleed bold into its list; see the
  comment on the `asBook` branch of `bestiary` in `ZombieModCommands`.

It bites hardest on the output that exists *to* be read out-of-game: `/zombiemod status` is an
admin's command, and `/zombiemod corpse list` is read on a console before deciding whether to
re-issue someone's inventory. The config path line in `status` exists to be **copied**, and a
section code makes it uncopyable from a console.

Fixed across the whole mod on 2026-08-21. The same bug was found and fixed independently in
Standards (`38cb7a0`) and LegendQuest (`dda06b6`) — it is a family-wide pattern, so check for it in
the next port rather than waiting to be told. The cheap check — **and it must look for both spellings**:

```bash
grep -rnE '§|\\\\u00[aA]7' src/main/java
```

Grepping for the character alone is not enough, and that gap hid three real cases until
2026-08-26: a string written `\\u00a7c` contains no section character in the source, but javac
decodes the escape and the running mod emits one. Confirm every hit is a comment, a
`GuiGraphics.drawString` on the client (where section codes *are* the correct mechanism), or a
regex that strips them. Legitimate hits today are `client/DexScreen.java` (font rendering),
`Bounties` (action bar) and `HordeDirector` (boss-bar name) — all client-rendered only.

### Command permissions: nodes whose defaults are the levels — and two Brigadier traps

**Six boolean nodes in `neoforge/ZombieModPermissions`, registered on NeoForge's
`PermissionGatherEvent.Nodes`, and every default resolver is the op level the command needed
before nodes existed** (2 for everything, 3 for `config`). That default is the load-bearing part:
NeoForge's own handler answers every query with it, so a server with no permissions mod, or one
that grants nothing, is unchanged, and `NODES.md` stays true. A command branch is gated with
`.requires(ZombieModPermissions.gate(NODE))`, which asks the permission handler for a player and
falls back to the level for the console and command blocks — a node is a question about a player,
and a command block has nobody to grant to. Since 3.5.0 (2026-09-14), built for a storyteller who
needed hordes and spawning without being handed `/stop`. [`NODES.md`](NODES.md) is the public
statement of exactly which command sits behind which node — **it is documentation of behaviour, so
changing a gate means changing that file in the same commit.**

Two things about Brigadier that this tree has already been bitten by:

- **A requirement on a literal gates its whole subtree**, and this stranded a real player. Observer
  mode was switched on for them, they were deopped, and `observe off` — the only way out — now
  needed the permission they had just lost. They were invulnerable and could not fix it, and neither
  could an op, because the command only ever acts on whoever types it. **So the bar goes on the
  things that grant something and never on the way out.** `observe off` is open to everyone
  permanently, and the self-toggle checks the level *in code* because one node cannot bar a single
  direction.
- **A child's requirement is ANDed with its parent's**, so a restrictive root cannot be relaxed by a
  permissive child. Neither `zombiemod` nor the `zm` redirect carries a bar; a level-2 root would
  put the bestiary permanently out of a normal player's reach. Each subcommand carries its own.

Permission level is also **not** the only thing deciding whether a command works from the console:
several call `getPlayerOrException` because they act on whoever typed them. `NODES.md` has the split.

### Picking a face for a new genus

Faces come from **minecraft-heads.com**, and the catalogue is fetchable rather than scrapeable:

```
https://minecraft-heads.com/scripts/api.php?cat=monsters&tags=true
```

`cat` is one of `monsters`, `animals`, `humanoid`, `humans`, `miscellaneous`, `decoration`, … Each
entry is `{name, uuid, value, tags}`, and **`value` is already exactly the base64 string that goes
into `head.properties.textures[0]`** — no transformation, paste it straight in. It decodes to
`{"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/<hash>"}}}`; the mod ships the
hash, never the artwork.

**Render every candidate and look at it. Never pick by catalogue name.** Nightstalker was given
"Masked Zombie", whose mask turned out to be a *surgical* one — it said nothing about hunting in the
dark. The renderer is a dozen lines with Pillow (`./venv/bin/python`, see RELEASE.md for the venv):

- The face is the **8×8 at (8,8)**, with the **hat layer at (40,8) composited over it**. Do both, or
  you are judging half the design — on many heads the hat layer carries the whole face.
- Scale with `Image.NEAREST`. Anything else invents pixels that are not there.
- Composite against **the base mob's body colour**, not neutral grey. The face is seen against the
  mob, and a pale face on a drowned reads differently from a pale face on a plain zombie.

**Judge on the dimmed version — that is the one that decides.** Multiply RGB by ~0.3 for night and
~0.18 for deep water or a cave. These are things you meet in the dark, and a face that only works in
daylight does not work.

That test is not a formality; it reverses picks. Choosing the Undertow's face (2026-08-24), **Deep
One** was the obvious winner on concept — Lovecraft's Deep Ones drag people into the sea, which is
precisely what `pull` does — and it *failed*: pale skin and small yellow eyes collapse into a dark
blob by night, unreadable at deep-water light. **Dagon** won on the one feature that survives
darkness, a band of bright teeth still legible at 0.18, and its green sits on a drowned body where
Leviathan's equally-legible orange eyes read as a dragon and fought the `dark_aqua` glow. The name
matched the mechanic; the pixels decided.

### Client screens: the colour is ARGB

`GuiGraphics.drawString` and friends take **ARGB**, so the obvious `0xFFFFFF` carries an alpha of
**zero** and draws nothing whatever. The symptom is a screen that opens and dims the world correctly
and is then completely empty, which reads like a layout or a data bug and is neither. Vanilla writes
`-1` everywhere; `LegendQuest`'s handbook, which works in this version, writes `0xFFFFFFFF`. Either
is fine, `0xFFFFFF` is not.

### Client screens: the inventory doll normalises scale away

`InventoryScreen.renderEntityInInventoryFollowsAngle` divides the render state's `boundingBoxHeight`
by its `scale` and then **forces that scale to 1**, so a genus's `Attributes.SCALE` never reaches the
model. The only size lever is the `size` argument — multiply it by the scale yourself.

Setting the attribute anyway is actively harmful, and the reason is the second half of the trap: **an
entity that was never added to a level does not refresh its dimensions.** `setBaseValue` on SCALE is
supposed to trigger `refreshDimensions()`, and on a live mob it does; on a bare
`EntityType.create(...)` doll it does not. So `getBbHeight()` stays at the unscaled default while
`getScale()` returns the new value, and the renderer's normalised height comes out as
`base / scale` — *smaller* the bigger you asked for. Everything computed from it (the translation
that anchors the feet, the box that clips the model) is then sized for a doll less than half the one
being drawn, and the big genera get cut off mid-torso at a fixed line.

Leave the attribute alone on a doll and `getBbHeight()` is the true model height. Note also that the
rect passed to the helper is both the viewport **and** the clip, and the entity always renders at its
centre — so it must be symmetric about the point you want the entity centred on, sized by whichever
of head or feet reaches further.

Proven with a temporary `DollProbe` on `ServerStartedEvent` that built every genus's doll and printed
`bbHeight`/`getScale`/the derived box against the renderer's actual placement. Pure arithmetic, so it
needed no renderer — worth rebuilding if that geometry is touched again.

### Cancelling an interaction client-side does not cancel the server's half

`PlayerInteractEvent.RightClickItem` fires on both sides independently. Cancelling on the client only
stops the client's own `ItemStack.use`; the `ServerboundUseItemPacket` is sent regardless, and the
server runs its own copy of the interaction.

That matters whenever the *server* is what opens a screen. `ServerPlayer.openItemGui` sends a
`ClientboundOpenBookPacket`, so a book is opened by the server telling the client to — cancelling
client-side gets the custom screen, then a flicker, then the book on top of it a tick later. Cancel
on **both** sides.

The server-side cancel must be gated on `Net.listening(player)`, or a vanilla client ends up
right-clicking a book that does nothing at all — a worse bug than the one being fixed.

### A FakePlayer makes player-driven systems testable headlessly

`new FakePlayer(level, new GameProfile(uuid, name))` is a real `ServerPlayer`, which is what lets the
horde director, proximity spawning and anything else keyed on a player be driven from a probe on a
dedicated server with nobody connected. It found the wave-delay bug — every wave was inheriting the
previous wave's delay, so a three-wave horde fired in three ticks and no shipped horde's numbers had
ever been experienced.

**Packets to it are safe; nothing reaches a client.** NeoForge gives it a `FakePlayerNetHandler` over a
dummy connection, so boss bars, chat and title packets go nowhere rather than throwing. Verified
2026-09-11: two FakePlayers were put on a real boss bar and taken off it again without error. (This
file used to say anything sending a packet would NPE, which led to probes avoiding `bar_color` for
no reason.) What it genuinely cannot do is keep a dedicated server awake: it is not a connected
player, so a 1.21.2+ server pauses 60 s after start. Set `pause-when-empty-seconds=0` for any
probe that runs longer than that.

### Verifying changes headlessly

`ServerStartedEvent` logs a genera summary permanently (`ZombieMod: 2 genera loaded - coward (5+0
goals, weight 30), ...`) — that line exists because an empty registry and a working one otherwise
look identical from the console.

For anything deeper, add a temporary probe on that same event, run `./gradlew runServer`, read the
log, then delete it. (There was one during the spike; CityWorld did the same with `PopulationProbe`.)
Two things it is worth re-deriving if you touch that area, because nothing else catches them:

- **Goal counts.** Compare `mob.goalSelector.getAvailableGoals().size()` against
  `genus.goals().size()` after applying. `GoalSpec.build` returning null is a log line, not a
  failure, so a genus can load "successfully" with no AI at all.
- **Force-load the chunk before reading light or biome.** `getMaxLocalRawBrightness` on an unloaded
  chunk returns **15**, so a probe testing a `max_light` condition underground will silently
  conclude "too bright" and prove nothing. `level.getChunkAt(pos)` first. (Not a problem in the real
  game — mobs only spawn in loaded chunks.)
- **An entity search needs the chunk *force*-loaded, and `getChunkAt` is not enough.** With no player
  on a dev server nothing is entity-ticking, so mobs added with `addFreshEntity` are absent from the
  index that `getEntitiesOfClass` reads — it returns 0 and a working feature looks broken. Use
  `level.setChunkForced(x >> 4, z >> 4, true)`. Two further traps in the same family: a freshly added
  entity is queued and does not appear until the **next tick**, so spawn on one tick and search on a
  later one; and `ServerStartedEvent` is too early for either, so defer to `ServerTickEvent.Post`.
- **`GenusApplier.assign` does not apply AI.** It writes the persistent half only; goals are built
  from `EntityJoinLevelEvent`, so a probe that calls `assign` on a bare entity and counts
  `goalSelector` is measuring **vanilla's own goals**. That reads as a dramatic bug for any base with
  no default AI — `Giant` has none, so a probe reported "0 of 5 goals applied" for a genus that was
  fine. Call `applyAi` explicitly, or add the entity to the world and let the event fire.
- **A probe must call the real code, not re-derive it.** A ritual probe recomputed pattern offsets
  itself instead of calling `RitualHandler.matchPattern`, reported 5/5 blocks matched, and sailed
  past the actual bug — the rotation helper was dropping the Y component, so every vertical offset
  in every pattern was silently discarded. The probe validated the *data* and never touched the
  *logic*. If a probe duplicates the thing it is testing, it is testing the duplicate.
- **Test conditions in both directions.** A filter that excludes everything looks identical to one
  that works. Prove a genus is admitted where it should be, not just excluded where it shouldn't.
- **Permission gates want a player, and a `FakePlayer` is one.** `fake.createCommandSourceStack()`
  goes through the node's resolver like a real player's would, and `server.getPlayerList().op(fake.nameAndId())`
  then `deop` flips it between the two answers, so a gate can be proven refused, admitted, and
  refused again in one run. The console source (`server.createCommandSourceStack()`) takes the
  level fallback, so test that separately. Deop in a `finally`: `op` writes `run/ops.json`.
- **Command parsing *and* execution.**
  `server.getCommands().getDispatcher().parse(cmd, server.createCommandSourceStack())`, check both
  `getExceptions()` and `getReader().canRead()`, then `execute(parsed)`. Parsing alone is not
  enough — a bare `coward` parses happily as `minecraft:coward` and only fails at resolution. Test a
  deliberately bad id too, or an over-eager resolver will match garbage unnoticed. Gradle cannot
  pipe stdin to the dev server console, so this is the only headless route.

### Abilities ride the goal selector

`AbilityGoal` wraps each ability as a `Goal` with an **empty flag set**, which is the whole trick:
`GoalSelector.tick` blocks a goal only when one of its flags is taken, so a flagless goal starts
immediately and runs alongside whatever the mob is actually doing. It also sets
`requiresUpdateEveryTick()`, which matters because `tickRunningGoals(false)` on off-ticks skips
goals that don't declare it.

This replaces the 1.8 plugin's global per-tick sweep (`Animations` + an `intervals` counter). Riding
the entity's own ticking means no live-mob registry to maintain, no leak on removal, no cost for
non-ticking chunks, and per-mob timing is just a field. Ability implementations are stateless and
shared; the timer lives in the goal.

### Persistence takes a mob out of vanilla's cap — never set it by default

**A persistent mob is invisible to the mob cap.** `NaturalSpawner.createState` skips any mob with
`isPersistenceRequired()` or `requiresCustomPersistence()` when it counts toward the cap, on purpose:
persistence normally means a player chose to keep this one. So `setPersistenceRequired()` does not
only stop a despawn. It stops the mob counting, and vanilla fills the space it apparently left.

Until the fix, `GenusApplier.assign` made every genus persistent ("something this distinctive
shouldn't quietly despawn") and that became a ratchet. A vanilla zombie spawned and became a
genus, left the count, and vanilla replaced it; the replacement became a genus in turn, and none of
them ever despawned. One test world held **over 2,000** when a `/killall` finally cleared it. Our own
proximity cap was fine the whole time, which is what made it hard to see: it counts by the genus tag
and never asks vanilla.

The rule now:

- **Default: not persistent.** Genera despawn like vanilla zombies and count toward the cap like
  vanilla zombies.
- **Persistent only when losing the mob loses something real:** a player corpse (carries an
  inventory; set in `PlayerZombies`) and a boss (`genus.boss()` present; every ritual summons one).
- **"Keep it only while X" is `MobDespawnEvent`, never the flag.** The flag can only be switched
  *on*: the field is private, nothing clears it, and it is saved with the mob. So a horde's zombies
  are kept by answering `DENY` while their horde is running, and they revert to ordinary the moment it
  ends. The event fires for every mob every tick, so check `RUNNING.isEmpty()` before anything else.
  It keeps them counting toward the cap too, which the flag would not.
- **A mutation inherits** the old mob's persistence rather than forcing it on.

Two things vanilla does that look like our bug but aren't: a **rider** is persistent while mounted
(`requiresCustomPersistence` is `isPassenger() || isLeashed()`), so the Outrider stays while it rides.
Its zombie horse is no longer forced persistent, and vanilla's `ZombieHorse.removeWhenFarAway` returns
true, so the pair leaves like a vanilla zombie horseman would.

### Changing the world

Anything that breaks or places blocks must go through
`EventHooks.canEntityGrief(level, entity)`, never `getGameRules().get(GameRules.MOB_GRIEFING)`. The
gamerule check compiles, works, and silently ignores every land-protection mod on the server,
because the hook is what fires `EntityMobGriefingEvent` for them to veto. The hook falls back to the
gamerule when nothing objects, so it is strictly the better call.

### Adding a goal type or spawn condition

Goals and spawn conditions use the identical shape — a record whose fields mirror the thing it
builds, a `MapCodec`, and a line in the matching `*Types` registry. `SpawnConditionTypes.register`
is deliberately **public**: the interesting conditions (CityWorld's wilderness-vs-city, a claim
mod's protected chunks) live outside this mod and must not become hard dependencies.

`SpawnRules` keeps `reasons` as a plain field rather than a condition, because spawn reason isn't a
property of the *place* and needs a restrictive default — an empty condition list has to mean
"anywhere", but an unspecified reason set must not mean "every reason".

### Goal specifics

1. A record in `core/goal/GoalSpecs` whose fields are the vanilla goal's constructor arguments, with
   a `MapCodec` and a `TYPE` id.
2. One `register(...)` line in `GoalSpecTypes`.

`GoalSpec.build` returns `null` when a goal cannot apply to that mob (many vanilla goals demand a
`PathfinderMob`, some a `TamableAnimal`). The applier logs and skips rather than failing the whole
genus — a deliberate call, but it means a typo'd genus degrades quietly-ish. Check the log.

`TargetClass` is a curated name→`Class` map because vanilla's targeting goals are typed on
`Class<? extends LivingEntity>`, not on `EntityType`. That's a real limitation: you can say "avoid
wolves", not "avoid a modded mob".

## 1.21.11 API notes specific to this mod

Verified against the decompiled sources, not guessed. Extract them with:

```bash
unzip -oq ~/.gradle/caches/neoformruntime/intermediate_results/decompile_*_output.jar 'net/minecraft/**' -d /tmp/mcsrc
unzip -oq build/moddev/artifacts/neoforge-21.11.42-sources.jar 'net/neoforged/**' -d /tmp/nfsrc
```

- **Entity classes moved in 1.21.11**, same reshuffle family as `AbstractArrow`:
  `monster.zombie.{Zombie,Husk,Drowned,ZombieVillager}`, `monster.spider.Spider`,
  `animal.feline.{Cat,Ocelot}`, `animal.wolf.Wolf`, `npc.villager.AbstractVillager`.
- **`MobSpawnType` is now `EntitySpawnReason`** (`FinalizeSpawnEvent.getSpawnType()` returns it).
- `ResourceLocation` → `Identifier`; `ResourceKey.location()` → `identifier()`.
- `Mob.goalSelector` / `targetSelector` / `navigation` are all **protected** → see
  `src/main/resources/META-INF/accesstransformer.cfg`. AT files use **Mojmap names**, not SRG.
- `GoalSelector.removeAllGoals(Predicate)` and `addGoal(int, Goal)` are public — no reflection.
- **`Attributes.SCALE` exists** (1.20.5+, range 0.0625–16, synced). Body size is an attribute now;
  the 1.8 code hacked bounding boxes for this.
- `CompoundTag.getString()` returns `Optional<String>`.
- Datapack registry lookups: `level.registryAccess().lookupOrThrow(KEY)`, then
  `.get(ResourceKey.create(KEY, id))` → `Optional<Holder.Reference<T>>`.

## Reading the original Bukkit plugin

**Removed from the working tree at the 3.0.0 release**, along with the Bukkit-era `config.yml`,
`lang.yml` and `plugin.yml` in the repo root. It lives in git history and in
`github.com/Sablednah/ZombieMod`. To read it again without restoring it:

```bash
# find the commit that removed it, then browse or restore from its parent
git log --diff-filter=D --oneline -- src/me/sablednah/
git show <sha>^:src/me/sablednah/zombiemod/ZombieType.java     # read one file
git checkout <sha>^ -- src/me/sablednah/                       # restore the tree
```

It was 6.4k lines of 1.8/`v1_8_R1` NMS, worth reading for **intent, not for API**. Highlights:
`ZombieType` (the goal set per genus), `ReadData`/`Config`/`PutredineImmortui` (the genus pipeline),
`Animations` (per-tick abilities, driven by an `intervals` counter and `% N` sub-rates),
`ProximitySystems` (spawn-near-player). Note it does **not** compile: `RegisterEntities` names a
`ZombieSteed` class that exists nowhere, and `bin/` was stale output.

Everything it had to teach is now either implemented or recorded in `docs/FROM_1_8.md`, so needing it
again should be rare.

Abilities the old mod shipped, as a to-port checklist: `BACKSTAB BORG BREEDER EXPLODE GHOST HEAL
HEROBRINE HUNTER INFEST INK LAZER LIGHTNING SHOCKWAVE SPIDER STOMP WEB`.

### Testing the CityWorld integration

It is reflective, so "it compiles" proves nothing at all. To exercise it for real:

```bash
cp ../CityWorld-ReForged/build/libs/cityworld-*.jar run/mods/
# in run/server.properties:  level-type=cityworld\:city  and a fresh level-name
./gradlew runServer
```

A probe walking a grid of `CityWorld.lotAt` should see several districts and all four lot styles;
one that only ever sees `NATURE` means the preset did not take and the generator is vanilla. Put
`run/server.properties` back and remove the jar afterwards, or every later dev run pays for it.

## Integrations

Of the 1.8 plugin's six soft-depends (Vault, Spout, LegendQuest, Factions, CityWorld, CreeperHeal),
only CityWorld survives — and it's the same author's, being ported next door.
`../CityWorld-ReForged/.../api/CityWorldAPI.java` keeps a stringly-typed
`getFullInfo(ServerLevel, BlockPos)` matching the Bukkit call the old `ProximitySystems` made, but
`compat/CityWorld` goes through the typed `lotAt`/`LotInfo` instead: the string map omits
`naturePercent`, the generator's own dense-city-to-wilderness grading, which is the most useful
number of the lot. Keep it **optional** — the 1.8
plugin's real bug was calling Factions' `BoardColl` with no `hasFactions` guard, making a soft
dependency mandatory in practice.

**Vanish is the shape of a good `compat/` seam, and the division is the point.** Standards answers
the one question it owns — *is this player hidden* — and each mod acts on it for the things that mod
is responsible for. It already blocks mob **targeting** globally (a `LivingChangeTargetEvent` veto
plus clearing existing targets when someone vanishes), so do **not** add our own: it is done, for
every mob in the game, and duplicating it was drafted here and thrown away.

What Standards cannot do is know that a Boomer's fuse is a **proximity sweep** with no target
involved — it asks who is standing nearby, and a vanished admin was answering, so the thing exploded
beside nobody. That is ours, and it lives in `Targets.nearbyPlayers`, where a vanished player now
joins spectators and creative players in the list of people who are present but not participating.
**Ask `StandardsVanish.anyVanished()` before asking about a player**: it is one field read on
Standards' side and false on virtually every server, and this runs per ability per tick.

Same rule anywhere else we single a player out: `ProximitySpawner` and `HordeDirector` both skip a
vanished player exactly where they already skip spectators and creative ones, and a horde whose
player vanishes mid-run goes down the existing `player.isRemoved()` path rather than a new one.
**The test for whether something needs this is "does it pick a player out and act on them", not
"does it damage them"** — the proximity crowd does no harm at all and still gives a vanish away.

**Permission managers are the exception to the `compat/` rule, and the exception matters.**
ZombieMod's nodes are registered on NeoForge's `PermissionGatherEvent` and nothing lives in
`compat/`. SableCraft Standards' permission system is a *handler* for NeoForge's own
`PermissionAPI` — the same interface LuckPerms implements — and it grants any mod's boolean nodes,
including ones it has never heard of. Both sides talk to NeoForge; nobody calls anybody, so there is
nothing to guard and no dependency to make optional. Wrapping it in a reflective `compat/Standards`
would be pure ceremony. Constraints, kept: **boolean nodes only** (Standards passes typed nodes
through to their own resolver on purpose — use the numbered `mod.thing.limit.5` idiom for
quantities), and **every default resolver must reproduce `NODES.md`**, so a server that installs a
manager and grants nothing behaves exactly as before. Verified headlessly with a `FakePlayer`: a
non-op is refused every gated branch and admitted to `bestiary`, `list` and `observe off`; opped
with `PlayerList.op` it is admitted to all of them; deopped it is refused again; and the console
source parses and executes `status`, `config` and `spawn <genus> <pos>` throughout. Both directions,
because a gate that refuses everything looks identical to one that works.
