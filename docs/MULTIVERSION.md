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
| Confirmed in play | yes | — | **yes** (2026-08-26) |

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

## Still open

- **Client code diverges in three places.** `mc.setScreen`, `mc.screen` and `getMainRenderTarget`
  differ on 26.2 only, and they sit in `DexScreen` and `DexRender` — the largest client files, so any
  future dex change will conflict there. A small `platform/Screens` seam would make the client files
  identical across all three branches, the way the server side already is.
- **26.1 has not been played**, only built and run headlessly. 26.2 has been played and was fine.
- **Nothing is pushed.** All three branches are local.
- **`master`'s jar name** does not carry `+mc`, unlike the branches. Worth aligning before the next
  multi-version release, since it changes the released artifact's filename.
