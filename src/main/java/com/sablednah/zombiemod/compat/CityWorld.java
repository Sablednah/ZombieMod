package com.sablednah.zombiemod.compat;

import java.lang.reflect.Method;
import java.util.Optional;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.core.BlockPos;
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
 */
public final class CityWorld {

    private static final Logger LOG = LogUtils.getLogger();

    private static boolean checked;
    private static boolean available;
    private static boolean warned;

    private static Method lotAt;
    private static Method contextFamily;
    private static Method contextClass;
    private static Method lotStyle;
    private static Method lotClass;
    private static Method naturePercent;
    private static Method schematicName;

    private CityWorld() {}

    /** True if CityWorld is present and its API answered as expected. */
    public static synchronized boolean available() {
        if (!checked) {
            checked = true;
            available = link();
            if (available) {
                LOG.info("ZombieMod: CityWorld detected - district-aware spawning available.");
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
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("ZombieMod: CityWorld is installed but its API did not match - "
                    + "district conditions will never match. ({})", e.toString());
            return false;
        }
    }

    /**
     * What CityWorld planned for the chunk containing {@code pos}.
     *
     * <p>Empty for a non-CityWorld level, which is the common case and not an error.
     */
    public static Optional<Lot> lotAt(Level level, BlockPos pos) {
        if (!available() || !(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        try {
            Object result = lotAt.invoke(null, server, pos);
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
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                LOG.warn("ZombieMod: CityWorld lookup failed; district conditions will not match", e);
            }
            return Optional.empty();
        }
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
