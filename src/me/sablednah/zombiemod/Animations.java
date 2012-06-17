package me.sablednah.zombiemod;

import java.util.List;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * Processes all repeating tasks that are NOT thread safe.
 * This function is executed every second.  It must be kept lightweight to prevent lag.
 *
 */
public class Animations implements Runnable {
	public ZombieMod plugin;

	public Animations(ZombieMod p) {
		this.plugin=p;
	}


	@Override
	public void run() {
		// trigger animations
		ZombieMod.intervals++;

		for (World w : plugin.getServer().getWorlds()) {
			for(Entity e : w.getEntitiesByClass(Zombie.class)) {
				if (!e.isDead()) {
					PutredineImmortui z = null;
					z = ZombieType.getZombie(e);
					if (z != null  && z.health>0) {
						Location l=e.getLocation();

						if (z.species.equals("PlayerZombie")) {
							z.lastLoc = l;
							Chunk c = l.getChunk();
							String cid = c.getX() + "|"+c.getZ();
							z.cid=cid;
						}

						// play effects
						if (z.effects != null) {
							for (Effect eff : z.effects) {
								//ZombieMod.logger.info("[" + ZombieMod.myName + "] eff - " + eff + " ["+ZombieMod.intervals+"] ");
								switch (eff) {
								case GHAST_SHRIEK:
									if (ZombieMod.intervals % 10 == 0) {  //scream less frequently than every second!
										e.getWorld().playEffect(l, eff, 0);
									}
									break;

								case ENDER_SIGNAL: // teleport - bamf
									e.getWorld().playEffect(l, eff, 0);
									if (ZombieMod.intervals % 3 == 0) {
										if (Math.random()>0.3) {
											LivingEntity targ = ((Zombie) e).getTarget();
											if (targ != null) {
												if (targ instanceof HumanEntity) {  // focused on player  was (true) {
													//bamf!!
													double x,y,zee;
													if (targ.getWorld() == e.getWorld()) {
														if (z.abilities!=null && z.abilities.contains("BACKSTAB")){
															String ord =  (Utils.ordinal(targ.getLocation()));
															//ZombieMod.logger.info("[" + ZombieMod.myName + "] Facing!: "+ord); 

															x = targ.getLocation().getX();
															zee = targ.getLocation().getZ();
															y = targ.getLocation().getY();

															if (ord.contains("North")) {x=x+5; }
															if (ord.contains("South")) {x=x-5; }
															if (ord.contains("East")) {zee=zee+5; }
															if (ord.contains("West")) {zee=zee-5; }
														} else {
															x = l.getX() + ((targ.getLocation().getX() - l.getX() ) /1.1);
															zee = l.getZ() + ((targ.getLocation().getZ() - l.getZ()) /1.1);
															y = l.getY() + ((targ.getLocation().getY() - l.getY()) /1.1);
														}
														Location newLoc = new Location(e.getWorld(), x, y, zee);
														Block blk = e.getWorld().getBlockAt(newLoc);
														if (blk!=null) {
															Block safeNewBlock = Utils.getNearestEmptySpace(blk, 5);
															Location safeNewLoc = null;
															if (safeNewBlock!=null) {safeNewLoc = safeNewBlock.getLocation();}
															if (safeNewLoc!=null) {
																e.teleport(safeNewLoc, TeleportCause.PLUGIN);
															}
														}
													}
												}
											}
										}
									}
									break;

								case GHAST_SHOOT:
									if (ZombieMod.intervals % 2 == 0) {
										if (Math.random()>0.5) {
											LivingEntity targ = ((Zombie) e).getTarget();
											if (targ != null) {
												if (targ.getWorld() == e.getWorld()) {
													Location target = targ.getLocation();
													Location from = l.add(0,2,0);
													if (from.distance(target)>2) {
														String ord =  (Utils.ordinal(l));
														//ZombieMod.logger.info("[" + ZombieMod.myName + "] FIRING GHASTBALL!: "+ord); 

														if (ord.contains("North")) {from.add(-1, 0, 0); }
														if (ord.contains("South")) {from.add( 1, 0, 0); }
														if (ord.contains("East"))  {from.add( 0, 0,-1); }
														if (ord.contains("West"))  {from.add( 0, 0, 1); }


														Location firePath = Utils.lookAt(from, target);
														Fireball fb = firePath.getWorld().spawn(firePath, Fireball.class);
														fb.setYield(1);
														fb.setBounce(false);

														e.getWorld().playEffect(l, eff, 0);
													}
												}
											}
										}
									}
									break;

								case BOW_FIRE:
									if (ZombieMod.intervals % 3 == 0) {
										if (Math.random()>0.4) {
											LivingEntity targ = ((Zombie) e).getTarget();
											if (targ != null) {
												if (((Zombie) e).getTarget() instanceof HumanEntity) {  // focused on player

													if (targ.getWorld() == e.getWorld()) {
														Location target = targ.getLocation();
														Location from = l.add(0,0,0);
														if (from.distance(target)>2) {

															Arrow a = ((Zombie) e).launchProjectile(Arrow.class);
															a.setFireTicks(0);
															e.getWorld().playEffect(l, eff, 0);

														}
													}
												}
											}
										}
									}
									break;

								case BLAZE_SHOOT:
									if (ZombieMod.intervals % 2 == 0) {
										if (Math.random()>0.4) {
											LivingEntity targ = ((Zombie) e).getTarget();
											if (targ != null) {
												if (((Zombie) e).getTarget() instanceof HumanEntity) {  // focused on player
													if (targ.getWorld() == e.getWorld()) {
														Location target = targ.getLocation();
														Location from = l.add(0,0,0);

														if (from.distance(target)>2) {
															Arrow a = ((Zombie) e).launchProjectile(Arrow.class);
															a.setFireTicks(99);
															e.getWorld().playEffect(l, eff, 0);

														}
													}
												}
											}
										}
									}
									break;

								default:  // just play effect
									e.getWorld().playEffect(l, eff, 0);
								}
							}
						}

						//apply potions (but no lotions)
						if (z.potions != null) {
							((LivingEntity) e).addPotionEffects(z.potions);
						}

						//apply special effects
						if (z.abilities != null) {
							if (z.abilities.contains("EXPLODE")){//kaboom
								Boolean kaboom = false;
								List<Entity> entlist = e.getNearbyEntities(3,1,3);
								for (Entity ent : entlist) {
									if (ent instanceof Player) {
										kaboom=true;
									}
								}
								Location from = e.getLocation();

								if (kaboom) {
									from.getWorld().createExplosion(from, 0);
									List<Entity> entlist2 = e.getNearbyEntities(5, 2, 5);
									for (Entity ent : entlist2) {
										if (ent instanceof Player) {
											((Player) ent).damage(z.damage);
										} else if (ent instanceof LivingEntity) {
											((LivingEntity) ent).damage(z.damage,e);
										}
									}
									if (!(z.potions.contains(ZombieMod.resistPotion))) {
										z.health = 0;
										((Zombie) e).damage(2000);
									}
								}
							}

							if (z.abilities.contains("HEAL")){//regenerate
								int hp = 0;
								if (Math.random()>0.5){
									hp = z.health + 1;
									if (hp > z.maxHealth) {
										z.health = z.maxHealth;
									} else {
										z.health = hp;
									}
								}
							}
							if (z.abilities.contains("BREEDER")){//breeder
								if (ZombieMod.intervals % 5 == 0) {
									if (Math.random()>0.6) {

										//ZombieMod.logger.info(" Checking spawns "); 

										Chunk c = l.getChunk();

										int zombieCount = 0;
										for (Entity thisE : c.getEntities()) {
											if ((thisE instanceof Zombie)) { zombieCount++; }
										}
										//ZombieMod.logger.info(" zombieCount : " + zombieCount); 

										if (zombieCount<10) {

											Random generator = new Random();
											int rndx = generator.nextInt( 16 ) -7;
											int rndy = generator.nextInt( 8 ) -3;
											int rndz = generator.nextInt( 16 ) -7;

											Block lookAt = l.add(rndx, rndy, rndz).getBlock();

											Block safeNewBlock = Utils.getNearestEmptySpace(lookAt, 5);
											if (safeNewBlock!=null) {
												Location sqawnLoc=safeNewBlock.getLocation();
												net.minecraft.server.World mcWorld = ((CraftWorld) w).getHandle();
												PutredineImmortui zomb;
												if (z.abilities.contains("BORG")){
													zomb = new PutredineImmortui(plugin,"borg.yml");
												} else {
													zomb = new PutredineImmortui(plugin);
												}
												if (ZombieMod.debugMode) { ZombieMod.logger.info("[" + ZombieMod.myName + "] " + zomb.commonName +" spawned via BREEDER"); }
												ZombieType newzomb = new ZombieType(mcWorld,zomb);
												newzomb.setPosition(sqawnLoc.getX(), sqawnLoc.getY(), sqawnLoc.getZ());
												mcWorld.addEntity(newzomb, SpawnReason.CUSTOM);

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
// :/ so much for "lightweight"...
