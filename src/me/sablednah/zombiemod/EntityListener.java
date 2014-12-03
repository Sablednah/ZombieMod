package me.sablednah.zombiemod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
//import org.bukkit.World.Environment;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
//import org.bukkit.craftbukkit.v1_7_R4.entity.CraftEntity;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Giant;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
//import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.FileManager;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.ps.PS;

public class EntityListener implements Listener {
    
    public ZombieMod plugin;
    
    public EntityListener(ZombieMod instance) {
        this.plugin = instance;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {

        if (event.isCancelled()) {
            return;
//        	System.out.print("spawn cancelled already!");
//        } else {
//        	System.out.print("spawning "+t.toString()+"!");
        }

    	SpawnReason spawnReason = event.getSpawnReason();
        
        Entity temp = event.getEntity();
        EntityType t = event.getEntityType();
        
        Location l = event.getLocation();
        PutredineImmortui zomb = null;
        
        if (temp instanceof Monster) {
            Faction fact = BoardColl.get().getFactionAt(PS.valueOf(l));
            
            Faction nz = FactionColl.get().getNone();
//            Faction sz = FactionColl.get().getSafezone();
            Faction wz = FactionColl.get().getWarzone();

            
            if (fact != null && fact != nz && fact != wz) {
                // safe location (non wilderness)
                zomb = Utils.getZombie(temp);
                if (zomb != null) {
                    if (!zomb.species.equals("PlayerZombie")) {
                        event.setCancelled(true);
                    }
                }
            }
        }
        
        if (temp instanceof Witch) {
//        	System.out.print("Witch spawn: " + spawnReason.toString() + " - " + temp.getLocation().toString());
        	if (spawnReason == SpawnReason.NATURAL) {
                Utils.spawnZombie(null, l, plugin);
                event.setCancelled(true);
                return;
        	} else {
        		((Witch) temp).setRemoveWhenFarAway(true);
        	}
        }
        if ((!(temp instanceof Zombie || temp instanceof Giant || temp instanceof Bat)) || (temp instanceof PigZombie) ) {
            if (ZombieMod.blocknaturalspawns) {
                if (spawnReason == SpawnReason.NATURAL) {
//                	System.out.print("Blocking natural "+t.toString()+"!");
                	// Replace monsters with zombs
                	if (temp instanceof Monster) {
                        Utils.spawnZombie(null, l, plugin);
                	}
                    event.setCancelled(true);
                    return;
                }
            }
        }
        
        // ok we have a entity that is allowed to exist.
        
        World w = l.getWorld();
        
        if (t == EntityType.ZOMBIE && spawnReason != SpawnReason.CUSTOM) { // && mcEntity instanceof ZombieType == false){
            // if (spawnReason != SpawnReason.CUSTOM){ // && mcEntity instanceof ZombieType == false){
 //       	System.out.print("Replacing zombie with custom: "+t.toString()+"!");
        	//cancel natural zombies to swap for custom
            Utils.spawnZombie(null, l, plugin);
            event.setCancelled(true);
            return;
        }
        
        if (spawnReason == SpawnReason.SPAWNER) {
  //      	System.out.print("Spawner "+t.toString()+"!");
            int probability = ZombieMod.zombiespawnerratio;
            Random rand = new Random();
            if (rand.nextInt(100) + 1 < probability) { // only run if < probability.
//            	System.out.print("Spawning: " + t.toString() + " via spawner replaced with AWESOME!");
                // kill the spawnered thingamajig!
                Utils.spawnZombie(null, l, plugin);
                event.setCancelled(true);
                return;
            } else {
            	System.out.print("Spawning: " + t.toString() + " via spawner");
            }
        }
        
        zomb = Utils.getZombie(temp);
        if (zomb != null) {
            if (zomb.potions != null) {
                ((LivingEntity) temp).addPotionEffects(zomb.potions);
            }
            if (zomb.effects != null) {
                for (Effect eff : zomb.effects) {
                    if (eff == Effect.GHAST_SHRIEK) {
                        w.playSound(l, Sound.GHAST_MOAN, 1, 1);
                    } else {
                        w.playEffect(l, eff, 0);
                    }
                }
            }
            zomb.ID = temp.getUniqueId();
            Chunk c = l.getChunk();
            String cid = c.getX() + "|" + c.getZ();
            zomb.cid = cid;
            if (zomb.species.equals("PlayerZombie")) {
                String name = zomb.commonName.substring(7);
                
                zomb.skin = "http://s3.amazonaws.com/MinecraftSkins/" + name + ".png";
                // only perma track if has items
                if (zomb.items != null && (!zomb.items.isEmpty()) && zomb.items.size() > 0) {
                    ZombieMod.playerZombies.put(zomb.ID, zomb);
                }
                Zombie zed = (Zombie) temp;
                zed.setRemoveWhenFarAway(false);
                ItemStack head = Utils.getSkullItemStack(1, name);
                Utils.setEquip(zed, head, 4);
                zed.setCanPickupItems(false);
                
            } else if (zomb.species.equals("Zombus Sapians")) {
                String name = zomb.commonName.substring(7);
                Zombie zed = (Zombie) temp;
                zed.setRemoveWhenFarAway(true);
                ItemStack head = Utils.getSkullItemStack(1, name);
                Utils.setEquip(zed, head, 4);
                zed.setCanPickupItems(false);
                
            } else {
                LivingEntity le = (LivingEntity) temp;
                le.setRemoveWhenFarAway(true);
                le.setCanPickupItems(false);
            }
            if (zomb.skin != null && Bukkit.getServer().getPluginManager().isPluginEnabled("Spout")) {
                FileManager fm = SpoutManager.getFileManager();
                fm.addToCache(plugin, zomb.skin);
                Utils.setSkin((LivingEntity) temp, zomb.skin);
            }
            
            if (!(zomb.effects.contains(Effect.BLAZE_SHOOT) || zomb.effects.contains(Effect.GHAST_SHOOT) || zomb.effects.contains(Effect.BOW_FIRE))) {
            	plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new BreakRunner(plugin, (LivingEntity) temp), 100L);
            }

            
        }
        
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
        
        PutredineImmortui zomb = Utils.getZombie(entity);
        
        if (zomb != null) {
            if (zomb.effects != null) {
                if (zomb.effects.contains(Effect.ENDER_SIGNAL)) {
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENDERMAN_DEATH, 1, 1);
                } else if (zomb.effects.contains(Effect.GHAST_SHRIEK)) {
                    entity.getWorld().playSound(entity.getLocation(), Sound.GHAST_DEATH, 1, 1);
                } else if (zomb.effects.contains(Effect.BOW_FIRE)) {
                    entity.getWorld().playSound(entity.getLocation(), Sound.SKELETON_HURT, 1, 1);
                }
            }
            if (zomb.species.contains("PlayerZombie")) {
                entity.getWorld().playSound(entity.getLocation(), Sound.PIG_DEATH, 1, 1);
            }
            
            // stat work
            if (ZombieMod.hasBeardStat) {
                
                EntityDamageEvent lastCause = event.getEntity().getLastDamageCause();
                EntityDamageEvent.DamageCause cause = null;
                if (lastCause != null) {
                    cause = lastCause.getCause();
                }
                
                Entity attacker = null;
                Projectile projectile = null;
                if ((lastCause instanceof EntityDamageByEntityEvent)) {
                    attacker = ((EntityDamageByEntityEvent) lastCause).getDamager();
                    if ((attacker instanceof Projectile)) {
                        projectile = (Projectile) attacker;
                        attacker = (Entity) projectile.getShooter();
                    }
                }
                
                if ((attacker instanceof Player)) {
                    Utils.addStat(((Player) attacker).getName(), "ZombieMod", "kills_allzombies", 1);
                    String thistype;
                    if (zomb.abilities != null && zomb.abilities.contains("GHOST")) {
                        thistype = "Ghost";
                    } else {
                        if (zomb.species.equalsIgnoreCase("PlayerZombie")) {
                            thistype = "PlayerZombie";
                            if (zomb.commonName.toLowerCase().contains(((Player) attacker).getName().toLowerCase())) {
                                // self kill!
                                Utils.addStat(((Player) attacker).getName(), "ZombieMod", "kill_self", 1);
                            }
                        } else {
                            thistype = zomb.commonName;
                        }
                    }
                    Utils.addStat(((Player) attacker).getName(), "ZombieMod", "kill_" + thistype, 1);
                    
                    if (cause != null) {
                        
                        Utils.addStat(((Player) attacker).getName(), "ZombieMod", "kill_" + cause.toString().toLowerCase().replace("_", "") + thistype, 1); // kill type and zomb
// type
                        Utils.addStat(((Player) attacker).getName(), "ZombieMod", "kill_" + cause.toString().toLowerCase().replace("_", ""), 1); // kill type
                    }
                    if (projectile != null) {
                        Utils.addStat(((Player) attacker).getName(), "kills", "kill_ranged", 1);
                        Utils.addStat(((Player) attacker).getName(), "kills", "kill_ranged_" + thistype, 1);
                    }
                }
            }
        }
        
