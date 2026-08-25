package com.sablednah.zombiemod.neoforge;

import java.util.ArrayList;
import java.util.List;

import com.sablednah.zombiemod.platform.Types;
import com.sablednah.zombiemod.ZombieModConfig;
import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.compat.FtbChunks;
import com.sablednah.zombiemod.core.Genus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Puts zombies near players, rather than only decorating the ones vanilla decided to make.
 *
 * <p>The 1.8 plugin's {@code ProximitySystems}, and the difference between a world that is populated
 * and one that feels occupied. Genera otherwise only ever claim spawns vanilla was already going to
 * produce, which means the mod can change *what* you meet but never *how much* is out there.
 *
 * <p>Off by default. It is the only feature here that adds mobs beyond vanilla's own rules, and a mob
 * pack should not quietly change how many things are hunting someone.
 *
 * <p>Each candidate still has to pass the genus's own {@link com.sablednah.zombiemod.core.spawn.SpawnRules}
 * at the chosen position, so a Frost still only appears in the snow and a Stalker still only in the
 * dark. This adds opportunities; it does not bypass the rules.
 */
public final class ProximitySpawner {

    /**
     * Counters, because this feature is designed to be unobservable and therefore indistinguishable
     * from broken. {@code outOfSightOnly} means you are never supposed to catch one arriving, so
     * "is it working?" cannot be answered by looking - only by asking.
     *
     * <p>The breakdown matters more than the total: 200 attempts producing nothing tells you very
     * little, while 200 attempts of which 180 were rejected for being in view tells you exactly which
     * knob to turn.
     */
    public static final class Counters {
        public int attempts;
        public int spawned;
        public int noSpot;
        public int inView;
        public int capped;
        public int claimed;
        public int noGenus;
        public int vetoed;

        public void reset() {
            attempts = spawned = noSpot = inView = capped = claimed = noGenus = vetoed = 0;
        }

        @Override
        public String toString() {
            return String.format(
                    "%d attempts, %d spawned (no spot %d, in view %d, at cap %d, claimed %d, "
                            + "no genus %d, rules said no %d)",
                    attempts, spawned, noSpot, inView, capped, claimed, noGenus, vetoed);
        }
    }

    public static final Counters COUNTERS = new Counters();

    private static final Logger LOG = LogUtils.getLogger();

