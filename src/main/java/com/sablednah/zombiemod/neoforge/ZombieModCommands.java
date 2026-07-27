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
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /zombiemod} — enough to exercise the mod by hand.
 *
 * <ul>
 *   <li>{@code /zombiemod list} — what genera the current datapacks define
 *   <li>{@code /zombiemod spawn <genus>} — put one in front of you
 * </ul>
 *
 * There is no {@code reload}: genera are datapack data, so vanilla {@code /reload} already does it.
 */
public final class ZombieModCommands {

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
                        .executes(ctx -> spawn(ctx.getSource(),
                                ResourceKeyArgument.getRegistryKey(
                                        ctx, "genus", ZombieModRegistries.GENUS, ERROR_UNKNOWN_GENUS)))));

        dispatcher.register(root);
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

    private static int spawn(CommandSourceStack source, ResourceKey<Genus> key) throws CommandSyntaxException {
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

        Vec3 at = source.getPosition();
        mob.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360.0F, 0.0F);

        // Assign before adding: EntityJoinLevelEvent builds the AI from the genus tag, so the tag
        // has to be on the entity by the time it lands in the level.
        GenusApplier.assign(mob, holder);
        level.addFreshEntity(mob);

        source.sendSuccess(() -> Component.literal("Spawned " + genus.name().orElse(id.getPath()) + "."), true);
        return 1;
    }

    private static HolderLookup.RegistryLookup<Genus> lookup(CommandSourceStack source) {
        return source.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS);
    }

    private ZombieModCommands() {}
}
