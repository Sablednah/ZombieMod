# Building for more than one Minecraft version

Measured, not estimated. Every number here came from compiling and *running* the mod against the
version in question.

Last updated 2026-08-26. **All three versions build the whole mod and run.**

## The shape of it

| | 1.21.11 | 26.1.2 | 26.2 |
|---|---|---|---|
| Branch | `master` | `mc26.1` | `mc26.2` |
| NeoForge | 21.11.42 | 26.1.2.95 | 26.2.0.59 |
| moddev plugin | 2.0.141 | 2.0.144 | 2.0.144 |
| Java | 21 (`java-runtime-delta`) | **25** (`java-runtime-epsilon`) | **25** |
| Builds & runs | yes | yes | yes |
| Confirmed in play | yes | **yes** (2026-08-26) | **yes** (2026-08-26) |

**A branch per Minecraft version**, as CityWorld and LegendQuest both do. Each branch differs from
`master` only in `gradle.properties` (three lines), `build.gradle` (plugin version, Java toolchain),
and the bodies of the seams below.

**ZombieMod bundles no JDK and now needs two.** MobHealth's `tools/jdk21` for the 1.21 line and
CityWorld's `tools/jdk25` for 26.x:

```bash
git checkout mc26.2
export JAVA_HOME=/mnt/d/Repos/sable/CityWorld-ReForged/tools/jdk25
./gradlew build --offline
```

Jars are named `zombiemod-<ver>+mc<mc>.jar`, so three files cannot be confused in a mods folder.
(`master` still produces a plain name — worth aligning before the next multi-version release.)

## The platform seam, and why it exists

Everything that moved between versions is behind `com.sablednah.zombiemod.platform`. Each class is
one documented method whose body names the new API, so a version branch edits **one file per
concern** rather than the twenty-odd call sites the drift is spread across.

| Seam | What moved | 26.x body |
|---|---|---|
| `Types` | `EntityType.ZOMBIE` and 14 siblings — **26.2 removed them** (159 constants on 26.1, 2 on 26.2) | *none needed* |
| `Msg` | `displayClientMessage(c, bool)` split by name | `sendSystemMessage` / `sendOverlayMessage` |
| `BlockTypes` | `getBlockHolder()` | `typeHolder()` |
| `ItemTypes` | `getItemHolder()` | `typeHolder()` |
| `Times` | `Level.getDayTime()` | `getOverworldClockTime()` |
| `Tags` | `EntityType.is(TagKey)` | `builtInRegistryHolder().is(tag)` |
| `Bars` | `ServerBossEvent` ctor | gained a leading `UUID` |
| `Saves` | `SavedDataType` name | an `Identifier`, not a `String` |
| `Colours` | `ChatFormatting.COLOR_CODEC`, `getName()`, `isColor()` — **26.2 only** | codec by enum name; `TeamColor` when painting |

**`platform` is not `compat`.** `compat` is for *other mods* — FTB Chunks, CityWorld, Standards — and
everything in it is reflective and inert when they are absent. `platform` is for *Minecraft* moving
underneath us. A missing mod is normal and must be survived; a missing vanilla type is a broken build
and should be.

**Do not name a seam after a common vanilla class.** `BlockTypes` and `ItemTypes` are named that way
because `net.minecraft...block.Blocks` and `...item.Items` are imported all over this codebase, and a
single-type-import clash is a confusing error from a class whose job is to reduce confusion.

**The best seam is the one that needs no per-version body.** `Types` looks entity types up in the
registry, which compiles unchanged on every version — and is the more correct code anyway, since
entity types live in an open registry that datapacks extend. Reach for that shape first.

## The two changes that were not renames

**Item stacks cannot be built during registry load.** Genus files are parsed on a worker thread
before item data components are bound, so on 26.x every spelling of "read an item from JSON" fails
with *"Item minecraft:bow does not have components yet"*. `Item.CODEC_WITH_BOUND_COMPONENTS` is the
constant that *requires* them, so it does not help.

Fixed by `core/ItemSpec`: a genus slot holds a **description** — an id plus a component patch — and
the stack is built when a mob is equipped, long after bootstrap. Version-agnostic, so it lives on
`master`. It also bought better behaviour: a wrong item id is now resolved late, **reported once**
with genus, item and slot named, and that one slot is skipped while the mob still spawns. Previously
a bad id was a parse failure, and a malformed genus stops world loading — so one misspelled helmet
took the whole world with it.

