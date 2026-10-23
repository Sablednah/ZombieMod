package com.sablednah.zombiemod.client.journeymap;

import com.sablednah.zombiemod.client.GenusSightings;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.entity.WrappedEntity;
import journeymap.api.v2.client.event.EntityRadarUpdateEvent;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.ClientEventRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/**
 * JourneyMap: a genus shows on the map with its own face.
 *
 * <p>The one place {@code journeymap.*} may be imported. JourneyMap finds this by its annotation and
 * only when JourneyMap is installed, so without it nothing here loads. The API it uses
 * ({@code ENTITY_RADAR_UPDATE_EVENT}, {@code WrappedEntity}) is identical in all four API builds
 * this mod compiles against, so the file is shared by every branch.
 *
 * <p>Only genera are touched - mobs the server has said are one (see {@code GenusSightings}).
 * Everything else, a name-tagged cow included, is left exactly as JourneyMap draws it.
 */
@JourneyMapPlugin(apiVersion = "2.0.0")
public final class ZombieModJourneyMapPlugin implements IClientPlugin {

    @Override
    public String getModId() {
        return "zombiemod";
    }

    @Override
    public void initialize(IClientAPI api) {
        ClientEventRegistry.ENTITY_RADAR_UPDATE_EVENT.subscribe("zombiemod", this::onRadar);
    }

    private void onRadar(EntityRadarUpdateEvent event) {
        if (event.getType() != EntityRadarUpdateEvent.EntityType.MOB) {
            return;
        }
        WrappedEntity wrapped = event.getWrappedEntity();
        if (wrapped.getEntityRef() == null || !(wrapped.getEntityRef().get() instanceof Mob mob)) {
            return;
        }
        Identifier genus = GenusSightings.of(mob.getId());
        if (genus == null) {
            return;
        }
        Identifier face = MapFaces.of(mob);
        if (face != null) {
            wrapped.setEntityIconLocation(face);
        }
        // The icon only. JourneyMap already labels a genus by the name it carries, and leaving
        // the name to it keeps its own show/hide setting for mob names working.
    }
}
