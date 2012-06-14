package me.sablednah.zombiemod;

import java.util.List;
import java.util.Random;

import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.classes.HeroClass.ExperienceType;


public class EntityListener implements Listener {

	public ZombieMod plugin;

	public EntityListener(ZombieMod instance) {
		this.plugin=instance;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (event.isCancelled()) { return; }

		SpawnReason spawnReason = event.getSpawnReason();
		Entity temp = event.getEntity();

		if (ZombieMod.blocknaturalspawns) {
			if (temp instanceof Zombie == false) { 
				if (spawnReason == SpawnReason.NATURAL) { 
					event.setCancelled(true);
				}
				return; 
			}
		}


		PutredineImmortui zomb = null;

		Location l = event.getLocation();
		EntityType t = event.getEntityType();
		World w = l.getWorld();

		net.minecraft.server.World mcWorld = ((CraftWorld) w).getHandle();
		//net.minecraft.server.Entity mcEntity = (((CraftEntity) temp).getHandle());
		if (t == EntityType.ZOMBIE && spawnReason != SpawnReason.CUSTOM){  // && mcEntity instanceof ZombieType == false){
			//if (spawnReason != SpawnReason.CUSTOM){  // && mcEntity instanceof ZombieType == false){
			zomb = new PutredineImmortui(plugin);
			ZombieType newzomb = new ZombieType(mcWorld,zomb);
			newzomb.setPosition(l.getX(), l.getY(), l.getZ());
			//mcWorld.removeEntity((net.minecraft.server.EntityZombie) mcEntity);  //better but causes errors.
			mcWorld.addEntity(newzomb, SpawnReason.CUSTOM);
			event.setCancelled(true);
			return;
		} else {
			zomb = ZombieType.getZombie(temp);
			if (zomb !=null) {
				if (zomb.potions != null) {
					((LivingEntity) temp).addPotionEffects(zomb.potions);
				}
				if (zomb.effects != null) {
					for (Effect eff : zomb.effects) {
						temp.getWorld().playEffect(l, eff, 0);
					}
				}
				zomb.ID = temp.getUniqueId();
				Chunk c = l.getChunk();
				String cid = c.getX() + "|"+c.getZ();
				zomb.cid = cid;
				if (zomb.species.equals("PlayerZombie")) {
					zomb.skin = "http://s3.amazonaws.com/MinecraftSkins/" + zomb.commonName + ".png";
					// only perma track if has items
					if (zomb.items != null && (!zomb.items.isEmpty()) && zomb.items.size()>0 ) {
						ZombieMod.playerZombies.put(zomb.ID,zomb);
					}
				}
				if(zomb.skin != null && Bukkit.getServer().getPluginManager().isPluginEnabled("Spout")) {
					Utils.setSkin((LivingEntity) temp,zomb.skin);
				}
			}
		}
		//		if (ZombieMod.debugMode) { ZombieMod.logger.info("[" + ZombieMod.myName + "] " + zomb.commonName +" spawned."); }
	}


	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {	
		Entity entity = event.getEntity();
		Boolean firedamage = false;
		if (entity.getLastDamageCause() != null) {
			if (entity.getLastDamageCause().getCause().equals(DamageCause.FIRE) || entity.getLastDamageCause().getCause().equals(DamageCause.FIRE_TICK)) {
				firedamage = true;
			}
		}

		if (!firedamage) {
			PutredineImmortui zomb = ZombieType.getZombie(entity);
			if (zomb != null) { 
				Player p = ((LivingEntity) entity).getKiller();

				if (zomb.items != null) { // Add drops
					List<ItemStack> drops = event.getDrops();
					drops.clear();

					for (int i = 0; i < zomb.items.size(); i++) {
						ItemStack item = zomb.items.get(i);
						//						ZombieMod.logger.info("[" + ZombieMod.myName + "] " + item.getAmount() + " x " + item.getType().toString());
						double drop;
						if (zomb.dropRates!=null) {
							drop = zomb.dropRates.get(i);
						} else {
							if (zomb.species == "PlayerZombie") {
								drop = 1.0;
							} else {
								drop = 0.7;
							}
						}
						double chance = Math.random();
						if (chance<drop) {
							drops.add(item);
						}
					}
				}

				if (zomb.xp >0  && p != null) { // add bonus xp
					int xp = zomb.xp;						
					xp += event.getDroppedExp();
					event.setDroppedExp(xp);
					if (ZombieMod.hasHeroes) {
						Heroes heroes = (Heroes) plugin.getServer().getPluginManager().getPlugin("Heroes");
						Hero hero = heroes.getCharacterManager().getHero(p);
						hero.gainExp((double) xp, ExperienceType.KILLING, entity.getLocation());
					}
				}


				if (zomb.bounty>0 && ZombieMod.economy != null && p != null) {  //add bounty
					double i = zomb.bounty;
					ZombieMod.economy.depositPlayer(p.getName(), i);
				}

				ZombieMod.playerZombies.remove(entity.getUniqueId());

			}
		} else {
			List<ItemStack> drops = event.getDrops();
			drops.clear();
		}
	}


	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled()) { return; }

		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity)) return;

		int damage = event.getDamage();
		Entity damager = null;
		if (event instanceof EntityDamageByEntityEvent) damager = ((EntityDamageByEntityEvent)event).getDamager();

		if (damager != null) { 
			PutredineImmortui attacker = null;
			if (damager instanceof Projectile) {
				LivingEntity shooter = ((Projectile)damager).getShooter();
				if (shooter != null && shooter instanceof Zombie) {
					attacker = ZombieType.getZombie(shooter);
				}
			} else {
				attacker = ZombieType.getZombie(damager);
			}
			if (attacker != null && attacker.damage > -1) {
				damage = attacker.damage;	
			}
		}

		PutredineImmortui zomb = ZombieType.getZombie(entity);
		if (zomb != null && zomb.health > -1) {
			Chunk c = entity.getLocation().getChunk();
			String cid = c.getX() + "|"+c.getZ();
			zomb.cid=cid;
			zomb.lastLoc=entity.getLocation();
			if (zomb.species.equals("PlayerZombie")) {
				ZombieMod.playerZombies.put(entity.getUniqueId(), zomb);
			}

			if (zomb.effects.contains(Effect.BLAZE_SHOOT) || zomb.effects.contains(Effect.GHAST_SHOOT) || zomb.effects.contains(Effect.EXTINGUISH) ) {
				if (event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.FIRE_TICK) {
					event.setDamage(0);
					event.setCancelled(true);
					return;
				}
			}

			net.minecraft.server.Entity mcEnt = (((CraftEntity) entity).getHandle());
			ZombieType zt = (ZombieType) mcEnt;



			if(zt.genus.abilities != null && zt.genus.abilities.contains("BORG")){
				Material bob = null;
				Player play = null;
				if (damager != null) {
					if (damager instanceof Player) {
						play = (Player) damager;
						bob = play.getItemInHand().getType();
					} else if (damager instanceof Projectile) {
						LivingEntity shooter = ((Projectile) damager).getShooter();
						if (shooter instanceof Player) {
							play = (Player) shooter;
						}
						if (damager instanceof Egg) {
							bob = Material.EGG;
						} else if (damager instanceof Snowball) {
							bob = Material.SNOW_BALL;
						} else if (damager instanceof ThrownPotion) {
							bob = Material.POTION;
						}else {
							bob = Material.ARROW;  //arrow
						}
					}
				} else {
					switch (event.getCause()) {
					case FIRE:
					case FIRE_TICK:
					case LAVA:
						bob = Material.FIRE;
						break;
					case CONTACT:
						bob = Material.CACTUS;
						break;
					case BLOCK_EXPLOSION:
					case ENTITY_EXPLOSION:
						bob = Material.TNT;
						break;
					default:
						// do nothing
					}
				}
				if (bob != null) {
					if (bob != Material.AIR && bob != null) {
						if (zt.checkResistance(bob)) {
							event.setDamage(0);
							event.setCancelled(true);
							damage=0;
							if (bob == Material.FIRE) {
								entity.setFireTicks(0);
							}
							if (play != null) {
								play.sendMessage("Zomborg is adapted to " + bob.name());
							} else {
								List<Entity> entlist = entity.getNearbyEntities(16, 20, 16);
								for (Entity ent : entlist) {
									if (ent instanceof Player) {
										((Player) ent).sendMessage("Zomborg is adapted to " + bob.name());
									}
								}
							}
						} else {
							if( damage>0) {
								Random generator = new Random();
								int randomIndex = generator.nextInt( 100 ) + 1;

								if (randomIndex==30 || randomIndex>damage+80) {
									zt.addResistance(bob);
									List<Entity> entlist = entity.getNearbyEntities(16, 20, 16);
									for (Entity ent : entlist) {
										if (ent instanceof Player) {
											((Player) ent).sendMessage("Zomborg have adapted to " + bob.name());
										} else {
											PutredineImmortui z = ZombieType.getZombie(ent);
											if (z != null){
												net.minecraft.server.Entity mcz = (((CraftEntity) ent).getHandle());
												ZombieType zt2 = (ZombieType) mcz;
												zt2.addResistance(bob);
											}
										}
									}
								}
							}
						}
					}
				}
			}

			if (damage>0) {
				damage=damage-zt.genus.armour;
				if (damage<1 && zt.genus.armour>0) {
					damage=1;	
				} 
			}

			if (zt.genus.potions.contains(ZombieMod.resistPotion) && damage>1) {
				zt.genus.health -= (damage/2);
			} else {
				zt.genus.health -= damage;
			}

			if (zt.genus.health < 1) { //  kill entity
				event.setDamage(2001);
			} else { // not dead yet
				LivingEntity tEnt = (LivingEntity) entity;
				tEnt.setHealth(tEnt.getMaxHealth());

				if (ZombieMod.hasHeroes) {
					Heroes heroes = (Heroes) plugin.getServer().getPluginManager().getPlugin("Heroes");
					Monster monsta = heroes.getCharacterManager().getMonster(tEnt);
					monsta.setHealth(monsta.getMaxHealth());
					//					ZombieMod.logger.info("Heroes Health - " + monsta.getHealth());
					//					ZombieMod.logger.info("bukkit Health - " + tEnt.getHealth());
					//					ZombieMod.logger.info("zomb   Health - " + zt.genus.health);

				}

				event.setDamage(0);
				//hack to set last damage to correct amount
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new setTempInvuln(entity,damage),1L);
			}
		} else { // damage entity normally with different damage
			event.setDamage(damage);
		}
	}

	// prevent overhealing
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityRegainHealth(EntityRegainHealthEvent event) {		
		if (event.isCancelled()) {  return; }	
		Entity entity = event.getEntity();
		PutredineImmortui zomb = ZombieType.getZombie(entity);	
		if (zomb != null) {
			int hp = zomb.health + event.getAmount();
			if (hp > zomb.maxHealth) {
				zomb.health = zomb.maxHealth;
			} else {
				zomb.health = hp;
			}
			net.minecraft.server.Entity mcEnt = (((CraftEntity) entity).getHandle());
			ZombieType zt = (ZombieType) mcEnt;
			zt.genus=zomb;  //push health back to entity
		}
	}

	//Stop burning....
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityCombust(EntityCombustEvent event) {
		if (event.isCancelled()) { return; }
		Entity entity = event.getEntity();
		PutredineImmortui zomb = ZombieType.getZombie(entity);	
		if (zomb != null && zomb.noBurn) {
			event.setCancelled(true);
			entity.setFireTicks(0);
		}
	}

	// disable targeting
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityTarget(EntityTargetEvent event) {
		if (event.isCancelled()) { return; }
		Entity entity = event.getEntity();
		PutredineImmortui zomb = ZombieType.getZombie(entity);	
		if (zomb != null) {
			if (zomb.passive) { 
				event.setCancelled(true); 
			} else {
				if (!(zomb.effects.contains(Effect.BLAZE_SHOOT) || zomb.effects.contains(Effect.GHAST_SHOOT) || zomb.effects.contains(Effect.BOW_FIRE))) {
					if (event.getTarget() instanceof HumanEntity) {
						Player p = (Player) event.getTarget();
						LivingEntity l = (LivingEntity)entity;
						//						ZombieMod.logger.info("[" + ZombieMod.myName + "] starting breaker - target -" + p.getDisplayName()); 
						plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new BreakRunner(plugin, l, p), 60L);
					}
				}
			}
		}
	}

	// exploding
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onExplosionPrime(ExplosionPrimeEvent event) {
		if (event.isCancelled()) { return; }
		Entity entity = event.getEntity();

		if (entity instanceof Fireball) entity = ((Fireball)entity).getShooter();

		PutredineImmortui zomb = ZombieType.getZombie(entity);	
		if (zomb != null) {
			event.setFire(true);
			if (zomb.passive) { event.setCancelled(true); }
		}
	}

	// boom!
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (event.isCancelled()) { return; }

		Entity entity = event.getEntity();
		if (entity == null) return;

		PutredineImmortui zomb = ZombieType.getZombie(entity);	
		if (zomb != null) {
			event.blockList().clear();
			if (zomb.passive) { event.setCancelled(true); }
		}
	}

	/*
	// chunk loader 
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		for (Entity ent: event.getChunk().getEntities()) {
			if (ent instanceof Zombie) {
				zombies.put(ent.getUniqueId(), new PutredineImmortui(SpawnReason.NATURAL.name()));
				if (ZombieMod.debugMode) ZombieMod.logger.info("[" + ZombieMod.myName + "] Zombie converted.");
			}
		}
	}
	 */


	@EventHandler(priority=EventPriority.HIGH)
	public void onProjectileHit(ProjectileHitEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof Projectile) {
			Projectile projectile = (Projectile)entity;
			LivingEntity shooter = projectile.getShooter();
			if ((shooter != null) &&  ((((CraftEntity)shooter).getHandle() instanceof ZombieType))) {
				Block block = projectile.getWorld().getBlockAt(projectile.getLocation());
				if (block.getType() != Material.AIR) { block.setType(Material.FIRE); }
			}
		}
	}

	@EventHandler(priority=EventPriority.HIGH)
	public void onBowShoot(EntityShootBowEvent event) {
		if (event.isCancelled()) { return; }

		Entity entity = event.getEntity();
		if (entity == null) return;
		PutredineImmortui zomb = ZombieType.getZombie(entity);	
		if (zomb != null) {
			if (zomb.effects.contains(Effect.BLAZE_SHOOT)) {
				Entity arrow = event.getProjectile();
				arrow.setFireTicks(99);
				event.setProjectile(arrow);
			} else if (zomb.effects.contains(Effect.GHAST_SHOOT)) {
				LivingEntity targ = (LivingEntity) ((EntityTargetEvent) event.getEntity()).getTarget();
				if (targ != null) {

					if (targ.getWorld() == entity.getWorld()) {
						Location target = targ.getLocation();
						Location from = entity.getLocation().add(0,2,0);
						if (from.distance(target)>2) {
							String ord =  (Utils.ordinal(entity.getLocation()));

							if (ord.contains("North")) {from.add(-1,0,0); }
							if (ord.contains("South")) {from.add(1,0,0); }
							if (ord.contains("East")) {from.add(0,0,0-1); }
							if (ord.contains("West")) {from.add(0,0,1); }


							Location firePath = Utils.lookAt(from, target);
							Fireball fb = firePath.getWorld().spawn(firePath, Fireball.class);
							fb.setYield(1);
							fb.setBounce(false);

							entity.getWorld().playEffect(entity.getLocation(), Effect.GHAST_SHOOT, 0);
						}
					}
				}
				event.setCancelled(true);
				event.setProjectile(null);
				return;
			}
		}
	}
}
