package com.sablednah.zombiemod.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
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
 * </ul>
 *
 * There is no {@code reload}: genera are datapack data, so vanilla {@code /reload} already does it.
 */
public final class ZombieModCommands {

    /** How far to trace when no position is given. Matches vanilla's own generous reach for /tp-alikes. */
    private static final double LOOK_DISTANCE = 48.0D;

    private static final DynamicCommandExceptionType ERROR_UNKNOWN_GENUS = new DynamicCommandExceptionType(
            genus -> Component.literal("No ZombieMod genus '" + genus + "'. Try /zombiemod list."));

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

        root.then(Commands.literal("observe")
                .executes(ctx -> setObserve(ctx.getSource(), !ObserverMode.isOn(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("on").executes(ctx -> setObserve(ctx.getSource(), true)))
                .then(Commands.literal("off").executes(ctx -> setObserve(ctx.getSource(), false))));

        dispatcher.register(root);
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
