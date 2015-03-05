package me.sablednah.zombiemod;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.v1_8_R1.BlockPosition;
import net.minecraft.server.v1_8_R1.EntityEnderSignal;

import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R1.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class PlayerListener implements Listener {

	public ZombieMod	plugin;

	public PlayerListener(ZombieMod instance) {
		this.plugin = instance;
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		int health;
		int armour;
		String name;

		Player p = event.getEntity();
		health = (int)p.getMaxHealth();
		name = p.getName();

		armour = (calculateArmour(p) / 4);

		Config newPlayer = new Config();

		newPlayer.commonName = "Corpse " + name;
		newPlayer.armour = armour;
		newPlayer.maxHealth = health;

		newPlayer.damage = calculateDammage(p);
		newPlayer.bounty = 5;
		newPlayer.species = "PlayerZombie";
		newPlayer.agro = 16;
		newPlayer.noBurn = true;
		newPlayer.passive = false;
		newPlayer.speed = 1.1;
		newPlayer.size = 1;
		newPlayer.xp = 10 + newPlayer.damage + (4 * newPlayer.armour);

		List<ItemStack> drops = event.getDrops();

		newPlayer.items = new ArrayList<ItemStack>();
		newPlayer.dropRates = new ArrayList<Double>();

		if (ZombieMod.givezombieplayeritems) {
			for (ItemStack drop : drops) {
				if (ZombieMod.debugMode) {
					ZombieMod.logger.info("[" + this.plugin.myName + "] " + drop.getAmount() + " x " + drop.getType().toString());
				}
				newPlayer.items.add(drop);
				newPlayer.dropRates.add(1.0);
			}
			drops.clear();
		}

		Location sqawnLoc = p.getLocation();
		Chunk chunkster = sqawnLoc.getChunk();
		if (chunkster.isLoaded()) {
			if (ZombieMod.debugMode) {
				ZombieMod.logger.info("[" + this.plugin.myName + "] Corpse chunk is already loaded.");
			}
		} else {
			if (ZombieMod.debugMode) {
				ZombieMod.logger.info("[" + this.plugin.myName + "] Loading corpse chunk.");
			}
			Boolean loaded;
			loaded = chunkster.load();
			if (loaded) {
				if (ZombieMod.debugMode) {
					ZombieMod.logger.info("[" + this.plugin.myName + "] corpse chunk loaded.");
				}
			} else {
				if (ZombieMod.debugMode) {
					ZombieMod.logger.info("[" + this.plugin.myName + "] corpse chunk epic load fail!.");
				}
			}
		}

		PutredineImmortui zomb = new PutredineImmortui(plugin, newPlayer);

		String cid = chunkster.getX() + "|" + chunkster.getZ();
		zomb.cid = cid;
		zomb.lastLoc = sqawnLoc;
		// if (ZombieMod.debugMode) { ZombieMod.logger.info("[" + ZombieMod.myName + "] " + zomb.commonName
		// +" spawned via player death."); }
		ZombieMod.logger.info("[" + this.plugin.myName + "] " + zomb.commonName + " spawned via player death at X:" + Math.floor(sqawnLoc.getChunk().getX()) + " Z:" + Math.floor(sqawnLoc.getChunk().getZ()));

		if (ZombieMod.hasBeardStat) {
			Utils.addStat(name,"ZombieMod","corpsespawns",1);
		}
		
		Utils.spawnZombie(zomb, sqawnLoc, plugin);
		// ZombieMod.playerZombies.put(cid,zomb);
		
		
	              PutredineImmortui zomb2 = Utils.getZombie(p.getKiller());
	                
	                if (zomb2 != null) {
	                    String thistype;
	                    if (zomb2.abilities != null && zomb2.abilities.contains("GHOST")) {
	                        thistype = "Ghost";
	                    } else {
	                        if (zomb2.species.equalsIgnoreCase("PlayerZombie")) {
	                            thistype = "PlayerZombie";
	                        } else {
	                            thistype = zomb2.commonName;
	                        }
	                    }
	                    Utils.addStat(name, "ZombieMod", "deaths_"+thistype, 1);
	                    
	                   
	                    DamageCause cause = p.getLastDamageCause().getCause();
	                    if (cause != null) {
	                        
	                        Utils.addStat(name, "ZombieMod", "killedby_ "+cause.toString().toLowerCase().replace("_", "") + thistype, 1); // kill type and zomb type
	                        Utils.addStat(name, "ZombieMod", "killedby_"+cause.toString().toLowerCase().replace("_", ""), 1); // kill type
	                    }
	                    if (cause == DamageCause.PROJECTILE) {
	                        Utils.addStat(name, "ZombieMod", "killedby_range_" + thistype, 1);
	                    }
	                }
		
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		ItemStack item = event.getItem();
		if (event.getPlayer().getName().toLowerCase().equals("sablednah")) {
			if (item != null && item.getType() == Material.BLAZE_ROD) {
				List<Entity> entlist = event.getPlayer().getNearbyEntities(16, 16, 16);
				System.out.print("You're my wife now dave!!");
				for (Entity ent : entlist) {
					PutredineImmortui z = Utils.getZombie(ent);
					if (z != null) {
						z.setOwner(event.getPlayer().getName());
						((Monster) ent).setTarget(null);
					}
				}
			}
		}
		// System.out.print(item.getType());
		// cancel ender eye signal and create artificial one pointing to closest player
		if (item != null && item.getType() == Material.EYE_OF_ENDER) {
			if (event.getAction() != null && (event.getAction() == Action.RIGHT_CLICK_AIR)) {
				event.setCancelled(true);
				Player p = event.getPlayer();
				if (p.hasPermission("zombiemod.endereye")) {
					Location l = p.getEyeLocation();

					List<Player> targets = p.getWorld().getPlayers();
					Location closest = null;
					double closestDist = 32768.00;
					for (Player target : targets) {
						if (target.isOnline() && target != p) {
							if (target.getEyeLocation().distanceSquared(l) < closestDist) {
								closestDist = target.getEyeLocation().distance(l);
								closest = target.getEyeLocation();
							}
						}
					}

					if (closest != null) {
						System.out.print("Closest: " + closest.getBlockX() + " , " + closest.getBlockY() + " , " + closest.getBlockZ());

						ItemStack ep = new ItemStack(Material.EYE_OF_ENDER, 1);
						p.getInventory().remove(ep);
						net.minecraft.server.v1_8_R1.World paramWorld = ((CraftWorld) p.getWorld()).getHandle();
						EntityEnderSignal localEntityEnderSignal = new EntityEnderSignal(paramWorld, l.getX(), l.getY(), l.getZ());
						// localEntityEnderSignal.a(closest.getChunk().getX(), (int) closest.getY() / 16,
						// closest.getChunk().getZ());
						localEntityEnderSignal.a(new BlockPosition(closest.getX(), closest.getBlockY(), closest.getZ()));
						paramWorld.addEntity(localEntityEnderSignal);
						l.getWorld().playEffect(l, Effect.BOW_FIRE, 0);
					}
				}
			}
		}
	}

	/*
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onChunkLoad(ChunkLoadEvent event) {
		Chunk c = event.getChunk();
		Utils.spawnCorpsesInChunk(c);
	}
	*/
	
	
	/*
	 * Removed no longer need to preserve chunks
	 * 
	 * @EventHandler(priority=EventPriority.HIGHEST) public void onChunkUnload(ChunkUnloadEvent event) { Chunk c =
	 * event.getChunk(); int chunkX = c.getX(); int chunkZ = c.getZ();
	 * 
	 * //ZombieMod.logger.info("[" + ZombieMod.myName + "] chunk " + c.getX() + ", " + c.getZ() + " wants to unload.");
	 * for (Entity e : c.getEntities()) { PutredineImmortui zomb = ZombieType.getZombie(e); if (zomb != null) { if
	 * (zomb.species.equals("PlayerZombie")) { event.setCancelled(true); ZombieMod.logger.info("[" + ZombieMod.myName +
	 * "] chunk " + chunkX + ", " + chunkZ + " preserved for "+zomb.species+"|"+zomb.commonName+"'."); return; } } } }
	 */

	public int calculateDammage(Player p) {
		int total = 0;
		if (p != null) {
			PlayerInventory inv = p.getInventory();
			if (inv.getItemInHand() != null && inv.getItemInHand().getType() != Material.AIR) {
				switch (inv.getItemInHand().getType()) {
					case DIAMOND_SWORD:
						total = 14;
						break;
					case GOLD_SWORD:
						total = 12;
						break;
					case IRON_SWORD:
						total = 10;
						break;
					case STONE_SWORD:
						total = 8;
						break;
					case WOOD_SWORD:
						total = 7;
						break;
					case DIAMOND_SPADE:
					case GOLD_SPADE:
					case IRON_SPADE:
					case WOOD_SPADE:

					case DIAMOND_AXE:
					case GOLD_AXE:
					case IRON_AXE:
					case WOOD_AXE:
						total = 6;
						break;
					default:
						total = 5;
				}
			}
		}
		return total;
	}

	public int calculateArmour(Player p) {
		int total = 0;
		if (p != null) {
			PlayerInventory inv = p.getInventory();
			if (inv.getHelmet() != null && inv.getHelmet().getType() != Material.AIR) {
				switch (inv.getHelmet().getType()) {
					case DIAMOND_HELMET:
						total += 3;
						break;
					case IRON_HELMET:
					case GOLD_HELMET:
					case CHAINMAIL_HELMET:
						total += 2;
						break;
					case LEATHER_HELMET:
						total += 1;
						break;
				}
			}
			if (inv.getChestplate() != null && inv.getChestplate().getType() != Material.AIR) {
				switch (inv.getChestplate().getType()) {
					case DIAMOND_CHESTPLATE:
						total += 3;
						break;
					case IRON_CHESTPLATE:
					case GOLD_CHESTPLATE:
					case CHAINMAIL_CHESTPLATE:
						total += 2;
						break;
					case LEATHER_CHESTPLATE:
						total += 1;
						break;
				}
			}
			if (inv.getBoots() != null && inv.getBoots().getType()  != Material.AIR) {
				switch (inv.getBoots().getType()) {
					case DIAMOND_BOOTS:
						total += 3;
						break;
					case IRON_BOOTS:
					case GOLD_BOOTS:
					case CHAINMAIL_BOOTS:
						total += 2;
						break;
					case LEATHER_BOOTS:
						total += 1;
						break;
				}
			}
			if (inv.getLeggings() != null && inv.getLeggings().getType() != Material.AIR) {
				switch (inv.getLeggings().getType()) {
					case DIAMOND_LEGGINGS:
						total += 3;
						break;
					case IRON_LEGGINGS:
					case GOLD_LEGGINGS:
					case CHAINMAIL_LEGGINGS:
						total += 2;
						break;
					case LEATHER_LEGGINGS:
						total += 1;
						break;
				}
			}
		}
		return total;
	}

}
