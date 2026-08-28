package com.sablednah.zombiemod.compat;

import java.lang.reflect.Method;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * Puts a player in combat through SableCraft Standards, if Standards is here at all.
 *
 * <p>Used for one thing: a server may decide that <b>being blinded counts as being in combat</b>, so
 * a player cannot blink out of a fight they can no longer see. Standards owns what "in combat" means
 * and what it costs — teleport blocking, logout rules — so this only reports the fact.
 *
 * <p><b>{@code SKILL} is the right kind</b>, by Standards' own description: "another mod says this
 * was an act of combat… acts of war with no damage event to notice." Blindness lands exactly there —
 * a Jack breathing Darkness over you does no damage at all, so nothing else on the server has any
 * reason to think a fight is happening.
 *
 * <p>Reflective, like the rest of {@code compat}, which also means an older Standards without the
 * combat API simply fails the lookup and the feature stays off. LegendQuest learned that
 * {@code ModList.isLoaded} alone is not enough — Standards can be present but predate the API — and
 * that calling the missing method kills the server.
 */
public final class StandardsCombat {

    private static final Logger LOG = LogUtils.getLogger();

    private static boolean checked;
    private static boolean available;
    private static boolean warned;

    private static Method tag;
    private static Object skillKind;

    private StandardsCombat() {}

    public static synchronized boolean available() {
        if (!checked) {
            checked = true;
            available = link();
            if (available) {
                LOG.info("ZombieMod: Standards combat detected - blindness can flag combat.");
            }
        }
        return available;
    }

    private static boolean link() {
        if (!ModList.get().isLoaded("standards")) {
            return false;
        }
        try {
            Class<?> combat = Class.forName("com.sablednah.standards.api.combat.Combat");
            Class<?> kind = Class.forName("com.sablednah.standards.api.combat.CombatKind");
            tag = combat.getMethod("tag", ServerPlayer.class, kind, String.class);
            skillKind = Enum.valueOf(kind.asSubclass(Enum.class), "SKILL");
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("ZombieMod: Standards is present but its combat API did not match what was "
                    + "expected; blindness will not flag combat. {}", e.toString());
            return false;
        }
    }

    /**
     * Report that this player is in a fight. Silent and harmless if Standards is absent.
     *
     * @param why a reason string Standards keeps, so "why am I stuck in combat" has an answer
     */
    public static void flag(ServerPlayer player, String why) {
        if (!available()) {
            return;
        }
        try {
            tag.invoke(null, player, skillKind, why);
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                LOG.warn("ZombieMod: flagging combat through Standards failed; blindness will not "
                        + "count as combat.", e);
            }
        }
    }
}
