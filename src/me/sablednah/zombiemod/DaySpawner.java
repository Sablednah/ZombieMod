package me.sablednah.zombiemod;

import java.util.Random;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public class DaySpawner implements Runnable {
	public ZombieMod plugin; 

	public DaySpawner(ZombieMod p) {
		this.plugin = p;
	}

	public void run() {
		for (World zombieWorld : plugin.getServer().getWorlds()) {
			for (Chunk chunk : zombieWorld.getLoadedChunks()) {
				int zombieCount = 0;
				for (Entity e : chunk.getEntities()) {
					if ((e instanceof Zombie)) { zombieCount++; }
				}
				if (zombieCount < ZombieMod.chunklimit) {
					int probability = ZombieMod.zombiespawnration;
					Random rand = new Random();
					int x = rand.nextInt(16);
					int z = rand.nextInt(16);
					if (rand.nextInt(100) + 1 < probability) { 
						continue;
					}
					int y = 6;
					do { y++; if (chunk.getBlock(x, y, z).getType() == Material.BEDROCK) break;  }
					while (y < chunk.getWorld().getMaxHeight());
					for (; y < chunk.getWorld().getMaxHeight(); y++) {
						if ((chunk.getBlock(x, y, z).getRelative(BlockFace.DOWN).getLightFromBlocks() < 8) && (chunk.getBlock(x, y, z).getType() == Material.AIR) && (chunk.getBlock(x, y, z).getRelative(BlockFace.UP).getType() == Material.AIR)) {
							for (int r = 0; r < ZombieMod.spawnmultiplier; r++) {

								net.minecraft.server.World mcWorld = ((CraftWorld) zombieWorld).getHandle();

								PutredineImmortui zomb = new PutredineImmortui(plugin);
								if (ZombieMod.debugMode) { ZombieMod.logger.info("[" + ZombieMod.myName + "] " + zomb.commonName +" spawned via dayspawner"); }
								ZombieType newzomb = new ZombieType(mcWorld);
								newzomb.setPosition(x,y,z);
								//mcWorld.removeEntity((net.minecraft.server.EntityZombie) mcEntity);  //better but causes errors.
								mcWorld.addEntity(newzomb, SpawnReason.CUSTOM);

								//zombieWorld.spawnCreature(chunk.getBlock(x, y, z).getLocation(), EntityType.ZOMBIE);
							}
							y = zombieWorld.getMaxHeight();
						}
					}
				}
			}
		}
	}
}