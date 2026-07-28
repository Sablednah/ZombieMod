package com.sablednah.zombiemod.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.sablednah.zombiemod.ZombieModConfig;
import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.core.Genus;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /zombiemod} — enough to exercise the mod by hand.
 *
 * <ul>
 *   <li>{@code /zombiemod list} — what genera the current datapacks define
 *   <li>{@code /zombiemod spawn <genus>} — put one where you're looking
 *   <li>{@code /zombiemod spawn <genus> <x> <y> <z>} — put one at a position
 *   <li>{@code /zombiemod observe [on|off]} — take no damage, stay a target
 *   <li>{@code /zombiemod corpse list|give|respawn|forget} — player-zombie recovery
 * </ul>
 *
 * There is no {@code reload}: genera are datapack data, so vanilla {@code /reload} already does it.
 */
public final class ZombieModCommands {

    /** How far to trace when no position is given. Matches vanilla's own generous reach for /tp-alikes. */
    private static final double LOOK_DISTANCE = 48.0D;

    private static final DynamicCommandExceptionType ERROR_UNKNOWN_GENUS = new DynamicCommandExceptionType(
            genus -> Component.literal("No ZombieMod genus '" + genus + "'. Try /zombiemod list."));

    private static final DynamicCommandExceptionType ERROR_NO_CORPSE = new DynamicCommandExceptionType(
            player -> Component.literal("No such corpse for " + player + ". Try /zombiemod corpse list."));

    private static final DynamicCommandExceptionType ERROR_AMBIGUOUS_GENUS = new DynamicCommandExceptionType(
            matches -> Component.literal("Ambiguous genus name - matches " + matches + ". Use the full id."));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("zombiemod")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

        root.then(Commands.literal("list").executes(ctx -> {
            HolderLookup.RegistryLookup<Genus> lookup = lookup(ctx.getSource());
            var ids = lookup.listElementIds().map(k -> k.identifier().toString()).sorted().toList();
            if (ids.isEmpty()) {
                ctx.getSource().sendSuccess(() -> Component.literal("No genera loaded."), false);
            } else {
                ctx.getSource().sendSuccess(
                        () -> Component.literal(ids.size() + " genera: " + String.join(", ", ids)), false);
            }
            return ids.size();
        }));