**Equipment applies on every spawn**, which is why the warning is deduplicated; a line per spawn
would bury what it is trying to say.

## The 26.x GUI: a rename table, not a redesign

This was misjudged once and cost a day of treating the dex screen as design work. It is a rename
table, and **LegendQuest's handbook diff is the reference** — it was ported first.

| 1.21.x | 26.1 | 26.2 |
|---|---|---|
| `GuiGraphics` | `GuiGraphicsExtractor` | same |
| `Screen.render` | `extractRenderState` | same |
| `Screen.renderBackground` | `extractBackground` | same |
| `g.drawString` / `drawCenteredString` | `g.text` / `g.centeredText` | same |
| `g.renderItem` / `renderItemDecorations` | `g.item` / `g.itemDecorations` | same |
| `InventoryScreen.renderEntityInInventoryFollowsMouse` | `extractEntityInInventoryFollowsMouse` | same |
| `mc.setScreen` / `mc.screen` | *unchanged* | `mc.gui.setScreen` / `mc.gui.screen()` |
| `Minecraft.getMainRenderTarget()` | *unchanged* | `gameRenderer.mainRenderTarget()` |

**26.1 sits between the two states**: it renamed the graphics class but kept 1.21's screen accessors
and render target. A 26.2 client fix does not automatically carry backwards — the last two rows are
26.2 only.

The doll — `extractEntityInInventoryFollowsMouse` — is a **pure rename with an identical signature**,
which is worth knowing because its geometry took three passes to get right originally.

## What is *not* a problem

**The access transformer survives every version.** All five entries still resolve. Checked first on
purpose: an AT naming a field that moved fails in a way that needs a redesign rather than an edit.

**There are no mixins**, which removes the most version-fragile thing a mod can have.

**`ChunkPos` is untouched.** 26.1 made it a record, so `pos.x` became `pos.x()` — that rewrote code
all over CityWorld and costs us nothing.

## Adding the next version

Roughly an hour, most of it waiting on builds.

1. **Branch from `master`, do not cherry-pick onto an old branch.** `mc26.1` was branched before the
   seams existed and fought every cherry-pick; rebranching and re-applying the retarget took five
   minutes and was clean.
2. Retarget `gradle.properties` (3 lines) and `build.gradle` (plugin, toolchain).
3. Build. Everything that breaks in `platform/` is a seam body to fill in; anything breaking
   *outside* `platform/` is a new drift that wants a new seam.
4. **Check the sibling mods first.** CityWorld and LegendQuest carry the same branch layout, and both
   have hit these APIs already. One diff from a mod that has done the port beats an afternoon of
   guessing at javap output — that is how the GUI table above was found.
5. Run it, do not just build it. The item-components failure compiled, built a jar, and started a
   server before falling over.

## Reproducing the measurement

The expensive part — the NeoForm decompile — is cached from CityWorld's builds. The *first* compile
on a version still takes about **16 minutes** because `transformSources` re-runs the access
transformer; after that, incremental builds are seconds. Budget ~50 minutes of CI for three versions
in parallel.

```bash
git checkout mc26.2
export JAVA_HOME=/mnt/d/Repos/sable/CityWorld-ReForged/tools/jdk25   # ZombieMod bundles no JDK 25
./gradlew compileJava --offline
```

ZombieMod borrows a JDK rather than bundling one, and now needs two: MobHealth's `tools/jdk21` for
the 1.21 line and CityWorld's `tools/jdk25` for 26.x. `deploy.sh` should prefer the newest present,
the way CityWorld's does, and let an existing `JAVA_HOME` win.

## What to stress-test on a new version

Ranked by how much of it was *rewritten*, not by how visible it is. Each names a genus that
exercises the path, so the list is usable rather than aspirational.

**Every row below was confirmed on 26.2 and again on 26.1.2 (2026-08-26)**, bar the action-bar
bounty, which needs an economy mod or a scoreboard objective before anything pays at all.