    private int ticks;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!ZombieModConfig.ENABLED.get() || !ZombieModConfig.PROXIMITY.get()) {
            return;
        }
        if (++ticks < ZombieModConfig.PROXIMITY_INTERVAL.get()) {
            return;
        }
        ticks = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator() || player.isCreative()) {
                    continue;
                }
                if (level.getRandom().nextDouble() >= ZombieModConfig.PROXIMITY_CHANCE.get()) {
                    continue;
                }
                COUNTERS.attempts++;
                trySpawnNear(level, player);
            }
        }
    }

    private void trySpawnNear(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();

        if (countNearby(level, player) >= ZombieModConfig.PROXIMITY_CAP.get()) {
            COUNTERS.capped++;
            return;
        }

        BlockPos at = findSpot(level, player, random);
        if (at == null) {
            COUNTERS.noSpot++;
            return;
        }

        if (ZombieModConfig.CLAIM_PROTECTION.get()
                && ZombieModConfig.CLAIM_SPAWNS.get() != ZombieModConfig.ClaimSpawns.ALLOW
                && FtbChunks.isClaimed(level, at)) {
            COUNTERS.claimed++;
            return;
        }

        HolderLookup.RegistryLookup<Genus> lookup =
                level.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS);

        // Weighted among genera that both want to spawn naturally and accept this spot. Passing
        // NATURAL means a genus restricted to spawners or breeding is not dragged in by this.
        List<Holder.Reference<Genus>> eligible = new ArrayList<>();
        int total = 0;
        for (Holder.Reference<Genus> holder : lookup.listElements().toList()) {
            Genus genus = holder.value();
            if (genus.weight() <= 0 || !genus.spawn().allows(level, at, EntitySpawnReason.NATURAL)) {
                continue;
            }
            eligible.add(holder);
            total += genus.weight();
        }
        if (eligible.isEmpty()) {
            COUNTERS.noGenus++;
            return;
        }

        int roll = random.nextInt(total);
        Holder.Reference<Genus> chosen = null;
        for (Holder.Reference<Genus> holder : eligible) {
            roll -= holder.value().weight();
            if (roll < 0) {
                chosen = holder;
                break;
            }
        }
        if (chosen == null) {
            return;
        }

        var created = chosen.value().base().create(level, EntitySpawnReason.NATURAL);
        if (!(created instanceof Mob mob)) {
            return;
        }
        mob.snapTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);

        // Let other mods veto, exactly as they could for a natural spawn. Skipping this would make
        // proximity spawns invisible to every spawn-control mod on the server.
        if (!net.neoforged.neoforge.event.EventHooks.checkSpawnPosition(mob, level, EntitySpawnReason.NATURAL)) {
            // Counted, because this is where DAYLIGHT lands: checkSpawnRules is what enforces a
            // monster's darkness requirement. Uncounted, it left the breakdown not adding up to the
            // attempt total and the commonest outcome of all invisible - which defeats the point of
            // having counters for a feature you are not meant to be able to watch.
            COUNTERS.vetoed++;
            mob.discard();
            return;
        }

        GenusApplier.assign(mob, chosen);
        level.addFreshEntity(mob);
        COUNTERS.spawned++;

        if (ZombieModConfig.LOG_SPAWNS.get()) {
            // Proximity spawns bypass FinalizeSpawnEvent, so logSpawns missed them entirely - which
            // is precisely the feature you most want a log line for.
            LOG.info("ZombieMod: proximity {} at {} {} {} ({} blocks from {})",
                    chosen.key().identifier(), at.getX(), at.getY(), at.getZ(),
                    (int) Math.sqrt(player.distanceToSqr(at.getX(), at.getY(), at.getZ())),
                    player.getName().getString());
        }
    }

    /** How far below the local surface a player has to be before we stop aiming at the sky. */
    private static final int UNDERGROUND = 6;
    /** How far up and down to look for a floor when they are. */
    private static final int VERTICAL_REACH = 12;

    /**
     * A spot at a random bearing and distance that a mob can stand in.
     *
     * <p>Usually the surface, which is what the heightmap gives. But a player in a cave, a mine or a
     * cellar is nowhere near it, and aiming at the surface anyway put the zombies on the roof of the
     * world above them — technically within range, and no use to anybody. So when they are well
     * below the local surface, this looks for a floor at <em>their</em> depth instead, which is
     * where a mine's occupants ought to come from.
     */
    private BlockPos findSpot(ServerLevel level, ServerPlayer player, RandomSource random) {
        int min = ZombieModConfig.PROXIMITY_MIN.get();
        int max = Math.max(min + 1, ZombieModConfig.PROXIMITY_MAX.get());

        boolean sawIt = false;
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = min + random.nextDouble() * (max - min);
            int x = (int) (player.getX() + Math.cos(angle) * distance);
            int z = (int) (player.getZ() + Math.sin(angle) * distance);

            // Never reach into unloaded chunks: generating terrain because someone walked around is
            // how a spawner becomes a performance complaint.
            if (!level.isLoaded(new BlockPos(x, player.getBlockY(), z))) {
                continue;
            }

            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos candidate = surface - player.getBlockY() > UNDERGROUND
                    ? floorNear(level, x, player.getBlockY(), z)
                    : new BlockPos(x, surface, z);

            if (candidate == null || !standable(level, candidate)) {
                continue;
            }
            if (ZombieModConfig.PROXIMITY_UNSEEN.get() && canSee(level, player, candidate)) {
                sawIt = true;
                continue;
            }
            return candidate;
        }
        // Attribute the failure to the most useful cause: if any candidate was rejected only for
        // being visible, that is the knob to turn, not the terrain.
        if (sawIt) {
            COUNTERS.inView++;
            COUNTERS.noSpot--;
        }
        return null;
    }

    /**
     * The nearest floor to a given height, searched outwards so the closest one wins.
     *
     * <p>Outwards rather than downwards on purpose: a player on the middle level of a mine should
     * get company from that level, not from whatever cavern happens to be beneath it.
     */
    private static BlockPos floorNear(ServerLevel level, int x, int y, int z) {
        for (int offset = 0; offset <= VERTICAL_REACH; offset++) {
            for (int sign : offset == 0 ? new int[] {0} : new int[] {-1, 1}) {
                BlockPos pos = new BlockPos(x, y + sign * offset, z);
                if (level.isInsideBuildHeight(pos.getY()) && standable(level, pos)) {
                    return pos;
                }
            }
        }
        return null;
    }

    /**
     * Vanilla's own test for standing room, rather than a stricter guess at it.
     *
     * <p>The first version asked for two blocks of literal {@code isAir} over something
     * {@code isSolidRender}, and that quietly refused most of the outdoors. A tuft of grass, a
     * flower, a fern or a layer of snow is not air, so every grassy field and every forest floor was
     * rejected; so was standing on a slab, a path, or a farm. In play that read as "a lot of no
     * place found" while the ground underfoot looked perfectly walkable, which it was.
     *
     * <p>This is {@code SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk} in longhand, pinned to
     * zombie because a spot is picked before a genus is. That is safe rather than sloppy: the chosen
     * mob is put through {@code EventHooks.checkSpawnPosition} further down, which runs its real
     * spawn rules and its obstruction check. This only has to find plausible ground.
     */
    private static boolean standable(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isValidSpawn(level, below, Types.zombie())
                && roomy(level, pos)
                && roomy(level, pos.above());
    }

    private static boolean roomy(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return NaturalSpawner.isValidEmptySpawnBlock(level, pos, state, state.getFluidState(),
                Types.zombie());
    }

    /** Would the player watch it appear? */
    private static boolean canSee(ServerLevel level, ServerPlayer player, BlockPos pos) {
        Vec3 eye = player.getEyePosition();
        Vec3 target = Vec3.atCenterOf(pos.above());
        HitResult hit = level.clip(new ClipContext(eye, target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS;
    }

    private static int countNearby(ServerLevel level, ServerPlayer player) {
        double radius = ZombieModConfig.PROXIMITY_MAX.get() + 8.0D;
        return level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius),
                m -> m.getPersistentData().getString(GenusApplier.GENUS_TAG).isPresent()).size();
    }
}
