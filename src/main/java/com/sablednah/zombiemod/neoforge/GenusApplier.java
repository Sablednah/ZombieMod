package com.sablednah.zombiemod.neoforge;

import java.util.Optional;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.ZombieMod;
import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.core.Genus;
import com.sablednah.zombiemod.core.goal.GoalSpec;

import org.slf4j.Logger;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerDataHolder;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Turns a {@link Genus} into changes on a live vanilla mob.
 *
 * <p>Two halves, and the split matters:
 * <ul>
 *   <li><b>Persistent</b> — attributes, equipment, name, scale. Written once when the mob is first
 *       spawned; Minecraft saves these in the entity's NBT, so they survive a restart on their own.
 *   <li><b>Transient</b> — the AI. Goals are objects on a {@code GoalSelector}, not NBT, so they
 *       are gone after a reload and have to be rebuilt every time the entity joins a level.
 * </ul>
 *
 * <p>The genus id itself goes in the entity's persistent data, which is how we know on reload what
 * this thing was meant to be. (The 1.8 plugin held its equivalent in a memory map and lost every
 * player-corpse zombie on restart — a known bug in its README. This is the fix, for free.)
 */
public final class GenusApplier {

    private static final Logger LOG = LogUtils.getLogger();

    /** Key inside {@code entity.getPersistentData()} holding the genus id. */
    public static final String GENUS_TAG = "zombiemod:genus";

    /** Teams we create for glow colours, one per colour, shared by every genus that asks. */
    private static final String TEAM_PREFIX = "zombiemod_";

    /**
     * Marks the entity as belonging to a genus and applies everything that persists. Call once, at
     * spawn.
     */
    public static void assign(Mob mob, Holder.Reference<Genus> holder) {
        Genus genus = holder.value();
        mob.getPersistentData().putString(GENUS_TAG, holder.key().identifier().toString());

        setBase(mob, Attributes.MAX_HEALTH, genus.health());
        genus.health().ifPresent(h -> mob.setHealth(h.floatValue()));
        setBase(mob, Attributes.ATTACK_DAMAGE, genus.damage());
        setBase(mob, Attributes.FOLLOW_RANGE, genus.followRange());

        // Speed is a multiplier on whatever the base mob runs at, matching the old config's
        // semantics — "speed: 1.15" meant 15% quicker than a zombie, not 1.15 blocks/tick.
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && genus.speed() != 1.0D) {
            speed.setBaseValue(speed.getBaseValue() * genus.speed());
        }

        // Size is a first-class synced attribute now. In 1.8 this meant hacking the bounding box.
        if (genus.scale() != 1.0D) {
            setBase(mob, Attributes.SCALE, Optional.of(genus.scale()));
        }

        genus.displayName().ifPresent(name -> {
            mob.setCustomName(name);
            mob.setCustomNameVisible(false); // visible on look, not permanently overhead
        });

        genus.armorColor().ifPresent(rgb -> dressIn(mob, rgb));

        // A player head beats the dyed helmet: it is the strongest identity a vanilla client can
        // show, so if a genus asks for one it wins the slot.
        genus.head().ifPresent(profile -> {
            ItemStack skull = new ItemStack(Items.PLAYER_HEAD);
            skull.set(DataComponents.PROFILE, profile);
            mob.setItemSlot(EquipmentSlot.HEAD, skull);
            mob.setDropChance(EquipmentSlot.HEAD, 0.0F);
        });

        // Explicit equipment last, so it wins any slot it names. The cascade is broad -> specific:
        // armor_color dresses all four armour slots, head replaces the helmet, equipment overrides
        // whichever slots it mentions.
        genus.equipment().forEach((slot, stack) -> {
            mob.setItemSlot(slot, stack.copy());
            mob.setDropChance(slot, genus.equipment().dropChance());
        });

