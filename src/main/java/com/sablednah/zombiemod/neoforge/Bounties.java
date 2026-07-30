package com.sablednah.zombiemod.neoforge;

import java.util.ArrayList;
import java.util.List;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.ZombieModConfig;

import org.slf4j.Logger;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

/**
 * Pays out what a genus was worth.
 *
 * <p>Deliberately split from the question of <em>what money is</em>. There is no Vault on NeoForge —
 * no abstraction that every economy mod implements — so tying this to any one of them would pick a
 * faction on the server owner's behalf. Instead a genus carries a number, and whoever knows how to
 * pay it registers themselves here.
 *
 * <p>Which leaves the case of no economy mod at all, and the honest answer there is the scoreboard.
 * It's vanilla, every server already has it, commands and datapacks can read it, and display slots
 * make it visible with one command. A bounty that lands in a scoreboard objective is a real reward
 * on a server with nothing installed, rather than a number in a JSON file waiting for a dependency.
 *
 * <p>Payers are additive rather than exclusive: an Impactor adapter registering itself does not stop
 * the scoreboard tally, because a server may reasonably want both the money and the count.
 */
public final class Bounties {

    private static final Logger LOG = LogUtils.getLogger();

    /** Something that knows how to give a player money. */
    @FunctionalInterface
    public interface Payer {
        /**
         * @return true if this payer handled it, purely so nothing is silently dropped everywhere
         */
        boolean pay(ServerPlayer player, double amount);
    }

    private static final List<Payer> PAYERS = new ArrayList<>();

    private Bounties() {}

    /** For an economy adapter to call during mod construction. */
    public static void register(Payer payer) {
        PAYERS.add(payer);
        LOG.info("ZombieMod: bounty payer registered ({} total)", PAYERS.size());
    }

    static void award(ServerLevel level, ServerPlayer player, double amount, Component what) {
        if (amount <= 0.0D || !ZombieModConfig.BOUNTY.get()) {
            return;
        }

        boolean paid = false;
        for (Payer payer : PAYERS) {
            try {
                paid |= payer.pay(player, amount);
            } catch (RuntimeException e) {
                LOG.warn("ZombieMod: a bounty payer threw; the kill still counted.", e);
            }
        }
        paid |= toScoreboard(level, player, amount);

        if (paid && ZombieModConfig.BOUNTY_ANNOUNCE.get()) {
            player.displayClientMessage(Component.literal(
                    String.format("§6+%s §7bounty §r(%s)", trim(amount), what.getString())), true);
        }
    }

    /**
     * The built-in payer. Only tallies into an objective that already exists, so a server opts in
     * with one command rather than finding an objective it never asked for:
     * {@code /scoreboard objectives add zombiemod.bounty dummy}
     */
    private static boolean toScoreboard(ServerLevel level, ServerPlayer player, double amount) {
        String name = ZombieModConfig.BOUNTY_OBJECTIVE.get();
        if (name.isBlank()) {
            return false;
        }
        Scoreboard scoreboard = level.getScoreboard();
        Objective objective = scoreboard.getObjective(name);
        if (objective == null) {
            return false;
        }
        // Scores are integers, so fractional bounties accumulate as their floor. Documented rather
        // than rounded up: paying more than the genus is worth is the worse error.
        scoreboard.getOrCreatePlayerScore(player, objective).add((int) amount);
        return true;
    }

    private static String trim(double amount) {
        return amount == Math.floor(amount) ? String.valueOf((long) amount) : String.valueOf(amount);
    }
}
