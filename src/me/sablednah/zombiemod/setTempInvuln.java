package me.sablednah.zombiemod;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class setTempInvuln implements Runnable {
	public Entity e;
	public int d;
	
	public setTempInvuln(Entity entity, int damage) {
		e = entity;
		d = damage;
	}

	@Override
	public void run() {
		if (e != null) {
			Utils.setTempnvluln((LivingEntity) e,d);
		}

	}

}
