# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

**ZombieMod ReForged** — a NeoForge rewrite of ZombieMod, a 2013 Bukkit/Spigot plugin that added
configurable custom zombie types. The port was built in place at the repo root. **Released as 3.0.0**
(2026-08-17); the port is complete and the 1.8 reference tree has been removed — see *Reading the
original Bukkit plugin* below for how to get it back when you need it.

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

Caveats: it has no connection, so anything that sends a packet to it will NPE. Build the spec you are
testing without a `bar_color`, and don't rely on chat. `displayClientMessage` is already a no-op.

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
