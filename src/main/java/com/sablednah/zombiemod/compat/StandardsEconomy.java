package com.sablednah.zombiemod.compat;

import java.lang.reflect.Method;
import java.util.UUID;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.neoforge.Bounties;

import org.slf4j.Logger;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * Pays bounties into SableCraft Standards' economy, if Standards is here at all.
 *
 * <p>This is the answer to the question {@link Bounties} was deliberately left open: there is no
 * Vault on NeoForge, so ZombieMod never picked an economy. Standards ships a ledger behind an
 * interface precisely so it does not have to be the one holding the money — a dedicated economy mod
 * registers a higher-priority provider and takes over, and neither side needs to know. So paying
 * through Standards is not choosing Standards' ledger; it is choosing whichever ledger won.
 *
 * <p>Reflection rather than a compile-time dependency, for the same reason as {@link FtbChunks}: no
 * new Maven repository, no pinned version, and no way for an optional integration to break the build
 * of the thing it is optional to. The surface is three calls.
 *
 * <p><b>Availability is checked at pay time, not at registration.</b> Standards registers its own
 * provider during its setup, and two mods' setup listeners have no ordering guarantee — asking
 * "is there an economy?" while wiring ourselves up could get "no" from a Standards that simply had
 * not got there yet. Asking when a zombie actually dies cannot be early.
 *
 * <p>Everything fails safe: absent mod, changed API, or any thrown exception returns false, which
 * means "not paid by me" and leaves the scoreboard tally to do its job. A bounty is never lost to a
 * broken integration, only to a missing one.
 */
public final class StandardsEconomy {

    private static final Logger LOG = LogUtils.getLogger();

    /** The reason string every deposit carries; the only thing that can ever answer "from where?". */
    private static final String REASON = "zombiemod:bounty";

    private static boolean checked;
    private static boolean available;
    private static boolean warned;

    private static Method isAvailable;
    private static Method deposit;
    private static Method success;

    private StandardsEconomy() {}

    /**
     * Registers a bounty payer if Standards is installed. Additive by design — the scoreboard tally
     * carries on regardless, because a server may reasonably want the money and the count.
     */
    public static void install() {
        if (!ModList.get().isLoaded("standards")) {
            return;
        }
        Bounties.register(StandardsEconomy::pay);
        LOG.info("ZombieMod: Standards detected - bounties will be paid into its economy.");
    }

    /** Whether Standards is installed at all, for {@code /zombiemod status}. */
    public static boolean present() {
        return ModList.get().isLoaded("standards");
    }

    private static synchronized boolean link() {
        if (!checked) {
            checked = true;
            try {
                Class<?> economy = Class.forName("com.sablednah.standards.api.economy.Economy");
                Class<?> result = Class.forName("com.sablednah.standards.api.economy.TransactionResult");
                isAvailable = economy.getMethod("isAvailable");
                deposit = economy.getMethod("deposit", UUID.class, double.class, String.class);
                success = result.getMethod("success");
                available = true;
            } catch (ReflectiveOperationException e) {
                LOG.warn("ZombieMod: Standards is present but its economy API did not match what was "
                        + "expected; bounties will not be paid into it. {}", e.toString());
                available = false;
            }
        }
        return available;
    }

    private static boolean pay(ServerPlayer player, double amount) {
        if (!link()) {
            return false;
        }
        try {
            // Asked every time rather than cached: a server can gain an economy provider after
            // startup, and "no economy right now" is an ordinary answer rather than an error.
            if (!((Boolean) isAvailable.invoke(null))) {
                return false;
            }
            Object outcome = deposit.invoke(null, player.getUUID(), amount, REASON);
            return outcome != null && (Boolean) success.invoke(outcome);
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                LOG.warn("ZombieMod: paying a bounty through Standards failed; falling back to "
                        + "whatever else is registered.", e);
            }
            return false;
        }
    }
}
