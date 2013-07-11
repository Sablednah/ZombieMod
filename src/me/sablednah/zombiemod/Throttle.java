package me.sablednah.zombiemod;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
import org.bukkit.util.Vector;
/**
 * Redundant code - used to modify speed
 * @author sable
 *
 */
public class Throttle implements Runnable {
	public ZombieMod plugin;

	public Throttle(ZombieMod p) {
		this.plugin=p;
	}

	@Override
	public void run() {
		// change speeds
		for (World w : plugin.getServer().getWorlds()) {
			for(Entity e : w.getEntitiesByClass(Zombie.class)) {
				if (((Zombie) e).getNoDamageTicks()==0) {// don't trigger during knockback
					PutredineImmortui z = null;
					z = Utils.getZombie(e);	
					if (z != null) {
						// speed modifiers.
						if (z.speed != 1 || z.potions.contains(ZombieMod.speedPotion)) {
							Vector oldspeed = e.getVelocity();
							Vector newspeed = oldspeed.multiply(z.speed);

							if (z.potions.contains(ZombieMod.speedPotion)) {
								newspeed.setY(e.getVelocity().getY()*1.5); // jumper speed amplifying jumping!
							} else {
								newspeed.setY(e.getVelocity().getY()); // stops speed amplifying jumping!
							}
							e.setVelocity(newspeed);
							oldspeed = null;
							newspeed = null;
						}
					}
				}
			}
		}
	}
}

