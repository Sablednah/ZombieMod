package com.sablednah.zombiemod.compat;

import java.lang.reflect.Method;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;

/**
 * Whether a player is vanished, through SableCraft Standards, if Standards is here at all.
 *
 * <p><b>Invisible is not the same as absent, and the gap is what gives a vanish away.</b> Standards
 * hides a vanished player from other clients, but the player is still a real entity on the server —
 * so our mobs saw one, walked to it, and a Boomer detonated beside nobody. A crater with no cause
 * locates hidden staff as precisely as seeing them would.
 *
 * <p>Standards is explicit that it cannot fix this from its side: it answers the question it owns —
 * "is this player hidden" — and each mod acts on it for the things that mod is responsible for.
 * Hiding entities near a vanished player would catch other people's pets and dropped items and
 * still miss anything tracking them from further off. Which mobs are reacting <em>to a player</em>
 * is a question only we can answer.
 *
 * <p><b>Ask {@link #anyVanished()} first on anything per-tick.</b> On Standards' side that is a
 * single field read, and it is false on virtually every server, so the per-player check never
 * happens. {@code AbilityGoal} consults this on every tick of every ability-carrying mob in the
 * world, which is exactly the shape that punishes a lazy lookup.
 *
 * <p>Reflective, like the rest of {@code compat}, and inert without Standards — which also covers
 * an older Standards predating the vanish API: the lookup fails, the feature stays off, and nothing
 * throws. {@code ModList.isLoaded} alone is not enough, as LegendQuest found the hard way.
 * Signatures verified against the shipped {@code standards-1.8.0} jar.
 */
public final class StandardsVanish {

    private static final Logger LOG = LogUtils.getLogger();

    private static boolean checked;
    private static boolean available;
    private static boolean warned;

    private static Method isVanished;
    private static Method anyVanished;

    private StandardsVanish() {}

    public static synchronized boolean available() {
        if (!checked) {
            checked = true;
            available = link();
            if (available) {
                LOG.info("ZombieMod: Standards vanish detected - mobs will ignore vanished players.");
            }
        }
        return available;
    }

    private static boolean link() {
        if (!ModList.get().isLoaded("standards")) {
            return false;
        }
        try {
            Class<?> vanish = Class.forName("com.sablednah.standards.api.vanish.Vanish");
            isVanished = vanish.getMethod("isVanished", ServerPlayer.class);
            anyVanished = vanish.getMethod("anyVanished");
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("ZombieMod: Standards is present but its vanish API did not match what was "
                    + "expected; mobs will treat vanished players as ordinary. {}", e.toString());
            return false;
        }
    }

    /**
     * Whether anybody at all is vanished. The cheap question — ask it before asking about a
     * specific player, and skip the work entirely when the answer is no.
     */
    public static boolean anyVanished() {
        if (!available()) {
            return false;
        }
        try {
            return (boolean) anyVanished.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return fail(e);
        }
    }

    /**
     * Whether this entity is a player who is currently hidden.
     *
     * <p>Takes an {@link Entity} rather than a player because every caller here is holding
     * something that <em>might</em> be a player — a mob's target, a member of an area sweep — and
     * making each one test that first would be the same line written nine times, which is the shape
     * a tenth caller forgets. Anything that is not a server player is not vanished.
     */
    public static boolean isVanished(Entity entity) {
        if (!(entity instanceof ServerPlayer player) || !available()) {
            return false;
        }
        try {
            return (boolean) isVanished.invoke(null, player);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return fail(e);
        }
    }

    /**
     * Answer "not vanished" and say so once. Failing open is the right way round: treating a
     * visible player as hidden would make mobs ignore everybody on a server where something is
     * wrong, which is a far worse bug than the one this fixes.
     */
    private static boolean fail(Exception e) {
        if (!warned) {
            warned = true;
            LOG.warn("ZombieMod: asking Standards about vanish failed; vanished players will be "
                    + "treated as ordinary from here on.", e);
        }
        return false;
    }
}
