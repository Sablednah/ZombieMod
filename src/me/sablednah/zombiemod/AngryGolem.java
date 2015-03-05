package me.sablednah.zombiemod;

import java.lang.reflect.Field;
import java.util.List;

import me.sablednah.zombiemod.AI.PathfinderGoalNearestGolemTargetNew;
import net.minecraft.server.v1_8_R1.EntityHuman;
import net.minecraft.server.v1_8_R1.EntityInsentient;
import net.minecraft.server.v1_8_R1.EntityIronGolem;
import net.minecraft.server.v1_8_R1.IMonster;
import net.minecraft.server.v1_8_R1.PathfinderGoalHurtByTarget;
import net.minecraft.server.v1_8_R1.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_8_R1.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_8_R1.PathfinderGoalMoveTowardsTarget;
import net.minecraft.server.v1_8_R1.PathfinderGoalRandomLookaround;
import net.minecraft.server.v1_8_R1.PathfinderGoalRandomStroll;
import net.minecraft.server.v1_8_R1.World;

public class AngryGolem extends EntityIronGolem {

	public AngryGolem(World world) {
		super(world);

		try {
			/*
			 * gsa.setAccessible(true); gsa.set(this.goalSelector, new UnsafeList()); gsa.set(this.targetSelector, new
			 * UnsafeList());
			 */

			Field goala = this.goalSelector.getClass().getDeclaredField("b");
			goala.setAccessible(true);

			((List<?>) goala.get(this.goalSelector)).clear();

			Field targeta = this.targetSelector.getClass().getDeclaredField("b");
			targeta.setAccessible(true);
			((List<?>) targeta.get(this.targetSelector)).clear();
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.goalSelector.a(1, new PathfinderGoalMeleeAttack(this, 1.0D, true));
		this.goalSelector.a(2, new PathfinderGoalMoveTowardsTarget(this, 0.9D, 32.0F));
		this.goalSelector.a(3, new PathfinderGoalRandomStroll(this, 0.6D));
		this.goalSelector.a(4, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 6.0F));
		this.goalSelector.a(5, new PathfinderGoalRandomLookaround(this));

		this.targetSelector.a(2, new PathfinderGoalHurtByTarget(this, false));
	    this.targetSelector.a(3, new PathfinderGoalNearestGolemTargetNew(this, EntityInsentient.class, 10, false, true, IMonster.e));
		setPlayerCreated(false);
	}
}
