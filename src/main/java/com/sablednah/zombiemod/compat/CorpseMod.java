package com.sablednah.zombiemod.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The Corpse mod (henkelmax's, mod id {@code corpse}), if it is here at all.
 *
 * <p>The naming is unavoidable, so to be exact about it: <b>our</b> corpse is a zombie that gets up
 * wearing the dead player's face and carrying their things. <b>Corpse's</b> corpse — called a
 * <em>body</em> throughout this class, to keep the two apart — is an entity that lies where the
 * player fell and opens as a container. With both installed they are two stages of one death: the
 * body gets up, and when somebody puts it down again it <em>stays</em> down, as a Corpse body
 * holding what it carried, instead of bursting into a pile of item entities.
 *
 * <p>That is better than the drop in every way that matters. A Corpse body does not despawn in five
 * minutes, does not burn in lava unless its own config says so, cannot be hoovered up by a passing
 * hopper or a passing stranger ({@code only_owner} is Corpse's setting and is honoured, because the
 * body carries the dead player's uuid), and its transfer button puts armour back on.
 *
 * <p><b>The two mods already collide without this class</b>, which is the other half of the job.
 * Corpse listens for a player's drops at {@code LOWEST}; we take them at the default priority, so
 * by the time Corpse looks, the list is empty — and it spawns a body anyway, unconditionally. The
 * result is an empty body lying at the death spot for thirty seconds while the real one walks off.
 * {@link #isEmptyBodyOf} is what lets {@code PlayerZombies} refuse that body entry to the world.
 * It did get up, after all.
 *
 * <p>Reflective, like the rest of {@code compat}, and inert without Corpse. The surface is
 * {@code Death.Builder}, six setters on {@code CorpseEntity} and one config read; signatures were
 * read off {@code corpse-neoforge-1.21.11-1.1.16}, {@code 1.1.16+26.1.2} and {@code 1.1.19+26.2},
 * and are identical across all three, so this file is shared by every branch unchanged.
 *
 * <p>Corpse registers an entity type of its own, so a server running it has already given up
 * vanilla clients. Nothing here costs our own promise anything: we send no packets, and a server
 * without Corpse never reaches past {@link #available()}.
 */
public final class CorpseMod {

    private static final Logger LOG = LogUtils.getLogger();

    /** Corpse's own sizes. A body whose lists are any other length breaks its container screen. */
    private static final int MAIN_SLOTS = 36;
    private static final int ARMOUR_SLOTS = 4;

    private static boolean checked;
    private static boolean available;
    private static boolean warned;

    private static Class<?> bodyClass;
    private static Constructor<?> newBody;
    private static Method setDeath;
    private static Method setPlayerUuid;
    private static Method setCorpseName;
    private static Method setEquipment;
    private static Method getPlayerUuid;
    private static Method isEmpty;

    private static Constructor<?> newBuilder;
    private static Method playerName;
    private static Method mainInventory;
    private static Method armorInventory;
    private static Method offHandInventory;
    private static Method additionalItems;
    private static Method equipment;
    private static Method timestamp;
    private static Method posX;
    private static Method posY;
    private static Method posZ;
    private static Method dimension;
    private static Method build;

    private static Field serverConfig;
    private static Field lavaDamage;

    private CorpseMod() {}

    public static synchronized boolean available() {
        if (!checked) {
            checked = true;
            available = link();
            if (available) {
                LOG.info("ZombieMod: Corpse detected - a slain player zombie leaves a Corpse body "
                        + "holding what it carried.");
            }
        }
        return available;
    }

    private static boolean link() {
        if (!ModList.get().isLoaded("corpse")) {
            return false;
        }
        try {
            bodyClass = Class.forName("de.maxhenkel.corpse.entities.CorpseEntity");
            Class<?> death = Class.forName("de.maxhenkel.corpse.corelib.death.Death");
            Class<?> builder = Class.forName("de.maxhenkel.corpse.corelib.death.Death$Builder");

            newBody = bodyClass.getConstructor(Level.class);
            setDeath = bodyClass.getMethod("setDeath", death);
            setPlayerUuid = bodyClass.getMethod("setPlayerUuid", UUID.class);
            setCorpseName = bodyClass.getMethod("setCorpseName", String.class);
            setEquipment = bodyClass.getMethod("setEquipment", EnumMap.class);
            getPlayerUuid = bodyClass.getMethod("getPlayerUuid");
            isEmpty = bodyClass.getMethod("isEmpty");

            newBuilder = builder.getConstructor(UUID.class, UUID.class);
            playerName = builder.getMethod("playerName", String.class);
            mainInventory = builder.getMethod("mainInventory", NonNullList.class);
            armorInventory = builder.getMethod("armorInventory", NonNullList.class);
            offHandInventory = builder.getMethod("offHandInventory", NonNullList.class);
            additionalItems = builder.getMethod("additionalItems", NonNullList.class);
            equipment = builder.getMethod("equipment", EnumMap.class);
            timestamp = builder.getMethod("timestamp", long.class);
            posX = builder.getMethod("posX", double.class);
            posY = builder.getMethod("posY", double.class);
            posZ = builder.getMethod("posZ", double.class);
            dimension = builder.getMethod("dimension", String.class);
            build = builder.getMethod("build");

            serverConfig = Class.forName("de.maxhenkel.corpse.CorpseMod").getField("SERVER_CONFIG");
            lavaDamage = serverConfig.getType().getField("lavaDamage");
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOG.warn("ZombieMod: Corpse is present but did not match what was expected; a slain "
                    + "player zombie will drop its items as usual. {}", e.toString());
            return false;
        }
    }

    /**
     * Lay a Corpse body where this mob fell, holding these items. False means nothing was spawned
     * and nothing was consumed — the caller still owns the items and must drop them itself.
     *
     * <p>The carried list is flat; Corpse's is not. Armour goes to the armour slots and a shield to
     * the off hand, so that Corpse's transfer button re-equips them, and those same pieces are what
     * the body is drawn wearing. The rest fills the main inventory in order. That order is better
     * than it sounds: vanilla drops a dead player's inventory from slot 0 up, so the carried list
     * <em>is</em> the old inventory with the gaps closed, and the hotbar comes back as the hotbar.
     * Anything past 36 goes to Corpse's overflow page, which is unbounded.
     *
     * @param id the death's id on Corpse's side. Pass the ledger id, so the two records of one
     *     death can be matched up by whoever is looking at both
     */
    public static boolean layBody(ServerLevel level, Mob fallen, UUID id, UUID player, String name,
            List<ItemStack> items) {
        if (!available()) {
            return false;
        }
        try {
            NonNullList<ItemStack> main = NonNullList.withSize(MAIN_SLOTS, ItemStack.EMPTY);
            NonNullList<ItemStack> armour = NonNullList.withSize(ARMOUR_SLOTS, ItemStack.EMPTY);
            NonNullList<ItemStack> offHand = NonNullList.withSize(1, ItemStack.EMPTY);
            NonNullList<ItemStack> overflow = NonNullList.create();
            EnumMap<EquipmentSlot, ItemStack> worn = new EnumMap<>(EquipmentSlot.class);

            int next = 0;
            for (ItemStack stack : items) {
                if (stack.isEmpty()) {
                    continue;
                }
                EquipmentSlot slot = fallen.getEquipmentSlotForItem(stack);
                if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR
                        && armour.get(slot.getIndex()).isEmpty()) {
                    armour.set(slot.getIndex(), stack);
                    worn.put(slot, stack.copy());
                } else if (slot == EquipmentSlot.OFFHAND && offHand.get(0).isEmpty()) {
                    offHand.set(0, stack);
                    worn.put(slot, stack.copy());
                } else if (next < MAIN_SLOTS) {
                    main.set(next++, stack);
                } else {
                    overflow.add(stack);
                }
            }

            Object b = newBuilder.newInstance(player, id);
            playerName.invoke(b, name);
            mainInventory.invoke(b, main);
            armorInventory.invoke(b, armour);
            offHandInventory.invoke(b, offHand);
            additionalItems.invoke(b, overflow);
            equipment.invoke(b, worn);
            timestamp.invoke(b, System.currentTimeMillis());
            posX.invoke(b, fallen.getX());
            posY.invoke(b, fallen.getY());
            posZ.invoke(b, fallen.getZ());
            dimension.invoke(b, level.dimension().identifier().toString());
            Object death = build.invoke(b);

            // By hand rather than through createFromDeath, which wants the Player for its level
            // and facing. Ours may be logged off, or alive in another dimension entirely.
            Entity body = (Entity) newBody.newInstance(level);
            setDeath.invoke(body, death);
            setPlayerUuid.invoke(body, player);
            setCorpseName.invoke(body, name);
            setEquipment.invoke(body, worn);
            body.setPos(fallen.getX(), Math.max(fallen.getY(), level.getMinY()), fallen.getZ());
            body.setYRot(fallen.getYRot());
            return level.addFreshEntity(body);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail("laying a Corpse body", e);
            return false;
        }
    }

    /**
     * Whether this entity is a Corpse body for that player with nothing in it — the one Corpse
     * leaves at the death spot after we have already taken the drops.
     *
     * <p>Empty is part of the question on purpose. If anything did reach that body — another mod
     * adding to the drops after us — it is holding somebody's items, and removing it would be the
     * exact loss this whole feature exists to prevent.
     */
    public static boolean isEmptyBodyOf(Entity entity, UUID player) {
        if (!available() || !bodyClass.isInstance(entity)) {
            return false;
        }
        try {
            return player.equals(getPlayerUuid.invoke(entity)) && (boolean) isEmpty.invoke(entity);
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail("reading a Corpse body", e);
            return false;
        }
    }

    /**
     * Whether a body lying in lava or fire keeps its contents. Corpse's {@code lava_damage}, which
     * ships false: a body is otherwise invulnerable, and floats.
     *
     * <p>Answers "no" when it cannot tell. The caller is deciding whether to call an inventory
     * recovered, and the wrong "yes" closes a ledger entry for items that burned.
     */
    public static boolean bodiesSurviveFire() {
        if (!available()) {
            return false;
        }
        try {
            Object config = serverConfig.get(null);
            return !((ModConfigSpec.BooleanValue) lavaDamage.get(config)).get();
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail("reading Corpse's lava_damage setting", e);
            return false;
        }
    }

    /** Say so once. Every caller has a fallback that is simply how the mod behaved before. */
    private static void fail(String what, Throwable e) {
        if (!warned) {
            warned = true;
            LOG.warn("ZombieMod: {} failed; player zombies fall back to dropping items.", what, e);
        }
    }
}
