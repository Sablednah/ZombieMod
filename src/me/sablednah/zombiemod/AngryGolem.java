package me.sablednah.zombiemod;

import java.lang.reflect.Field;

import net.minecraft.server.v1_5_R3.DamageSource;
import net.minecraft.server.v1_5_R3.Entity;
import net.minecraft.server.v1_5_R3.EntityHuman;
import net.minecraft.server.v1_5_R3.EntityZombie;
import net.minecraft.server.v1_5_R3.PathfinderGoalHurtByTarget;
import net.minecraft.server.v1_5_R3.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_5_R3.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_5_R3.PathfinderGoalNearestAttackableTarget;
import net.minecraft.server.v1_5_R3.PathfinderGoalRandomLookaround;
import net.minecraft.server.v1_5_R3.PathfinderGoalRandomStroll;
import net.minecraft.server.v1_5_R3.World;

public class AngryGolem extends net.minecraft.server.v1_5_R3.EntityIronGolem {
    
    private int att;
    
    public AngryGolem(World world) {
        super(world);
        this.texture = "/mob/villager_golem.png";
        a(1.4F, 2.9F);
        getNavigation().a(true);
        this.goalSelector.a(1, new PathfinderGoalMeleeAttack(this, EntityHuman.class, 0.25F, false));
        this.goalSelector.a(2, new PathfinderGoalRandomStroll(this, 0.16F));
        this.goalSelector.a(3, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 6.0F));
        this.goalSelector.a(4, new PathfinderGoalRandomLookaround(this));
        
        this.targetSelector.a(2, new PathfinderGoalHurtByTarget(this, false));
        this.targetSelector.a(3, new PathfinderGoalNearestAttackableTarget(this, EntityHuman.class, 16.0F, 0, true));
        
    }
    
    public void c() {
        super.c();
        if (this.att > 0) {
            this.att -= 1;
        }
    }
    
    @Override
    public boolean m(Entity entity) {
        Field fld;
        try {
            fld = net.minecraft.server.v1_5_R3.EntityIronGolem.class.getDeclaredField("f");
            fld.setAccessible(true);
            fld.setInt(this, 10);
        } catch (SecurityException e1) {
            e1.printStackTrace();
        } catch (NoSuchFieldException e1) {
            e1.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        
        // this.f = 10;
        boolean flag = false;
        if (!(entity instanceof EntityZombie)) {
            this.world.broadcastEntityEffect(this, (byte) 4);
            flag = entity.damageEntity(DamageSource.mobAttack(this), 7 + this.random.nextInt(15));
            
            if (flag) {
                entity.motY += 0.4000000059604645D;
            }
        }
        makeSound("mob.irongolem.throw", 1.0F, 1.0F);
        return flag;
    }
}