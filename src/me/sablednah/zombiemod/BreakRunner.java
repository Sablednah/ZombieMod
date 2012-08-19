package me.sablednah.zombiemod;

import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

class BreakRunner implements Runnable {
	private Player player;
	public LivingEntity zombie;
	public ZombieMod plugin;

	public BreakRunner(ZombieMod p, LivingEntity z, Player t) {
		this.zombie = z;
		this.player = t;
		this.plugin = p;
	}

	public void run() {
		for (int b = -1; b < 1; b++) {
			Block destroyBlock = this.zombie.getTargetBlock(null, 3).getRelative(0, b, 0);			
			if (plugin.allowedbreaks.contains(destroyBlock.getType())) {
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Break(plugin, destroyBlock, this.zombie, this.player), 20L);
				return;
			}
		}
	}
}
