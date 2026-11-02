package com.sablednah.zombiemod.compat;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/**
 * Asks CityWorld what it planned for a place, if CityWorld is here at all.
 *
 * <p>The tie CityWorld and ZombieMod were always meant to have: CityWorld knows this chunk is a
 * highrise district, that one is farmland and the road between them is a road, and ZombieMod is the
 * thing that cares. It is the difference between monsters distributed across a world and monsters
 * that belong to the parts of it they are found in.
 *
 * <p>Reflection rather than a compile-time dependency, for the same reason as {@link FtbChunks}: an
 * optional integration must not be able to break the thing it is optional to, and the two mods being
 * by the same author is a reason to be more careful about that, not less — a shared build would make
 * a broken CityWorld commit into a broken ZombieMod build.
 *
 * <p>Goes through {@code lotAt} and the typed {@code LotInfo} rather than the older
 * {@code getFullInfo} string map, because the map leaves out the single most useful number:
 * {@code naturePercent}, the generator's own grading from dense city to wilderness. Every value read
 * here is a String, a primitive or an enum read via {@code toString}, so no CityWorld class ever
 * needs to be on our classpath.
 *
 * <p><b>Never plans on the caller's thread</b> when CityWorld can avoid it (6.12+). CityWorld keeps its
 * plans in memory only, so after a restart the first {@code lotAt} for each platmap plans it right
 * there - tens of milliseconds to seconds - and at the exploration frontier it joins a worldgen
 * worker's plan already in flight and waits out the rest. Every spawn draw asks, four or five times
 * over, so on the server thread that was ZARP's 55-second stall (2026-11-02). Now a draw reads only
 * what is already planned, asks for the rest off-thread, and treats "not known yet" as no lot - a
 * plain mob for a moment rather than a frozen server. Answers are memoised per chunk, since a plan
 * never changes once made.
 */
public final class CityWorld {

    private static final Logger LOG = LogUtils.getLogger();

    private static boolean checked;
    private static boolean available;
    private static boolean warned;

    private static Method lotAt;
    /** CityWorld 6.12+; null on an older one, which falls back to the blocking {@link #lotAt}. */
    private static Method lotAtIfPlanned;
    private static Method lotAtAsync;
    private static Method contextFamily;
    private static Method contextClass;
    private static Method lotStyle;
    private static Method lotClass;
    private static Method naturePercent;
    private static Method schematicName;

    private static Set<String> lotClasses;
    private static Set<String> contextClasses;

    /** "Planned, and there is no lot here" - distinct from not knowing yet, which is no entry. */
    private static final Object NO_LOT = new Object();
    /**
     * Chunks remembered per dimension before that dimension's memo starts again. A 128-chunk square
     * of answers; a long journey refills it rather than growing without end.
     */
    private static final int MEMO_LIMIT = 16_384;
    /** Per dimension, per chunk: a {@link Lot}, {@link #NO_LOT}, or the pending future. */
    private static final Map<ResourceKey<Level>, Map<Long, Object>> MEMO = new ConcurrentHashMap<>();

    private static final String PLAT_LOT = "me/daddychurchill/CityWorld/Plats/PlatLot";
    private static final String DATA_CONTEXT = "me/daddychurchill/CityWorld/Context/DataContext";

    private CityWorld() {}

    /**
     * Simple names of every lot class CityWorld has - what a {@code city_lot} {@code classes} entry
     * can match. Empty without CityWorld.
     *
     * <p>Read from the loader's scan of CityWorld's jar rather than from CityWorld itself, so it costs
     * CityWorld no API and tracks whatever lots the installed version actually has: a list written
     * down here would go stale the first time a lot was added or renamed, which is the very mistake
     * it exists to catch.
     */
    public static synchronized Set<String> lotClasses() {
        if (lotClasses == null) {
            scan();
        }
        return lotClasses;
    }

    /** As {@link #lotClasses}, for {@code city_district}'s context classes. */
    public static synchronized Set<String> contextClasses() {
        if (contextClasses == null) {
            scan();
        }
        return contextClasses;
    }

