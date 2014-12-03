package me.sablednah.zombiemod;

import net.minecraft.server.v1_7_R4.MathHelper;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import me.sablednah.zombiemod.Break;

class BreakRunner implements Runnable {
	public LivingEntity	zombie;
	public ZombieMod	plugin;

	public BreakRunner(ZombieMod p, LivingEntity z) {
		this.zombie = z;
		this.plugin = p;
	}

	public void run() {

		if (zombie != null && !zombie.isDead()) {
			if (((Creature) zombie).getTarget() != null && ((Creature) zombie).getTarget() instanceof Player) {
				Player player = (Player) ((Creature) zombie).getTarget();
				if (zombie.getLocation().getWorld().getName().equals(player.getLocation().getWorld().getName())) {
					double dist = zombie.getLocation().distanceSquared(player.getLocation());
					// System.out.print("dist: " + dist);
					if (dist > 1.1D && dist < 36.0D) {
						double speed = zombie.getVelocity().length();
						// System.out.print("speed: " + speed);
						if (speed < 0.09D) {
							for (int b = 0; b > -2; b--) {
								// @SuppressWarnings("deprecation")
								// z loc
								int x1 = MathHelper.floor(zombie.getLocation().getBlockX());
								int z1 = MathHelper.floor(zombie.getLocation().getBlockZ());

								// owner loc
								int x2 = MathHelper.floor(player.getLocation().getBlockX());
								int z2 = MathHelper.floor(player.getLocation().getBlockZ());

								int xDiff = x2 - x1;
								int zDiff = z2 - z1;
								// int xDiff = x1-x2;
								// int zDiff = z1-z2;

								// System.out.print("xDiff: " + xDiff);
								// System.out.print("zDiff: " + zDiff);

								double angle = Math.toDegrees(Math.atan2(xDiff, zDiff));
								if (angle < 0.0D) {
									angle += 360.0D;
								}

								BlockFace ytf = Utils.yawToFace((float) angle);

								Block destroyBlock = this.zombie.getLocation().getBlock().getRelative(ytf);
								if (plugin.allowedbreaks.contains(destroyBlock.getType())) {
									plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Break(plugin, destroyBlock, this.zombie), 10L);
								} else {
									destroyBlock = destroyBlock.getRelative(BlockFace.UP);
									if (plugin.allowedbreaks.contains(destroyBlock.getType())) {
										plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Break(plugin, destroyBlock, this.zombie), 10L);
									}/*
									 * else { destroyBlock = destroyBlock.getRelative(BlockFace.DOWN,2);
									 * System.out.print("checking break: " + destroyBlock.toString()); if
									 * (plugin.allowedbreaks.contains(destroyBlock.getType())) {
									 * System.out.print("aLLOWED break: " + destroyBlock.toString());
									 * plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new
									 * Break(plugin, destroyBlock, this.zombie, this.player), 20L); return; } }
									 */
								}
							}
						}
					}
				}
			}
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new BreakRunner(plugin, zombie), 60L);
		}
	}
}
