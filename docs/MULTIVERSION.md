# Building for more than one Minecraft version

Measured, not estimated. Every number here came from actually compiling the mod against the version
in question; nothing in this file is a guess about what *might* break.

Last measured 2026-08-25, against `master` at 3.1.1.

## The state of it

| | 1.21.11 | 26.1.2 | 26.2 |
|---|---|---|---|
| Branch | `master` | `mc26.1` | `mc26.2` |
| NeoForge | 21.11.42 | 26.1.2.95 | 26.2.0.59 |
| moddev plugin | 2.0.141 | 2.0.144 | 2.0.144 |
| Java | 21 (`java-runtime-delta`) | **25** (`java-runtime-epsilon`) | **25** |
| Compiles | yes | **no — 80 errors** | **no — 142 errors** |

`mc26.1` and `mc26.2` exist as branches carrying the retarget only. Neither compiles yet; both are
honest starting points rather than half-finished ports.

## What is *not* a problem

**The access transformer survives both versions.** All five entries — `Mob.goalSelector`,
`Mob.targetSelector`, `Mob.navigation`, `Guardian.setActiveAttackTarget`, `Entity.hasVisualFire` —
still resolve, and both builds got past the JST step to `compileJava`. This was checked first on
purpose: an AT naming a field that has moved fails the build in a way that needs a redesign rather
than an edit, and it would have changed the whole plan.

**There are no mixins.** Nothing in this mod patches vanilla bytecode, which removes the single most
version-fragile thing a mod can have.

**`ChunkPos` is not touched.** 26.1 turned it into a record, so `pos.x` became `pos.x()`. That change
rewrote code all over CityWorld and costs us nothing, because nothing here reads those fields.

## The drift, by version

26.2 breaks everything 26.1 breaks, plus more. The shared set:

| Change | 26.1 hits | 26.2 hits | Notes |
|---|---:|---:|---|
| `GuiGraphics` gone | 20 | 20 | **client only** — the dex screen |
| `displayClientMessage(Component, boolean)` | 20 | 20 | split into `sendSystemMessage` / `sendOverlayMessage` |
| `BlockState.getBlockHolder()` | 10 | 10 | |
| `SavedDataType<>` inference | 6 | 6 | Bestiary, CorpseLedger |
| `EntityType.is(TagKey)` | 4 | 4 | |
| `ServerBossEvent` constructor | 6 | 6 | |
| `ItemStack.STRICT_SINGLE_ITEM_CODEC` / `SIMPLE_ITEM_CODEC` | 4 | 4 | |
| `ItemStack.getItemHolder()`, `Level.getDayTime()` | 2 each | 2 each | |
| override mismatches | 6 | 6 | fallout from `GuiGraphics` |

**The `displayClientMessage` split is the friendliest of them:** the old boolean said "action bar or
chat", and the two replacements say it in their names. It is a rename with a decision attached, not
a redesign.

## What makes 26.2 different in kind

**26.2 removed the `EntityType` constants.**

| | `public static final` fields on `EntityType` |
|---|---:|
| 26.1.2 | 159 |
| 26.2 | **2** — and both are codecs |

So `EntityType.ZOMBIE`, `.ARROW`, `.GUARDIAN`, `.ELDER_GUARDIAN` and `.LIGHTNING_BOLT` are gone, and
an entity type is reached through the registry instead. That one change is most of the gap between
80 errors and 142: `Convert.java` goes from 2 errors to 24 and `Abilities.java` from 2 to 12.

26.2 also drops `ChatFormatting.COLOR_CODEC`, which is what a genus's `glow` field is built on — so
the Undertow's `"glow": "dark_aqua"` sits directly on this path. And `Minecraft.setScreen` and
`getMainRenderTarget()` changed, both client-side.

**CityWorld solved exactly this shape** with an interned shim over `BuiltInRegistries`, and its
reasoning applies here with more force than it does there: *entity types live in an open registry
that datapacks and other mods extend, so an enum would be wrong.* For a mod whose whole premise is
datapack-defined genera, a registry lookup is not a workaround — it is the more correct code, and it
compiles on every version including 1.21.11.

## The plan

