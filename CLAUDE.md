# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

**ZombieMod ReForged** — a NeoForge rewrite of ZombieMod, a 2013 Bukkit/Spigot plugin that added
configurable custom zombie types. The port is being built in place at the repo root; the original
1.8 plugin source is still in the working tree under `src/me/sablednah/zombiemod/` as a reference
(Gradle does not compile it — it sits outside `src/main/java`).

This is the **fourth** Bukkit→NeoForge port in a series. `../MobHealth-Forge` is the canonical
template and `../CityWorld-ReForged/PORTING.md` is the richest source of verified 1.21.11 API notes.
Read those before inventing anything.

| | |
|---|---|
| Minecraft | 1.21.11 |
| Loader | NeoForge 21.11.42 |
| Java | 21 |
| Build | Gradle + ModDevGradle (`net.neoforged.moddev`) |
| Licence | MIT (the 1.8 plugin was CC BY-NC-ND; same author relicensed, as with WoodDye) |
| Mod id | `zombiemod`, package `com.sablednah.zombiemod` |

## Build & run

No system Java. Borrow the portable JDK from the first port in the series:

```bash
export JAVA_HOME=/mnt/d/Repos/sable/MobHealth-Forge/tools/jdk21
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
  report, which reads like a code fault and is not one. `pkill -f "gradlew runServer"`, then confirm
  with `ss -ltn | grep 25567`.

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
- **Test conditions in both directions.** A filter that excludes everything looks identical to one
  that works. Prove a genus is admitted where it should be, not just excluded where it shouldn't.
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

Still in the tree at `src/me/sablednah/zombiemod/` (6.4k lines, 1.8/`v1_8_R1` NMS). Worth reading for
intent, not for API. Highlights: `ZombieType` (the goal set per genus), `ReadData`/`Config`/
`PutredineImmortui` (the genus pipeline), `Animations` (per-tick abilities, driven by an `intervals`
counter and `% N` sub-rates), `ProximitySystems` (spawn-near-player). Note it does **not** compile:
`RegisterEntities` names a `ZombieSteed` class that exists nowhere, and `bin/` is stale output.

Abilities the old mod shipped, as a to-port checklist: `BACKSTAB BORG BREEDER EXPLODE GHOST HEAL
HEROBRINE HUNTER INFEST INK LAZER LIGHTNING SHOCKWAVE SPIDER STOMP WEB`.

## Integrations

Of the 1.8 plugin's six soft-depends (Vault, Spout, LegendQuest, Factions, CityWorld, CreeperHeal),
only CityWorld survives — and it's the same author's, being ported next door.
`../CityWorld-ReForged/.../api/CityWorldAPI.java` deliberately keeps a stringly-typed
`getFullInfo(ServerLevel, BlockPos)` matching the Bukkit call the old `ProximitySystems` made, so
context/schematic-driven spawn weighting can be ported near 1:1. Keep it **optional** — the 1.8
plugin's real bug was calling Factions' `BoardColl` with no `hasFactions` guard, making a soft
dependency mandatory in practice.
