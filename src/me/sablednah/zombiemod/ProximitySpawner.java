package me.sablednah.zombiemod;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;

public class ProximitySpawner implements Runnable {
	public ZombieMod	plugin;

	public ProximitySpawner(ZombieMod p) {
		this.plugin = p;
	}

	public void run() {
		if (ZombieMod.proximityspawner) {

			for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
				int probability = ZombieMod.zombiespawnration;
				Random rand = new Random();
				if (ZombieMod.debugMode) {
					ZombieMod.logger.info("[" + ZombieMod.myName + "] Begin Proximity chek for " + onlinePlayer.getName());
				}
				if (rand.nextInt(100) + 1 < probability) { // only run if < probability.
					if (ZombieMod.debugMode) {
						ZombieMod.logger.info("[" + ZombieMod.myName + "] Random probability passed");
					}
					Location l = onlinePlayer.getLocation();
					World zombieWorld = l.getWorld();

					if (!safeCheck(l)) {// if not in safe area

						if (ZombieMod.debugMode) {
							ZombieMod.logger.info("[" + ZombieMod.myName + "] Player safe zone check passed");
						}

						// ok player is NOT in a safe area.
						for (int cnt = 0; cnt < ZombieMod.spawnmultiplier; cnt++) {
							Random r = new Random();
							int ahead = r.nextInt(100) + 1;
							int randomoffset = (r.nextInt(120) + 1) - 60;

							if (ahead > 50) {
								randomoffset = (randomoffset + 180);
							}

							randomoffset = randomoffset + 90;

							Location cloneloc = l.clone();
							float yaw = cloneloc.getYaw();
							yaw = yaw + randomoffset;
							cloneloc.setYaw(yaw);

							double newX, newY, newZ;
							int offset;

							newX = cloneloc.getX();
							newZ = cloneloc.getZ();
							newY = cloneloc.getY();

							int magnitude = 24 + (r.nextInt(12) - 7);
							double direction = yaw;

							if (direction < 0) {
								direction += 360;
							}
							if (direction > 359) {
								direction -= 360;
							}

							direction = Math.toRadians(direction);

							double xOffset = (Math.cos(direction)) * magnitude;
							double zOffset = (Math.sin(direction)) * magnitude;

							newX = newX + xOffset;
							newZ = newZ - zOffset;

							/*
							 * String ord = (Utils.ordinal(cloneloc)); // ZombieMod.logger.info("[" + ZombieMod.myName +
							 * "] Proximity spawn: " + ord);
							 * 
							 * if (ord.contains("North")) { newX = newX + 12; } if (ord.contains("South")) { newX = newX
							 * - 12; } if (ord.contains("East")) { newZ = newZ + 12; } if (ord.contains("West")) { newZ
							 * = newZ - 12; }
							 * 
							 * offset = r.nextInt(15) - 8; newX = newX + offset + .5; offset = r.nextInt(15) - 8; newZ =
							 * newZ + offset + .5;
							 */
							offset = r.nextInt(7) - 2;
							newY = newY + offset;

							Location newLoc = new Location(zombieWorld, newX, newY, newZ);

							if (ZombieMod.debugMode) {
								ZombieMod.logger.info("[" + ZombieMod.myName + "] new loc found x:" + newX + ", y:" + newY + ", z:" + newZ);
							}

							int zombieCount = 0;
							for (Entity e : newLoc.getChunk().getEntities()) {
								if ((e instanceof Zombie)) {
									zombieCount++;
								}
							}
							if (zombieCount < ZombieMod.chunklimit) {

								Block blk = zombieWorld.getBlockAt(newLoc);
								if (blk != null) {
									Block safeNewBlock = Utils.getNearestEmptySpace(blk, 8);
									Location safeNewLoc = null;
									if (safeNewBlock != null) {
										if (ZombieMod.debugMode) {
											ZombieMod.logger.info("[" + ZombieMod.myName + "] setting safeNewLoc");
										}
										safeNewLoc = safeNewBlock.getLocation();
									} else {
										ZombieMod.logger.info("[" + ZombieMod.myName + "] safeNewBlock is null");
									}
									if (safeNewLoc != null) {
										if (safeNewLoc.getY() > zombieWorld.getHighestBlockYAt(safeNewLoc)) {
											newY = zombieWorld.getHighestBlockYAt(safeNewLoc);
											safeNewLoc.setY(newY);
										}
										if (safeCheck(safeNewLoc)) {
											if (ZombieMod.debugMode) {
												ZombieMod.logger.info("[" + ZombieMod.myName + "] safe: spawn point denied.");
											}
										} else {
											if (ZombieMod.debugMode) {
												ZombieMod.logger.info("[" + ZombieMod.myName + "] new loc is safe spawning ");
											}
											net.minecraft.server.World mcWorld = ((CraftWorld) zombieWorld).getHandle();
											PutredineImmortui zomb = new PutredineImmortui(plugin);
											if (ZombieMod.debugMode) {
												ZombieMod.logger.info("[" + ZombieMod.myName + "] " + zomb.commonName + " spawned via proximity");
											}
											ZombieType newzomb = new ZombieType(mcWorld, zomb);
											newzomb.setPosition(newX, newY, newZ);
											mcWorld.addEntity(newzomb, SpawnReason.CUSTOM);
										}
									} else {
										if (ZombieMod.debugMode) {
											ZombieMod.logger.info("[" + ZombieMod.myName + "] safeNewLoc is null");
										}
									}
								} else {
									if (ZombieMod.debugMode) {
										ZombieMod.logger.info("[" + ZombieMod.myName + "] blk is null");
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public boolean safeCheck(Location l) {

		if (ZombieMod.hasFactions) {
			// P f = (P) plugin.getServer().getPluginManager().getPlugin("Factions");
			// Faction fact = Board.getFactionAt(l);
			FLocation fl = new FLocation(l);
			Faction fact = Board.getFactionAt(fl);

			String factName = fact.getTag();

			factName = factName.toLowerCase();
			factName = ChatColor.stripColor(factName);

			if (!factName.equals(ZombieMod.factionsWildName)) {
				if (ZombieMod.debugMode) {
					ZombieMod.logger.info("[" + ZombieMod.myName + "] factName: " + factName + " denied (" + ZombieMod.factionsWildName + ")");
				}
				return true;
			}
		} else {
			Location spawn = l.getWorld().getSpawnLocation();
			double dist = spawn.distance(l);
			if (dist < 100) {
				return true;
			}
		}
		return false;

	}
}