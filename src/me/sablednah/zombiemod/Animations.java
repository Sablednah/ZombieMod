package me.sablednah.zombiemod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.server.v1_8_R1.MathHelper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
//import org.bukkit.craftbukkit.v1_7_R4.entity.CraftEntity;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Golem;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Processes all repeating tasks that are NOT thread safe. This function is executed every second. It must be kept
 * lightweight to prevent lag.
 */
public class Animations implements Runnable {

	public ZombieMod	plugin;

	public Animations(ZombieMod p) {
		this.plugin = p;
	}

	@Override
	public void run() {
		// trigger animations
		ZombieMod.intervals++;

		for (World w : plugin.getServer().getWorlds()) {
			for (Entity e : w.getEntities()) {
				if (e.hasMetadata("NPC")) {
					continue;
				}
				if (!e.isDead()) {
					if (e instanceof Monster || e instanceof Horse || e instanceof Wolf || e instanceof Golem || e instanceof Ocelot) {
						Location l = e.getLocation();
						// spawn protector
						if (Utils.isSafe(l)) {
							if (e instanceof Horse || e instanceof Wolf || e instanceof Golem) {
								String nme = ((LivingEntity) e).getCustomName();
								if (nme != null) {
									String cleanname = Utils.cleanName(nme).toLowerCase();
									// if clean name is a zombie type - swapit!
									for (Config c : plugin.genera.configs.values()) {
										if (c.jockey != null && !c.jockey.isEmpty()) {
											String[] jInf = c.jockey.split("\\|");
											if (jInf.length > 1) {
												if (!jInf[1].isEmpty()) {
													if (jInf[1].toLowerCase().equals(cleanname)) {
														if (((LivingEntity) e).getPassenger() == null) {
															e.remove();
															break;
														}
													}
												}
											}
										}
									}
								}
							} else {

								// we#re not out of spawn here
								// what do we do with them then...
								// workout angle #tween them and spawn.. then push away
								double x1, x2, z1, z2, y1, y2, xDiff, zDiff, angle, distance, magnitude;

								Location spawn = ZombieMod.spawnLoc; // w.getSpawnLocation();

								x1 = l.getX();
								x2 = spawn.getX();
								z1 = l.getZ();
								z2 = spawn.getZ();
								y1 = l.getY();
								y2 = spawn.getY();

								x2 = (((int) x1) >> 4) * 16;
								z2 = (((int) z1) >> 4) * 16;
								x2 += x2 > 0 ? 8 : -8;
								z2 += z2 > 0 ? 8 : -8;

								xDiff = x2 - x1;
								zDiff = z2 - z1;

								// if (l.getWorld().getName().equals(spawn.getWorld().getName())) {
								// distance = spawn.distance(l);
								// } else {
								// distance = 8;
								// }
								distance = 8; // Math.sqrt(Math.pow(Math.abs(xDiff),2) +
												// Math.pow(Math.abs(zDiff),2));

								angle = (Math.atan2(xDiff, zDiff));
								// angle = Math.toDegrees(Math.atan2(xDiff, zDiff));
								// if (angle < 0.0D) {
								// angle += 360.0D;
								// }

								magnitude = distance;
								magnitude += 8;

								double xOffset = (Math.sin(angle)) * magnitude;
								double zOffset = (Math.cos(angle)) * magnitude;

								x1 -= xOffset;
								z1 -= zOffset;

								Location newLoc = new Location(w, x1, y1, z1);

								Location safeNewLoc = Utils.getNearestEmptyLoc(newLoc);
								if (safeNewLoc != null) {
									if (safeNewLoc.getY() > w.getHighestBlockYAt(safeNewLoc)) {
										y1 = w.getHighestBlockYAt(safeNewLoc);
										safeNewLoc.setY(y1);
									}

									if (e.isInsideVehicle()) {
										Entity v = e.getVehicle();
										v.eject();
										v.teleport(safeNewLoc, TeleportCause.PLUGIN);
										e.teleport(safeNewLoc.add(0.0D, 1.0D, 0.0D), TeleportCause.PLUGIN);
										v.setPassenger(e);
									} else {
										e.teleport(safeNewLoc, TeleportCause.PLUGIN);
									}

									// stop targeting (so enderzombies don't bounce!)
									Creature z = (Creature) e;
									z.setTarget(null);

									if (ZombieMod.debugMode) {
										ZombieMod.logger.info("[" + this.plugin.myName + "] ---------------------------------------");
										ZombieMod.logger.info("[" + this.plugin.myName + "] Moved zombie from x:" + x1 + " y:" + l.getY() + " z:" + z1);
										ZombieMod.logger.info("[" + this.plugin.myName + "] angle: " + Math.toDegrees(angle));
										ZombieMod.logger.info("[" + this.plugin.myName + "] distance :" + distance);
										ZombieMod.logger.info("[" + this.plugin.myName + "] magnitude :" + magnitude);
										ZombieMod.logger.info("[" + this.plugin.myName + "] Moved zombie to x:" + x2 + " y:" + y2 + " z:" + z2);
										ZombieMod.logger.info("[" + this.plugin.myName + "] spawnloc x:" + spawn.getX() + " y:" + spawn.getY() + " z:" + spawn.getZ());
										ZombieMod.logger.info("[" + this.plugin.myName + "] ---------------------------------------");
									}
								}
							}
						} // end of spwn protector

						if (e instanceof Horse || e instanceof Wolf || e instanceof Golem) {
							String cleanname = ((LivingEntity) e).getCustomName();
							cleanname = Utils.cleanName(cleanname);
							// if clean name is a zombie type - swapit!
							if (cleanname != null) {
								cleanname = cleanname.toLowerCase();
								for (Config c : plugin.genera.configs.values()) {
									if (c.jockey != null && !c.jockey.isEmpty()) {
										String[] jInf = c.jockey.split("\\|");
										if (jInf.length > 1) {
											if (!jInf[1].isEmpty()) {
												if (jInf[1].toLowerCase().equals(cleanname)) {
													if (((LivingEntity) e).getPassenger() == null) {
														if (e.hasMetadata("lostrider")) {
															int lostrider = e.getMetadata("lostrider").get(0).asInt();
															if ((lostrider + 30) < ZombieMod.intervals) {
																e.remove();
															}
														} else {
															e.setMetadata("lostrider", new FixedMetadataValue(plugin, ZombieMod.intervals));
														}
													}
												}
											}
										}
									}
								}
							}
						}

						PutredineImmortui z = null;
						if (e instanceof Monster) { // limit get zomb call to just monsters
							z = Utils.getZombie(e);
						}
						if (z != null && !e.isDead()) {
							if (z.species.equals("PlayerZombie")) {
								z.lastLoc = l;
								Chunk c = l.getChunk();
								String cid = c.getX() + "|" + c.getZ();
								z.cid = cid;
							}

							if (z.owner != null && ((Monster) e).getTarget() != null) {
								LivingEntity focus = ((Monster) e).getTarget();
								if (focus instanceof Player) {
									Player pFocus = (Player) focus;
									if (z.owner.toLowerCase().equals(pFocus.getName().toLowerCase())) {
										((Monster) e).setTarget(null);
									}
								}
							}

							// play effects
							if (z.effects != null) {
								for (Effect eff : z.effects) {
									// ZombieMod.logger.info("[" + plugin.myName + "] eff - " + eff
									// +" ["+ZombieMod.intervals+"] ");
									switch (eff) {
										case GHAST_SHRIEK:
											if (ZombieMod.intervals % 10 == 0) { // scream less frequently than
																					// every
																					// second!
												// e.getWorld().playEffect(l, eff, 0);
												if (Math.random() > 0.9) {
													if (Math.random() > 0.9) {
														e.getWorld().playSound(l, Sound.GHAST_SCREAM2, 1, 1);
													} else {
														e.getWorld().playSound(l, Sound.GHAST_SCREAM, 1, 1);
													}
												} else {
													e.getWorld().playSound(l, Sound.GHAST_MOAN, 1, 1);
												}
											}
											break;

										case ENDER_SIGNAL: // teleport - bamf
											e.getWorld().playEffect(l, eff, 0);
											if (ZombieMod.intervals % 5 == 0) {
												if (Math.random() > 0.75) {
													e.getWorld().playSound(l, Sound.ENDERMAN_IDLE, 1, 1);
												}
											}

											if (ZombieMod.intervals % 3 == 0) {
												if (Math.random() > 0.3) {
													LivingEntity targ = ((Monster) e).getTarget();
													if (targ != null) {
														if (targ instanceof LivingEntity) {
															if (targ.isDead() || (Utils.isSafe(targ.getLocation()))) {
																((Monster) e).setTarget(null);
																continue;
															}

															// bamf!!

															double x, y, zee;
															if (targ.getWorld() == e.getWorld()) {
																if (z.abilities != null && z.abilities.contains("BACKSTAB")) {
																	String ord = (Utils.ordinal(targ.getLocation()));
																	// ZombieMod.logger.info("[" + ZombieMod.myName
																	// +
																	// "] Facing!: "+ord);

																	x = targ.getLocation().getX();
																	zee = targ.getLocation().getZ();
																	y = targ.getLocation().getY();

																	if (ord.contains("North")) {
																		x = x + 5;
																	}
																	if (ord.contains("South")) {
																		x = x - 5;
																	}
																	if (ord.contains("East")) {
																		zee = zee + 5;
																	}
																	if (ord.contains("West")) {
																		zee = zee - 5;
																	}
																} else {
																	x = l.getX() + ((targ.getLocation().getX() - l.getX()) / 1.1);
																	zee = l.getZ() + ((targ.getLocation().getZ() - l.getZ()) / 1.1);
																	y = l.getY() + ((targ.getLocation().getY() - l.getY()) / 1.1);
																}
																Location newLoc = new Location(e.getWorld(), x, y, zee);
																Location safeLoc = Utils.getNearestEmptyLoc(newLoc);
																if (safeLoc != null) {
																	e.teleport(safeLoc, TeleportCause.PLUGIN);
																	e.getWorld().playSound(l, Sound.ENDERMAN_TELEPORT, 1, 1);

																}
															}
														}
													}
												}
											}
											break;

										case GHAST_SHOOT:
											if (ZombieMod.intervals % 2 == 0) {
												if (Math.random() > 0.5) {
													LivingEntity targ = ((Monster) e).getTarget();
													if (targ != null) {
														if (targ.isDead() || (!Utils.isWild(targ.getLocation()))) {
															((Monster) e).setTarget(null);
															continue;
														}
														if (targ.getWorld() == e.getWorld()) {
															Location target = targ.getLocation();
															Location from = l.add(0, 2, 0);
															double fdt = from.distance(target);
															if (fdt > 5.0 && fdt < 24) {
																String ord = (Utils.ordinal(l));
																// ZombieMod.logger.info("[" + ZombieMod.myName +
																// "] FIRING GHASTBALL!: "+ord);
																int out = 1;
																int up = 0;
																if (z.size > 1) {
																	out = 3;
																	up = 8;
																}
																if (ord.contains("North")) {
																	from.add(-out, up, 0);
																}
																if (ord.contains("South")) {
																	from.add(out, up, 0);
																}
																if (ord.contains("East")) {
																	from.add(0, up, -out);
																}
																if (ord.contains("West")) {
																	from.add(0, up, out);
																}

																Location firePath = Utils.lookAt(from, target);
																Fireball fb = firePath.getWorld().spawn(firePath, Fireball.class);
																fb.setYield(1);
																fb.setBounce(false);

																e.getWorld().playSound(l, Sound.GHAST_FIREBALL, 1, 1);
															}
														}
													}
												}
											}
											break;

										case BOW_FIRE:
											if (ZombieMod.intervals % 6 == 0) {
												e.getWorld().playSound(l, Sound.SKELETON_IDLE, 1, 1);
											}

											if (ZombieMod.intervals % 3 == 0) {
												if (Math.random() > 0.4) {
													LivingEntity targ = ((Monster) e).getTarget();
													if (targ != null) {

														if (targ.getWorld() == e.getWorld()) {
															Location target = targ.getLocation();
															Location from = l.add(0, 0, 0);
															if (from.distance(target) > 2) {

																Arrow a = ((Monster) e).launchProjectile(Arrow.class);
																a.setFireTicks(0);
																e.getWorld().playSound(l, Sound.ARROW_HIT, 1, 1);

															}
														}
													}
												}
											}
											break;

										case BLAZE_SHOOT:
											if (ZombieMod.intervals % 2 == 0) {
												if (Math.random() > 0.4) {
													LivingEntity targ = ((Monster) e).getTarget();
													if (targ != null) {
														if (targ.getWorld() == e.getWorld()) {
															Location target = targ.getLocation();
															Location from = l.add(0, 0, 0);

															if (from.distance(target) > 2) {
																Arrow a = ((Monster) e).launchProjectile(Arrow.class);
																a.setFireTicks(99);
																e.getWorld().playSound(l, Sound.GHAST_FIREBALL, 1, 1);
															}
														}

													}
												}
											}
											break;
										case MOBSPAWNER_FLAMES:
											e.getWorld().playEffect(l, eff, 15, 32);
											break;
										default: // just play effect
											e.getWorld().playEffect(l, eff, 0);
									}
								}
							}

							// apply potions (but no lotions)
							if (z.potions != null) {
								((LivingEntity) e).addPotionEffects(z.potions);
							}

							// apply special effects
							if (z.abilities != null) {
								if (z.abilities.contains("HEROBRINE")) {// HEROBRINE
									// 32
									boolean hasown = true;
									if (z.getOwner() == null || z.getOwner().isEmpty()) {
										hasown = false;
									}

									if (hasown) {
										Player play = Bukkit.getPlayer(z.getOwner());
										if (play == null) {
											hasown = false;
										} else {
											if (!play.isOnline()) {
												hasown = false;
											}
										}
									}

									if (!hasown) {
										// no "owner" find one
										List<Entity> nbe = e.getNearbyEntities(32.0D, 32.0D, 32.0D);
										for (Entity ent : nbe) {
											if (ent.getType() == EntityType.PLAYER && !(e.hasMetadata("NPC"))  ) {
												z.setOwner(((Player) ent).getName());
												System.out.print("Haunting: " + ((Player) ent).getName());
												break;
											}
										}
										// ok if no owner found - end the haunting
										if (z.getOwner() == null || z.getOwner().isEmpty()) {
											e.remove();
										}

									}

									// if (ZombieMod.intervals % 2 == 0) { // scream less frequently than every
									List<Entity> nbe = e.getNearbyEntities(12.0D, 12.0D, 12.0D);
									boolean tooclose = false;
									Player play = null;
									for (Entity ent : nbe) {
										if (ent.getType() == EntityType.PLAYER) {
											tooclose = true;
											play = (Player) ent;
											break;
										}
									}

									if (tooclose) {
										// herobrine loc
										int x1 = MathHelper.floor(e.getLocation().getBlockX());
										int y1 = MathHelper.floor(e.getLocation().getBlockY());
										int z1 = MathHelper.floor(e.getLocation().getBlockZ());

										// owner loc
										int x2 = MathHelper.floor(play.getLocation().getBlockX());
										int y2 = MathHelper.floor(play.getLocation().getBlockY());
										int z2 = MathHelper.floor(play.getLocation().getBlockZ());

										/*
										 * int xDiff = x1 - x2; int zDiff = z1 - z2;
										 * 
										 * 
										 * int big = Math.max(Math.abs(xDiff),Math.abs(zDiff)); double multi = (big /
										 * (25.0D)); e.teleport(new Location(e.getWorld(), x2+(xDiff/multi),
										 * ((y2-y1)/2), z2+(zDiff/multi)));
										 */

										int xDiff = x2 - x1;
										int zDiff = z2 - z1;

										double angle = Math.toDegrees(Math.atan2(xDiff, zDiff));
										if (angle < 0.0D) {
											angle += 360.0D;
										}

										int magnitude = 24;

										double xOffset = (Math.sin(angle)) * magnitude;
										double zOffset = (Math.cos(angle)) * magnitude;

										x2 -= xOffset;
										z2 -= zOffset;
										y2 = y1 + ((y2 - y1) / 2);

										e.teleport(new Location(e.getWorld(), x2, y2, z2));

									}

									// }

									if (ZombieMod.intervals % 45 == 0) { // scream less frequently than every
										// second!
										if (z.getOwner() != null && !z.getOwner().isEmpty()) {
											String pname = Bukkit.getPlayer(z.getOwner()).getName();
											if (Math.random() > 0.2) {
												// e.getWorld().playEffect(l, eff, 0);
												double rnd = Math.random();
												Location ploc = l;

												if (z.getOwner() != null) {
													ploc = Bukkit.getPlayer(z.getOwner()).getLocation();
												}

												Sound snd = null;
												float pitch = 1.0F;
												if (rnd > 0.99D) {
													snd = Sound.GHAST_SCREAM2;
													pitch = 0.4F;
												} else if (rnd > 0.90D) {
													snd = Sound.GHAST_SCREAM;
													pitch = 0.2F;
												} else if (rnd > 0.80D) {
													snd = Sound.GHAST_MOAN;
													pitch = 0.1F;
												} else if (rnd > 0.7D) {
													snd = Sound.AMBIENCE_CAVE;
													pitch = 0.7F;
												} else if (rnd > 0.55D) {
													snd = Sound.AMBIENCE_CAVE;
													pitch = 0.5F;
												} else if (rnd > 0.50D) {
													snd = Sound.AMBIENCE_THUNDER;
													pitch = 0.8F;
												} else if (rnd > 0.45D) {
													snd = Sound.WOLF_HOWL;
													pitch = 0.7F;
												} else if (rnd > 0.40D) {
													snd = Sound.ZOMBIE_INFECT;
													pitch = 0.07F;
												} else if (rnd > 0.35D) {
													snd = Sound.ENDERMAN_TELEPORT;
													pitch = 0.1F;
												} else if (rnd > 0.30D) {
													snd = Sound.ENDERMAN_STARE;
													pitch = 0.2F;
												} else if (rnd > 0.10D) {
													snd = Sound.ENDERMAN_STARE;
													pitch = 0.5F;
												} else {
													snd = Sound.ENDERMAN_SCREAM;
													pitch = 0.4F;
												}
												System.out.print("Sound: " + snd.toString() + " - pitch: " + pitch + " - " + pname);
												e.getWorld().playSound(ploc, snd, 1, pitch);
												if (Math.random() > 0.9) {
													e.remove();
												}

											}
											if (Math.random() > 0.6) {
												String randomname = "sharksterboy";
												OfflinePlayer[] names = plugin.getServer().getOfflinePlayers();
												if (names != null) {
													if (names.length > 0) {
														Random generator = new Random();
														int rndx = generator.nextInt(names.length);
														OfflinePlayer plyr = names[rndx];
														randomname = plyr.getName();
													}
												}

												String msg = "";
												List<String> msgs = new ArrayList<String>();
												msgs.add("Help me " + pname + "...");
												msgs.add("You think that's air you're breathing now?");
												msgs.add(ChatColor.MAGIC + "--| /-|\\-;'# ';.,| #';] [- /|¬`¦¦");
												msgs.add("Have you seen " + randomname + ", I need them");
												msgs.add(randomname + " said you'd be here..");
												msgs.add("Why won't you help me?");
												msgs.add("Are you " + pname + "?");
												msgs.add("Why won't you help me?");
												msgs.add("There's nothing to be afraid of. They were right. It's painless. It's good. Come. Sleep");
												msgs.add("Whatever you do, don't fall asleep");
												msgs.add("I see dead people");
												msgs.add("What's blood for, if not for shedding?");
												msgs.add("They’re coming to get you, " + pname);
												msgs.add("When there is no room left in hell, the dead will walk the earth.");
												msgs.add("Be afraid... Be very afraid.");
												msgs.add("Every town has an Elm Street.");
												msgs.add("Are you my mummy?");
												msgs.add("Bad Wolf");
												msgs.add("They mostly come at night.     Mostly.");
												msgs.add("You still don't understand what you're dealing with, do you? A perfect organism. It's structural perfection is matched only by its hostility...");
												msgs.add("I can't lie to you about your chances, but...you have my sympathies");
												msgs.add("Not bad, for a human.");
												if (Bukkit.getPlayer(z.getOwner()).getItemInHand() != null) {
													String itemName = Bukkit.getPlayer(z.getOwner()).getItemInHand().getType().toString().toLowerCase().replace("_", " ");
													msgs.add("Say, " + pname + ". That's a nice " + itemName + "...");
													msgs.add("Got any spare " + itemName + "?");
													msgs.add("I died holding " + itemName + " too ~sigh~");
													msgs.add("Does that " + itemName + " look ok to you?");
													msgs.add("I heard the walkers only want to find " + itemName + ", you'll be ok unless you're holding " + itemName);
													msgs.add(itemName + " is bad for your health...");
													msgs.add(randomname + " was asking for " + itemName + " do you have any?");
													msgs.add(randomname + " lost some " + itemName + " is that his?  I'll tell him");
												}

												Random randomizer = new Random();
												msg = msgs.get(randomizer.nextInt(msgs.size()));

												if (Bukkit.getPlayer(z.getOwner()).getVehicle() != null && Bukkit.getPlayer(z.getOwner()).getVehicle() instanceof Boat) {
													if (Math.random() > 0.5) {
														msg = "You’re gonna need a bigger boat.";
													}
												}

												System.out.print("H - " + pname + " : " + msg);
												Bukkit.getPlayer(z.getOwner()).sendMessage(msg);
											}
										}
									}
								}

								if (z.abilities.contains("EXPLODE")) {// kaboom
									Boolean kaboom = false;
									List<Entity> entlist = e.getNearbyEntities(3, 1, 3);
									for (Entity ent : entlist) {
										if (ent instanceof Player) {
											kaboom = true;
										}
									}
									Location from = e.getLocation();
									int ticktock = 0;
									if (e.hasMetadata("kaboom")) {
										ticktock = e.getMetadata("kaboom").get(0).asInt();
									}

									if (kaboom) {
										ticktock = ticktock + 1;
										e.setMetadata("kaboom", new FixedMetadataValue(plugin, (Integer) ticktock));

										if (ticktock > 5) {
											from.getWorld().createExplosion(from.getX(), from.getY(), from.getZ(), 4.0F, false, false);
											// List<Entity> entlist2 = e.getNearbyEntities(4, 3, 4);
											// net.minecraft.server.v1_7_R4.Entity h = ((CraftEntity)e).getHandle();
											// ZombieType zt = (ZombieType)h;
											// Double dlvl = zt.getDamage();

											if (!(z.potions.contains(ZombieMod.resistPotion))) {
												((LivingEntity) e).damage(z.maxHealth);
											}
											/*
											 * for (Entity ent : entlist2) { if (ent instanceof Player) { ((Player)
											 * ent).damage(dlvl, e); } else if (ent instanceof LivingEntity) {
											 * ((LivingEntity) ent).damage(dlvl, e); } }
											 */
										} else {
											from.getWorld().playSound(from, Sound.CREEPER_HISS, 20, 1);
										}
									} else {
										if (ticktock > 0) {
											ticktock = ticktock - 1;
											e.setMetadata("kaboom", new FixedMetadataValue(plugin, (Integer) ticktock));
										}
									}
								}

								if (z.abilities.contains("HEAL")) {// regenerate
									if (Math.random() > 0.7) {
										Damageable de = (Damageable) e;

										if (de.getHealth() < de.getMaxHealth()) {
											if (de.getHealth() + 1 < de.getMaxHealth()) {
												de.setHealth(de.getHealth() + 1);
											} else {
												de.setHealth(de.getMaxHealth());
											}

										}
									}
								}
								if (z.abilities.contains("BREEDER")) {// breeder
									if (ZombieMod.intervals % 5 == 0) {
										if (Math.random() > 0.6) {

											// ZombieMod.logger.info(" Checking spawns ");

											Random generator = new Random();
											int rndx = generator.nextInt(16) - 7;
											int rndy = generator.nextInt(8) - 3;
											int rndz = generator.nextInt(16) - 7;

											Block lookAt = l.add(rndx, rndy, rndz).getBlock();

											Block safeNewBlock = Utils.getNearestEmptySpace(lookAt, 5);
											if (safeNewBlock != null) {
												Location sqawnLoc = safeNewBlock.getLocation();
												PutredineImmortui zomb;
												if (z.abilities.contains("BORG")) {
													zomb = new PutredineImmortui(plugin, "zomborg");
												} else if (z.abilities.contains("SPIDER")) {
													zomb = new PutredineImmortui(plugin, "spiderdrone");
												} else {
													zomb = new PutredineImmortui(plugin);
												}
												if (ZombieMod.debugMode) {
													ZombieMod.logger.info("[" + this.plugin.myName + "] " + zomb.commonName + " spawned via BREEDER");
												}
												Utils.spawnZombie(zomb, sqawnLoc, plugin);

											}
										}
									}
								}

								if (z.abilities.contains("STOMP")) { // stop
									if (ZombieMod.intervals % 3 == 2) {
										if (Math.random() > 0.9) {
											Boolean stompdayard = false;
											int radius = 6;
											List<Entity> entlist = e.getNearbyEntities(radius, (radius / 2), radius);
											for (Entity ent : entlist) {
												if (ent instanceof Player) {
													stompdayard = true;
												}
											}
											if (stompdayard) {
												Utils.stomp(l, e, radius, z.damage);
											}
										}
									}
								}

								if (z.abilities.contains("LIGHTNING")) {// ZAP!
									if (ZombieMod.intervals % 2 == 1) {
										if (Math.random() > 0.6) {
											LivingEntity targ = ((Monster) e).getTarget();
											if (targ != null) {
												if (targ.isDead() || (Utils.isSafe(targ.getLocation()))) {
													((Monster) e).setTarget(null);
												} else {
													if (targ.getWorld() == e.getWorld()) {
														Location target = targ.getLocation();
														Location from = l.add(0, 1, 0);
														double fdt = from.distance(target);
														if (fdt > 4.0 && fdt < 24) {
															targ.getWorld().strikeLightning(targ.getLocation());
															targ.getWorld().strikeLightningEffect(e.getLocation());
														}
													}
												}
											}
										}
									}
								}

								if (z.abilities.contains("INK")) {// squirt!
									if (ZombieMod.intervals % 2 == 1) {
										if (Math.random() > 0.6) {
											LivingEntity targ = ((Monster) e).getTarget();
											if (targ != null) {
												if (targ.isDead() || (Utils.isSafe(targ.getLocation()))) {
													((Monster) e).setTarget(null);
												} else {
													targ.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 3));
												}
											}
										}
									}
								}

								if (z.abilities.contains("WEB")) {// Sticky!!
									if (ZombieMod.intervals % 2 == 1) {
										if (Math.random() > 0.6) {
											LivingEntity targ = ((Monster) e).getTarget();
											if (targ != null) {
												if (targ.isDead() || (Utils.isSafe(targ.getLocation()))) {
													((Monster) e).setTarget(null);
												} else {
													if (targ.getWorld() == e.getWorld()) {
														Location target = targ.getLocation();
														Location from = l.add(0, 1, 0);
														double fdt = from.distance(target);
														if (fdt < 12) {
															Block b1 = target.getBlock();
															Block b2 = target.add(0, 1, 0).getBlock();
															if (b1.getType() == Material.AIR) {
																plugin.getServer().getScheduler().runTaskLater(plugin, new ReplaceMaterial(b1.getLocation(),b1.getType(),Material.WEB), 100);
																b1.setType(Material.WEB);
															}
															if (b2.getType() == Material.AIR) {
																plugin.getServer().getScheduler().runTaskLater(plugin, new ReplaceMaterial(b2.getLocation(),b2.getType(),Material.WEB), 100);
																b2.setType(Material.WEB);
															}
														}
													}
												}
											}
										}
									}
								}

								if (z.abilities.contains("SHOCKWAVE")) {// ZAP!
									if (ZombieMod.intervals % 2 == 1) {
										if (Math.random() > 0.8) {
											LivingEntity targ = ((Monster) e).getTarget();
											if (targ != null) {
												if (targ.isDead() || (Utils.isSafe(targ.getLocation()))) {
													((Monster) e).setTarget(null);
												} else {
													if (targ.getWorld() == e.getWorld()) {
														Location target = targ.getLocation();
														targ.setVelocity(target.getDirection().multiply(-1));
													}
												}
											}
											List<Entity> entlist = e.getNearbyEntities(12, 12, 12);
											for (Entity le : entlist) {
												if (le instanceof LivingEntity) {
													if (le != targ) {
														le.setVelocity(le.getLocation().getDirection().multiply(-1));
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
			}
		}

	}

	public class ReplaceMaterial implements Runnable {
		public Location l;
		public Material m;
		public Material temp;
		public ReplaceMaterial(Location l, Material m, Material temp){
			this.l=l;
			this.m=m;
			this.temp=temp;
		}
		public void run() {
			if (l.getBlock().getType() == temp) { // only swap if correct material
				l.getBlock().setType(m);
			}
		}
	}

}

// :/ so much for "lightweight"...