        // ResourceKeyArgument, not StringArgumentType: Brigadier's unquoted string stops at a
        // colon, so `zombiemod:coward` parsed as `zombiemod` plus trailing junk. This is the
        // argument type vanilla uses for datapack ids, and its suggestions come from the registry.
        root.then(Commands.literal("spawn")
                .then(Commands.argument("genus", ResourceKeyArgument.key(ZombieModRegistries.GENUS))
                        // Replaces the argument type's own suggestions so the bare name is offered
                        // alongside the full id.
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(names(ctx.getSource()), builder))
                        // No position: spawn where the player is looking.
                        .executes(ctx -> spawn(ctx.getSource(),
                                ResourceKeyArgument.getRegistryKey(
                                        ctx, "genus", ZombieModRegistries.GENUS, ERROR_UNKNOWN_GENUS),
                                lookingAt(ctx.getSource())))
                        // Explicit position. Vec3Argument gives ~ ~ ~ and ^ ^ ^5 for free, so
                        // "5 blocks in front of me" is `^ ^ ^5` with no work on our part - and it
                        // makes the command usable from the console and from command blocks.
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(ctx -> spawn(ctx.getSource(),
                                        ResourceKeyArgument.getRegistryKey(
                                                ctx, "genus", ZombieModRegistries.GENUS, ERROR_UNKNOWN_GENUS),
                                        Vec3Argument.getVec3(ctx, "pos"))))));

        // Corpse recovery. Op-only like the rest, and worth having even if player zombies are
        // working perfectly: "my corpse went missing" was the single most common complaint about
        // the 1.8 version, and an admin with no way to check had to guess.
        root.then(Commands.literal("corpse")
                .then(Commands.literal("list")
                        .executes(ctx -> listCorpses(ctx.getSource(), Optional.empty()))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> listCorpses(ctx.getSource(),
                                        Optional.of(StringArgumentType.getString(ctx, "player"))))))
                .then(Commands.literal("give")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> giveCorpse(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"), 1))
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .executes(ctx -> giveCorpse(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "index"))))))
                .then(Commands.literal("respawn")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> respawnCorpse(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"), 1))))
                .then(Commands.literal("forget")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> forgetCorpse(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"), 1)))));

        root.then(Commands.literal("observe")
                .executes(ctx -> setObserve(ctx.getSource(), !ObserverMode.isOn(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("on").executes(ctx -> setObserve(ctx.getSource(), true)))
                .then(Commands.literal("off").executes(ctx -> setObserve(ctx.getSource(), false))));

        dispatcher.register(root);
    }

    // ================================================================= corpse recovery

    private static List<CorpseLedger.Entry> corpses(CommandSourceStack source, Optional<String> player) {
        return CorpseLedger.get(source.getLevel()).find(player, false);
    }

    private static int listCorpses(CommandSourceStack source, Optional<String> player) {
        List<CorpseLedger.Entry> found = corpses(source, player);
        if (found.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No corpses recorded."), false);
            return 0;
        }
        for (int i = 0; i < found.size() && i < 20; i++) {
            CorpseLedger.Entry e = found.get(i);
            int n = i + 1;
            source.sendSuccess(() -> Component.literal(String.format(
                    "#%d %s - %d %d %d in %s, day %d, %d item stacks%s",
                    n, e.playerName(), e.x(), e.y(), e.z(), e.dimension(), e.day(), e.items().size(),
                    e.claimed() ? " (already recovered)" : "")), false);
        }
        return found.size();
    }

    /** Index is 1-based and newest-first, matching what `list` just printed. */
    private static CorpseLedger.Entry pick(CommandSourceStack source, String player, int index)
            throws CommandSyntaxException {
        List<CorpseLedger.Entry> found = corpses(source, Optional.of(player));
        if (index < 1 || index > found.size()) {
            throw ERROR_NO_CORPSE.create(player);
        }
        return found.get(index - 1);
    }

    private static int giveCorpse(CommandSourceStack source, String player, int index)
            throws CommandSyntaxException {
        CorpseLedger.Entry entry = pick(source, player, index);
        if (entry.items().isEmpty()) {
            source.sendFailure(Component.literal("That corpse was not carrying anything."));
            return 0;
        }

        // Hand straight to the owner when they're online; otherwise drop at the admin's feet so
        // the items exist somewhere rather than being quietly consumed by a failed lookup.
        ServerPlayer owner = source.getServer().getPlayerList().getPlayerByName(entry.playerName());
        ServerPlayer target = owner != null ? owner : source.getPlayerOrException();

        for (ItemStack stack : entry.items()) {
            ItemStack copy = stack.copy();
            if (!target.getInventory().add(copy)) {
                target.drop(copy, false);
            }
        }
        CorpseLedger.get(source.getLevel()).claim(entry.id());

        String where = owner != null ? entry.playerName() : source.getTextName() + " (owner offline)";
        source.sendSuccess(() -> Component.literal(
                "Returned " + entry.items().size() + " stacks to " + where + "."), true);
        if (owner != null) {
            owner.displayClientMessage(Component.literal("\u00a7eYour corpse's belongings have been returned."), false);
        }
        return 1;
    }

    private static int respawnCorpse(CommandSourceStack source, String player, int index)
            throws CommandSyntaxException {
        CorpseLedger.Entry entry = pick(source, player, index);
        ServerLevel level = source.getLevel();

        var genusId = Identifier.tryParse(ZombieModConfig.PLAYER_ZOMBIE_GENUS.get());
        var holder = genusId == null ? Optional.<Holder.Reference<Genus>>empty()
                : lookup(source).get(ResourceKey.create(ZombieModRegistries.GENUS, genusId));
        if (holder.isEmpty()) {
            source.sendFailure(Component.literal("Corpse genus not loaded; cannot rebuild it."));
            return 0;
        }

        Entity created = holder.get().value().base().create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Mob corpse)) {
            return 0;
        }
        corpse.snapTo(entry.x() + 0.5D, entry.y() + 0.5D, entry.z() + 0.5D, 0.0F, 0.0F);
        GenusApplier.assign(corpse, holder.get());
        corpse.setCustomName(Component.literal(
                ZombieModConfig.PLAYER_ZOMBIE_NAME.get().replace("%P", entry.playerName())));
        corpse.setCustomNameVisible(true);
        PlayerZombies.rebuild(level, corpse, entry);
        level.addFreshEntity(corpse);

        source.sendSuccess(() -> Component.literal(String.format(
                "Rebuilt %s's corpse at %d %d %d with %d stacks.",
                entry.playerName(), entry.x(), entry.y(), entry.z(), entry.items().size())), true);
        return 1;
    }

    private static int forgetCorpse(CommandSourceStack source, String player, int index)
            throws CommandSyntaxException {
        CorpseLedger.Entry entry = pick(source, player, index);
        CorpseLedger.get(source.getLevel()).forget(entry.id());
        source.sendSuccess(() -> Component.literal("Removed that corpse from the ledger."), true);
        return 1;
    }

    /**
     * Take no damage without becoming invisible to mobs.
     *
     * <p>The reason this exists rather than pointing at {@code /gamemode creative} or another mod's
     * god mode: both make mobs stop targeting you, and watching what zombies do is the entire point
     * of testing this mod.
     */
    private static int setObserve(CommandSourceStack source, boolean on) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ObserverMode.set(player, on);
        source.sendSuccess(() -> Component.literal(on
                ? "Observer mode ON - mobs still hunt you, nothing hurts you."
                : "Observer mode OFF."), false);
        return 1;
    }

    /** Full ids plus bare names, so both {@code coward} and {@code zombiemod:coward} tab-complete. */
    private static List<String> names(CommandSourceStack source) {
        List<String> out = new ArrayList<>();
        lookup(source).listElementIds().forEach(key -> {
            out.add(key.identifier().toString());
            out.add(key.identifier().getPath());
        });
        return out;
    }

    /**
     * Resolve what the player typed to a genus, accepting the bare name.
     *
     * <p>An identifier with no namespace reads as {@code minecraft:<path>}, so that is the signal
     * that the namespace was omitted — in which case we match on path across every namespace, and a
     * genus from someone else's datapack is reachable by its short name too. An exact hit always
     * wins first, and two datapacks using the same short name is reported rather than guessed at.
     */
    private static Holder.Reference<Genus> resolve(CommandSourceStack source, ResourceKey<Genus> key)
            throws CommandSyntaxException {
        HolderLookup.RegistryLookup<Genus> lookup = lookup(source);

        Optional<Holder.Reference<Genus>> exact = lookup.get(key);
        if (exact.isPresent()) {
            return exact.get();
        }

        Identifier typed = key.identifier();
        if (!typed.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
            throw ERROR_UNKNOWN_GENUS.create(typed);
        }

        List<Holder.Reference<Genus>> byPath = lookup.listElements()
                .filter(h -> h.key().identifier().getPath().equals(typed.getPath()))
                .toList();

        return switch (byPath.size()) {
            case 1 -> byPath.getFirst();
            case 0 -> throw ERROR_UNKNOWN_GENUS.create(typed.getPath());
            default -> throw ERROR_AMBIGUOUS_GENUS.create(
                    byPath.stream().map(h -> h.key().identifier().toString()).collect(Collectors.joining(", ")));
        };
    }

    /**
     * Where the player is looking, backed off from the surface they hit.
     *
     * <p>This is the default because spawning at the caller's feet is actively wrong for a good
     * chunk of the roster: a Bloater born inside its own 3-block trigger radius detonates on the
     * spot, and a Coward spawned underfoot spends its first second shoving you. The 1.8 plugin
     * used the looked-at block for the same reason.
     */
    private static Vec3 lookingAt(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HitResult hit = player.pick(LOOK_DISTANCE, 1.0F, false);

        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
            // Step out along the face we hit, so the mob stands on the surface rather than in it.
            BlockPos at = block.getBlockPos().relative(block.getDirection());
            return Vec3.atBottomCenterOf(at);
        }
        // Nothing in range - drop it at the end of the ray, wherever that is.
        return player.getEyePosition().add(player.getLookAngle().scale(LOOK_DISTANCE));
    }

    private static int spawn(CommandSourceStack source, ResourceKey<Genus> key, Vec3 at)
            throws CommandSyntaxException {
        ServerLevel level = source.getLevel();

        Holder.Reference<Genus> holder = resolve(source, key);
        Identifier id = holder.key().identifier();
        Genus genus = holder.value();

        Entity created = genus.base().create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Mob mob)) {
            source.sendFailure(Component.literal(
                    "Genus '" + id + "' has base " + genus.base().builtInRegistryHolder().key().identifier()
                            + ", which is not a mob."));
            return 0;
        }

        mob.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360.0F, 0.0F);

        // Assign before adding: EntityJoinLevelEvent builds the AI from the genus tag, so the tag
        // has to be on the entity by the time it lands in the level.
        GenusApplier.assign(mob, holder);
        level.addFreshEntity(mob);

        source.sendSuccess(() -> Component.literal(String.format("Spawned %s at %.1f %.1f %.1f.",
                genus.name().orElse(id.getPath()), at.x, at.y, at.z)), true);
        return 1;
    }

    private static HolderLookup.RegistryLookup<Genus> lookup(CommandSourceStack source) {
        return source.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS);
    }

    private ZombieModCommands() {}
}
