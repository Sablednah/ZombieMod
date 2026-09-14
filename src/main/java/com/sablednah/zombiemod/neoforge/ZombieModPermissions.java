package com.sablednah.zombiemod.neoforge;

import java.util.function.Predicate;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.ZombieMod;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.slf4j.Logger;

/**
 * ZombieMod's permission nodes, through NeoForge's own {@link PermissionAPI}.
 *
 * <p>This is deliberately <em>not</em> a {@code compat/} integration. LuckPerms and SableCraft
 * Standards are both <em>handlers</em> for the same NeoForge API; ZombieMod registers its nodes on
 * {@link PermissionGatherEvent.Nodes} and whichever handler the server owner selected answers for
 * them. Nobody calls anybody, so there is nothing to guard and no dependency to make optional.
 *
 * <p><b>Every default reproduces the permission level the command had before nodes existed</b>, so
 * a server with no permission manager - or one that installs a manager and grants nothing - behaves
 * exactly as {@code NODES.md} describes. NeoForge's default handler answers every query with the
 * node's own default resolver, which here is "does this player hold op level 2" (3 for config). The
 * nodes only <em>add</em> a way to hand one branch of the tree to somebody who is not an op: a
 * storyteller who should be able to call a horde without also being handed {@code /stop}.
 *
 * <p>Six nodes rather than one per command, because the useful split is by what a server owner
 * delegates, not by what a command is called: spawning things and running hordes are what a game
 * master does; the corpse ledger and observer mode are staff tools; {@code config} changes what the
 * server does for everyone. Wildcards ({@code zombiemod.*}) are the manager's business - both
 * LuckPerms and Standards expand them - so there is no master node here to keep in step.
 *
 * <p>Booleans only. Standards passes typed nodes through to their own resolver on purpose, and
 * nothing here needs a quantity.
 */
public final class ZombieModPermissions {

    private static final Logger LOG = LogUtils.getLogger();

    private ZombieModPermissions() {}

    /** {@code /zombiemod spawn}. Default: op level 2. */
    public static final PermissionNode<Boolean> SPAWN = node("spawn", Commands.LEVEL_GAMEMASTERS);

    /** {@code /zombiemod horde list|start|stop}. Default: op level 2. */
    public static final PermissionNode<Boolean> HORDE = node("horde", Commands.LEVEL_GAMEMASTERS);

    /** {@code /zombiemod corpse ...} - the whole ledger. Default: op level 2. */
    public static final PermissionNode<Boolean> CORPSE = node("corpse", Commands.LEVEL_GAMEMASTERS);

    /**
     * Turning observer mode <em>on</em>, for yourself or anybody, and off for somebody else.
     * Never {@code observe off} on yourself, which is open to everyone and stays that way - see the
     * comment on the {@code observe} literal in {@link ZombieModCommands}. Default: op level 2.
     */
    public static final PermissionNode<Boolean> OBSERVE = node("observe", Commands.LEVEL_GAMEMASTERS);

    /** {@code /zombiemod status}. Default: op level 2. */
    public static final PermissionNode<Boolean> STATUS = node("status", Commands.LEVEL_GAMEMASTERS);

    /** {@code /zombiemod config}. Default: op level <b>3</b>, a step above the rest of the tree. */
    public static final PermissionNode<Boolean> CONFIG = node("config", Commands.LEVEL_ADMINS);

    /** Registered from the game bus in {@link ZombieMod}. */
    public static void onGatherNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(SPAWN, HORDE, CORPSE, OBSERVE, STATUS, CONFIG);
        LOG.info("ZombieMod: registered 6 permission nodes (zombiemod.*)");
    }

    /**
     * Does this player hold the node, as the active permission handler sees it? With no manager
     * installed that is the node's default: the op level the command always needed.
     */
    public static boolean has(ServerPlayer player, PermissionNode<Boolean> node) {
        return PermissionAPI.getPermission(player, node);
    }

    /**
     * The {@code requires()} predicate for a command branch.
     *
     * <p>A player is asked through the permission handler. Anything else - the console, a command
     * block, an {@code /execute} with no player behind it - has no identity a manager could grant
     * to, so it keeps passing on the level it always did. That is what keeps the console and
     * command-block rows of {@code NODES.md} true after nodes exist.
     */
    public static Predicate<CommandSourceStack> gate(PermissionNode<Boolean> node) {
        return source -> source.getEntity() instanceof ServerPlayer player
                ? has(player, node)
                : Commands.hasPermission(fallback(node)).test(source);
    }

    private static PermissionNode<Boolean> node(String name, PermissionCheck level) {
        return new PermissionNode<>(ZombieMod.MOD_ID, name, PermissionTypes.BOOLEAN,
                (player, uuid, context) -> player != null && level.check(player.permissions()));
    }

    /** The level a node falls back to for a source that is not a player. */
    private static PermissionCheck fallback(PermissionNode<Boolean> node) {
        return node == CONFIG ? Commands.LEVEL_ADMINS : Commands.LEVEL_GAMEMASTERS;
    }
}
