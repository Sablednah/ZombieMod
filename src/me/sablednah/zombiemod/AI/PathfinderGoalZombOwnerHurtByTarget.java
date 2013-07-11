package me.sablednah.zombiemod.AI;

import me.sablednah.zombiemod.ZombieType;
import net.minecraft.server.v1_5_R3.EntityLiving;
import net.minecraft.server.v1_5_R3.PathfinderGoalTarget;

public class PathfinderGoalZombOwnerHurtByTarget extends PathfinderGoalTarget {
    
    ZombieType a;
    EntityLiving b;
    
    public PathfinderGoalZombOwnerHurtByTarget(ZombieType paramEntityTameableAnimal) {
        super(paramEntityTameableAnimal, 32.0F, false);
        this.a = paramEntityTameableAnimal;
        a(1);
    }
    
    public boolean a()
    {
        if (!this.a.isTamed())
            return false;
        EntityLiving localEntityLiving = this.a.getOwner();
        if (localEntityLiving == null)
            return false;
        this.b = localEntityLiving.aF();
        return a(this.b, false);
    }
    
    public void c()
    {
        this.d.setGoalTarget(this.b);
        super.c();
    }
}