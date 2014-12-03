package me.sablednah.zombiemod.AI;

import me.sablednah.zombiemod.ZombieType;
import net.minecraft.server.v1_7_R4.EntityLiving;

public class PathfinderGoalZombOwnerHurtByTarget extends PathfinderGoalZombTarget {
	
	ZombieType		a;
	EntityLiving	b;
	private int		e;

	public PathfinderGoalZombOwnerHurtByTarget(ZombieType paramEntityTameableAnimal) {
		super(paramEntityTameableAnimal, false);
		this.a = paramEntityTameableAnimal;
		a(1);
	}

	public boolean a() {

		if (!this.a.isTamed())
			return false;
		EntityLiving localEntityLiving = this.a.getOwner();
		if (localEntityLiving == null)
			return false;

		this.b = localEntityLiving.getLastDamager();
		int i = localEntityLiving.aK();
		boolean ret = (i != this.e) && (a(this.b, false)) && (this.a.a(this.b, localEntityLiving));

		//System.out.print("Running ownerhurtby target: a()  ret: "+ret);
		
		return ret;
	}

	public void c() {

		this.c.setGoalTarget(this.b);

		EntityLiving localEntityLiving = this.a.getOwner();
		if (localEntityLiving != null) {
			//System.out.print("Running ownerhurtby target: c()");
			this.e = localEntityLiving.aK();
		}

		super.c();
	}
}