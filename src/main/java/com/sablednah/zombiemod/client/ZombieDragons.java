package com.sablednah.zombiemod.client;

import com.sablednah.zombiemod.ZombieMod;
import com.sablednah.zombiemod.platform.Types;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/**
 * Which dragons are the Zombie Dragon, as far as this client has been told, and what to draw them with.
 *
 * <p>Kept by network id, which is all {@code ZombieDragonPayload} carries and all a renderer has to
 * hand. Ids are per level and get reused, so one is forgotten when its entity leaves and the whole set
 * when the player disconnects; the server re-sends to anyone who starts tracking a risen dragon.
 *
 * <p><b>The art can be replaced without touching this jar.</b> A resource pack (or Threadwork, for the
 * ZARP pack) that provides {@code threadwork:textures/entity/zombie_dragon_rot.png} or
 * {@code ..._eyes.png} is used in preference to ours. Both are 256x256 on the vanilla dragon's UV map.
 * The rot is drawn translucent over the vanilla dragon, so it carries the colour - the vanilla skin
 * underneath is nearly black. The eyes replace vanilla's and glow.
 */
public final class ZombieDragons {

    private static final IntSet ZOMBIES = new IntOpenHashSet();

    private static final Identifier OWN_ROT = Identifier.fromNamespaceAndPath(ZombieMod.MOD_ID,
            "textures/entity/zombie_dragon_rot.png");
    private static final Identifier OWN_EYES = Identifier.fromNamespaceAndPath(ZombieMod.MOD_ID,
            "textures/entity/zombie_dragon_eyes.png");
    private static final Identifier PACK_ROT = Identifier.fromNamespaceAndPath("threadwork",
            "textures/entity/zombie_dragon_rot.png");
    private static final Identifier PACK_EYES = Identifier.fromNamespaceAndPath("threadwork",
            "textures/entity/zombie_dragon_eyes.png");

    private static Identifier rot;
    private static Identifier eyes;

    private ZombieDragons() {}

    static void init(IEventBus modBus) {
        modBus.addListener(ZombieDragons::onRegisterRenderers);
        NeoForge.EVENT_BUS.addListener(ZombieDragons::onLogout);
        NeoForge.EVENT_BUS.addListener(ZombieDragons::onLeave);
    }

    @SuppressWarnings("unchecked")
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Replaces vanilla's dragon renderer outright; every dragon that is not ours is drawn by
        // vanilla's own code through super, untouched.
        event.registerEntityRenderer((EntityType<EnderDragon>) Types.enderDragon(), ZombieDragonRenderer::new);
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ZOMBIES.clear();
        // Looked up again next time, so a resource pack swapped between sessions is noticed.
        rot = null;
        eyes = null;
    }

    private static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            ZOMBIES.remove(event.getEntity().getId());
        }
    }

    public static void mark(int entityId) {
        ZOMBIES.add(entityId);
    }

    public static boolean isZombie(int entityId) {
        return ZOMBIES.contains(entityId);
    }

    static Identifier rot() {
        if (rot == null) {
            rot = present(PACK_ROT) ? PACK_ROT : OWN_ROT;
        }
        return rot;
    }

    static Identifier eyes() {
        if (eyes == null) {
            eyes = present(PACK_EYES) ? PACK_EYES : OWN_EYES;
        }
        return eyes;
    }

    private static boolean present(Identifier texture) {
        return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
    }
}
