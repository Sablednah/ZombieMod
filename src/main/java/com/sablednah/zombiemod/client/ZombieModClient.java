package com.sablednah.zombiemod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sablednah.zombiemod.ZombieMod;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The client half, behind its own {@code dist = Dist.CLIENT} entrypoint.
 *
 * <p>Why this lives in the same jar as the server: two jars have to be kept in step by whoever
 * installs them, forever, and the failure is a quiet one — a screen drawing stale nonsense rather
 * than an error. One jar makes the mismatch impossible.
 *
 * <p>Nothing is given up. A dedicated server never constructs this class, and the promise that
 * matters was never "this jar has no client code" — it is <b>a player does not need this mod to
 * join</b>, which the optional payload registration and the guarded sends in
 * {@code Net} keep.
 */
@Mod(value = ZombieMod.MOD_ID, dist = Dist.CLIENT)
public final class ZombieModClient {

    private static final KeyMapping OPEN_DEX = new KeyMapping(
            "key.zombiemod.dex", InputConstants.Type.KEYSYM, InputConstants.KEY_J,
            KeyMapping.Category.MISC);

    public ZombieModClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::onRegisterKeys);
        NeoForge.EVENT_BUS.register(this);
    }

    private void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_DEX);
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return;
        }
        while (OPEN_DEX.consumeClick()) {
            mc.setScreen(new DexScreen());
        }
    }
}
