package me.sablednah.zombiemod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

import me.daddychurchill.CityWorld.CityWorld;
import me.daddychurchill.CityWorld.CityWorldAPI;
import me.sablednah.legendquest.Main;
import me.sablednah.legendquest.experience.SetExp;
import me.sablednah.legendquest.playercharacters.PC;

import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.massivecore.ps.PS;

public class ProximitySystems implements Runnable {
	public ZombieMod	plugin;
	private boolean		debugMe	= false;

	private static int MAX_LEVEL = 50;
	
	public ProximitySystems(ZombieMod p) {
		this.plugin = p;
	}

	public void run() {

		for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
			// firstly - check for corpses whilst we're going through all players anyways.

			Location l = onlinePlayer.getLocation();
			Chunk c = l.getChunk();
			Utils.spawnCorpsesInChunk(c);

			
			if (onlinePlayer.getGameMode() == GameMode.CREATIVE) { continue; } 
			if (l.getWorld().getName().equalsIgnoreCase("ZARPTIME")) { continue; }

			// now spawn zombies!!!
			if (ZombieMod.proximityspawner) {
			        int level = MAX_LEVEL;
			        
			        if (ZombieMod.hasLegendQuest) {
			            Main legendQuest = (Main)plugin.getServer().getPluginManager().getPlugin("LegendQuest");
			            PC pc = legendQuest.players.getPC(onlinePlayer);
			            level = (SetExp.getLevelOfXpAmount(pc.currentXP)) / 3;
			            if (level<1) {level=1;}
//			            System.out.print("Level:"+level);
			        }

				int probability = ZombieMod.zombiespawnratio;
				probability = (int) (probability - (25 - (level / 2.0))) ;
				
				Random rand = new Random();
				if (ZombieMod.debugMode) {
					ZombieMod.logger.info("[" + this.plugin.myName + "] Begin Proximity chek for " + onlinePlayer.getName());
				}
				if (rand.nextInt(100) + 1 < probability) { // only run if < probability.
					if (debugMe && ZombieMod.debugMode) {
						ZombieMod.logger.info("[" + this.plugin.myName + "] Random probability passed");
					}

					World zombieWorld = l.getWorld();

					Faction fact = BoardColl.get().getFactionAt(PS.valueOf(l));

			            Faction nz = FactionColl.get().getNone();
//			            Faction sz = FactionColl.get().getSafezone();
			            Faction wz = FactionColl.get().getWarzone();

				        
					if ( fact == null || fact == nz || fact == wz ) {
						if (debugMe && ZombieMod.debugMode) {
							ZombieMod.logger.info("[" + this.plugin.myName + "] Player safe zone check passed");
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

							if (debugMe && ZombieMod.debugMode) {
								ZombieMod.logger.info("[" + this.plugin.myName + "] new loc found x:" + newX + ", y:" + newY + ", z:" + newZ);
							}

							int zombieCount = 0;
							for (Entity e : newLoc.getChunk().getEntities()) {
								if ((e instanceof Zombie || e instanceof Giant)) {
									zombieCount++;
								}
							}
							if (zombieCount < ZombieMod.chunklimit) {
							        Boolean sewer = false;
								Block blk = zombieWorld.getBlockAt(newLoc);
								if (blk != null) {
									Block safeNewBlock = Utils.getNearestEmptySpace(blk, 8);
									Location safeNewLoc = null;
									if (safeNewBlock != null) {
										if (debugMe && ZombieMod.debugMode) {
											ZombieMod.logger.info("[" + this.plugin.myName + "] setting safeNewLoc");
										}
										safeNewLoc = safeNewBlock.getLocation();
									} else {
										if (debugMe && ZombieMod.debugMode) {
											ZombieMod.logger.info("[" + this.plugin.myName + "] safeNewBlock is null");
										}
									}
									if (safeNewLoc != null) {
										if (safeNewLoc.getY() > zombieWorld.getHighestBlockYAt(safeNewLoc)) {
											newY = zombieWorld.getHighestBlockYAt(safeNewLoc);
											safeNewLoc.setY(newY);
										}
										if (Utils.isSafe(safeNewLoc)) {
											if (debugMe && ZombieMod.debugMode) {
												ZombieMod.logger.info("[" + this.plugin.myName + "] safe: spawn point denied.");
											}
										} else {
											if (debugMe && ZombieMod.debugMode) {
												ZombieMod.logger.info("[" + this.plugin.myName + "] new loc is safe spawning ");
											}
											PutredineImmortui zomb = null;
											if (ZombieMod.hasCityWorld) {
												CityWorld cw = (CityWorld) plugin.getServer().getPluginManager().getPlugin("CityWorld");
												CityWorldAPI cwAPI = cw.getAPI(cw);
												HashMap<String, String> info = cwAPI.getFullInfo(safeNewLoc.getChunk());
												
												if (info!= null && info.containsKey("lot")) {
												    if (info.get("lot").equalsIgnoreCase("ROAD")) {
												        if (safeNewLoc.getBlockY() < 64 && safeNewLoc.getBlockY() > 54 ) {
												            //valid "sewer" like location
												            sewer = true;
												        }
												    }
												    
												}
												
												if (info!= null && info.containsKey("schematic")) {
													// look through genus for schematic matches...
													ArrayList<Pair<Integer, String>> freq = new ArrayList<Pair<Integer, String>>();
													Iterator<Entry<String, Config>> confs = plugin.genera.configs.entrySet().iterator();
													while (confs.hasNext()) {
														Entry<String, Config> element = confs.next();
														Config testconf = element.getValue();
														if (testconf.schematics != null && testconf.schematics.contains(info.get("schematic"))) {
//															System.out.print(element.getKey());
//															System.out.print(testconf.altfrequency);
															freq.add(new Pair<Integer, String>(testconf.altfrequency, element.getKey()));
														}
													}
													if (freq.size() > 0) {
														WeightedProbMap<String> newmap = new WeightedProbMap<String>(freq);
														String thisgenera = newmap.nextElt();
														zomb = new PutredineImmortui(plugin, thisgenera);
														newmap = null;
													}
												}
												if (info!= null && info.containsKey("lotclass")) {
													// look through genus for schematic matches...
													ArrayList<Pair<Integer, String>> freq = new ArrayList<Pair<Integer, String>>();
													Iterator<Entry<String, Config>> confs = plugin.genera.configs.entrySet().iterator();
													while (confs.hasNext()) {
														Entry<String, Config> element = confs.next();
														Config testconf = element.getValue();
														if (testconf.schematics != null && testconf.schematics.contains(info.get("lotclass"))) {
//															System.out.print(element.getKey());
//															System.out.print(testconf.altfrequency);
															freq.add(new Pair<Integer, String>(testconf.altfrequency, element.getKey()));
														}
													}
													if (freq.size() > 0) {
														WeightedProbMap<String> newmap = new WeightedProbMap<String>(freq);
														String thisgenera = newmap.nextElt();
														zomb = new PutredineImmortui(plugin, thisgenera);
														newmap = null;
													}
												}
												
												if (zomb == null) {
													if (info != null && info.containsKey("context")) {
														ArrayList<Pair<Integer, String>> freq = new ArrayList<Pair<Integer, String>>();
														Iterator<Entry<String, Config>> confs = plugin.genera.configs.entrySet().iterator();
														
														String cxt = info.get("context");
														boolean beach = false;
														
														if (safeNewLoc.getBlockY()>50) {
															if (safeNewLoc.getBlockX()>-3600 && safeNewLoc.getBlockX()<-3560) {
																if (safeNewLoc.getBlockZ()>-2041 && safeNewLoc.getBlockZ()<-1900) {
																	cxt = "NATURE";
																	beach=true;
																}
															}
														}
														while (confs.hasNext()) {
															Entry<String, Config> element = confs.next();
															Config testconf = element.getValue();
															if (testconf.contexts != null && testconf.contexts.contains(cxt)) {
																if (testconf.size > 2.9F && beach) { 
																	freq.add(new Pair<Integer, String>(testconf.altfrequency+200, element.getKey()));
//																	if (beach) { System.out.print(element.getKey() + " - " + (testconf.altfrequency+200)); }																	
																} else {
//																	if (beach) { System.out.print(element.getKey() + " - " + testconf.altfrequency); }
																	freq.add(new Pair<Integer, String>(testconf.altfrequency, element.getKey()));
																}
															}
														}
														if (freq.size() > 0) {
															WeightedProbMap<String> newmap = new WeightedProbMap<String>(freq);
															String thisgenera = newmap.nextElt();
															zomb = new PutredineImmortui(plugin, thisgenera);
															newmap = null;
														}
													}
												}
											}
											
											if (sewer && Math.random()>0.90D) {
											    if (Math.random()>0.3D) {
											        safeNewLoc.getWorld().spawnEntity(safeNewLoc, EntityType.OCELOT);
											    } else {
											        double packsize = Math.random();
											        safeNewLoc.getWorld().spawnEntity(safeNewLoc, EntityType.WOLF);
											        if (packsize<0.6D) {
	                                                                                            safeNewLoc.getWorld().spawnEntity(safeNewLoc, EntityType.WOLF);											            
											        }
                                                                                                if (packsize<0.4D) {
                                                                                                    safeNewLoc.getWorld().spawnEntity(safeNewLoc, EntityType.WOLF);                                                                                                 
                                                                                                }
                                                                                                if (packsize<0.2D) {
                                                                                                    safeNewLoc.getWorld().spawnEntity(safeNewLoc, EntityType.WOLF);                                                                                                 
                                                                                                }
											    }
											    
											} else {
											
											if (zomb == null) {
												zomb = new PutredineImmortui(plugin);
											}

											if (ZombieMod.debugMode) {
												ZombieMod.logger.info("[" + this.plugin.myName + "] " + zomb.commonName + " spawned via proximity");
											}
											Utils.spawnZombie(zomb, safeNewLoc, plugin, level);
										}
										}
									} else {
										if (debugMe && ZombieMod.debugMode) {
											ZombieMod.logger.info("[" + this.plugin.myName + "] safeNewLoc is null");
										}
									}
								} else {
									if (debugMe && ZombieMod.debugMode) {
										ZombieMod.logger.info("[" + this.plugin.myName + "] blk is null");
									}
								}
							}
						}
					}
				}
			}
		}
	}
}