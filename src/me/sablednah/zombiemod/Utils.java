package me.sablednah.zombiemod;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

/*
import com.tehbeard.beardstat.BeardStat;
import com.tehbeard.beardstat.containers.EntityStatBlob;
import com.tehbeard.beardstat.listeners.defer.DelegateIncrement;
import com.tehbeard.beardstat.listeners.defer.DelegateSet;

import net.dragonzone.promise.Promise;
*/

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
//import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Dispenser;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Furnace;
import org.bukkit.block.Jukebox;
import org.bukkit.block.NoteBlock;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Variant;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import org.getspout.spoutapi.SpoutServer;
import org.getspout.spoutapi.player.EntitySkinType;

import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.massivecore.ps.PS;
import com.nitnelave.CreeperHeal.CreeperHandler;

public class Utils {
    
    /**
     * Converts InputStream to String
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
            e.setNoDamageTicks(e.getMaximumNoDamageTicks());
            e.setLastDamage(d);
        }
    }
    
    public static Block getNearestEmptySpace(Block b, int maxradius) {
/*        
        BlockFace[] faces = { BlockFace.UP, BlockFace.NORTH, BlockFace.EAST };
        BlockFace[][] orth = { { BlockFace.NORTH, BlockFace.EAST }, { BlockFace.UP, BlockFace.EAST }, { BlockFace.NORTH, BlockFace.UP } };
        for (int r = 0; r <= maxradius; r++) {
            for (int s = 0; s < 6; s++) {
                BlockFace f = faces[s % 3];
                BlockFace[] o = orth[s % 3];
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
  */
        try {
            Location loc = LocationUtil.getSafeDestination(b.getLocation());
            return loc.getBlock();
        } catch (Exception e) {
            // error finding safe
            return null;
        }
    }
    
    public static Location getNearestEmptyLoc(Location l) {
        try {
            Location loc = LocationUtil.getSafeDestination(l);
            return loc;
        } catch (Exception e) {
            // error finding safe
            return null;
        }
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
            for (Entity e : w.getEntitiesByClass(Zombie.class)) {
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
                PutredineImmortui zomb = Utils.getZombie(e);
                if (zomb != null && zomb.species.equals("PlayerZombie")) {
                    if (zomb.uniqueid.equals(id)) {
                        return e;
                    }
                }
            }
        }
        return null;
    }
    
    public static void setSkin(LivingEntity target, String url) {
        if (url != null) {
            // if (ZombieMod.debugMode) { System.out.print("url=" + url); }
            SpoutServer bob = new SpoutServer();
            bob.setEntitySkin(target, url, EntitySkinType.DEFAULT);
        }
    }
    
    public static void spawnCorpsesInChunk(Chunk c) {
        
        String cid = c.getX() + "|" + c.getZ();
        
        // ZombieMod.logger.info("[" + ZombieMod.myName + "] Checking  chunk "+cid);
        
        Iterator<Map.Entry<UUID, PutredineImmortui>> it = ZombieMod.playerZombies.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PutredineImmortui> entry = it.next();
            PutredineImmortui z = entry.getValue();
            if (z.cid.equals(cid)) {
                // found potential
                UUID key = entry.getKey();
                if (key != null && Utils.findZombie(key)) {
                    // found it
                    if (ZombieMod.debugMode) {
                        ZombieMod.logger.info("[zm] Found player zombie 'elsewhere'.");
                    }
                } else {
                    // should have playerzombie - cant find it!
                    if (ZombieMod.debugMode) {
                        ZombieMod.logger.info("[zm] Player zombie lost - recreating..." + key);
                    }
                    
                    //Block newLoc = z.lastLoc.getBlock();
                    
                    //Block safeNewBlock = Utils.getNearestEmptySpace(newLoc, 4);
                    Location safeLoc = Utils.getNearestEmptyLoc(z.lastLoc);
                    if (safeLoc != null) {
                        
                        Location sqawnLoc = safeLoc.clone();
                        net.minecraft.server.v1_7_R4.World mcWorld = ((CraftWorld) c.getWorld()).getHandle();
                        
//                      if (ZombieMod.debugMode) {
                            ZombieMod.logger.info("[zm]  " + z.commonName + " rises from the dead - via proximity check!");
  //                    }
                        z.cid = cid;
                        ZombieType newzomb = new ZombieType(mcWorld, z);
                        newzomb.setPosition(sqawnLoc.getX(), sqawnLoc.getY(), sqawnLoc.getZ());
                        mcWorld.addEntity(newzomb, SpawnReason.CUSTOM);
                        
                        it.remove();
                    } else {
                        if (ZombieMod.debugMode) {
                            ZombieMod.logger.info("[zm]  safe place not found");
                        }
                    }
                }
            }
        }
    }
    
    public static String getFactName(Location l) {
        if (ZombieMod.hasFactions) {
            Faction fact = BoardColl.get().getFactionAt(PS.valueOf(l));
            
            String factName;
            
            if (fact != null) {
                factName = fact.getName();
            } else {
                factName = null;
            }
            
            factName = factName.toLowerCase();
            factName = ChatColor.stripColor(factName);
            
            return factName;
        }
        return null;
    }
    
    public static boolean isCalled(Location l, String factionname) {
        if (ZombieMod.hasFactions) {
            Faction fact = BoardColl.get().getFactionAt(PS.valueOf(l));
            String factName;
            
            if (fact != null) {
                factName = fact.getName();
            } else {
                return false;
            }
            
            factName = factName.toLowerCase();
            factName = ChatColor.stripColor(factName);
            
            if (factName.equals(factionname)) {
                return true;
            }
        }
        return false;
    }
    
    
    public static boolean isWild(Location l) {
        Faction fact = BoardColl.get().getFactionAt(PS.valueOf(l));
        return fact.getName().equalsIgnoreCase("wilderness");
    }

    
    public static boolean isSafe(Location l) {
        if (ZombieMod.hasFactions) {
            Faction fact = BoardColl.get().getFactionAt(PS.valueOf(l));
            
            String factName;
            
            if (fact != null) {
                factName = fact.getName();
            } else {
                return false;
            }
            
            factName = factName.toLowerCase();
            factName = ChatColor.stripColor(factName);
            
            Faction sz = FactionColl.get().getSafezone();
            
            if (fact==sz || factName.equals("rentableplot")) {
                return true;
            }
        } else {
            if (ZombieMod.spawnLoc.getWorld().getName().equals(l.getWorld().getName())) {
                double dist = ZombieMod.spawnLoc.distanceSquared(l);
                if (dist < 2500) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }
    
    public static void spawnZombie(PutredineImmortui z, Location l, ZombieMod plugin) {
    	Location spawn = l.getWorld().getSpawnLocation();
        double d = spawn.distanceSquared(l);        
        // 29160000 = 5400 blocks squared
        int level = (int)(Math.floor((d/29160000.0D) * 50.0D));
        
        // System.out.print("Setting Zombie distance level to:"+level);
        
        spawnZombie(z, l, plugin, level);
    }

    public static boolean spawnZombie(PutredineImmortui z, Location l, ZombieMod plugin, int level) {
        // System.out.print("Spawning level:"+level);
        return spawnZombie(z, l, plugin, level, 0);
    }

    
    public static boolean spawnZombie(PutredineImmortui z, Location l, ZombieMod plugin, int level, int hp) {
        // System.out.print("Final spawning level:"+level);

    	net.minecraft.server.v1_7_R4.World mcWorld = ((CraftWorld) l.getWorld()).getHandle();
        if (z == null) {
            z = new PutredineImmortui(plugin);
        }
        if (z.size > 99) {  // set to 2 for giant spawning re-enableing...
            // ZombieMod.logger.info("[zm] Spawning giant - " + z.size);
            ZombieGiantType newzomb = new ZombieGiantType(mcWorld, z);
            newzomb.setPosition(l.getX(), l.getY() + 5, l.getZ());
            newzomb.maxHealth = z.maxHealth;
            // newzomb.heal(z.maxHealth);
            int thishealth;
            thishealth = z.maxHealth;
            if (level < 50) {
                if (level < 1) {
                    level = 1;
                }
                int healthPC = 50 + level;
                thishealth = (int) ((thishealth / 100.00D) * healthPC);                
                
            }
            if (hp > 0) {
                thishealth = hp;
            }
            int xp = newzomb.genus.xp;
            xp = (int) ((xp/100.0D)*(50+level));
            newzomb.genus.xp=xp;

            newzomb.setHealth(thishealth);
            newzomb.setCustomName(z.commonName);

            mcWorld.addEntity(newzomb, SpawnReason.CUSTOM);
            
            // l.getWorld().spawnEntity(l, EntityType.GIANT);
        } else {
            ZombieType newzomb = new ZombieType(mcWorld, z);
            
            newzomb.setPosition(l.getX(), l.getY() + 1, l.getZ());
            newzomb.maxHealth = z.maxHealth;
            int thishealth;
            thishealth = z.maxHealth;
            if (level < 50) {
                if (level < 1) {
                    level = 1;
                }
                int healthPC = 50 + level;
                thishealth = (int) ((thishealth / 100.00D) * healthPC);
                
                Double dmg = newzomb.getDamage();
                newzomb.setDamage((dmg/100.00D) * healthPC);
                
            }
            
            newzomb.getBukkitEntity().setMetadata("level", new FixedMetadataValue(plugin, (Integer) level));
            
            // System.out.print("Setting Zombie level to:"+level);

            
            if (hp > 0) {
                thishealth = hp;
            }
            // newzomb.heal(z.maxHealth);
            newzomb.setHealth(thishealth);
            newzomb.setCustomName(z.commonName);
                        
            if (ZombieMod.debugMode) {
                ZombieMod.logger.info("[ZM] mcworld : " + mcWorld);
                ZombieMod.logger.info("[ZM] newzomb : " + newzomb);
            } 
            
                        
            mcWorld.addEntity(newzomb, SpawnReason.CUSTOM);

            if (ZombieMod.debugMode) {
                ZombieMod.logger.info("[zm] newzomb added");
            } 

            
            if (z.jockey != null) {
                // split jocky on | to get id/hp/name etc
                String[] jInf = z.jockey.split("\\|");
                
                EntityType et =null;
                LivingEntity mount = null;
                
                if (jInf[0].equalsIgnoreCase("AngryGolem")) {
                    AngryGolem ag = new AngryGolem(mcWorld);
                    ag.setPosition(l.getX(), l.getY() + 1, l.getZ());
                    mcWorld.addEntity(ag, SpawnReason.CUSTOM);
                    et = EntityType.IRON_GOLEM;
                    mount = (LivingEntity) ag.getBukkitEntity();
                } else {
                    et = EntityType.valueOf(jInf[0].toUpperCase().trim());
                    if (et == null) {
                        if (jInf[0] != null){
                             if (jInf[0].equalsIgnoreCase("horse")) {
                                 et = EntityType.HORSE;
                             } else if (jInf[0].equalsIgnoreCase("spider")) {
                                 et = EntityType.SPIDER;
                             } else if (jInf[0].equalsIgnoreCase("enderdragon")) {
                                 et = EntityType.ENDER_DRAGON;
                             } else if (jInf[0].equalsIgnoreCase("wolf")) {
                                 et = EntityType.WOLF;
                             } else if (jInf[0].equalsIgnoreCase("ocelot")) {
                                 et = EntityType.OCELOT;
                             }
                         }
                     }
                    if (et != null) {
                        mount  = (LivingEntity) l.getWorld().spawnEntity(l, et);
                    }
                }
                
                
                if (mount != null) {
                                
                switch (mount.getType()) {
                    case WOLF:
                        Wolf w = (Wolf)mount;
                        w.setAngry(true);
                        w.setMaxHealth(z.maxHealth);
                        w.setHealth(w.getMaxHealth());
                        break;
                    case IRON_GOLEM:
                        IronGolem ig = (IronGolem)mount;
                        ig.setPlayerCreated(false);
                        if (ig.getMaxHealth()<z.maxHealth) {
                            ig.setMaxHealth(z.maxHealth);
                            ig.setHealth(ig.getMaxHealth());
                        }
                        break;
                    case HORSE:
                          Horse mlp = (Horse)mount;
                          mlp.setDomestication(mlp.getMaxDomestication());
                          mlp.setTamed(true);
                          mlp.setMaxHealth(z.maxHealth);
                          mlp.setHealth(mount.getMaxHealth());
                          
                    default:
                        mount.setMaxHealth(z.maxHealth);
                        mount.setHealth(mount.getMaxHealth());
                }
                
                if (jInf.length>1) { 
                    if (!jInf[1].isEmpty()) {
                        mount.setCustomName(jInf[1]);
                    }
                    if (jInf.length>2) {
                        mount.setMaxHealth(Integer.parseInt(jInf[2]));
                        mount.setHealth(mount.getMaxHealth());
                    }
                    // check for other values in jInf[3] for flags such as horse type
                    if (jInf.length>3) {
                        if (!jInf[3].isEmpty()) {
                            if (mount.getType()==EntityType.HORSE) { 
                                Variant v = null;
                                v = Variant.valueOf(jInf[3]);
                                if ( v == null ) {
                                    if (jInf[3].equalsIgnoreCase("UNDEAD_HORSE")) {
                                        v = Variant.UNDEAD_HORSE;
                                    }
                                }
                                ((Horse)mount).setVariant(v);
                            }
                        }                        
                    }
                }
                
                mount.setPassenger(newzomb.getBukkitEntity());
                mount.setRemoveWhenFarAway(true);
                newzomb.locY = newzomb.locY - 1.0D; 
                
                }
            }
        }
        return true;
    }
    
    /**
     * Fetch the PutredineImmortui instance for a given entity.
     * 
     * @param entity
     *            The entity to fetch the Zombie setting for
     * @return Returns the PutredineImmortui instance that matches the Entity
     */
    public static PutredineImmortui getZombie(Entity entity) {
        if (entity == null) {
            return null;
        }
        net.minecraft.server.v1_7_R4.Entity mcEntity = (((CraftEntity) entity).getHandle());
        if (mcEntity instanceof ZombieType) {
            ZombieType zt = (ZombieType) mcEntity;
            if (zt.genus != null) {
                return zt.genus;
            }
        } else if (mcEntity instanceof ZombieGiantType) {
            ZombieGiantType zt = (ZombieGiantType) mcEntity;
            if (zt.genus != null) {
                return zt.genus;
            }
        }
        return null;
    }
    
    public static boolean checkResistance(Entity entity, Material bob) {
        if (entity == null) {
            return false;
        }
        net.minecraft.server.v1_7_R4.Entity mcEntity = (((CraftEntity) entity).getHandle());
        if (mcEntity instanceof ZombieType) {
            ZombieType zt = (ZombieType) mcEntity;
            if (zt.genus != null) {
                return zt.checkResistance(bob);
            }
        } else if (mcEntity instanceof ZombieGiantType) {
            ZombieGiantType zt = (ZombieGiantType) mcEntity;
            if (zt.genus != null) {
                return zt.checkResistance(bob);
            }
        }
        return false;
    }
    
    public static void addResistance(Entity entity, Material bob) {
        net.minecraft.server.v1_7_R4.Entity mcEntity = (((CraftEntity) entity).getHandle());
        if (mcEntity instanceof ZombieType) {
            ZombieType zt = (ZombieType) mcEntity;
            if (zt.genus != null) {
                zt.addResistance(bob);
            }
        } else if (mcEntity instanceof ZombieGiantType) {
            ZombieGiantType zt = (ZombieGiantType) mcEntity;
            if (zt.genus != null) {
                zt.addResistance(bob);
            }
        }
    }
    
    public static void stomp(Location from, Entity stomper, int radius, int damage) {
//      Bukkit.broadcastMessage("Stomp!,);
        for (Entity bounced : stomper.getNearbyEntities(radius, radius, radius)) {
            if (bounced != stomper) {
                if (bounced instanceof Player) {
                    Player p = (Player) bounced;
                    p.damage(damage, stomper);
                    Vector v = p.getVelocity();
                    v.setY(v.getY() + 1.5);
                    p.setVelocity(v);
                    p.sendMessage("Stomp!");
                } else if ((bounced instanceof Monster)) {
                    PutredineImmortui zdata = Utils.getZombie(bounced);
                    if (zdata == null || zdata.size < 2) {
                        Monster c = (Monster) bounced;
                        c.damage(damage, stomper); // damage
                        Vector vc = c.getVelocity();
                        vc.setY(vc.getY() + 1.5);
                        c.setVelocity(vc);
                    }
                }
            }
        }
        
        // from.getWorld().strikeLightningEffect(from);
        Block block = from.getBlock();
        
        ArrayList<BlockState> blocks = new ArrayList<BlockState>();
        for (int x = (radius); x >= (0 - radius); x--) {
            for (int zed = (radius); zed >= (0 - radius); zed--) {
                for (int y = (radius); y >= (0 - radius); y--) {
                    if (x == 0 && zed == 0) {
                        // skip own blocks...
                    } else {
                        Block b = block.getRelative(x, y, zed);
                        Location distanceBlock;
                        if (y >= 0) { // sphere if y<0 cylinder above
                            distanceBlock = block.getRelative(x, 0, zed).getLocation();
                        } else {
                            distanceBlock = b.getLocation();
                        }
                        if (block.getLocation().distance(distanceBlock) < radius) {
                            BlockState thisstate = b.getState();
                            if (b.getType() != Material.AIR) {
                                if (thisstate instanceof Chest || thisstate instanceof BrewingStand || thisstate instanceof CreatureSpawner || thisstate instanceof Dispenser
                                        || thisstate instanceof DoubleChest || thisstate instanceof Furnace
                                        || thisstate instanceof Jukebox || thisstate instanceof NoteBlock || thisstate instanceof Sign) {
                                    // skip me
                                } else {
                                    String fName = getFactName(block.getLocation());
                                    if (fName == null || fName.equals("") || fName.equals("warzone") || fName.equals("wilderness")) {
                                        blocks.add(thisstate);
                                        if (ZombieMod.hasCreeperHeal) {
                                            CreeperHandler.recordBlock(b);
                                        } else {
                                            b.setType(Material.AIR);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        for (BlockState bs : blocks) {
            Material m = bs.getType();
            @SuppressWarnings("deprecation")
            byte d = bs.getData().getData();
            int bsX = bs.getX();
            int bsY = bs.getY();
            int bsZ = bs.getZ();
            double depth = (block.getY() + radius) - bsY + 1;
            double speed = .5 + ((1.00D / depth) * 2); // (1.00D/distance)
            Location fbl = new Location(block.getWorld(), bsX, bsY, bsZ);
            @SuppressWarnings("deprecation")
            FallingBlock fb = fbl.getWorld().spawnFallingBlock(fbl, m, d);
            fb.setVelocity(new Vector(0.00D, speed, 0.00D));
            if (Math.random()>0.95D) {
                fb.setDropItem(true);
            } else {
                fb.setDropItem(false);
            }            
        }
        blocks = null;
    }
    
    public static void setWarZone(Chunk c) {
        if (c == null)
            return; // sanity check.
        if (ZombieMod.hasFactions) {
            Location l = new Location(c.getWorld(), (double)c.getX(), 64.0D, (double)c.getZ());
            PS ps = PS.valueOf(l);
            
            Faction wz = FactionColl.get().getWarzone();
            
            BoardColl.get().setFactionAt(ps, wz);
        }
    }
    
    public static void setEquip(LivingEntity mob, ItemStack item, int slot) {
        EntityEquipment eq = mob.getEquipment();
        if (slot == 0) {
            eq.setItemInHand(item);
            if (item.getType() == Material.FIRE) {
                eq.setItemInHandDropChance(0.05F);
            } else {
                eq.setItemInHandDropChance(0);
            }
        }
        if (slot == 1) {
            eq.setBoots(item);
            eq.setBootsDropChance(0);
        }
        if (slot == 2) {
            eq.setLeggings(item);
            eq.setLeggingsDropChance(0);
        }
        if (slot == 3) {
            eq.setChestplate(item);
            eq.setChestplateDropChance(0);
        }
        if (slot == 4) {
            eq.setHelmet(item);
            eq.setHelmetDropChance(0);
        }
    }

    public static void addStat(String playerName, String category, String statname, int statAdd) {
    	/*
        BeardStat beardStat = (BeardStat) Bukkit.getServer().getPluginManager().getPlugin("BeardStat");
        Promise<EntityStatBlob> promiseblob = beardStat.getStatManager().getOrCreatePlayerStatBlob(playerName);
        String w = "none";
        @SuppressWarnings("deprecation")
        Player p = Bukkit.getPlayer(playerName);
        if(p!= null) {
            w = p.getWorld().getName();
        }
        promiseblob.onResolve(new DelegateIncrement("ZombieMod", w, category, statname, statAdd));
        */
    }
    
    public static void setStat(String playerName, String category, String statname, int statValue) {
    	/*
        BeardStat beardStat = (BeardStat) Bukkit.getServer().getPluginManager().getPlugin("BeardStat");
        Promise<EntityStatBlob> promiseblob = beardStat.getStatManager().getOrCreatePlayerStatBlob(playerName);
        String w = "none";
        @SuppressWarnings("deprecation")
        Player p = Bukkit.getPlayer(playerName);
        if(p!= null) {
            w = p.getWorld().getName();
        }
        promiseblob.onResolve(new DelegateSet("ZombieMod", w, category, statname, statValue));
        */
    }
    
    public static String cleanName(String name) {
        String newname = name;
        String searchcode = ZombieMod.heatlhPrefix;
        if (newname!= null && newname.contains(searchcode)) {
            int loc = newname.indexOf(searchcode);
            int start = 0;
            if (newname.startsWith("§f")) {
                start = 2;
            }
            if (loc > -1) {
                newname = newname.substring(start, loc);
            }
        }
        return newname;
    }
    
    public static ItemStack getSkullItemStack(int amount, String playerName) {
        ItemStack s = new ItemStack(Material.SKULL_ITEM, amount);
        s.setDurability((short) 3);
        SkullMeta meta = (SkullMeta) s.getItemMeta();
        meta.setOwner(playerName);
        s.setItemMeta(meta);
        return s;
    }


	 public static final BlockFace[] axis = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
	 public static final BlockFace[] radial = { BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST };
	   
	    /**
	    * Gets the horizontal Block Face from a given yaw angle<br>
	    * This includes the NORTH_WEST faces
	    *
	    * @param yaw angle
	    * @return The Block Face of the angle
	    */
	    public static BlockFace yawToFace(float yaw) {
	        return yawToFace(yaw, true);
	    }
	 
	    /**
	    * Gets the horizontal Block Face from a given yaw angle
	    *
	    * @param yaw angle
	    * @param useSubCardinalDirections setting, True to allow NORTH_WEST to be returned
	    * @return The Block Face of the angle
	    */
	    public static BlockFace yawToFace(float yaw, boolean useSubCardinalDirections) {
	        if (useSubCardinalDirections) {
	            return radial[Math.round(yaw / 45f) & 0x7];
	        } else {
	            return axis[Math.round(yaw / 90f) & 0x3];
	        }
	    }


}


