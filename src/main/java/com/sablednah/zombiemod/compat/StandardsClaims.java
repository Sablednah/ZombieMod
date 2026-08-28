package com.sablednah.zombiemod.compat;

import java.lang.reflect.Method;
import java.util.Optional;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/**
 * Asks SableCraft Standards whether land is claimed, if Standards is here at all.
 *
 * <p>Standards is a <em>seam</em> rather than a land model: a faction mod or an FTB Chunks bridge
 * supplies the claims and Standards answers the question. So this one adapter covers whatever the
 * server actually runs, which is the point of asking it rather than asking FTB directly.
 *
 * <p><b>Why not {@code mayModify}.</b> That is the call its documentation tells consumers to prefer,
 * and it is right for a player: it folds in membership, trust lists, faction relations and admin
 * bypass. None of those mean anything for a Breaker chewing through a wall. There is no player to
 * pass, so the question a mob can actually ask is the positional one — <em>is this land claimed by
 * anyone</em> — and "claimed" is treated as "do not grief here".
 *
 * <p><b>{@code griefAllowed(level, pos)} is preferred where the installed Standards has it</b> — a
 * positional "may a non-player modify blocks here", which lets a land mod say mobs may wreck a war
 * zone but not a home claim, instead of this mod deciding that on its behalf. Raised with the
 * Standards session on 2026-08-28 and shipped the same day; its default is
 * {@code owner(chunk).isEmpty()}, so it agrees exactly with the {@code owner()} fallback below and
 * adopting it changed no behaviour anywhere.
 *
 * <p><b>It fails closed, and that is deliberately unlike the rest of {@code Claims}.</b> A claims
 * provider that throws makes {@code griefAllowed} answer {@code false} — no griefing — where
 * everywhere else in that class a broken provider permits. Standards' reasoning: wrongly permitting
 * a *build* can be undone, wrongly permitting a *mob* means somebody's base is eaten while nobody is
 * watching, and that cannot. <b>So the tell for a broken claims provider is zombies that mysteriously
 * refuse to break anything</b> — look for a provider throwing in the server log before suspecting
 * ZombieMod. This adapter respects that: if the call itself fails it drops to {@code owner()} rather
 * than permitting.
 *
 * <p>Reflection rather than a compile-time dependency, as with {@link FtbChunks} and
 * {@link StandardsEconomy}. It also gets the version check free: LegendQuest found that
 * {@code ModList.isLoaded} is not enough, because Standards can be present but older than the API
 * you built against, and calling a method that is not there kills the server. A missing method here
 * is a failed lookup and a disabled integration.
 */
public final class StandardsClaims {

    private static final Logger LOG = LogUtils.getLogger();

    private static boolean checked;
    private static boolean available;
    private static boolean warned;

    private static Method isAvailable;
    private static Method owner;
    private static Method griefAllowed;   // optional; null if this Standards has no such method

    private StandardsClaims() {}

    public static synchronized boolean available() {
        if (!checked) {
            checked = true;
            available = link();
            if (available) {
                LOG.info("ZombieMod: Standards claims detected{}.",
                        griefAllowed != null ? " (with griefAllowed)" : "");
            }
        }
        return available;
    }

    private static boolean link() {
        if (!ModList.get().isLoaded("standards")) {
            return false;
        }
        try {
            Class<?> claims = Class.forName("com.sablednah.standards.api.groups.Claims");
            isAvailable = claims.getMethod("isAvailable");
            owner = claims.getMethod("owner",
                    net.minecraft.server.level.ServerLevel.class, ChunkPos.class);
            try {
                griefAllowed = claims.getMethod("griefAllowed",
                        net.minecraft.server.level.ServerLevel.class, BlockPos.class);
            } catch (NoSuchMethodException absent) {
                griefAllowed = null;   // fine: fall back to owner()
            }
            return true;
        } catch (ReflectiveOperationException e) {
            LOG.warn("ZombieMod: Standards is present but its claims API did not match what was "
                    + "expected; its claims will be ignored. {}", e.toString());
            return false;
        }
    }

    /**
     * @return whether this position is inside a claim. False whenever the answer cannot be had, so a
     *         missing or changed Standards never blocks anything — the same fail-open rule the rest
     *         of {@code compat} follows.
     */
    public static boolean isClaimed(Level level, BlockPos pos) {
        if (!available() || !(level instanceof net.minecraft.server.level.ServerLevel server)) {
            return false;
        }
        try {
            if (!((Boolean) isAvailable.invoke(null))) {
                return false;   // Standards installed, but nothing supplying claims
            }
            if (griefAllowed != null) {
                try {
                    return !((Boolean) griefAllowed.invoke(null, server, pos));
                } catch (ReflectiveOperationException | RuntimeException e) {
                    // Fall through to owner(). Not a swallow: griefAllowed already fails closed on
                    // a provider that throws, so reaching here means the *call* broke rather than
                    // the claims model, and the older question still has a correct answer.
                    if (!warned) {
                        warned = true;
                        LOG.warn("ZombieMod: Standards griefAllowed failed; falling back to the "
                                + "claim-owner check.", e);
                    }
                }
            }
            // ChunkPos(BlockPos) is gone from 26.1 - ChunkPos became a record. The (int, int)
            // constructor exists on every supported version, so shift rather than seam it.
            ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
            return ((Optional<?>) owner.invoke(null, server, chunk)).isPresent();
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                LOG.warn("ZombieMod: a Standards claim lookup failed; treating everywhere as "
                        + "unclaimed.", e);
            }
            return false;
        }
    }
}