    /** Subclasses of {@code PlatLot} and {@code DataContext}, by walking each class's parent chain. */
    private static void scan() {
        lotClasses = new HashSet<>();
        contextClasses = new HashSet<>();
        if (!available()) {
            return;
        }
        try {
            Map<String, String> parents = new HashMap<>();
            for (var data : ModList.get().getModFileById("cityworld").getFile().getScanResult().getClasses()) {
                parents.put(data.clazz().getInternalName(),
                        data.parent() == null ? null : data.parent().getInternalName());
            }
            for (String name : parents.keySet()) {
                for (String up = parents.get(name); up != null; up = parents.get(up)) {
                    if (up.equals(PLAT_LOT) || up.equals(DATA_CONTEXT)) {
                        String simple = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('$')) + 1);
                        (up.equals(PLAT_LOT) ? lotClasses : contextClasses).add(simple);
                        break;
                    }
                }
            }
        } catch (RuntimeException e) {
            // Only ever used to warn. Not knowing the names must never cost anything else.
            LOG.debug("ZombieMod: could not list CityWorld's classes ({})", e.toString());
            lotClasses.clear();
            contextClasses.clear();
        }
    }

    /** True if CityWorld is present and its API answered as expected. */
    public static synchronized boolean available() {
        if (!checked) {
            checked = true;
            available = link();
            if (available) {
                LOG.info("ZombieMod: CityWorld detected - district-aware spawning available{}.",
                        nonBlocking() ? "" : " (an older CityWorld: the first lookup in each place "
                                + "plans it on the spot - 6.12 or later avoids that)");
            }
        }
        return available;
    }

    private static boolean link() {
        if (!ModList.get().isLoaded("cityworld")) {
            return false;
        }
        try {
            Class<?> api = Class.forName("me.daddychurchill.CityWorld.api.CityWorldAPI");
            Class<?> info = Class.forName("me.daddychurchill.CityWorld.api.LotInfo");
            lotAt = api.getMethod("lotAt", ServerLevel.class, BlockPos.class);
            contextFamily = info.getMethod("contextFamily");
            contextClass = info.getMethod("contextClass");
            lotStyle = info.getMethod("lotStyle");
            lotClass = info.getMethod("lotClass");
            naturePercent = info.getMethod("naturePercent");
            schematicName = info.getMethod("schematicName");
            lotAtIfPlanned = optional(api, "lotAtIfPlanned");
            lotAtAsync = optional(api, "lotAtAsync");
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("ZombieMod: CityWorld is installed but its API did not match - "
                    + "district conditions will never match. ({})", e.toString());
            return false;
        }
    }

    private static Method optional(Class<?> api, String name) {
        try {
            return api.getMethod(name, ServerLevel.class, BlockPos.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static boolean nonBlocking() {
        return lotAtIfPlanned != null && lotAtAsync != null;
    }

    /** Forget every remembered answer. A new world may reuse the dimension keys of the last. */
    public static void forgetSession() {
        MEMO.clear();
    }

    /**
     * What CityWorld planned for the chunk containing {@code pos}, if it is known yet.
     *
     * <p>Empty for a non-CityWorld level, which is the common case and not an error - and also, with
     * CityWorld 6.12+, for a place whose plan is not made yet. That is asked for off-thread and the
     * answer is here on a later call; meanwhile the city conditions fail closed, which gives an
     * ordinary mob. Safe to call from any thread: it never plans and never waits, except on a
     * CityWorld too old to offer that, where it is the old blocking lookup, memoised.
     */
    public static Optional<Lot> lotAt(Level level, BlockPos pos) {
        if (!available() || !(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        // Packed by hand rather than through ChunkPos, whose helpers have moved between versions.
        long chunk = ((long) (pos.getX() >> 4) & 0xFFFFFFFFL) | ((long) (pos.getZ() >> 4) << 32);
        Map<Long, Object> memo = MEMO.computeIfAbsent(server.dimension(), k -> new ConcurrentHashMap<>());
        Object known = memo.get(chunk);
        if (known instanceof Lot lot) {
            return Optional.of(lot);
        }
        if (known == NO_LOT) {
            return Optional.empty();
        }
        try {
            Object answer;
            if (!nonBlocking()) {
                answer = lotAt.invoke(null, server, pos);
            } else if (known instanceof CompletableFuture<?> pending) {
                if (!pending.isDone()) {
                    return Optional.empty();
                }
                answer = settled(pending);
            } else {
                answer = lotAtIfPlanned.invoke(null, server, pos);
                if (!(answer instanceof Optional<?> maybe) || maybe.isEmpty()) {
                    // Not planned yet, or planned with no lot here: the future tells the two apart.
                    // CityWorld shares one per platmap, so this is cheap even when it is not new.
                    if (!(lotAtAsync.invoke(null, server, pos) instanceof CompletableFuture<?> future)) {
                        return Optional.empty();
                    }
                    if (!future.isDone()) {
                        remember(memo, chunk, future);
                        return Optional.empty();
                    }
                    answer = settled(future);
                }
            }
            Optional<Lot> lot = read(answer);
            remember(memo, chunk, lot.isPresent() ? lot.get() : NO_LOT);
            return lot;
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                LOG.warn("ZombieMod: CityWorld lookup failed; district conditions will not match", e);
            }
            return Optional.empty();
        }
    }

    /** A finished future's answer; a failed plan counts as no lot rather than being asked forever. */
    private static Object settled(CompletableFuture<?> future) {
        if (future.isCompletedExceptionally()) {
            LOG.debug("ZombieMod: CityWorld could not plan a place; treating it as no lot");
            return Optional.empty();
        }
        return future.join();
    }

    private static void remember(Map<Long, Object> memo, long chunk, Object value) {
        if (memo.size() >= MEMO_LIMIT) {
            memo.clear();
        }
        memo.put(chunk, value);
    }

    /** CityWorld's {@code Optional<LotInfo>}, read into our own record. */
    private static Optional<Lot> read(Object result) throws ReflectiveOperationException {
        if (!(result instanceof Optional<?> maybe) || maybe.isEmpty()) {
            return Optional.empty();
        }
        Object info = maybe.get();
        Object schematic = schematicName.invoke(info);
        return Optional.of(new Lot(
                String.valueOf(contextFamily.invoke(info)),
                String.valueOf(contextClass.invoke(info)),
                String.valueOf(lotStyle.invoke(info)),
                String.valueOf(lotClass.invoke(info)),
                ((Number) naturePercent.invoke(info)).doubleValue(),
                schematic == null ? null : String.valueOf(schematic)));
    }

    /**
     * A place, as the generator planned it.
     *
     * @param context   district family — {@code HIGHRISE}, {@code FARM}, {@code NATURE}, …
     * @param contextClass the district context's class name, e.g. {@code MidriseContext}
     * @param style     lot style — {@code NATURE}, {@code STRUCTURE}, {@code ROAD}, {@code ROUNDABOUT}
     * @param lotClass  the lot's class name, e.g. {@code StoreBuildingLot}
     * @param nature    0.0 for dense city, 1.0 for wilderness
     * @param schematic the placed schematic's name, or null
     */
    public record Lot(String context, String contextClass, String style, String lotClass,
            double nature, String schematic) {}
}
