package com.sablednah.zombiemod.core;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sablednah.zombiemod.core.ability.Ability;
import com.sablednah.zombiemod.core.goal.GoalSpec;
import com.sablednah.zombiemod.core.spawn.SpawnRules;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.component.ResolvableProfile;

/**
 * One zombie type, as defined by a datapack file.
 *
 * <p>The 1.8 plugin called these "genera" and read them from {@code plugins/ZombieMod/genera/*.yml};
 * the modern equivalent is a datapack registry, so they live in
 * {@code data/<pack>/zombiemod/genus/<name>.json}, sync to clients, and reload with {@code /reload}.
 *
 * <p>Note what is <em>not</em> here: any notion of a custom entity type. A genus decorates an
 * instance of a <em>vanilla</em> mob — that is what lets a plain vanilla client see it.
 *
 * @param name          display name shown above the mob; absent means no name tag
 * @param base          which vanilla mob to dress up (zombie, husk, drowned, …)
 * @param weight        relative spawn frequency against other genera for the same base mob;
 *                      0 means "never spawn naturally, command/spawner only"
 * @param health        max health, or absent to keep the vanilla value
 * @param damage        attack damage, or absent to keep the vanilla value
 * @param speed         movement speed <em>multiplier</em> on the vanilla base, as in the old configs
 * @param followRange   how far it notices you (the old {@code agro} field)
 * @param scale         body size multiplier — a real synced attribute since 1.20.5, so giants and
 *                      tiddlers cost nothing and need no client mod
 * @param armorColor    dyed-leather colour, an RGB int; the cheapest way to tell genera apart on a
 *                      vanilla client
 * @param clearGoals    wipe the vanilla AI before adding ours (what the 1.8 code did by reflection)
 * @param goals         behaviours, added to the mob's goalSelector
 * @param targetGoals   who to pick a fight with, added to the targetSelector
 * @param spawn         where and when this genus may claim a spawn; absent means anywhere its base
 *                      mob appears
 * @param abilities     things it does repeatedly while alive, independent of where it walks
 * @param attributes    any other attribute by id, e.g. {@code minecraft:armor}. The named fields
 *                      above are shorthand for the common ones; this covers everything else,
 *                      including attributes added by other mods
 * @param head          a player head to wear, by name or with an explicit texture. The strongest
 *                      per-genus identity available to a vanilla client
 * @param phases        stages that open up as it is worn down
 * @param loot          what it drops
 * @param boss          present makes this a boss: a bar at the top of the screen
 * @param behaviours    goal sets that switch on and off with a condition — day/night and the like
 * @param navigation    how it moves — {@code climb} borrows the spider's wall navigator
 */