        // GHOST: wear a real player's name and face. The 1.8 version could only borrow the name,
        // because skins needed Spout; a head component makes the whole disguise free.
        if (genus.ghost() && mob.level() instanceof ServerLevel serverLevel) {
            KnownPlayers.get(serverLevel).random(mob.getRandom(), serverLevel.getServer())
                    .ifPresent(seen -> {
                        mob.setCustomName(net.minecraft.network.chat.Component.literal(seen.name()));
                        mob.setCustomNameVisible(false);
                        ItemStack face = new ItemStack(Items.PLAYER_HEAD);
                        // By id for someone who has played here, by name for a seed entry - a
                        // name-only profile is exactly what "head": "Herobrine" already uses.
                        face.set(DataComponents.PROFILE, seen.id()
                                .map(net.minecraft.world.item.component.ResolvableProfile::createUnresolved)
                                .orElseGet(() -> net.minecraft.world.item.component.ResolvableProfile
                                        .createUnresolved(seen.name())));
                        mob.setItemSlot(EquipmentSlot.HEAD, face);
                        mob.setDropChance(EquipmentSlot.HEAD, 0.0F);
                    });
        }

        // Applied here as well as in the upkeep goal, and the difference is visible: assign() runs
        // before the entity is ever added to the level, while the goal is built on the join event -
        // by which point a client can already have been told about a perfectly visible zombie. The
        // goal still does it, because this flag is the one appearance field vanilla does not save,
        // so a reloaded chunk has to set it again.
        if (genus.invisible()) {
            mob.setInvisible(true);
        }
        if (genus.arrows() > 0) {
            mob.setArrowCount(genus.arrows());
        }

        // Alight, without being on fire. Display-only, and saved by vanilla as HasVisualFire, so it
        // neither damages the mob nor needs topping up - see the access transformer for why the
        // obvious route (remainingFireTicks) is wrong.
        if (genus.burning()) {
            mob.hasVisualFire = true;
            mob.setSharedFlagOnFire(true);
        }

        // The vanilla baby variant: half size, quicker, visibly a child. Saved as IsBaby, so this
        // belongs here rather than in the transient pass.
        if (genus.baby() && mob instanceof Zombie zombie) {
            zombie.setBaby(true);
        }

        // Which villager it used to be. Ninety-odd vanilla textures for one field, and it says
        // something the mod otherwise cannot: this was a person, and that was their job.
        genus.villager().ifPresent(look -> {
            if (!(mob instanceof VillagerDataHolder villager) || !(mob.level() instanceof ServerLevel server)) {
                return;
            }
            VillagerData data = villager.getVillagerData();
            if (look.type().isPresent()) {
                data = data.withType(server.registryAccess(), look.type().get());
            }
            if (look.profession().isPresent()) {
                data = data.withProfession(server.registryAccess(), look.profession().get());
            }
            villager.setVillagerData(data);
        });

        // The outline colour rides a scoreboard team, because that is the only thing vanilla
        // consults for it. The Glowing tag itself is saved in entity NBT, so it survives a restart
        // without help - the team membership is what needs the scoreboard to remember.
        genus.glow().ifPresent(colour -> {
            mob.setGlowingTag(true);
            if (mob.level() instanceof ServerLevel server) {
                Scoreboard scoreboard = server.getScoreboard();
                String name = TEAM_PREFIX + colour.getName();
                PlayerTeam team = scoreboard.getPlayerTeam(name);
                if (team == null) {
                    team = scoreboard.addPlayerTeam(name);
                    team.setColor(colour);
                    // Or a dozen glowing zombies read as one blob with no edges between them.
                    team.setSeeFriendlyInvisibles(false);
                }
                scoreboard.addPlayerToTeam(mob.getScoreboardName(), team);
            }
        });

        // Anything the named fields don't cover, including other mods' attributes.
        genus.attributes().forEach((attribute, value) -> {
            AttributeInstance instance = mob.getAttribute(attribute);
            if (instance != null) {
                instance.setBaseValue(value);
            } else {
                LOG.warn("[{}] {} has no attribute {} - ignored", ZombieMod.MOD_ID,
                        mob.getType().builtInRegistryHolder().key().identifier(),
                        attribute.getRegisteredName());
            }
        });

