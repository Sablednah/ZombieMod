package me.sablednah.zombiemod;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import com.nitnelave.CreeperHeal.CreeperHandler;

class Break implements Runnable {
    
    private Block b;
    private LivingEntity zombie;
    private Player player;
    public ZombieMod plugin;
    
    public Break(ZombieMod p, Block b, LivingEntity z, Player t) {
        this.b = b;
        this.zombie = z;
        this.player = t;
        this.plugin = p;
    }
    
    public void run() {
        if (!zombie.isDead()) {
            if (!Utils.isSafe(this.b.getLocation())) {
                Boolean breakblock = true;
                PutredineImmortui zm = Utils.getZombie(zombie);
                if (zm != null) {
                    if (zm.abilities != null && zm.abilities.contains("INFEST")) {
                        breakblock = false;
                        this.b.setType(Material.MONSTER_EGGS);
                    }
                }
                if (breakblock) {
                    // this.b.breakNaturally();
                    if (ZombieMod.hasCreeperHeal) {
                        if (plugin.allowedpermbreaks.contains(b.getType())) {
                            this.b.breakNaturally();
                        } else {                           
                            CreeperHandler.recordBlock(b);
                        }
                    } else {
                        this.b.breakNaturally();
                    }
                }
            }
            
            if ((((Monster) zombie).getTarget() != null) && (((Monster) zombie).getTarget().equals(player)))
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new BreakRunner(plugin, this.zombie, this.player), 20L);
        }
    }
}