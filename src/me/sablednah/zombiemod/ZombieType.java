package me.sablednah.zombiemod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;

import net.minecraft.server.EntityHuman;
import net.minecraft.server.PathfinderGoalArrowAttack;
import net.minecraft.server.PathfinderGoalFleeSun;
import net.minecraft.server.PathfinderGoalFloat;
import net.minecraft.server.PathfinderGoalHurtByTarget;
import net.minecraft.server.PathfinderGoalLookAtPlayer;
import net.minecraft.server.PathfinderGoalMeleeAttack;
import net.minecraft.server.PathfinderGoalNearestAttackableTarget;
import net.minecraft.server.PathfinderGoalRandomLookaround;
import net.minecraft.server.PathfinderGoalRandomStroll;
import net.minecraft.server.PathfinderGoalRestrictSun;
//import net.minecraft.server.PathfinderGoalSelector;
import net.minecraft.server.World;

public class ZombieType extends net.minecraft.server.EntityZombie{
    public PutredineImmortui genus = null; 
    public List<Material> borgResist = new ArrayList<Material>(); 

    @SuppressWarnings("rawtypes")
    public ZombieType(World world, PutredineImmortui genus) {
        super(world);
        this.texture = "/mob/char.png";

        this.genus = genus;

        Boolean melee = true;
        Boolean noBurn = true;
        float attackSpeed = 0.23F;
        int aggro = 16;
        if (genus!=null) {
            if (genus.effects.contains(Effect.BOW_FIRE) || genus.effects.contains(Effect.BLAZE_SHOOT) || genus.effects.contains(Effect.GHAST_SHOOT) ){
                melee = false;
            }
            this.bw = (float) (genus.speed * 0.23);
            attackSpeed = (float) (genus.attackSpeed * 0.23);
            noBurn = genus.noBurn;
            aggro = genus.agro;
        } else {
            this.bw = 0.23F;
            noBurn=false;
        }

 //       PathfinderGoalSelector gs = new PathfinderGoalSelector(((world != null) && (world.methodProfiler != null)) ? world.methodProfiler : null);
 //       PathfinderGoalSelector ts = new PathfinderGoalSelector(((world != null) && (world.methodProfiler != null)) ? world.methodProfiler : null);

        try {
            /*
            Field[] goallist = net.minecraft.server.EntityLiving.class.getDeclaredFields();

            for (Field fld : goallist ) {
                System.out.print(fld.getName());
            }
            
            Field goallist = net.minecraft.server.EntityLiving.class.getDeclaredField("goalSelector");
            goallist.setAccessible(true);
            goallist.set(this, gs);
            Field targetlist = net.minecraft.server.EntityLiving.class.getDeclaredField("targetSelector");
            targetlist.setAccessible(true);
            targetlist.set(this, ts);
            */
            
            Field gsa = net.minecraft.server.PathfinderGoalSelector.class.getDeclaredField("a");
            gsa.setAccessible(true);
            gsa.set(this.goalSelector, new ArrayList());
            gsa.set(this.targetSelector, new ArrayList());
        } catch (SecurityException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }



//        this.goalSelector.a = new ArrayList();

//           this.goalSelector = new PathfinderGoalSelector(((world != null) && (world.methodProfiler != null)) ? world.methodProfiler : null);
//           this.targetSelector = new PathfinderGoalSelector(((world != null) && (world.methodProfiler != null)) ? world.methodProfiler : null);       

        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(5, new PathfinderGoalRandomStroll(this, this.bw));
        this.goalSelector.a(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, (aggro/2)));
        this.goalSelector.a(6, new PathfinderGoalRandomLookaround(this));



        if (melee) {
            this.goalSelector.a(4, new PathfinderGoalMeleeAttack(this, EntityHuman.class, attackSpeed, false));
        } else {
            this.goalSelector.a(4, new PathfinderGoalArrowAttack(this, attackSpeed, 1, 60));
        }
        if (!noBurn) {
            this.goalSelector.a(2, new PathfinderGoalRestrictSun(this));
            this.goalSelector.a(3, new PathfinderGoalFleeSun(this, this.bw));
        }
        this.targetSelector.a(1, new PathfinderGoalHurtByTarget(this, false));
        this.targetSelector.a(2, new PathfinderGoalNearestAttackableTarget(this, EntityHuman.class, aggro, 0, true));

    }

    public ZombieType(World world) {
        this(world,null); // move speed, normal zombie's 0.23F
    }


    /**
     * Fetch the PutredineImmortui instance for a given entity.
     * 
     * @param entity
     * The entity to fetch the Zombie setting for
     * @return Returns the PutredineImmortui instance that matches the Entity
     */
    public static PutredineImmortui getZombie(Entity entity) {
        if (entity == null) return null;
        net.minecraft.server.Entity mcEntity = (((CraftEntity) entity).getHandle());
        if (mcEntity instanceof ZombieType) {
            ZombieType zt= (ZombieType) mcEntity;
            if (zt.genus != null){ return zt.genus; }
        }
        return null;
    }

    public void addResistance(Material m) {
        if (genus.abilities != null && genus.abilities.contains("BORG")) {
            borgResist.add(m);
        }
    }
    public Boolean checkResistance(Material m) {
        if (genus.abilities != null && genus.abilities.contains("BORG")) {
            return (borgResist.contains(m));
        }
        return false;
    }
}
