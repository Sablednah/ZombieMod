package com.sablednah.zombiemod.neoforge;

import com.mojang.logging.LogUtils;
import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.core.ability.Ability;
import com.sablednah.zombiemod.core.ability.Convert;
import com.sablednah.zombiemod.platform.Types;
import org.slf4j.Logger;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** TEMPORARY. Why does a Carrier not convert a sheep? Delete after reading. */
public final class ConvertProbe {

    private static final Logger LOG = LogUtils.getLogger();

    @SubscribeEvent
    public void onStarted(ServerStartedEvent event) {
        var level = event.getServer().overworld();
        var lookup = level.registryAccess().lookupOrThrow(ZombieModRegistries.GENUS);
        var carrier = lookup.get(ResourceKey.create(ZombieModRegistries.GENUS,
                Identifier.fromNamespaceAndPath("zombiemod", "carrier"))).orElse(null);
        if (carrier == null) {
            LOG.info("CONVPROBE no carrier genus");
            return;
        }
        Convert convert = null;
        for (Ability a : carrier.value().abilities()) {
            if (a instanceof Convert c) {
                convert = c;
            }
        }
        if (convert == null) {
            LOG.info("CONVPROBE carrier has no Convert ability");
            return;
        }

        var sheepType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getValue(Identifier.withDefaultNamespace("sheep"));
        if (!(sheepType.create(level, EntitySpawnReason.COMMAND) instanceof LivingEntity sheep)) {
            LOG.info("CONVPROBE could not make a sheep");
            return;
        }
        sheep.snapTo(0, 100, 0, 0, 0);

        LOG.info("CONVPROBE victims.contains(sheep) = {}",
                convert.victims().contains(sheep.getType().builtInRegistryHolder()));
        LOG.info("CONVPROBE Tags.is(sheep, UNDEAD) = {}  (must be false)",
                com.sablednah.zombiemod.platform.Tags.is(sheep.getType(), EntityTypeTags.UNDEAD));
        LOG.info("CONVPROBE accepts(sheep) = {}", convert.accepts(sheep));
        LOG.info("CONVPROBE undeadFormOf(sheep) = {}  zombie() = {}  same object = {}",
                Convert.undeadFormOf(sheep.getType()), Types.zombie(),
                Convert.undeadFormOf(sheep.getType()) == Types.zombie());

        // Now the real thing, with a real carrier as the killer.
        if (!(carrier.value().base().create(level, EntitySpawnReason.COMMAND) instanceof Mob killer)) {
            return;
        }
        killer.snapTo(0, 100, 0, 0, 0);
        GenusApplier.assign(killer, carrier);
        level.setChunkForced(0, 0, true);
        level.addFreshEntity(killer);
        level.addFreshEntity(sheep);
        boolean raised = Conversions.raise(level, killer, sheep, convert);
        LOG.info("CONVPROBE Conversions.raise(...) = {}   <-- the answer", raised);
    }
}