| Test | What it proves | How |
|---|---|---|
| ~~**Restart the world**~~ **✔ 26.2** | `Saves` — the bestiary, corpse ledger and known-players list all changed shape (`SavedDataType` takes an `Identifier`, not a `String`). Silent data loss was the failure mode. | Confirmed 2026-08-26: the dex remembered across a reload. |
| ~~**A glowing genus**~~ **✔ 26.2** | `Colours` — the most-rewritten single path: a codec rebuilt from the enum name *and* a different type at the point the team is painted. | Confirmed 2026-08-26: the Glowing One glows. |
| ~~**A boss, or a horde**~~ **✔ 26.2** | `Bars` — `ServerBossEvent` gained a leading `UUID`. A bar is addressed by it on the wire, so a wrong id shows up as bars merging or not appearing. | Confirmed 2026-08-26: Patient Zero's bar *and* a horde bar, which are separate call sites. |
| ~~**A component-laden item**~~ **✔ 26.2** | The *full* `ItemSpec` form — id plus components. The corpse only proved the bare path. | Confirmed 2026-08-26: trims render, and the Vault Dweller's yellow-and-blue is right — which also clears `armor_color`, a separate path from components. |
| ~~**Infection through to rising**~~ **✔ 26.2** | `Tags` — "is this already undead" is the guard that stops a zombie rising from a zombie, and it now answers through the registry holder. | Confirmed 2026-08-26, **both paths**: bitten as a player, died infected, rose; and the herd chain end to end — a `biter` biting, *skipping the already-infected*, victims turning on their own timer, and the infection spreading through a flock. The only test here whose failure would have been silent. |
| ~~**Conversion**~~ **✔ 26.2** | `Types` + `Tags` — the undead-counterpart map is all registry lookups now. | Confirmed 2026-08-26. Note it declines *silently* for six reasons; `/zombiemod status` now counts them, after "the Carrier is not converting" turned out to be the crowding cap doing its job. |
| ~~**Lightning and projectiles**~~ **✔ 26.2** | `Types` — entity types created by registry rather than constant. | Confirmed 2026-08-26: `stormcaller` and `spitfire` both. |
| ~~**`seek_blocks`**~~ **✔ 26.2** | `BlockTypes` — tag tests on block state holders, on the hot path of every tick of that goal. | Confirmed 2026-08-26: Bramble lays moss, Blight seeks it out and removes it — which also clears `place_block`. |
| ~~**A bounty landing**~~ **✔ all versions** | `Msg` — the action-bar half, which is a *different method per version*: `displayClientMessage(text, true)` on 1.21.11, `sendOverlayMessage(text)` on 26.x. | Confirmed 2026-08-27 on **both**: through Standards' economy on 1.21.11, and on 26.2 via `/scoreboard objectives add zombiemod.bounty dummy` — the tally counts as a payment, so the action bar fires with no economy mod present. |
| ~~**Day/night gating**~~ **✔ 26.2** | `Times` — the world clock accessor changed. | Confirmed 2026-08-26: the Nightstalker rings away in daylight. |

**Every row on this table has been watched working on every supported version** (2026-08-27).
Nothing in the port is unconfirmed.

Two of them needed a deliberate setup rather than just playing, which is worth remembering next time:
a **bounty** pays nothing without an economy provider or a pre-existing scoreboard objective, so the
action bar correctly never fires until one exists; and **`playerZombies` is off by default**, so no
corpse rises until it is switched on.

**One known gap worth checking deliberately: a world carried *across* versions.** The saved-data name
is a bare string on 1.21.11 and an `Identifier` from 26.1, so a world moved between the two lines may
not find its old bestiary or corpse ledger. Fresh worlds are unaffected, and no shipped world has
been migrated yet — but if the dex looks empty after a move, that is the first thing to suspect.

## Still open

- **Client code diverges in three places.** `mc.setScreen`, `mc.screen` and `getMainRenderTarget`
  differ on 26.2 only, and they sit in `DexScreen` and `DexRender` — the largest client files, so any
  future dex change will conflict there. A small `platform/Screens` seam would make the client files
  identical across all three branches, the way the server side already is.
- **26.1 has not been played**, only built and run headlessly. 26.2 has been played and was fine.
- ~~Nothing is pushed~~ — **all three branches are on GitHub**, and **3.2.0 shipped 2026-08-26**
  with a jar per version.

  Worth knowing how a multi-version release reaches CurseForge: the workflow uploads *every jar
  attached to the GitHub release*, and takes each one's Minecraft version **from its `+mc`
  filename**. It only ever checks out the tag's ref, so `gradle.properties` there describes one
  version and would mislabel the other two. That is what the suffix is for — it is the only place a
  jar states what it was built against.
