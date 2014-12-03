package me.sablednah.zombiemod;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nitnelave.CreeperHeal.CreeperHandler;

class Break implements Runnable {

	private Block			b;
	private LivingEntity	zombie;
	public ZombieMod		plugin;

	public Break(ZombieMod p, Block b, LivingEntity z) {
		this.b = b;
		this.zombie = z;
		this.plugin = p;
	}

	public void run() {
		if (!zombie.isDead()) {
			if (!Utils.isSafe(this.b.getLocation())) {
				// System.out.print("break: " + b.toString());
				Boolean breakblock = true;
				PutredineImmortui zm = Utils.getZombie(zombie);
				if (zm != null) {
					if (zm.abilities != null && zm.abilities.contains("INFEST")) {
						breakblock = false;
						this.b.setType(Material.MONSTER_EGGS);
					}
				}
				if (breakblock) {
					// this.b.breakNaturally();
					if (ZombieMod.hasCreeperHeal) {
						if (plugin.allowedpermbreaks.contains(b.getType())) {
							this.b.breakNaturally();
						} else {
							CreeperHandler.recordBlock(b);
						}
					} else {
						this.b.breakNaturally();
					}
				}
			} else {
				// System.out.print("nobreak safe: " + b.toString());

			}
		}
	}
}