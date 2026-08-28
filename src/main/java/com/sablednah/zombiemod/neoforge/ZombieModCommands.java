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
import com.sablednah.zombiemod.platform.Msg;
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
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
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
 *   <li>{@code /zombiemod horde start|stop|list} — wave events
 *   <li>{@code /zombiemod status} — what the mod thinks its settings are
 * </ul>
 *
 * There is no {@code reload}: genera are datapack data, so vanilla {@code /reload} already does it.
 */
public final class ZombieModCommands {

    /** How far to trace when no position is given. Matches vanilla's own generous reach for /tp-alikes. */
    private static final double LOOK_DISTANCE = 48.0D;

    private static final DynamicCommandExceptionType ERROR_UNKNOWN_GENUS = new DynamicCommandExceptionType(
            genus -> Component.literal("No ZombieMod genus '" + genus + "'. Try /zombiemod list."));

    private static final DynamicCommandExceptionType ERROR_NO_HORDE = new DynamicCommandExceptionType(
            horde -> Component.literal("No horde '" + horde + "'. Try /zombiemod horde list."));

    private static final DynamicCommandExceptionType ERROR_NO_CORPSE = new DynamicCommandExceptionType(
            player -> Component.literal("No such corpse for " + player + ". Try /zombiemod corpse list."));

