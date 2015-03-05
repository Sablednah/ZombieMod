package me.sablednah.zombiemod.AI;

import me.sablednah.zombiemod.ZombieType;
import net.minecraft.server.v1_8_R1.EntityLiving;
import net.minecraft.server.v1_8_R1.PathfinderGoalTarget;

import org.bukkit.event.entity.EntityTargetEvent;

public class PathfinderGoalZombOwnerHurtTarget extends PathfinderGoalTarget {
	ZombieType		a;
	EntityLiving	b;
	private int		c;

	public PathfinderGoalZombOwnerHurtTarget(ZombieType entitytameableanimal) {
		super(entitytameableanimal, false);
		this.a = entitytameableanimal;
		a(1);
	}

	public boolean a() {
		if (!this.a.isTamed()) {
			return false;
		}
		EntityLiving entityliving = this.a.getOwner();
		if (entityliving == null) {
			return false;
		}
		this.b = entityliving.be();
		int i = entityliving.bf();

		return (i != this.c) && (a(this.b, false)) && (this.a.a(this.b, entityliving));
	}

	public void c() {
		this.e.setGoalTarget(this.b, EntityTargetEvent.TargetReason.OWNER_ATTACKED_TARGET, true);
		EntityLiving entityliving = this.a.getOwner();
		if (entityliving != null) {
			this.c = entityliving.bf();
		}
		super.c();
	}
}
