package com.sablednah.zombiemod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sablednah.zombiemod.ZombieMod;
import com.sablednah.zombiemod.core.DexBook;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

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

    @SubscribeEvent
    public void onLoggingOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        DexState.clear();
    }

    /**
     * The book is the same book for everybody.
     *
     * <p>{@code /zm bestiary book} hands out an ordinary written book, and that is deliberate: a
     * vanilla player right-clicks it and reads the pages, and nothing here changes what the server
     * sent them. This handler only exists on a client that has the mod, so on that client the same
     * item opens the tome instead — one item, two readings, no second command.
     *
     * <p>This half only stops the <em>client</em> opening the book. The server has to be stopped
     * separately, in {@code ZombieModEvents}, because it opens the book by sending a packet rather
     * than by anything this side does.
     */
    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        // Fires on both sides in single-player, and a screen is a client-thread thing.
        // No dex means no ZombieMod on the other end - the server sends the whole roster on login,
        // so an empty one is not "met nothing", it is "nobody told us anything". Leave a book named
        // ZombieDex on somebody else's server alone and let it open as the book it is.
        if (!event.getLevel().isClientSide() || DexState.entries().isEmpty()
                || !DexBook.is(event.getItemStack())) {
            return;
        }
        Minecraft.getInstance().setScreen(new DexScreen());
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
