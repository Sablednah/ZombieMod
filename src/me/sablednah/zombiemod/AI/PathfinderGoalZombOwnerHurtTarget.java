package me.sablednah.zombiemod.AI;

import me.sablednah.zombiemod.ZombieType;
import net.minecraft.server.v1_7_R4.EntityLiving;

public class PathfinderGoalZombOwnerHurtTarget extends PathfinderGoalZombTarget {

	ZombieType		a;
	EntityLiving	b;
	private int		e;

	public PathfinderGoalZombOwnerHurtTarget(ZombieType paramEntityTameableAnimal) {
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
		
		//System.out.print("Running ownerhurt target: a()");
		
		this.b = localEntityLiving.aL();
		int i = localEntityLiving.aM();
		return (i != this.e) && (a(this.b, false)) && (this.a.a(this.b, localEntityLiving));
	}

	public void c() {
		this.c.setGoalTarget(this.b);

		EntityLiving localEntityLiving = this.a.getOwner();
		if (localEntityLiving != null) {
			//System.out.print("Running ownerhurt target: c()");
			this.e = localEntityLiving.aM();
		}

		super.c();
	}
}