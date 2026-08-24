package com.sablednah.zombiemod;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.neoforge.LootHandler;
import com.sablednah.zombiemod.neoforge.HordeDirector;
import com.sablednah.zombiemod.neoforge.PlayerZombies;
import com.sablednah.zombiemod.neoforge.ProximitySpawner;
import com.sablednah.zombiemod.neoforge.RitualHandler;
import com.sablednah.zombiemod.neoforge.ZombieModCommands;
import com.sablednah.zombiemod.neoforge.ZombieModEvents;

import org.slf4j.Logger;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * ZombieMod ReForged — datapack-defined zombie types with customisable AI.
 *
 * <p>A rewrite of the 2013 Bukkit plugin. The one structural decision worth knowing: this mod
 * registers <b>no entity types of its own</b>. A vanilla client cannot render an entity type it
 * has never heard of, and supporting unmodified clients is the point — so a "genus" is a set of
 * changes applied to an ordinary vanilla mob at the moment it spawns. Everything a player sees
 * (size, colour, name, behaviour) is expressed through things vanilla already knows how to draw.
 *
 * <p>Pleasingly, this is what the original did too, for the same reason: it replaced the vanilla
 * zombie <i>class</i> while keeping vanilla entity id 54, so clients saw a normal zombie. It just
 * needed reflection against obfuscated names to do it.
 */
@Mod(ZombieMod.MOD_ID)
public class ZombieMod {

    public static final String MOD_ID = "zombiemod";
    private static final Logger LOG = LogUtils.getLogger();

    public ZombieMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(ZombieModRegistries::register);
        modEventBus.addListener(com.sablednah.zombiemod.net.Net::register);
        modContainer.registerConfig(ModConfig.Type.SERVER, ZombieModConfig.SPEC);

        com.sablednah.zombiemod.core.ability.Convert.setRaiser(
                com.sablednah.zombiemod.neoforge.Conversions::raise);
        com.sablednah.zombiemod.core.ability.Abilities.Summon.setDresser((mob, genusId) ->
                mob.level().registryAccess().lookupOrThrow(ZombieModRegistries.GENUS)
                        .get(net.minecraft.resources.ResourceKey.create(ZombieModRegistries.GENUS, genusId))
                        .ifPresent(holder -> {
                            com.sablednah.zombiemod.neoforge.GenusApplier.assign(mob, holder);
                            com.sablednah.zombiemod.neoforge.GenusApplier.applyAi(mob, holder.value());
                        }));

        com.sablednah.zombiemod.compat.StandardsEconomy.install();

        NeoForge.EVENT_BUS.register(new ZombieModEvents());
        NeoForge.EVENT_BUS.register(new RitualHandler());
        NeoForge.EVENT_BUS.register(new LootHandler());
        NeoForge.EVENT_BUS.register(new PlayerZombies());
        NeoForge.EVENT_BUS.register(new ProximitySpawner());
        NeoForge.EVENT_BUS.register(new HordeDirector());
        NeoForge.EVENT_BUS.register(this);

        LOG.info("ZombieMod ReForged loaded - genera come from datapacks (data/<pack>/zombiemod/genus/).");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ZombieModCommands.register(event.getDispatcher());
    }
}
