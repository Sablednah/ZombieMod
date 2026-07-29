package com.sablednah.zombiemod.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/**
 * Asks FTB Chunks whether a position is inside a claim, if FTB Chunks is here at all.
 *
 * <p>Reflection rather than a compile-time dependency, deliberately. The surface needed is three
 * methods and one constructor, verified against the installed jar, and doing it this way means no new
 * Maven repository, no pinned FTB version, and no possibility of ZombieMod failing to build because
 * someone else's repository moved. Optional integrations should not be able to break the thing they
 * are optional to.
 *
 * <p>Everything fails safe: absent mod, changed API, unloaded manager or any thrown exception all
 * answer "not claimed", so the worst outcome is that protection quietly does nothing rather than
 * zombies quietly stopping working. The first failure is logged once, because silent is not the same
 * as invisible.
 *
 * <p>Worth knowing why this exists: FTB Chunks protects explosions in claimed chunks but does not
 * cover general mob block-breaking — there is an open feature request for Wither protection for
 * exactly that reason. So our breakers ask NeoForge's griefing hook, FTB declines to answer, and the
 * claim does nothing. This closes that gap from our side.
 */
public final class FtbChunks {

    private static final Logger LOG = LogUtils.getLogger();

    private static boolean checked;
    private static boolean available;
    private static boolean warned;

    private static Method apiMethod;
    private static Method isManagerLoaded;
    private static Method getManager;
    private static Method getChunk;
    private static Constructor<?> chunkDimPos;

    private FtbChunks() {}

    /** True if FTB Chunks is present and its API answered as expected. */
    public static synchronized boolean available() {
        if (!checked) {
            checked = true;
            available = link();
            if (available) {
                LOG.info("ZombieMod: FTB Chunks detected - claim protection available.");
            }
        }
        return available;
    }

    private static boolean link() {
        if (!ModList.get().isLoaded("ftbchunks")) {
            return false;
        }
        try {
            Class<?> api = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
            Class<?> apiIface = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI$API");
            Class<?> manager = Class.forName("dev.ftb.mods.ftbchunks.api.ClaimedChunkManager");
            Class<?> pos = Class.forName("dev.ftb.mods.ftblibrary.math.ChunkDimPos");

            apiMethod = api.getMethod("api");
            isManagerLoaded = apiIface.getMethod("isManagerLoaded");
            getManager = apiIface.getMethod("getManager");
            getChunk = manager.getMethod("getChunk", pos);
            chunkDimPos = pos.getConstructor(Level.class, BlockPos.class);
            return true;
        } catch (ReflectiveOperationException e) {
            LOG.warn("ZombieMod: FTB Chunks is present but its API did not match what was expected; "
                    + "claim protection is off. {}", e.toString());
            return false;
        }
    }

    /**
     * @return whether this position is inside a claimed chunk. False whenever the answer cannot be
     *         obtained, so a missing or changed FTB never blocks anything.
     */
    public static boolean isClaimed(Level level, BlockPos pos) {
        if (!available()) {
            return false;
        }
        try {
            Object api = apiMethod.invoke(null);
            if (api == null || !((Boolean) isManagerLoaded.invoke(api))) {
                return false;
            }
            Object manager = getManager.invoke(api);
            Object cdp = chunkDimPos.newInstance(level, pos);
            return getChunk.invoke(manager, cdp) != null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                LOG.warn("ZombieMod: FTB Chunks claim lookup failed; treating everywhere as unclaimed.", e);
            }
            return false;
        }
    }
}