    private static final DynamicCommandExceptionType ERROR_AMBIGUOUS_GENUS = new DynamicCommandExceptionType(
            matches -> Component.literal("Ambiguous genus name - matches " + matches + ". Use the full id."));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // No bar on the root. Brigadier ANDs a child's requires() with its parent's, so a
        // restrictive root cannot be relaxed by a permissive child - the bestiary is a player
        // feature and would have been unreachable behind a gamemaster root forever.
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("zombiemod");

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
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
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
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
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
                // `here` rebuilds it where you are looking rather than where they died, which is
                // what you actually want when the death spot is the problem - lava, a grinder, the
                // void, or simply the bottom of a ravine you would rather not visit twice. `index`
                // matches `give`, so an older corpse is reachable too.
                .then(Commands.literal("respawn")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> respawnCorpse(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"), 1, null))
                                .then(Commands.literal("here")
                                        .executes(ctx -> respawnCorpse(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"), 1,
                                                lookingAt(ctx.getSource()))))
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .executes(ctx -> respawnCorpse(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "index"), null))
                                        .then(Commands.literal("here")
                                                .executes(ctx -> respawnCorpse(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player"),
                                                        IntegerArgumentType.getInteger(ctx, "index"),
                                                        lookingAt(ctx.getSource())))))))
                .then(Commands.literal("forget")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> forgetCorpse(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"), 1)))));

        // Shows what the mod thinks its settings are. Exists because a server config is per-world in
        // singleplayer, so editing the global config/ copy silently does nothing - and "I turned it
        // on and nothing happened" is indistinguishable from a bug without a way to look.
        root.then(Commands.literal("horde")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("list").executes(ctx -> {
                    var lookup = ctx.getSource().registryAccess()
                            .lookupOrThrow(ZombieModRegistries.HORDE);
                    var ids = lookup.listElementIds().map(k -> k.identifier().toString()).sorted().toList();
                    ctx.getSource().sendSuccess(() -> Component.literal(ids.isEmpty()
                            ? "No hordes defined." : ids.size() + " hordes: " + String.join(", ", ids)), false);
                    return ids.size();
                }))
                .then(Commands.literal("stop").executes(ctx -> {
                    boolean stopped = HordeDirector.stop(ctx.getSource().getPlayerOrException());
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            stopped ? "Horde stopped." : "No horde running."), true);
                    return stopped ? 1 : 0;
                }))
                .then(Commands.literal("start")
                        .then(Commands.argument("horde", ResourceKeyArgument.key(ZombieModRegistries.HORDE))
                                .executes(ctx -> startHorde(ctx.getSource(),
                                        ResourceKeyArgument.getRegistryKey(ctx, "horde",
                                                ZombieModRegistries.HORDE, ERROR_NO_HORDE))))));

        // Readable on a vanilla client, which is the point: the checklist is the feature, and a
        // companion client mod should only ever be a nicer window onto it.
        root.then(Commands.literal("bestiary")
                .executes(ctx -> bestiary(ctx.getSource(), false))
                .then(Commands.literal("book").executes(ctx -> bestiary(ctx.getSource(), true)))
                .then(Commands.literal("info")
                        .then(Commands.argument("genus", ResourceKeyArgument.key(ZombieModRegistries.GENUS))
                                .suggests((c, sb) -> SharedSuggestionProvider.suggestResource(
                                        lookup(c.getSource()).listElementIds().map(ResourceKey::identifier), sb))
                                .executes(ctx -> info(ctx.getSource(),
                                        ResourceKeyArgument.getRegistryKey(ctx, "genus",
                                                ZombieModRegistries.GENUS, ERROR_UNKNOWN_GENUS))))));

        // Admin-only, unlike the rest of the tree: these change what the server does for everyone,
        // not just what happens in front of the person typing.
        var config = Commands.literal("config")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .executes(ctx -> listToggles(ctx.getSource()));
        for (var entry : TOGGLES.entrySet()) {
            config.then(Commands.literal(entry.getKey())
                    .executes(ctx -> setToggle(ctx.getSource(), entry.getKey(), !entry.getValue().get()))
                    .then(Commands.literal("on").executes(ctx -> setToggle(ctx.getSource(), entry.getKey(), true)))
                    .then(Commands.literal("off").executes(ctx -> setToggle(ctx.getSource(), entry.getKey(), false))));
        }
        root.then(config);

        root.then(Commands.literal("status")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> status(ctx.getSource())));

        root.then(Commands.literal("observe")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> setObserve(ctx.getSource(), !ObserverMode.isOn(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("on").executes(ctx -> setObserve(ctx.getSource(), true)))
                .then(Commands.literal("off").executes(ctx -> setObserve(ctx.getSource(), false))));

        var registered = dispatcher.register(root);

        // /zm, redirected rather than re-registered: a redirect shares the one node tree, so every
        // subcommand, argument type and suggestion provider is the same object. Building the tree
        // twice would mean two trees to keep in step, and the second would eventually drift.
        //
        // No bar here either, for the same reason the root has none. Brigadier checks the redirect
        // node's own requirement before following it, so a gamemaster bar on `zm` ANDs with every
        // child and put `/zm bestiary` and `/zm list` out of a normal player's reach - reintroducing,
        // for the alias, exactly the bug the root comment above describes avoiding. Each subcommand
        // carries its own bar, so the alias is as restricted as the full name and no more.
        dispatcher.register(Commands.literal("zm").redirect(registered));
    }


    /**
     * The checklist, as chat or as something you can carry.
     *
     * <p>A book because it is the one rich, paged, scrollable display a vanilla client already has,
     * and it survives being handed to somebody else.
     */
    private static int bestiary(CommandSourceStack source, boolean asBook) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!ZombieModConfig.BESTIARY.get()) {
            source.sendFailure(Component.literal("The bestiary is switched off in the server config."));
            return 0;
        }
        Bestiary bestiary = Bestiary.get(source.getLevel());
        var lookup = source.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS);

        // Sorted by name, not by id, because the book is read by a person.
        List<Row> rows = new ArrayList<>();
        lookup.listElements().forEach(holder -> {
            var id = holder.key().identifier();
            if (bestiary.concealed(player.getUUID(), id, holder.value())) {
                return;
            }
            rows.add(new Row(holder.value().name().orElse(id.getPath()),
                    bestiary.hasMet(player.getUUID(), id),
                    bestiary.killsOf(player.getUUID(), id)));
        });
        rows.sort(java.util.Comparator.comparing(Row::name, String.CASE_INSENSITIVE_ORDER));

        long slain = rows.stream().filter(r -> r.kills() > 0).count();
        long met = rows.stream().filter(Row::met).count();
        String header = slain + " of " + rows.size() + " slain, " + met + " met";

        if (!asBook) {
            source.sendSuccess(() -> Component.literal("ZombieDex").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(" - " + header).withStyle(ChatFormatting.GRAY)), false);
            for (Row row : rows) {
                source.sendSuccess(() -> line(row, ChatFormatting.GREEN, ChatFormatting.YELLOW,
                        ChatFormatting.DARK_GRAY, ChatFormatting.WHITE, ChatFormatting.GRAY), false);
            }
            return (int) slain;
        }

        // Explicit styles rather than legacy section codes. The first page was the only one to
        // carry a bold heading, and the only one to render wrong - with §-codes the style is a
        // running mode that the rest of the page inherits, so a heading can bleed into the list
        // under it. A component tree cannot do that: every span states its own style and siblings
        // inherit nothing from each other.
        List<Filterable<Component>> pages = new ArrayList<>();
        // An EMPTY, unstyled root, with the heading appended as a child rather than being the root.
        // Siblings inherit nothing from each other, but children absolutely inherit from their
        // parent - so making the bold heading the root made every row on page one bold, and only
        // page one, which is exactly what it looked like.
        MutableComponent page = Component.empty();
        page.append(Component.literal("ZombieDex\n").withStyle(ChatFormatting.BOLD));
        page.append(Component.literal(header + "\n\n").withStyle(ChatFormatting.DARK_GRAY));
        // The book is one item, read two ways: this client has the mod, so right-clicking it will
        // open the illustrated dex instead of these pages. Only printed for a player who will
        // actually see that happen - telling a vanilla reader about a screen they cannot open would
        // be worse than saying nothing.
        boolean illustrated = com.sablednah.zombiemod.net.Net.listening(player);
        if (illustrated) {
            page.append(Component.literal("Right-click to open the\nillustrated edition.\n\n")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        // A page holds fourteen lines. The heading costs three, so the first page carries fewer -
        // the previous version put eleven rows on every page and filled the first one exactly to
        // the brim, where one wrapped name would have silently dropped a genus off the end.
        int room = illustrated ? 7 : 10;
        for (Row row : rows) {
            page.append(line(row, ChatFormatting.DARK_GREEN, ChatFormatting.GOLD,
                    ChatFormatting.GRAY, ChatFormatting.BLACK, ChatFormatting.DARK_GRAY));
            page.append(Component.literal("\n"));
            if (--room == 0) {
                pages.add(Filterable.passThrough(page));
                page = Component.empty();
                room = 13;
            }
        }
        if (room < 13) {
            pages.add(Filterable.passThrough(page));
        }

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough("ZombieDex"), player.getGameProfile().name(), 0, pages, false));
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        source.sendSuccess(() -> Component.literal(header).withStyle(ChatFormatting.GOLD), false);
        return (int) slain;
    }


    /**
     * The switches worth flipping without restarting the server.
     *
     * <p>Deliberately not every config key. A command that can set anything is a second, worse
     * config editor; these are the handful whose answer is yes or no and whose effect is immediate,
     * which is exactly the set you want to change while standing in the world it affects.
     */
    private static final java.util.Map<String, net.neoforged.neoforge.common.ModConfigSpec.BooleanValue> TOGGLES =
            new java.util.LinkedHashMap<>();

    static {
        TOGGLES.put("enabled", ZombieModConfig.ENABLED);
        TOGGLES.put("hordes", ZombieModConfig.HORDES);
        TOGGLES.put("playerZombies", ZombieModConfig.PLAYER_ZOMBIES);
        TOGGLES.put("proximity", ZombieModConfig.PROXIMITY);
        TOGGLES.put("bestiary", ZombieModConfig.BESTIARY);
        TOGGLES.put("perGenus", ZombieModConfig.BESTIARY_PER_GENUS);
        TOGGLES.put("hideUnspawnable", ZombieModConfig.BESTIARY_HIDE_UNSPAWNABLE);
        TOGGLES.put("unspawnableRevealedWhenMet", ZombieModConfig.BESTIARY_UNSPAWNABLE_MET);
        TOGGLES.put("logSpawns", ZombieModConfig.LOG_SPAWNS);
    }

    private static int listToggles(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("ZombieMod toggles").withStyle(ChatFormatting.GOLD), false);
        TOGGLES.forEach((name, value) -> source.sendSuccess(() -> Component.literal(" " + name + " ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value.get() ? "on" : "off")
                        .withStyle(value.get() ? ChatFormatting.GREEN : ChatFormatting.RED)), false));
        return TOGGLES.size();
    }

    private static int setToggle(CommandSourceStack source, String name, boolean on) {
        var value = TOGGLES.get(name);
        if (value.get() == on) {
            source.sendSuccess(() -> Component.literal(name + " is already " + (on ? "on" : "off"))
                    .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        value.set(on);
        // set() is explicitly documented as not writing to disk. Without this the change works
        // perfectly until the next restart, which is the worst way for a setting to fail.
        value.save();
        source.sendSuccess(() -> Component.literal(name + " -> ")
                .append(Component.literal(on ? "on" : "off")
                        .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED)), true);
        return 1;
    }


    /**
     * A genus's write-up, in chat, for a player with no mod installed.
     *
     * <p>Gated on having met the thing, because an entry you can read before the encounter is a
     * manual rather than a bestiary. Same gate the client screen applies, enforced here too — the
     * screen's copy is a courtesy, this one is the rule.
     */
    private static int info(CommandSourceStack source, ResourceKey<Genus> key)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!ZombieModConfig.BESTIARY.get()) {
            source.sendFailure(Component.literal("The bestiary is switched off in the server config."));
            return 0;
        }
        // Through resolve(), same as spawn: a bare "walker" is parsed by Brigadier as
        // "minecraft:walker" and would otherwise fail here while working two subcommands away.
        var holder = resolve(source, key);
        Genus genus = holder.value();
        Identifier id = holder.key().identifier();

        Bestiary bestiary = Bestiary.get(source.getLevel());
        // Concealed reads as unmet on purpose: "no such genus" would confirm the id exists.
        if (bestiary.concealed(player.getUUID(), id, genus) || !unlocked(bestiary, player, id)) {
            source.sendFailure(Component.literal(switch (ZombieModConfig.BESTIARY_INFO.get()) {
                case KILLED -> "You have not killed one of those yet.";
                default -> "You have not met one of those yet.";
            }));
            return 0;
        }

        String name = stripCodes(genus.name().orElse(id.getPath()));
        for (Component line : com.sablednah.zombiemod.core.DexEntry.chat(genus, name)) {
            source.sendSuccess(() -> line, false);
        }
        // Drops unlock on a kill specifically, whatever the info gate is set to - the reward
        // before the first kill is a spoiler rather than a record.
        if (genus.loot().isPresent()) {
            if (bestiary.killsOf(player.getUUID(), id) > 0) {
                var drops = DexDrops.itemIds(source.getServer(), genus);
                if (!drops.isEmpty()) {
                    var line = Component.literal(" Leaves behind: ").withStyle(ChatFormatting.DARK_GRAY);
                    for (int i = 0; i < drops.size(); i++) {
                        var itemId = net.minecraft.resources.Identifier.tryParse(drops.get(i));
                        var item = itemId == null ? null
                                : net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(itemId);
                        line.append(Component.literal(i > 0 ? ", " : "").withStyle(ChatFormatting.DARK_GRAY))
                                .append((item == null ? Component.literal(drops.get(i))
                                        : new net.minecraft.world.item.ItemStack(item).getHoverName().copy())
                                        .withStyle(ChatFormatting.WHITE));
                    }
                    var built = line;
                    source.sendSuccess(() -> built, false);
                }
            } else {
                source.sendSuccess(() -> Component.literal(" Kill one to learn what it leaves behind.")
                        .withStyle(ChatFormatting.DARK_GRAY), false);
            }
        }
        return 1;
    }

    /** The one place that decides whether a player has earned an entry. */
    static boolean unlocked(Bestiary bestiary, ServerPlayer player, Identifier genus) {
        return switch (ZombieModConfig.BESTIARY_INFO.get()) {
            case ALWAYS -> true;
            case KILLED -> bestiary.killsOf(player.getUUID(), genus) > 0;
            case MET -> bestiary.hasMet(player.getUUID(), genus);
        };
    }

    /** One line of the checklist. */
    private record Row(String name, boolean met, int kills) {}

    /**
     * One checklist row, told which palette to use.
     *
     * <p>Chat is light text on a dark background and a book is dark text on parchment, so the two
     * need opposite ends of the same colours - passing them in keeps one definition of what a row
     * <em>is</em>.
     */
    private static MutableComponent line(Row row, ChatFormatting slain, ChatFormatting met,
            ChatFormatting unmet, ChatFormatting name, ChatFormatting count) {
        MutableComponent out = row.kills() > 0
                ? Component.literal("\u2714 ").withStyle(slain)
                : row.met() ? Component.literal("? ").withStyle(met)
                        : Component.literal("\u2718 ").withStyle(unmet);
        out.append(Component.literal(stripCodes(row.name())).withStyle(name));
        if (row.kills() > 0) {
            out.append(Component.literal(" x" + row.kills()).withStyle(count));
        }
        return out;
    }

    /** Genus names carry legacy colour codes; a book page renders those literally. */
    private static String stripCodes(String name) {
        return name.replaceAll("(?i)&[0-9a-fk-or]", "");
    }

    private static int startHorde(CommandSourceStack source,
            ResourceKey<com.sablednah.zombiemod.core.HordeSpec> key) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var spec = source.registryAccess().lookupOrThrow(ZombieModRegistries.HORDE).get(key)
                .orElseThrow(() -> ERROR_NO_HORDE.create(key.identifier()));
        if (!HordeDirector.start(source.getLevel(), player, spec.value())) {
            source.sendFailure(Component.literal("A horde is already running for you."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Started ")
                .append(com.sablednah.zombiemod.core.Announce.format(spec.value().name()))
                .append(Component.literal(".")), true);
        return 1;
    }

    // Styles, never legacy section codes. Status exists to be read by an admin, and an admin is
    // as likely to be at a console or on RCON as standing in the world - and everything that is
    // not a client reads a component through getString(), which hands section codes straight back
    // as literal text. The config path two screens down is the sharp end of it: that line exists
    // to be copied. A component tree renders identically in chat and flattens to a clean sentence
    // everywhere else. Note the empty roots below - a coloured span cannot be the root or its
    // siblings inherit the colour, which is the same trap documented on the dex book above.
    private static int status(CommandSourceStack source) {
        var src = source;
        src.sendSuccess(() -> Component.literal("ZombieMod status")
                .withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(() -> Component.literal("  enabled: " + ZombieModConfig.ENABLED.get()
                + "   vanillaWeight: " + ZombieModConfig.VANILLA_WEIGHT.get()
                + "   logSpawns: " + ZombieModConfig.LOG_SPAWNS.get()), false);

        boolean pz = ZombieModConfig.PLAYER_ZOMBIES.get();
        src.sendSuccess(() -> Component.empty()
                .append(Component.literal("  playerZombies: " + pz)
                        .withStyle(pz ? ChatFormatting.GREEN : ChatFormatting.RED))
                .append(Component.literal("   takeItems: "
                        + ZombieModConfig.PLAYER_ZOMBIE_TAKES_ITEMS.get())), false);

        // Resolve the corpse genus, because a valid-looking id that is not loaded fails silently at
        // the moment of death - which is the worst possible time to find out.
        String genusId = ZombieModConfig.PLAYER_ZOMBIE_GENUS.get();
        Identifier id = Identifier.tryParse(genusId);
        boolean ok = id != null && lookup(source).get(ResourceKey.create(ZombieModRegistries.GENUS, id)).isPresent();
        src.sendSuccess(() -> Component.literal("  corpse genus: " + genusId
                + (ok ? " (loaded)" : " (NOT LOADED - no corpse will be raised)"))
                .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED), false);

        boolean ftb = com.sablednah.zombiemod.compat.LandClaims.anyProvider();
        src.sendSuccess(() -> Component.empty()
                .append(Component.literal("  claims: "
                        + com.sablednah.zombiemod.compat.LandClaims.providers())
                        .withStyle(ftb ? ChatFormatting.GREEN : ChatFormatting.GRAY))
                .append(Component.literal(
                        "   respectClaims: " + ZombieModConfig.CLAIM_PROTECTION.get()
                        + "   noGriefingInClaims: " + ZombieModConfig.CLAIM_NO_GRIEFING.get()
                        + "   inClaims: " + ZombieModConfig.CLAIM_SPAWNS.get()
                        + "   " + ZombieModEvents.CLAIMS)), false);
        // The three ways this setting looks broken when it is not, in the order people hit them.
        if (ZombieModConfig.CLAIM_PROTECTION.get()
                && ZombieModConfig.CLAIM_SPAWNS.get() != ZombieModConfig.ClaimSpawns.ALLOW) {
            if (!ftb) {
                src.sendSuccess(() -> Component.literal(
                        "  note: inClaims does nothing without a claims provider - FTB Chunks or Standards")
                        .withStyle(ChatFormatting.YELLOW), false);
            } else if (ZombieModEvents.CLAIMS.cancelled == 0
                    && ZombieModEvents.CLAIMS.keptVanilla == 0) {
                src.sendSuccess(() -> Component.literal(
                        "  note: no spawn has been inside a claim yet - claim the chunk you are"
                        + " standing in, then wait somewhere a mob would normally appear")
                        .withStyle(ChatFormatting.YELLOW), false);
            }
        }

        // Bounty is the one reward that can be switched on, configured, and still pay nobody -
        // because "who holds the money" is a question about the rest of the server, not about us.
        // Say how many payers actually answered, or the failure looks like a bug in the bounty.
        boolean bounty = ZombieModConfig.BOUNTY.get();
        boolean std = com.sablednah.zombiemod.compat.StandardsEconomy.present();
        int payers = Bounties.payerCount();
        src.sendSuccess(() -> Component.empty()
                .append(Component.literal("  bounty: " + bounty)
                        .withStyle(bounty ? ChatFormatting.GREEN : ChatFormatting.GRAY))
                .append(Component.literal("   payers: " + payers
                        + "   Standards economy: " + (std ? "linked" : "not present")
                        + "   scoreboard: " + ZombieModConfig.BOUNTY_OBJECTIVE.get())), false);
        if (bounty && payers == 0) {
            src.sendSuccess(() -> Component.literal(
                    "  note: no economy payer registered - bounties go to the scoreboard objective"
                    + " only, and only if that objective already exists")
                    .withStyle(ChatFormatting.YELLOW), false);
        }

        // Conversion declines for several reasons and every one of them looks the same from in the
        // world: a mob dies and nothing gets up. Say which reason, or "the Carrier is broken" is the
        // only conclusion available.
        src.sendSuccess(() -> Component.literal("  conversions: " + Conversions.COUNTERS)
                .withStyle(ChatFormatting.GRAY), false);
        if (Conversions.COUNTERS.crowded > 0 && Conversions.COUNTERS.raised == 0) {
            src.sendSuccess(() -> Component.literal(
                    "  note: every conversion so far was blocked by the nearby cap - a genus only"
                    + " converts while fewer than its max_nearby of the risen shape are within its"
                    + " radius, which a busy area passes constantly")
                    .withStyle(ChatFormatting.YELLOW), false);
        }

        // Seasonal genera are invisible for most of the year, which is indistinguishable from
        // broken. Say what date is in force and what it lets through, or the only conclusion
        // available in June is "the Halloween zombies do not work".
        List<String> seasonal = new ArrayList<>();
        for (var holder : lookup(source).listElements().toList()) {
            for (var c : holder.value().spawn().conditions()) {
                if (c instanceof com.sablednah.zombiemod.core.spawn.SpawnConditions.OnDate d) {
                    seasonal.add(holder.key().identifier().getPath()
                            + (d.inSeason() ? " (in season)" : " (out of season)"));
                }
            }
        }
        if (!seasonal.isEmpty()) {
            String override = ZombieModConfig.DATE_OVERRIDE.get();
            src.sendSuccess(() -> Component.literal("  date: "
                    + com.sablednah.zombiemod.core.spawn.SpawnConditions.OnDate.today()
                    + (override.isBlank() ? "" : "  (dateOverride=" + override + ")"))
                    .withStyle(override.isBlank() ? ChatFormatting.GRAY : ChatFormatting.YELLOW), false);
            src.sendSuccess(() -> Component.literal("  seasonal: " + String.join(", ", seasonal))
                    .withStyle(ChatFormatting.GRAY), false);
        }

        boolean prox = ZombieModConfig.PROXIMITY.get();
        src.sendSuccess(() -> Component.empty()
                .append(Component.literal("  proximity: " + prox)
                        .withStyle(prox ? ChatFormatting.GREEN : ChatFormatting.GRAY))
                .append(Component.literal("   " + ProximitySpawner.COUNTERS)), false);
        // The question this line answers cost a real debugging session: proximity skips creative
        // and spectator players entirely, and a tester flying about in creative sees "enabled" and
        // zero effect. Status should say so to their face.
        if (prox && src.getEntity() instanceof ServerPlayer self
                && (self.isCreative() || self.isSpectator())) {
            src.sendSuccess(() -> Component.literal(
                    "  note: you are in " + (self.isCreative() ? "creative" : "spectator")
                    + " - proximity ignores you until you are in survival")
                    .withStyle(ChatFormatting.YELLOW), false);
        }

        src.sendSuccess(() -> Component.literal("  genera loaded: "
                + lookup(source).listElementIds().count()), false);
        // Printed because "I edited the config and nothing changed" is the commonest way this
        // mod looks broken. NeoForge's own serverconfig/readme.txt is the authority: config/ holds
        // the file, and a copy under a world's serverconfig/ *overrides* it for that world only.
        // This used to say the opposite - that config/ did nothing in singleplayer - which sent
        // people to edit a file that does not exist.
        src.sendSuccess(() -> Component.literal("  config: config/zombiemod-server.toml")
                .withStyle(ChatFormatting.GRAY), false);
        src.sendSuccess(() -> Component.literal(
                "  per-world override: saves/<world>/serverconfig/zombiemod-server.toml")
                .withStyle(ChatFormatting.GRAY), false);
        if (!pz) {
            src.sendSuccess(() -> Component.literal(
                    "  keepInventory must also be off, or there are no drops to take.")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
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
            // Three states, not two, and the third is the one worth having: an entry that is still
            // outstanding AND says why the items are not lying where the corpse fell. An admin
            // reading "already recovered" about an inventory that went into lava would tell the
            // player it had been handed back, which is the wrong answer given confidently.
            // Styled span rather than a section code, for the same reason as status: this list
            // is printed to be read by an admin deciding whether to re-issue items, and on a
            // console the section code would arrive as literal text wrapped round the one detail
            // the decision turns on.
            Component note = e.lostTo()
                    .map(how -> Component
                            .literal(" (died in " + how + " - items destroyed, not yet re-issued)")
                            .withStyle(ChatFormatting.RED))
                    .orElseGet(() -> e.claimed()
                            ? Component.literal(" (already recovered)")
                                    .withStyle(ChatFormatting.DARK_GRAY)
                            : Component.empty());
            source.sendSuccess(() -> Component.literal(String.format(
                    "#%d %s - %d %d %d in %s, day %d, %d item stacks",
                    n, e.playerName(), e.x(), e.y(), e.z(), e.dimension(), e.day(),
                    e.items().size())).append(note), false);
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
            Msg.chat(owner, Component.literal("Your corpse's belongings have been returned.")
                    .withStyle(ChatFormatting.YELLOW));
        }
        return 1;
    }

    /** {@code at} null means the death spot recorded in the ledger; otherwise, put it there. */
    private static int respawnCorpse(CommandSourceStack source, String player, int index, Vec3 at)
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
        Vec3 where = at != null ? at
                : new Vec3(entry.x() + 0.5D, entry.y() + 0.5D, entry.z() + 0.5D);
        corpse.snapTo(where.x, where.y, where.z, 0.0F, 0.0F);
        GenusApplier.assign(corpse, holder.get());
        // Announce.format, not Component.literal - the death path formats this name, so a corpse
        // rebuilt by command would otherwise wear "&7Corpse Sable" with the code showing.
        corpse.setCustomName(com.sablednah.zombiemod.core.Announce.format(
                ZombieModConfig.PLAYER_ZOMBIE_NAME.get().replace("%P", entry.playerName())));
        corpse.setCustomNameVisible(true);
        PlayerZombies.rebuild(level, corpse, entry);
        level.addFreshEntity(corpse);

        // Report where it actually went, not where it died - those are now different things, and
        // the message is how an admin tells the player where to go looking.
        source.sendSuccess(() -> Component.literal(String.format(
                "Rebuilt %s's corpse at %d %d %d with %d stacks%s.",
                entry.playerName(), (int) where.x, (int) where.y, (int) where.z,
                entry.items().size(),
                at != null ? " (moved from " + entry.x() + " " + entry.y() + " " + entry.z() + ")"
                        : "")), true);
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

        // The name goes through the formatter, not into String.format: a genus name is authored
        // text with & colour codes in it, and printing it raw shows the player the markup.
        source.sendSuccess(() -> Component.literal("Spawned ")
                .append(genus.displayName().orElseGet(() -> Component.literal(id.getPath())))
                .append(Component.literal(String.format(" at %.1f %.1f %.1f.", at.x, at.y, at.z))), true);
        return 1;
    }

    private static HolderLookup.RegistryLookup<Genus> lookup(CommandSourceStack source) {
        return source.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS);
    }

    private ZombieModCommands() {}
}
