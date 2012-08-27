package me.sablednah.zombiemod;

import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

class Break implements Runnable {
	private Block b;
	private LivingEntity zombie;
	private Player player;
	public ZombieMod plugin;

	public Break(ZombieMod p, Block b, LivingEntity z, Player t) {
		this.b = b;
		this.zombie = z;
		this.player = t;
		this.plugin = p;
	}

	public void run() {
		if (!zombie.isDead()) {
			if (Utils.isNotCalled(this.b.getLocation(), ZombieMod.factionsSafeName)) {
				this.b.breakNaturally();
			}
			
			if ((((Zombie) zombie).getTarget() != null) && (((Zombie) zombie).getTarget().equals(player)))
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new BreakRunner(plugin, this.zombie, this.player), 20L);
		}
	}
}