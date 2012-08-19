/**
 * @author  sable <darren.douglas@gmail.com>
 * @version 1.0.0
 * 
 */
package me.sablednah.zombiemod;

import org.bukkit.Chunk;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.*;
import com.herocraftonline.heroes.characters.Monster;


public class SkillScheduler implements Listener {
    public ZombieMod plugin;

    public SkillScheduler(ZombieMod instance) {
        this.plugin=instance;
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void SkillDamageEvent(SkillDamageEvent event){

        if (event.isCancelled()) { return; }

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

        PutredineImmortui zomb = ZombieType.getZombie(entity);
        if (zomb != null && zomb.health > -1) {
            Chunk c = entity.getLocation().getChunk();
            String cid = c.getX() + "|"+c.getZ();
            zomb.cid=cid;
            zomb.lastLoc=entity.getLocation();
            if (zomb.species.equals("PlayerZombie")) {
                ZombieMod.playerZombies.put(entity.getUniqueId(), zomb);
            }

            net.minecraft.server.Entity mcEnt = (((CraftEntity) entity).getHandle());
            ZombieType zt = (ZombieType) mcEnt;


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
//              event.setDamage(2001);
                LivingEntity le = (LivingEntity) entity;
                le.setHealth(1);
                le.damage(event.getDamage());
                
            } else { // not dead yet
                LivingEntity tEnt = (LivingEntity) entity;
                tEnt.setHealth(tEnt.getMaxHealth());

                Heroes heroes = (Heroes) plugin.getServer().getPluginManager().getPlugin("Heroes");
                Monster monsta = heroes.getCharacterManager().getMonster(tEnt);
                monsta.setHealth(monsta.getMaxHealth());
            }

            event.setDamage(0);
            //hack to set last damage to correct amount
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new setTempInvuln(entity,damage),1L);
        }
    }
}
