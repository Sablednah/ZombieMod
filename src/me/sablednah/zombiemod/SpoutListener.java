package me.sablednah.zombiemod;

import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.event.spout.SpoutCraftEnableEvent;
import org.getspout.spoutapi.player.EntitySkinType;
import org.getspout.spoutapi.player.FileManager;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SpoutListener implements Listener {
	ZombieMod plugin;
	public SpoutListener(ZombieMod zombieMod) {
		this.plugin=zombieMod;
	}

	@EventHandler
	public void onSpoutCraftEnableEvent(SpoutCraftEnableEvent event) {
		if (ZombieMod.debugMode) {
			SpoutPlayer p = event.getPlayer();
			FileManager fm = SpoutManager.getFileManager();
			Iterator<Entry<String, Config>> it = plugin.genera.configs.entrySet().iterator();
			while (it.hasNext()) {
				Config c = it.next().getValue();
				String skin = c.skin;
				if (skin != null) {
					fm.addToCache(plugin, skin);
				}
			}

			for(Entity e : p.getWorld().getEntitiesByClass(Zombie.class)) {
				PutredineImmortui z = null;
				z = Utils.getZombie(e);
				if (z != null) {
					if (z.species.equals("PlayerZombie")) {
						fm.addToCache(plugin, z.skin);
					}
					if (z.skin != null) {
						p.setEntitySkin((LivingEntity) e, z.skin, EntitySkinType.DEFAULT);
					}
				}
			}
		}
	}
}