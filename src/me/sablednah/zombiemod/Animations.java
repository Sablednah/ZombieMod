package me.sablednah.zombiemod;

import java.util.List;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Golem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Processes all repeating tasks that are NOT thread safe. This function is executed every second. It must be kept
 * lightweight to prevent lag.
 */
public class Animations implements Runnable {
    
    public ZombieMod plugin;
    
    public Animations(ZombieMod p) {
        this.plugin = p;
    }
    
    @Override
    public void run() {
        // trigger animations
        ZombieMod.intervals++;
        
        for (World w : plugin.getServer().getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e instanceof Monster || e instanceof Wolf || e instanceof Golem || e instanceof Ocelot) {
                    if (!e.isDead()) {
                        Location l = e.getLocation();
                        if (Utils.isSafe(l)) {
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
                            
//							if (l.getWorld().getName().equals(spawn.getWorld().getName())) {
//								distance = spawn.distance(l);
//							} else {
//								distance = 8;
//							}
                            distance = 8; // Math.sqrt(Math.pow(Math.abs(xDiff),2) + Math.pow(Math.abs(zDiff),2));
                            
                            angle = (Math.atan2(xDiff, zDiff));
                            
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
                                e.teleport(safeNewLoc, TeleportCause.PLUGIN);
                                
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
                                            if (ZombieMod.intervals % 10 == 0) { // scream less frequently than every
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
                                                                    // ZombieMod.logger.info("[" + ZombieMod.myName +
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
                                                        if (targ.isDead() || (Utils.isSafe(targ.getLocation()))) {
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
                                                                fb.setYield(2);
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
                                if (z.abilities.contains("EXPLODE")) {// kaboom
                                    Boolean kaboom = false;
                                    List<Entity> entlist = e.getNearbyEntities(3, 1, 3);
                                    for (Entity ent : entlist) {
                                        if (ent instanceof Player) {
                                            kaboom = true;
                                        }
                                    }
                                    Location from = e.getLocation();
                                    
                                    if (kaboom) {
                                        from.getWorld().createExplosion(from, 0);
                                        List<Entity> entlist2 = e.getNearbyEntities(5, 2, 5);
                                        for (Entity ent : entlist2) {
                                            if (ent instanceof Player) {
                                                ((Player) ent).damage(z.damage, e);
                                            } else if (ent instanceof LivingEntity) {
                                                ((LivingEntity) ent).damage(z.damage, e);
                                            }
                                        }
                                        if (!(z.potions.contains(ZombieMod.resistPotion))) {
                                            ((LivingEntity) e).damage(z.maxHealth);
                                        }
                                    }
                                }
                                
                                if (z.abilities.contains("HEAL")) {// regenerate
                                    if (Math.random() > 0.7) {
                                        Damageable de = (Damageable) e;
                                        
                                        if (de.getHealth() < de.getMaxHealth()) {
                                            de.setHealth(de.getHealth() + 1);
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
                                                                b1.setType(Material.WEB);
                                                            }
                                                            if (b2.getType() == Material.AIR) {
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
}

// :/ so much for "lightweight"...