        if (!firedamage || (zomb != null && zomb.species.contains("PlayerZombie"))) {
            if (zomb != null) {
                
                Player p = ((LivingEntity) entity).getKiller();
                
                double adj = 0.0;
                if (p!=null) {
                    ItemStack helditem = p.getItemInHand();
                    if (helditem != null) {
                        int enchants = helditem.getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
                        adj = adj + (enchants/10.0);
                    }
                }
                
                if (zomb.items != null) { // Add drops
                    List<ItemStack> drops = event.getDrops();
                    Boolean firstPork = true;
                    drops.clear();
                    
                    
                    for (int i = 0; i < zomb.items.size(); i++) {
                        ItemStack item = zomb.items.get(i);
                        // ZombieMod.logger.info("[" + ZombieMod.myName + "] " + item.getAmount() + " x " +
                        // item.getType().toString());
                        double drop;
                        if (zomb.dropRates != null) {
                            drop = zomb.dropRates.get(i);
                        } else {
                            if (zomb.species.contains("PlayerZombie")) {
                                drop = 1.0D;
                            } else {
                                drop = 0.7D;
                            }
                        }
                        drop = drop + adj;
                        
                        double chance = Math.random();
                        if (chance < drop) {
                            if (firstPork) {
                                if (zomb.species.contains("PlayerZombie") || zomb.species.equalsIgnoreCase("Zombus Sapians")) {
                                    if (item.getType() == Material.PORK) {  // porkchop
                                        String name = zomb.commonName.substring(7);
                                        ItemMeta porkData = item.getItemMeta();
                                        porkData.setDisplayName(name + " Burger");
                                        item.setItemMeta(porkData);
                                        firstPork = false;
                                    }
                                }
                            }
                            Material[] foods = {Material.PORK,Material.COOKED_BEEF, Material.COOKED_CHICKEN,Material.RAW_BEEF,Material.RAW_CHICKEN,Material.RAW_FISH,Material.COOKED_FISH,
                            		Material.BAKED_POTATO,Material.POISONOUS_POTATO,Material.POTATO,Material.POISONOUS_POTATO, Material.ROTTEN_FLESH, Material.APPLE, Material.BREAD,
                            		Material.CAKE, Material.CAKE_BLOCK, Material.CARROT,Material.CARROT_ITEM,Material.GRILLED_PORK,Material.MELON,Material.MELON_BLOCK, Material.MUSHROOM_SOUP,
                            		Material.PUMPKIN_PIE};
                            
                            if (Math.random()>0.9) {
                            	if (Arrays.asList(foods).contains(item.getType())) {
                                    ItemMeta foodData = item.getItemMeta();
                                	List<String> radioactive = new ArrayList<String>();
                                	radioactive.add("Radioactive");
                                	foodData.setLore(radioactive);
                                    item.setItemMeta(foodData);
                                }
                            }
                            drops.add(item);
                        }
                    }
                    if (Math.random()>0.85D) {
                    	if (zomb.equip != null && zomb.equip[0]!= null && zomb.equip[0].getType()==Material.BOW) {
                    		ItemStack bow = new ItemStack(Material.BOW,1);
                    		short durability = (short) ((Math.random()*50.0D)+1.0D);
                    		bow.setDurability(durability);
                    		drops.add(bow);
                    	}
                    }
                }
                
                if (zomb.xp > 0 && p != null) { // add bonus xp
                    int xp = zomb.xp;
                    xp += event.getDroppedExp();
                    event.setDroppedExp(xp);
                }
                
                if (zomb.bounty > 0 && this.plugin.economy != null && p != null) { // add bounty
                    double i = zomb.bounty;
                    this.plugin.economy.depositPlayer(p.getName(), i);
                }
                
                ZombieMod.playerZombies.remove(entity.getUniqueId());
                
            }
        } else {
            List<ItemStack> drops = event.getDrops();
            drops.clear();
        }
    }

    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity))
            return;
        
        if (event.getCause() == DamageCause.SUFFOCATION) {
            if (entity instanceof Zombie) {
                event.setDamage(0);
                Location l = entity.getLocation();
                Location newSafeLoc = Utils.getNearestEmptyLoc(l);
                if (newSafeLoc!=null) {
                    entity.teleport(newSafeLoc);
                }
                event.setCancelled(false);
                return;
            }
        }
        
        
        if (event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.FIRE_TICK) {
            if (entity instanceof Player) {
                Player p = (Player) event.getEntity();
                if (p.hasPermission("zombiemod.flameproof")) {
                    event.setCancelled(true);
                    p.setFireTicks(0);
                    return;
                }
            }
        }
        
        int damage = (int)event.getDamage();
        
        
        Entity damager = null;
        if (event instanceof EntityDamageByEntityEvent)
            damager = ((EntityDamageByEntityEvent) event).getDamager();
        
        if (damager != null) {
            PutredineImmortui attacker = null;
            if (damager instanceof Projectile) {
                LivingEntity shooter = (LivingEntity) ((Projectile) damager).getShooter();
                if (shooter != null) { // && shooter instanceof Zombie
                    attacker = Utils.getZombie(shooter);
                    if (entity.getType()==EntityType.PLAYER) {
                    	if (shooter.hasMetadata("level")) { ((Player)entity).sendMessage("shooter Level: "+shooter.getMetadata("level").get(0).asInt()); }
                    	
/*                    	if (attacker != null ) {
	                    	net.minecraft.server.v1_7_R4.Entity h = ((CraftEntity)shooter).getHandle();
	                		ZombieType zt = (ZombieType)h;                		
		                	Double dlvl = zt.getDamage();
		            		((Player)entity).sendMessage("Zomb damage: "+ dlvl );
                    	}
*/                    }
                }
            } else {
                attacker = Utils.getZombie(damager);
                if (entity.getType()==EntityType.PLAYER) {
                	if (damager.hasMetadata("level")) { ((Player)entity).sendMessage("damager Level: "+damager.getMetadata("level").get(0).asInt()); }
/*                	if (attacker != null ) {
                		net.minecraft.server.v1_7_R4.Entity h = ((CraftEntity)damager).getHandle();
                		ZombieType zt = (ZombieType)h;                		
	                	Double dlvl = zt.getDamage();
	            		((Player)entity).sendMessage("Zomb damage: "+ dlvl );
                	}
*/            	}
            }
            if (attacker != null && attacker.damage > -1) {
                //damage = attacker.damage;            	
            	
				if (attacker.abilities!=null && attacker.abilities.contains("EXPLODE")) {// kaboom
					
					int ticktock = 0;
					if (damager.hasMetadata("kaboom")) {
						ticktock = damager.getMetadata("kaboom").get(0).asInt();
					}
					if (ticktock<=5 && ((LivingEntity)damager).getHealth()>0) {
						event.setCancelled(true);
						event.setDamage(0);
					}
				}
				
                if (entity instanceof Wolf || entity instanceof Ocelot) {
                    if (attacker.abilities != null && attacker.abilities.contains("HUNTER")) {
                        damage = (int) (damage * 0.5F);
                    } else {
                        damage = (int) (damage * 0.2F); 
                    }
                }
            }
        }
        
        PutredineImmortui zomb = Utils.getZombie(entity);
        if (zomb != null && ((LivingEntity) entity).getHealth() >= 0.0D) {
        	
// System.out.print("Type:" + entity.getType());
        	
            Chunk c = entity.getLocation().getChunk();
            String cid = c.getX() + "|" + c.getZ();
            zomb.cid = cid;
            zomb.lastLoc = entity.getLocation();
            if (zomb.species.equals("PlayerZombie")) {
                ZombieMod.playerZombies.put(entity.getUniqueId(), zomb);
            }
            
			if (zomb.abilities != null) {
				if (zomb.abilities.contains("HEROBRINE")) {// HEROBRINE
                    event.setDamage(0);
                    event.setCancelled(true);
                    entity.remove();
                    return;					
				}
			}

            
            if (zomb.effects.contains(Effect.BLAZE_SHOOT) || zomb.effects.contains(Effect.GHAST_SHOOT) || zomb.effects.contains(Effect.EXTINGUISH)) {
                if (event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.FIRE_TICK) {
                    event.setDamage(0);
                    event.setCancelled(true);
                    return;
                }
            }
            
            if (zomb.abilities != null && zomb.abilities.contains("BORG")) {
                Material bob = null;
                Player play = null;
                if (damager != null) {
                    if (damager instanceof Player) {
                        play = (Player) damager;
                        bob = play.getItemInHand().getType();
                    } else if (damager instanceof Projectile) {
                        LivingEntity shooter = (LivingEntity) ((Projectile) damager).getShooter();
                        if (shooter instanceof Player) {
                            play = (Player) shooter;
                        }
                        if (damager instanceof Egg) {
                            bob = Material.EGG;
                        } else if (damager instanceof Snowball) {
                            bob = Material.SNOW_BALL;
                        } else if (damager instanceof ThrownPotion) {
                            bob = Material.POTION;
                        } else {
                            bob = Material.ARROW; // arrow
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
                        
                        if (Utils.checkResistance(entity, bob)) {
                            event.setDamage(0);
                            event.setCancelled(true);
                            damage = 0;
                            boolean send_message = true;
                            if (bob == Material.FIRE) {
                                entity.setFireTicks(0);
                                if (zomb.noBurn) {
                                    send_message = false;
                                }
                                zomb.noBurn = true;
                            }
                            if (send_message) {
                                if (play != null) {
                                    play.sendMessage("Zomborg is adapted to " + bob.name());
                                } else {
                                    List<Entity> entlist = entity.getNearbyEntities(16, 20, 16);
                                    for (Entity ent : entlist) {
                                        if (ent instanceof Player) {
                                            ((Player) ent).sendMessage("Zomborg adapted to " + bob.name());
                                        }
                                    }
                                }
                            }
                        } else {
                            if (damage > 0) {
                                Random generator = new Random();
                                int randomIndex = generator.nextInt(100) + 1;
                                
                                if (randomIndex == 30 || randomIndex > damage + 80) {
                                    Utils.addResistance(entity, bob);
                                    List<Entity> entlist = entity.getNearbyEntities(16, 20, 16);
                                    for (Entity ent : entlist) {
                                        if (ent instanceof Player) {
                                            ((Player) ent).sendMessage("Zomborg have adapted to " + bob.name());
                                            ent.getWorld().playSound(ent.getLocation(), Sound.FIZZ, 1, 1);
                                        } else {
                                            PutredineImmortui z = Utils.getZombie(ent);
                                            if (z != null) {
                                                Utils.addResistance(ent, bob);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (damager != null && damager instanceof Wolf) {
                    damage=damage*5;
                }
            }
            
            
            
            if (damage > 0) {
                damage = damage - zomb.armour;
                if (damage < 1 && zomb.armour > 0) {
                    damage = 1;
                }
            }
            
            if (zomb.potions.contains(ZombieMod.resistPotion) && damage > 1) {
                damage = (damage / 2);
            }
            
            event.setDamage(damage);
            
            LivingEntity tEnt = (LivingEntity) entity;
            
            if (tEnt.getHealth() > damage) { // not dead yet
            
                if (zomb.effects != null) {
                    if (zomb.effects.contains(Effect.ENDER_SIGNAL)) {
                        entity.getWorld().playSound(entity.getLocation(), Sound.ENDERMAN_HIT, 1, 1);
                    } else if (zomb.effects.contains(Effect.GHAST_SHRIEK)) {
                        if (Math.random() > 0.9) {
                            entity.getWorld().playSound(entity.getLocation(), Sound.GHAST_SCREAM2, 1, 1);
                        } else {
                            entity.getWorld().playSound(entity.getLocation(), Sound.GHAST_SCREAM, 1, 1);
                        }
                    } else if (zomb.effects.contains(Effect.BOW_FIRE)) {
                        entity.getWorld().playSound(entity.getLocation(), Sound.SKELETON_HURT, 1, 1);
                    }
                }
                if (zomb.species.contains("PlayerZombie") || (zomb.abilities != null && zomb.abilities.contains("GHOST"))) {
                    entity.getWorld().playSound(entity.getLocation(), Sound.HURT_FLESH, 1, 1);
                }
            }
        } else { // damage entity normally with different damage
            event.setDamage(damage);
        }
    }
    
    // Stop burning....
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityCombust(EntityCombustEvent event) {
        if (event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();
            if (p.hasPermission("zombiemod.flameproof")) {
                event.setCancelled(true);
                p.setFireTicks(0);
            }
        }
    }
    
    // disable targeting
    @SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        Entity entity = event.getEntity();
        PutredineImmortui zomb = Utils.getZombie(entity);
        if (zomb != null) {
            if (zomb.passive) {
                event.setCancelled(true);
            } else {
                if (event.getTarget() instanceof Player) {
                    String pname = ((Player) event.getTarget()).getName();
                    if ((zomb.getOwner() != null) ) { 
                    	if ((zomb.getOwner().equals(pname))) {       
                    		event.setCancelled(true);
                    		return;
                    	}
                    	if (!canHurt(Bukkit.getPlayer(zomb.getOwner()),(Player) event.getTarget())) {
//                        	System.out.print(" targeting cancelled target owners ally");
                    		event.setCancelled(true);
                    		return;
                    	}
                    }
                }                
            }
        } else {
            if (!entity.isEmpty()) {
                Entity pass = entity.getPassenger();
                if (pass instanceof Creature) {
                    event.setTarget(((Creature)entity).getTarget());
                }
            }
        }
    }
    public static Boolean canHurt(Player p, Player t) {
	    MPlayer up = MPlayer.get(p);
	    MPlayer ut = MPlayer.get(t);
	    
		Rel rel = up.getRelationTo(ut);
		
		switch(rel) {
			case LEADER:
			case OFFICER: 
			case MEMBER: 
			case RECRUIT: 
			case ALLY: 
			case TRUCE:
				return false;
			case NEUTRAL: 
			case ENEMY:
				return true;
		}		
		return null;
	}
    // exploding
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        Entity entity = event.getEntity();
        
        if (entity instanceof Fireball)
            entity = (Entity) ((Fireball) entity).getShooter();
        
        PutredineImmortui zomb = Utils.getZombie(entity);
        if (zomb != null) {
            event.setFire(true);
            if (zomb.passive) {
                event.setCancelled(true);
            }
        }
    }
    
    // boom!
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity == null)
            return;
        
        if (entity instanceof EnderDragon){
            event.setCancelled(true);
            return;
        }
        
        PutredineImmortui zomb = Utils.getZombie(entity);
        if (zomb != null) {
            event.blockList().clear();
            if (zomb.passive) {
                event.setCancelled(true);
                return;
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onPotionSplashEvent(PotionSplashEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            Player molatov = (Player) event.getEntity().getShooter();
            if (molatov.hasPermission("zombiemod.tamer")) {
                for (PotionEffect pe : event.getPotion().getEffects()) {
                    if (pe.getType().equals(PotionEffectType.POISON)) {
                        for (LivingEntity le : event.getAffectedEntities()) {
                            PutredineImmortui z = Utils.getZombie(le);
                            if (z != null) {
                                z.setOwner(molatov.getName());
                                ((Monster) le).setTarget(null);
                            }
                        }
                    }
                }
            }
        }
    }
    
    // chunk loader
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity ent : event.getChunk().getEntities()) {
            if (ent instanceof Zombie) {
                Zombie z = (Zombie) ent;
                if (z.getCustomName() != null) {
                    
                    if (Utils.getZombie(ent) != null) {  // skip 'real' zombies
                        continue;
                    }
                    
                    String cleanname = Utils.cleanName(z.getCustomName()).toLowerCase();
                    // if clean name is a zombie type - swapit!
                    if (plugin.genera.configs.containsKey(cleanname)) {
                        PutredineImmortui zomb = new PutredineImmortui(plugin, cleanname);
                        Utils.spawnZombie(zomb, z.getLocation(), plugin, 50, (int)z.getHealth());
                        z.remove();
                        if (ZombieMod.debugMode) {
                            ZombieMod.logger.info("Reg Zombie converted to " + cleanname + ".");
                        }
                    } else if (ChatColor.stripColor(cleanname.toLowerCase()).startsWith("corpse")) {
                        String pname = ChatColor.stripColor(cleanname.toLowerCase()).substring(7);
                        Iterator<Map.Entry<UUID, PutredineImmortui>> zombieCorpseIterator = ZombieMod.playerZombies.entrySet().iterator();
                        while (zombieCorpseIterator.hasNext()) {
                            Map.Entry<UUID, PutredineImmortui> entry = zombieCorpseIterator.next();
                            PutredineImmortui zombieCorpeData = entry.getValue();
                            
                            if (zombieCorpeData.commonName.toLowerCase().endsWith(pname)) { // found him!

                                //check for nearby corpses
                                Boolean solo = true;
                                List<Entity> elist = z.getNearbyEntities(16, 16, 16);
                                for (Entity nbe : elist) {
                                    if (nbe.getUniqueId() != z.getUniqueId()) { //!not same zombie
                                        PutredineImmortui potentialcorpse = Utils.getZombie(nbe);
                                        if (potentialcorpse != null) {  // skip 'real' zombies
                                            if (potentialcorpse.commonName.toLowerCase().endsWith(pname)) { // found him! again!
                                                //stop hes already here!
                                                solo=false;
                                            }
                                        }
                                    }
                                }
                                
                                if(solo){
                                
                                net.minecraft.server.v1_7_R4.World mcWorld = ((CraftWorld) z.getWorld()).getHandle();
                                
                                if (ZombieMod.debugMode) {
                                    ZombieMod.logger.info("[zm]  " + zombieCorpeData.commonName + " rises from the dead! ");
                                }
                                
                                ZombieType newzomb = new ZombieType(mcWorld, zombieCorpeData);
                                newzomb.setPosition(z.getLocation().getX(), z.getLocation().getY(), z.getLocation().getZ());
                                mcWorld.addEntity(newzomb, SpawnReason.CUSTOM);
                                
                                zombieCorpseIterator.remove();
                                z.remove();

                                ZombieMod.logger.info("Corpse Zombie converted to " + cleanname + ".");
                                continue;
                                }
                            }
                        }
                    } else {
                        if (ZombieMod.debugMode) {
                            ZombieMod.logger.info("Did not recognise: " + cleanname);
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        Entity entity = event.getEntity();
        if (entity == null)
            return;
        PutredineImmortui zomb = Utils.getZombie(entity);
        if (zomb != null) {
            if (zomb.effects.contains(Effect.BLAZE_SHOOT)) {
                Entity arrow = event.getProjectile();
                arrow.setFireTicks(99);
                event.setProjectile(arrow);
            } else if (zomb.effects.contains(Effect.GHAST_SHOOT)) {
                
                LivingEntity targ = null;
                if (entity instanceof Monster) {
                    targ = ((Monster) entity).getTarget();
                }
                if (targ != null) {
                    
                    if (targ.getWorld() == entity.getWorld()) {
                        Location target = targ.getLocation();
                        Location from = entity.getLocation().add(0, 2, 0);
                        if (from.distance(target) > 2) {

                            String ord = (Utils.ordinal(entity.getLocation()));
                            
                            if (ord.contains("North")) {
                                from.add(-1, 0, 0);
                            }
                            if (ord.contains("South")) {
                                from.add(1, 0, 0);
                            }
                            if (ord.contains("East")) {
                                from.add(0, 0, 0 - 1);
                            }
                            if (ord.contains("West")) {
                                from.add(0, 0, 1);
                            }
                            //Location firePath = Utils.lookAt(from, target);
                            
                            Entity tmparrow = event.getProjectile();   
                            Vector vec = tmparrow.getVelocity();
                            Location loc = tmparrow.getLocation(); 

                            //Fireball fb = (Fireball)entity.getWorld().spawnEntity(firePath, EntityType.FIREBALL);
                            Fireball fb = (Fireball)entity.getWorld().spawnEntity(loc, EntityType.FIREBALL);
                            fb.setYield(1);
                            fb.setBounce(false);
                            fb.setVelocity(vec);
                            
                            event.setProjectile(fb);
                            
                            entity.getWorld().playEffect(entity.getLocation(), Effect.GHAST_SHOOT, 0);
                        }
                    }
                }
                //event.setCancelled(true);
                //event.setProjectile(null);
                return;
            }
        }
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityCreatePortalEvent(EntityCreatePortalEvent event) {
            if (event.getEntity() instanceof EnderDragon) {
                //if (event.getEntity().getWorld().getEnvironment() == Environment.NETHER) {
                    event.setCancelled(true);
                //}
            }
    }
}