public record Genus(
        Appearance appearance,
        EntityType<?> base,
        int weight,
        Optional<Double> health,
        Optional<Double> damage,
        double speed,
        Optional<Double> followRange,
        boolean clearGoals,
        List<GoalSpec> goals,
        List<GoalSpec> targetGoals,
        SpawnRules spawn,
        List<Ability> abilities,
        Map<Holder<Attribute>, Double> attributes,
        List<Behaviour> behaviours,
        Encounter encounter,
        Navigation navigation) {

    /**
     * The boss-fight half of a genus: bar, stages, drops.
     *
     * <p>Grouped for the same reason as {@link Appearance} — {@code RecordCodecBuilder.group} stops
     * at 16 fields — and it groups honestly, since these three only ever matter together. A
     * {@code MapCodec} reads sibling keys, so all three stay flat in the JSON.
     */
    public record Encounter(Optional<BossSpec> boss, List<Phase> phases, Optional<LootSpec> loot,
            Optional<Integer> xp, Optional<Double> bounty,
            List<com.sablednah.zombiemod.core.mutate.MutationSpec> mutations) {

        public static final Encounter NONE = new Encounter(Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(), Optional.empty(), List.of());

        public static final com.mojang.serialization.MapCodec<Encounter> MAP_CODEC =
                RecordCodecBuilder.mapCodec(i -> i.group(
                        BossSpec.CODEC.optionalFieldOf("boss").forGetter(Encounter::boss),
                        Phase.CODEC.listOf().optionalFieldOf("phases", List.of()).forGetter(Encounter::phases),
                        LootSpec.CODEC.optionalFieldOf("loot").forGetter(Encounter::loot),
                        Codec.INT.optionalFieldOf("xp").forGetter(Encounter::xp),
                        Codec.DOUBLE.optionalFieldOf("bounty").forGetter(Encounter::bounty),
                        com.sablednah.zombiemod.core.mutate.MutationSpec.CODEC.listOf()
                                .optionalFieldOf("mutations", List.of()).forGetter(Encounter::mutations))
                        .apply(i, Encounter::new));
    }

    /**
     * Which villager a {@code zombie_villager} used to be.
     *
     * <p>Cheapest variety in the mod by a wide margin: seven biome styles times a dozen-odd
     * professions is roughly ninety distinct looks, all of them textures a vanilla client already
     * ships. Ignored by any base that is not a villager.
     */
    public record VillagerLook(Optional<ResourceKey<VillagerProfession>> profession,
            Optional<ResourceKey<VillagerType>> type) {

        public static final com.mojang.serialization.MapCodec<VillagerLook> MAP_CODEC =
                RecordCodecBuilder.mapCodec(i -> i.group(
                        ResourceKey.codec(Registries.VILLAGER_PROFESSION).optionalFieldOf("profession")
                                .forGetter(VillagerLook::profession),
                        ResourceKey.codec(Registries.VILLAGER_TYPE).optionalFieldOf("type")
                                .forGetter(VillagerLook::type))
                        .apply(i, VillagerLook::new));
    }

    /**
     * How it looks. Grouped only because {@code RecordCodecBuilder.group} tops out at 16 fields -
     * a {@code MapCodec} reads sibling keys, so these stay flat in the JSON.
     *
     * @param name       display name, shown when you look at it
     * @param description a line of flavour for the bestiary. Harvested from docs/ROSTER.md, which had
     *                    been carrying exactly this prose for the reader of a document rather than
     *                    the player of the game
     * @param scale      body size multiplier, a synced attribute since 1.20.5
     * @param armorColor dyes a full leather set this RGB colour
     * @param head       a player head to wear; beats armorColor for the head slot
     * @param equipment  held and worn items; beats both of the above for any slot it names
     * @param ghost      take the name and face of a random player who has played here
     * @param mount      something to ride in on - the old `jockey` field
     * @param invisible  render nothing but the equipment - armour walking on its own
     * @param baby       the vanilla baby variant: half size, different proportions
     * @param burning    permanently alight
     * @param arrows     arrows left sticking out of it
     * @param glow       an outline colour, via a scoreboard team. Visible through walls - use sparingly
     * @param villager   which villager it used to be, for a {@code zombie_villager} base
     */
    public record Appearance(Optional<String> name, Optional<String> description, double scale, Optional<Integer> armorColor,
            Optional<ResolvableProfile> head, Equipment equipment, boolean ghost,
            Optional<EntityType<?>> mount, boolean invisible, boolean baby, boolean burning,
            int arrows, Optional<ChatFormatting> glow, Optional<VillagerLook> villager) {

        public static final Appearance PLAIN = new Appearance(Optional.empty(), Optional.empty(), 1.0D,
                Optional.empty(), Optional.empty(), Equipment.NONE, false, Optional.empty(),
                false, false, false, 0, Optional.empty(), Optional.empty());

        public static final com.mojang.serialization.MapCodec<Appearance> MAP_CODEC =
                RecordCodecBuilder.mapCodec(i -> i.group(
                        Codec.STRING.optionalFieldOf("name").forGetter(Appearance::name),
                        Codec.STRING.optionalFieldOf("description").forGetter(Appearance::description),
                        Codec.DOUBLE.optionalFieldOf("scale", 1.0D).forGetter(Appearance::scale),
                        Codec.INT.optionalFieldOf("armor_color").forGetter(Appearance::armorColor),
                        ResolvableProfile.CODEC.optionalFieldOf("head").forGetter(Appearance::head),
                        Equipment.CODEC.optionalFieldOf("equipment", Equipment.NONE)
                                .forGetter(Appearance::equipment),
                        Codec.BOOL.optionalFieldOf("ghost", false).forGetter(Appearance::ghost),
                        BuiltInRegistries.ENTITY_TYPE.byNameCodec().optionalFieldOf("mount")
                                .forGetter(Appearance::mount),
                        Codec.BOOL.optionalFieldOf("invisible", false).forGetter(Appearance::invisible),
                        Codec.BOOL.optionalFieldOf("baby", false).forGetter(Appearance::baby),
                        Codec.BOOL.optionalFieldOf("burning", false).forGetter(Appearance::burning),
                        Codec.INT.optionalFieldOf("arrows", 0).forGetter(Appearance::arrows),
                        ChatFormatting.COLOR_CODEC.optionalFieldOf("glow").forGetter(Appearance::glow),
                        VillagerLook.MAP_CODEC.codec().optionalFieldOf("villager")
                                .forGetter(Appearance::villager))
                        .apply(i, Appearance::new));
    }

    /** How a genus gets around. */
    public enum Navigation {
        /** Whatever the base mob normally uses. */
        DEFAULT,
        /** The spider's wall-climbing navigator - the 1.8 mod's SPIDER ability. */
        CLIMB,
        /** Swims properly instead of walking along the bottom. */
        SWIM,
        /** Handles both land and water, like a drowned. */
        AMPHIBIOUS;

        public static final Codec<Navigation> CODEC = Codec.STRING.xmap(
                v -> valueOf(v.toUpperCase(java.util.Locale.ROOT)),
                v -> v.name().toLowerCase(java.util.Locale.ROOT));
    }

    public static final Codec<Genus> CODEC = RecordCodecBuilder.create(i -> i.group(
            Appearance.MAP_CODEC.forGetter(Genus::appearance),
            BuiltInRegistries.ENTITY_TYPE.byNameCodec()
                    .optionalFieldOf("base", (EntityType<?>) EntityType.ZOMBIE).forGetter(Genus::base),
            Codec.INT.optionalFieldOf("weight", 0).forGetter(Genus::weight),
            Codec.DOUBLE.optionalFieldOf("health").forGetter(Genus::health),
            Codec.DOUBLE.optionalFieldOf("damage").forGetter(Genus::damage),
            Codec.DOUBLE.optionalFieldOf("speed", 1.0D).forGetter(Genus::speed),
            Codec.DOUBLE.optionalFieldOf("follow_range").forGetter(Genus::followRange),
            Codec.BOOL.optionalFieldOf("clear_goals", true).forGetter(Genus::clearGoals),
            GoalSpec.CODEC.listOf().optionalFieldOf("goals", List.of()).forGetter(Genus::goals),
            GoalSpec.CODEC.listOf().optionalFieldOf("target_goals", List.of()).forGetter(Genus::targetGoals),
            SpawnRules.CODEC.optionalFieldOf("spawn", SpawnRules.ANY).forGetter(Genus::spawn),
            Ability.CODEC.listOf().optionalFieldOf("abilities", List.of()).forGetter(Genus::abilities),
            Codec.unboundedMap(BuiltInRegistries.ATTRIBUTE.holderByNameCodec(), Codec.DOUBLE)
                    .optionalFieldOf("attributes", Map.of()).forGetter(Genus::attributes),
            Behaviour.CODEC.listOf().optionalFieldOf("behaviours", List.of()).forGetter(Genus::behaviours),
            Encounter.MAP_CODEC.forGetter(Genus::encounter),
            Navigation.CODEC.optionalFieldOf("navigation", Navigation.DEFAULT).forGetter(Genus::navigation))
            .apply(i, Genus::new));

    // Convenience delegates so callers don't care that appearance is grouped.
    public Optional<String> name() {
        return appearance.name();
    }

    public Optional<String> description() {
        return appearance.description();
    }

    public double scale() {
        return appearance.scale();
    }

    public Optional<Integer> armorColor() {
        return appearance.armorColor();
    }

    public Optional<ResolvableProfile> head() {
        return appearance.head();
    }

    public Equipment equipment() {
        return appearance.equipment();
    }

    public boolean ghost() {
        return appearance.ghost();
    }

    public Optional<EntityType<?>> mount() {
        return appearance.mount();
    }

    /**
     * May this genus win a weighted draw?
     *
     * <p>False for a shipped genus when {@code builtinGenera} is off. Asked by all three draws -
     * natural spawns, unnamed horde waves and conversions - because a server running its own roster
     * wants its own roster everywhere, not only where mobs happen to spawn.
     *
     * <p>Only the draws. Anything that names a genus outright still gets it, so commands, rituals,
     * mutation targets and named horde waves keep working.
     */
    public static boolean drawable(Holder.Reference<Genus> holder) {
        return holder.value().weight() > 0
                && (com.sablednah.zombiemod.ZombieModConfig.BUILTIN_GENERA.get()
                        || !holder.key().identifier().getNamespace().equals("zombiemod"));
    }

    public boolean invisible() {
        return appearance.invisible();
    }

    public boolean baby() {
        return appearance.baby();
    }

    public boolean burning() {
        return appearance.burning();
    }

    public int arrows() {
        return appearance.arrows();
    }

    public Optional<ChatFormatting> glow() {
        return appearance.glow();
    }

    public Optional<VillagerLook> villager() {
        return appearance.villager();
    }

    public Optional<BossSpec> boss() {
        return encounter.boss();
    }

    public List<Phase> phases() {
        return encounter.phases();
    }

    public Optional<LootSpec> loot() {
        return encounter.loot();
    }

    /** Experience dropped on death, or absent to keep the base mob's. */
    public Optional<Integer> xp() {
        return encounter.xp();
    }

    /** What killing this is worth, in whatever currency the server has. */
    public List<com.sablednah.zombiemod.core.mutate.MutationSpec> mutations() {
        return encounter.mutations();
    }

    public Optional<Double> bounty() {
        return encounter.bounty();
    }

    /**
     * Display name as a component, or empty if this genus goes unnamed.
     *
     * <p>Through {@link Announce#format} so {@code &} colour codes work, exactly as they do in a
     * horde's name and its announcement. They did not, and the failure was invisible until a genus
     * finally used one: every shipped genus had a plain name, so {@code "&8Sleeper"} was the first
     * to render its own markup at the player.
     */
    public Optional<Component> displayName() {
        return name().map(Announce::format);
    }
}