        // Jockeys. The 1.8 `jockey` field, which was a pipe-delimited string; this is an entity id.
        genus.mount().ifPresent(type -> {
            if (!(mob.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            var steed = type.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.JOCKEY);
            if (steed != null) {
                steed.snapTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), 0.0F);
                if (steed instanceof Mob steedMob) {
                    steedMob.setPersistenceRequired();
                }
                serverLevel.addFreshEntity(steed);
                mob.startRiding(steed, true, true);
            }
        });

        // Something this distinctive shouldn't quietly despawn while the player walks away.
        mob.setPersistenceRequired();
    }

    /**
     * Drops a mob from its glow team, if it is on one of ours.
     *
     * <p>Vanilla already does this when a mob dies — {@code Scoreboard.entityRemoved} — but only for
     * one that is actually dead. A mob discarded while still alive, which is exactly what mutation
     * does, would otherwise leave its id in the team forever, and scoreboard data is saved. Only our
     * own teams are touched, so a team the server owner put a mob in is left alone.
     */
    static void clearGlowTeam(Mob mob) {
        if (!(mob.level() instanceof ServerLevel server)) {
            return;
        }
        PlayerTeam team = server.getScoreboard().getPlayersTeam(mob.getScoreboardName());
        if (team != null && team.getName().startsWith(TEAM_PREFIX)) {
            server.getScoreboard().removePlayerFromTeam(mob.getScoreboardName(), team);
        }
    }

    /**
     * Rebuilds the AI. Safe to call on every level join — it clears first when the genus says so.
     */
    public static void applyAi(Mob mob, Genus genus) {
        if (genus.clearGoals()) {
            // The 1.8 code did this by reflecting a private list out of the goal selector. Now it
            // is one public call.
            mob.goalSelector.removeAllGoals(g -> true);
            mob.targetSelector.removeAllGoals(g -> true);
        }
        applyNavigation(mob, genus);
        addAll(mob, genus.goals(), mob.goalSelector, "goals");
        addAll(mob, genus.targetGoals(), mob.targetSelector, "target_goals");

        // Conditional behaviours are registered alongside the base goals, not swapped in later -
        // see ConditionalGoal for why rebuilding a live goal list is a trap.
        for (com.sablednah.zombiemod.core.Behaviour behaviour : genus.behaviours()) {
            addAll(mob, behaviour.goals(), mob.goalSelector, "behaviour goals", behaviour.when());
            addAll(mob, behaviour.targetGoals(), mob.targetSelector, "behaviour target_goals",
                    behaviour.when());
        }

        // Abilities ride the goal selector too - see AbilityGoal for why. Priority is irrelevant
        // because they carry no control flags, but a high number keeps them visually last in any
        // debug dump of the goal list.
        // Looks that vanilla erodes - fire, arrows, the unsaved invisibility flag. Rebuilt on every
        // join like the goals themselves, because that is exactly what they are: transient state.
        UpkeepGoal upkeep = UpkeepGoal.forGenus(mob, genus);
        if (upkeep != null) {
            mob.goalSelector.addGoal(99, upkeep);
        }

        for (com.sablednah.zombiemod.core.ability.Ability ability : genus.abilities()) {
            mob.goalSelector.addGoal(99, new AbilityGoal(mob, ability));
        }

        // One goal for the whole mutation list rather than one each: they are checked in order and
        // the first to fire ends the mob, so they are not independent of one another.
        if (!genus.mutations().isEmpty()) {
            mob.goalSelector.addGoal(99, new MutationGoal(mob, genus.mutations()));
        }
        genus.boss().ifPresent(spec -> mob.goalSelector.addGoal(99, new BossBarGoal(mob, spec)));

        // Phase abilities are gated on health rather than added on entry, so they switch off again
        // if the mob heals - a boss that regenerates past the threshold should lose the enrage.
        for (com.sablednah.zombiemod.core.Phase phase : genus.phases()) {
            double threshold = phase.belowHealth();
            for (com.sablednah.zombiemod.core.ability.Ability ability : phase.abilities()) {
                mob.goalSelector.addGoal(99, new ConditionalGoal(mob, new AbilityGoal(mob, ability),
                        m -> m.getMaxHealth() > 0.0F && m.getHealth() / m.getMaxHealth() <= threshold,
                        "health<=" + threshold));
            }
        }
        if (!genus.phases().isEmpty()) {
            mob.goalSelector.addGoal(99, new PhaseGoal(mob, genus.phases()));
        }
    }

    /**
     * Swap the movement brain. This is the 1.8 mod's proudest trick — it bolted the spider's
     * navigator onto a zombie to make one that climbs walls — and it is now a field assignment
     * rather than reflection. {@code Mob.getNavigation()} only reads the field, so replacing it is
     * enough; every goal built afterwards picks up the new navigator.
     */
    private static void applyNavigation(Mob mob, Genus genus) {
        switch (genus.navigation()) {
            case DEFAULT -> {
                // leave whatever the base mob came with
            }
            case CLIMB -> {
                mob.navigation = new WallClimberNavigation(mob, mob.level());
                // The navigator plans the climb; ClimbGoal performs it. Neither works alone.
                mob.goalSelector.addGoal(0, new ClimbGoal(mob));
            }
            case SWIM -> mob.navigation = new WaterBoundPathNavigation(mob, mob.level());
            case AMPHIBIOUS -> mob.navigation = new AmphibiousPathNavigation(mob, mob.level());
        }
    }

    private static void addAll(Mob mob, java.util.List<GoalSpec> specs,
            net.minecraft.world.entity.ai.goal.GoalSelector selector, String what) {
        addAll(mob, specs, selector, what, null);
    }

    private static void addAll(Mob mob, java.util.List<GoalSpec> specs,
            net.minecraft.world.entity.ai.goal.GoalSelector selector, String what,
            com.sablednah.zombiemod.core.spawn.SpawnCondition when) {
        for (GoalSpec spec : specs) {
            Goal goal = spec.build(mob);
            if (goal == null) {
                // Not fatal: some vanilla goals demand a PathfinderMob or a TamableAnimal. Skipping
                // one bad goal beats refusing to spawn the whole genus, but it must be visible.
                LOG.warn("[{}] {} '{}' does not apply to {} - skipped",
                        ZombieMod.MOD_ID, what, spec.type(), mob.getType().builtInRegistryHolder().key().identifier());
                continue;
            }
            selector.addGoal(spec.priority(), when == null ? goal
                    : new ConditionalGoal(mob, goal,
                            m -> when.test(m.level(), m.blockPosition()), when.type().toString()));
        }
    }

    /** Reads back the genus this entity was assigned, if it still resolves in the current datapacks. */
    public static Optional<Holder.Reference<Genus>> genusOf(Mob mob, ServerLevel level) {
        CompoundTag data = mob.getPersistentData();
        return data.getString(GENUS_TAG)
                .map(Identifier::tryParse)
                .flatMap(id -> id == null
                        ? Optional.empty()
                        : level.registryAccess()
                                .lookupOrThrow(ZombieModRegistries.GENUS)
                                .get(ResourceKey.create(ZombieModRegistries.GENUS, id)));
    }

    private static void setBase(Mob mob, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            Optional<Double> value) {
        if (value.isEmpty()) {
            return;
        }
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value.get());
        }
    }

    /**
     * Dyed leather, the poor mod's reskin: four slots of arbitrary RGB that every vanilla client
     * already knows how to render. Drop chance zeroed so a colour scheme doesn't become loot.
     */
    private static void dressIn(Mob mob, int rgb) {
        equip(mob, EquipmentSlot.HEAD, Items.LEATHER_HELMET, rgb);
        equip(mob, EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE, rgb);
        equip(mob, EquipmentSlot.LEGS, Items.LEATHER_LEGGINGS, rgb);
        equip(mob, EquipmentSlot.FEET, Items.LEATHER_BOOTS, rgb);
    }

    private static void equip(Mob mob, EquipmentSlot slot, net.minecraft.world.item.Item item, int rgb) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb));
        mob.setItemSlot(slot, stack);
        mob.setDropChance(slot, 0.0F);
    }

    private GenusApplier() {}
}