**A branch per Minecraft version**, as CityWorld does. No build-system work, proven in the family,
and each branch differs by five lines of configuration.

**With a compat seam, which CityWorld could afford to skip and we cannot.** With branches, merge pain
is proportional to how *scattered* the version-specific code is; theirs is concentrated in a
generated `compat/` layer, ours is spread across roughly **45 call sites in 19 files**. Collect those
behind one thin layer and a genus or a bugfix cherry-picked across three branches touches one file
instead of twenty.

The seam earns its keep twice, because several of the fixes are **version-agnostic**: a registry
lookup for an entity type compiles on 1.21.11, 26.1 and 26.2 alike. Every call converted that way is
one the branches never have to differ over at all.

Order of work, cheapest and most valuable first:

1. **Entity types by registry lookup.** Kills most of the 26.2-only gap and is correct on every
   version, so it lands on `master` and helps immediately.
2. **Player messaging behind one helper.** 20 hits, purely mechanical.
3. **The remaining small ones** — block/item holders, day time, item codecs, boss events, saved data.
4. **The client dex screen last.** 44 of 26.2's 142 errors, 6 of 71 files, and the only part a
   server running for vanilla clients never loads. It is separable, and it is the piece most worth
   deferring if the goal is a working server jar sooner.

## Reproducing the measurement

The expensive part — the NeoForm decompile — is already cached from CityWorld's builds, but a compile
still takes about **16 minutes per version** because `transformSources` re-runs the AT. Budget ~50
minutes of CI for three versions in parallel.

```bash
git checkout mc26.2
export JAVA_HOME=/mnt/d/Repos/sable/CityWorld-ReForged/tools/jdk25   # ZombieMod bundles no JDK 25
./gradlew compileJava --offline
```

ZombieMod borrows a JDK rather than bundling one, and now needs two: MobHealth's `tools/jdk21` for
the 1.21 line and CityWorld's `tools/jdk25` for 26.x. `deploy.sh` should prefer the newest present,
the way CityWorld's does, and let an existing `JAVA_HOME` win.

## The one thing blocking 26.x, and it is not a rename

**An `ItemStack` cannot be constructed while a datapack registry is loading on 26.x.**

Genus files are parsed on a worker thread during registry data loading, before item data components
are bound, and every spelling of "read an item from JSON" fails there:

```
Failed to parse zombiemod:archer from pack mod/zombiemod
  Caused by: Item minecraft:chainmail_chestplate does not have components yet
```

Both accepted forms die the same way — the bare `"minecraft:bow"` and the full
`{"id": ..., "components": {...}}` — so it is not the codec pair that 26.2 removed. 26.2 makes the
rule visible by adding `Item.CODEC_WITH_BOUND_COMPONENTS` beside the plain `Item.CODEC`; the new
constant is the one that *requires* components, so it does not help either.

**The fix is to defer construction.** Parse equipment into a *description* — an item `Holder` plus a
`DataComponentPatch` — and materialise the `ItemStack` when a mob is actually equipped, which happens
at spawn, long after bootstrap. That is a change to `Equipment` and `GenusApplier`, not to the
platform layer, and it is **version-agnostic**: deferring works on 1.21.11 exactly as well, so it
lands on `master` like the other seams.

It is deliberately not done yet. It changes how every genus's equipment is read and applied, which
is worth doing with the result watched in game rather than merely compiled — six equipment slots,
trims, and the components that make a Vault Dweller's suit blue.

**Everything else on the server side is finished.** 26.2 compiles with zero server or common errors,
builds a jar, and starts a server; it falls over at genus parsing on this one point.

## Still unknown

- **Runtime behaviour on 26.x.** Everything here is a *compile* measurement. Nothing has been run.
- **The vanilla-client promise, per version.** The mod's central claim needs re-proving on each
  version rather than assumed; the procedure is in CLAUDE.md.
- **Jar naming.** All three versions currently produce `zombiemod-3.1.1.jar`, which is
  indistinguishable in a mods folder or on a releases page. CityWorld solves it with
  `version = "${mod_version}+mc${minecraft_version}"` and we will need the same before shipping any
  of this.
