package me.sablednah.zombiemod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
//import java.util.Random;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;


public class PlayerListener implements Listener {

	public ZombieMod plugin;

	public PlayerListener(ZombieMod instance) {
		this.plugin=instance;
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {	
		int health;
		int armour;
		String name;

		Player p = event.getEntity();
		health=p.getMaxHealth();
		name=p.getName();

		if (ZombieMod.hasHeroes)  {
			Heroes heroes = (Heroes) plugin.getServer().getPluginManager().getPlugin("Heroes");
			Hero hero = heroes.getCharacterManager().getHero(p);
			health=hero.getMaxHealth();
			hero = null;
			heroes = null;
		}

		armour = (calculateArmour(p) / 4);

		Config newPlayer = new Config();

		newPlayer.commonName=name;
		newPlayer.armour=armour;
		newPlayer.maxHealth=health;

		newPlayer.damage = calculateDammage(p);
		newPlayer.bounty = 5;
		newPlayer.species = "PlayerZombie";
		newPlayer.agro = 16;
		newPlayer.noBurn= true;
		newPlayer.passive=false;
		newPlayer.speed = 1.1;
		newPlayer.xp = 10 + newPlayer.damage + (4 * newPlayer.armour);


		List<ItemStack> drops = event.getDrops();

		newPlayer.items =  new ArrayList<ItemStack>();
		newPlayer.dropRates = new ArrayList<Double>();

		if (ZombieMod.givezombieplayeritems) {
			for (ItemStack drop : drops ) {
				if (ZombieMod.debugMode) { ZombieMod.logger.info("[" + ZombieMod.myName + "] " + drop.getAmount() + " x " + drop.getType().toString()); }
				newPlayer.items.add(drop);
				newPlayer.dropRates.add(1.0);
			}
			drops.clear();
		}

		Location sqawnLoc=p.getLocation();
		Chunk chunkster=sqawnLoc.getChunk();
		if (chunkster.isLoaded()) {
			ZombieMod.logger.info("[" + ZombieMod.myName + "] Corpse chunk is already loaded.");
		} else {
			ZombieMod.logger.info("[" + ZombieMod.myName + "] Loading corpse chunk.");
			Boolean loaded;
			loaded=chunkster.load();
			if (loaded) { 
				ZombieMod.logger.info("[" + ZombieMod.myName + "] corpse chunk loaded.");
			} else {
				ZombieMod.logger.info("[" + ZombieMod.myName + "] corpse chunk epic load fail!.");
			}
		}
		World w = sqawnLoc.getWorld();
		net.minecraft.server.World mcWorld = ((CraftWorld) w).getHandle();

		PutredineImmortui zomb = new PutredineImmortui(plugin,newPlayer);
		
		String cid = chunkster.getX() + "|"+chunkster.getZ();
		zomb.cid = cid;
		zomb.lastLoc=sqawnLoc;
		//if (ZombieMod.debugMode) { ZombieMod.logger.info("[" + ZombieMod.myName + "] " + zomb.commonName +" spawned via player death."); }
		ZombieMod.logger.info("[" + ZombieMod.myName + "] " + zomb.commonName +" spawned via player death at X:" + Math.floor(sqawnLoc.getChunk().getX()) + " Z:"+ Math.floor(sqawnLoc.getChunk().getZ()));
		
		ZombieType newzomb = new ZombieType(mcWorld,zomb);
		newzomb.setPosition(sqawnLoc.getX(), sqawnLoc.getY(), sqawnLoc.getZ());
		mcWorld.addEntity(newzomb, SpawnReason.CUSTOM);
		//ZombieMod.playerZombies.put(cid,zomb);
	}

	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onChunkLoad(ChunkLoadEvent event) { 
		Chunk c = event.getChunk();
		String cid = c.getX() + "|"+c.getZ();
		
		//ZombieMod.logger.info("[" + ZombieMod.myName + "] Checking  chunk "+cid);
		
		Iterator<Map.Entry<UUID, PutredineImmortui>> it = ZombieMod.playerZombies.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<UUID, PutredineImmortui> entry = it.next();
	        PutredineImmortui z = entry.getValue();
	        if (z.cid.equals(cid)) {
	        	//found potential
	        	UUID key = entry.getKey();
	        	if (key!= null && Utils.findZombie(key)) {
					// found it
					ZombieMod.logger.info("[" + ZombieMod.myName + "] found player zombie 'elsewhere'.");
				} else {
					// should have playerzombie - cant find it!
					ZombieMod.logger.info("[" + ZombieMod.myName + "] player zombie lost - recreating..." + key);
/*
 					Random rnd = new Random();

					
					int xPos = rnd.nextInt(6)+6+(c.getX()*16);
					int zPos = rnd.nextInt(6)+6+(c.getZ()*16);
					int yPos = 65;
*/
					
					//Block newLoc = new Location(c.getWorld(), xPos, yPos, zPos).getBlock();
					Block newLoc = z.lastLoc.getBlock();
					
					Block safeNewBlock = Utils.getNearestEmptySpace(newLoc, 4);
					if (safeNewBlock!=null) {
						
						Location sqawnLoc=safeNewBlock.getLocation();
						net.minecraft.server.World mcWorld = ((CraftWorld) c.getWorld()).getHandle();
						//if (ZombieMod.debugMode) { ZombieMod.logger.info("[" + ZombieMod.myName + "] Player Zombie " + z.commonName +" spawned via CHUNKLOAD"); }
						ZombieMod.logger.info("[" + ZombieMod.myName + "] Player Zombie " + z.commonName +" spawned via CHUNKLOAD");
						z.cid = cid;
						ZombieType newzomb = new ZombieType(mcWorld,z);
						newzomb.setPosition(sqawnLoc.getX(), sqawnLoc.getY(), sqawnLoc.getZ());
						mcWorld.addEntity(newzomb, SpawnReason.CUSTOM);
						it.remove();
					} else {
						ZombieMod.logger.info("[" + ZombieMod.myName + "] safe place not found");
					}
				}
	        }
	    }
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onChunkUnload(ChunkUnloadEvent event) { 
		Chunk c = event.getChunk();
		int chunkX = c.getX();
		int chunkZ = c.getZ();	
		
		//ZombieMod.logger.info("[" + ZombieMod.myName + "] chunk " + c.getX() + ", " + c.getZ() + " wants to unload.");
		for (Entity e : c.getEntities()) {
			PutredineImmortui zomb = ZombieType.getZombie(e);
			if (zomb != null) { 
				if (zomb.species.equals("PlayerZombie")) {
					event.setCancelled(true);
					ZombieMod.logger.info("[" + ZombieMod.myName + "] chunk " + chunkX + ", " + chunkZ + " preserved for "+zomb.species+"|"+zomb.commonName+"'.");
					return;
				}
			}
		}
	}

	public int calculateDammage(Player p) {
		int total=0;
		if (p != null ){
			PlayerInventory inv = p.getInventory();
			if (inv.getItemInHand() != null && inv.getItemInHand().getTypeId() > 0) {
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
		int total=0;
		if (p != null ){
			PlayerInventory inv = p.getInventory();
			if (inv.getHelmet() != null && inv.getHelmet().getTypeId() >0) {
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
			if (inv.getChestplate() != null && inv.getChestplate().getTypeId() >0) {
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
			if (inv.getBoots() != null && inv.getBoots().getTypeId() >0) {
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
			if (inv.getLeggings() != null && inv.getLeggings().getTypeId() >0) {
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
