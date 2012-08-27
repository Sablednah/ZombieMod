package me.sablednah.zombiemod;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import org.getspout.spoutapi.SpoutServer;
import org.getspout.spoutapi.player.EntitySkinType;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;


public class Utils {

	/**
	 * Converts InputStream to String
	 * 
	 * One-line 'hack' to convert InputStreams to strings.
	 * 
	 * @param is
	 *            The InputStream to convert
	 * @return returns a String version of 'is'
	 */
	public static String convertStreamToString(InputStream is) {
		return new Scanner(is).useDelimiter("\\A").next();
	}

	/**
	 * Joins two arrays
	 * 
	 * @param first
	 *            array
	 * @param second
	 *            array
	 * @return Arrays joined
	 */
	public static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}


	public static void setTempnvluln(LivingEntity e, int d) {
		if (e != null) {
			e.setMaximumNoDamageTicks(10);
			e.setNoDamageTicks(0);
			e.setLastDamage(d);
		}
	}

	public static Block getNearestEmptySpace(Block b, int maxradius) {
		BlockFace[] faces = {BlockFace.UP, BlockFace.NORTH, BlockFace.EAST};
		BlockFace[][] orth = {{BlockFace.NORTH, BlockFace.EAST}, {BlockFace.UP, BlockFace.EAST}, {BlockFace.NORTH, BlockFace.UP}};
		for (int r = 0; r <= maxradius; r++) {
			for (int s = 0; s < 6; s++) {
				BlockFace f = faces[s%3];
				BlockFace[] o = orth[s%3];
				if (s >= 3)
					f = f.getOppositeFace();
				Block c = b.getRelative(f, r);
				for (int x = -r; x <= r; x++) {
					for (int y = -r; y <= r; y++) {
						Block a = c.getRelative(o[0], x).getRelative(o[1], y);
						if (a.getTypeId() == 0 && a.getRelative(BlockFace.UP).getTypeId() == 0)
							return a;
					}
				}
			}
		}
		return null;// no empty space within a cube of (2*(maxradius+1))^3
	}

	public static Location lookAt(Location loc, Location lookat) {
		// Clone the loc to prevent applied changes to the input loc
		loc = loc.clone();

		// Values of change in distance (make it relative)
		double dx = lookat.getX() - loc.getX();
		double dy = lookat.getY() - loc.getY();
		double dz = lookat.getZ() - loc.getZ();

		// Set yaw
		if (dx != 0) {
			// Set yaw start value based on dx
			if (dx < 0) {
				loc.setYaw((float) (1.5 * Math.PI));
			} else {
				loc.setYaw((float) (0.5 * Math.PI));
			}
			loc.setYaw((float) loc.getYaw() - (float) Math.atan(dz / dx));
		} else if (dz < 0) {
			loc.setYaw((float) Math.PI);
		}

		// Get the distance from dx/dz
		double dxz = Math.sqrt(Math.pow(dx, 2) + Math.pow(dz, 2));

		// Set pitch
		loc.setPitch((float) -Math.atan(dy / dxz));

		// Set values, convert to degrees (invert the yaw since Bukkit uses a
		// different yaw dimension format)
		loc.setYaw(-loc.getYaw() * 180f / (float) Math.PI);
		loc.setPitch(loc.getPitch() * 180f / (float) Math.PI);

		return loc;

	}

	public static String ordinal(Location l) {
		double rot = (l.getYaw() - 90) % 360;
		if (rot < 0) {
			rot += 360.0;
		}
		return getDirection(rot);
	}

	private static String getDirection(double rot) {
		if (0 <= rot && rot < 22.5) {
			return "North";
		} else if (22.5 <= rot && rot < 67.5) {
			return "NorthEast";
		} else if (67.5 <= rot && rot < 112.5) {
			return "East";
		} else if (112.5 <= rot && rot < 157.5) {
			return "SouthEast";
		} else if (157.5 <= rot && rot < 202.5) {
			return "South";
		} else if (202.5 <= rot && rot < 247.5) {
			return "SouthWest";
		} else if (247.5 <= rot && rot < 292.5) {
			return "West";
		} else if (292.5 <= rot && rot < 337.5) {
			return "NorthWest";
		} else if (337.5 <= rot && rot < 360) {
			return "North";
		} else {
			return null;
		}
	}

	public static Boolean findZombie(UUID id) {
		for (World w : Bukkit.getServer().getWorlds()) {
			for(Entity e : w.getEntitiesByClass(Zombie.class)) {
				if (id == e.getUniqueId()) { 
					return true; 
				}
			}
		}
		return false;
	}

	public Zombie findPlayerZombie(String id) {
		List<World> worlds = Bukkit.getServer().getWorlds();
		for (World w : worlds) {
			Collection<Zombie> zombies = w.getEntitiesByClass(Zombie.class);
			for (Zombie e : zombies) {
				PutredineImmortui zomb = ZombieType.getZombie(e);
				if (zomb!=null && zomb.species.equals("PlayerZombie")) {
					if (zomb.uniqueid.equals(id)) { return e; }
				}
			}
		}
		return null;
	}

	public static void setSkin(LivingEntity target, String url) {
		if (url != null) {
//			if (ZombieMod.debugMode) { System.out.print("url=" + url); }
			SpoutServer bob = new SpoutServer();
			bob.setEntitySkin(target, url, EntitySkinType.DEFAULT);
		}
	}
	
	public static void spawnCorpsesInChunk(Chunk c) {
		
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
	        	    if (ZombieMod.debugMode) { ZombieMod.logger.info("[" + ZombieMod.myName + "] found player zombie 'elsewhere'."); }
				} else {
					// should have playerzombie - cant find it!
				    if (ZombieMod.debugMode) { ZombieMod.logger.info("[" + ZombieMod.myName + "] player zombie lost - recreating..." + key);}

					Block newLoc = z.lastLoc.getBlock();
					
					Block safeNewBlock = Utils.getNearestEmptySpace(newLoc, 4);
					if (safeNewBlock!=null) {
						
						Location sqawnLoc=safeNewBlock.getLocation();
						net.minecraft.server.World mcWorld = ((CraftWorld) c.getWorld()).getHandle();
						if (ZombieMod.debugMode) { ZombieMod.logger.info("[" + ZombieMod.myName + "] " + z.commonName +" rises from the dead! "); }
						z.cid = cid;
						ZombieType newzomb = new ZombieType(mcWorld,z);
						newzomb.setPosition(sqawnLoc.getX(), sqawnLoc.getY(), sqawnLoc.getZ());
						mcWorld.addEntity(newzomb, SpawnReason.CUSTOM);
						it.remove();
					} else {
						if (ZombieMod.debugMode) { ZombieMod.logger.info("[" + ZombieMod.myName + "] safe place not found"); }
					}
				}
	        }
	    }
	}
	

	public static boolean isNotCalled(Location l, String factionname) {
		if (ZombieMod.hasFactions) {
			// P f = (P) plugin.getServer().getPluginManager().getPlugin("Factions");
			// Faction fact = Board.getFactionAt(l);
			FLocation fl = new FLocation(l);
			Faction fact = Board.getFactionAt(fl);

			String factName;
			
			if (fact != null) { 
				factName = fact.getTag();
			} else {
				factName = "none";
			}

			factName = factName.toLowerCase();
			factName = ChatColor.stripColor(factName);

			if (!factName.equals(factionname)) {
//				if (ZombieMod.debugMode) {
//					ZombieMod.logger.info("[" + ZombieMod.myName + "] factName: '" + factName + "' denied (looked for " + factionname + ")");
//				}
				return true;
			}
		} else {
			double dist = ZombieMod.spawnLoc.distance(l);
			if (dist < 50) {
				return true;
			}
		}
		return false;
	}
}
