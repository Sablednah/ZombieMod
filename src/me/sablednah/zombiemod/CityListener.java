package me.sablednah.zombiemod;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import me.daddychurchill.CityWorld.CityWorldEvent;
import me.daddychurchill.CityWorld.Plats.PlatLot.LotStyle;

public class CityListener implements Listener {

	public ZombieMod plugin;

	public CityListener(ZombieMod instance) {
		this.plugin=instance;
	}
	
	@EventHandler
	public void onCityWorldEvent(CityWorldEvent event) {
		String s;
		if (event.hasSchematic()) {
			s = "Scematic: " + event.getSchematicName();
			s = s + " | x: " + (event.getChunkX()*16) + ", z: " + (event.getChunkZ()*16);
			System.out.print(s);
		}
		
		LotStyle thisPlat = event.getPlatlot().style;
		
		switch (thisPlat) {
			case NATURE:
				if (!event.getContextName().toUpperCase().equals("NATURE")) {
					break;
				}
			case ROAD: //set roads and true nature as warzone
				Utils.setWarZone(event.getChunk());
				break;
			default:
				//all others - do nothing - yet.
		}
	}		
}
