package me.sablednah.zombiemod;

import java.util.Random;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
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
				int probability = ZombieMod.zombiespawnration;
				Random rand = new Random();

				if (rand.nextInt(100) + 1 < probability) { // only run if < probability.

					int zombieCount = 0;
					for (Entity e : chunk.getEntities()) {
						if ((e instanceof Zombie)) { zombieCount++; }
					}
					if (zombieCount < ZombieMod.chunklimit) {
						int x = rand.nextInt(8)+4;
						int z = rand.nextInt(8)+4;
						// ok this would spawn on the top
						//int y = zombieWorld.getHighestBlockYAt(x, z) + 1;
						
						// somewhere around the ground level ground
						// Natural vanilla spawn routine will produce zombies in caves / dark areas.
						int y = 65; 
						//find safe block
						Block blk = Utils.getNearestEmptySpace(zombieWorld.getBlockAt(x,y,z),4);// look for 2 air spaces in radius
						if (blk != null && blk.getLightFromBlocks()<8) { // must be air pocket and no torches nearby (dislike fire?)
							x=blk.getX();
							y=blk.getY();
							z=blk.getZ();
							for (int r = 0; r < ZombieMod.spawnmultiplier; r++) {
								net.minecraft.server.World mcWorld = ((CraftWorld) zombieWorld).getHandle();
								PutredineImmortui zomb = new PutredineImmortui(plugin);
								if (ZombieMod.debugMode) { ZombieMod.logger.info("[" + ZombieMod.myName + "] " + zomb.commonName +" spawned via dayspawner"); }
								ZombieType newzomb = new ZombieType(mcWorld);
								newzomb.setPosition(x,y,z);
								mcWorld.addEntity(newzomb, SpawnReason.CUSTOM);
							}							
						}
					}
				}
			}
		}
	}
}