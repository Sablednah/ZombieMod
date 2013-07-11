/**
 * @author  sable <darren.douglas@gmail.com>
 * @version 1.0.0
 * 
 */
package me.sablednah.zombiemod;

import org.bukkit.Chunk;
import org.bukkit.craftbukkit.v1_5_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.api.events.*;


public class SkillScheduler implements Listener {
    public ZombieMod plugin;

    public SkillScheduler(ZombieMod instance) {
        this.plugin=instance;
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void SkillDamageEvent(SkillDamageEvent event){

        if (event.isCancelled()) { 
        	System.out.print("Skill Damage cancelled");
        	return; }

        Entity entity = event.getEntity();

        int damage = event.getDamage();
        /*
        Entity damager = null;

        if(event instanceof SkillDamageEvent) {
            SkillDamageEvent damageEvent = event;

            if (damageEvent.getDamager().getEntity() instanceof Player) {
                damager = (Player) damageEvent.getDamager().getEntity();
            }
        }   
         */
        System.out.print("Skill Damage ocuring - " + damage);

        PutredineImmortui zomb = Utils.getZombie(entity);
        if (zomb != null && !entity.isDead()) {
            Chunk c = entity.getLocation().getChunk();
            String cid = c.getX() + "|"+c.getZ();
            zomb.cid=cid;
            zomb.lastLoc=entity.getLocation();
            if (zomb.species.equals("PlayerZombie")) {
                ZombieMod.playerZombies.put(entity.getUniqueId(), zomb);
            }

            net.minecraft.server.v1_5_R3.Entity mcEnt = (((CraftEntity) entity).getHandle());
            ZombieType zt = (ZombieType) mcEnt;


            if (damage>0) {
                damage=damage-zt.genus.armour;
                if (damage<1 && zt.genus.armour>0) {
                    damage=1;   
                } 
            }

            if (zt.genus.potions.contains(ZombieMod.resistPotion) && damage>1) {
                damage = (damage/2);
            }
        }
    }
}
