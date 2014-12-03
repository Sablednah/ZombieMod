package me.sablednah.zombiemod;

import java.lang.reflect.Field;
import java.util.List;

import org.bukkit.craftbukkit.v1_7_R4.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_7_R4.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

import net.minecraft.server.v1_7_R4.Entity;
import net.minecraft.server.v1_7_R4.EntityHuman;
import net.minecraft.server.v1_7_R4.EntityLiving;
import net.minecraft.server.v1_7_R4.IMonster;
import net.minecraft.server.v1_7_R4.PathfinderGoalHurtByTarget;
import net.minecraft.server.v1_7_R4.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_7_R4.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_7_R4.PathfinderGoalMoveTowardsTarget;
import net.minecraft.server.v1_7_R4.PathfinderGoalNearestAttackableTarget;
import net.minecraft.server.v1_7_R4.PathfinderGoalRandomLookaround;
import net.minecraft.server.v1_7_R4.PathfinderGoalRandomStroll;
import net.minecraft.server.v1_7_R4.World;

public class AngryGolem extends net.minecraft.server.v1_7_R4.EntityIronGolem {

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
		this.targetSelector.a(3, new PathfinderGoalNearestAttackableTarget(this, EntityHuman.class, 0, true));
		setPlayerCreated(false);
	}

	@Override
	protected void o(Entity entity) {
		if (((entity instanceof IMonster)) && (aI().nextInt(20) == 0)) {
			if (this.passenger !=null && entity.uniqueID.equals(this.passenger.uniqueID)) {
				setGoalTarget(null);
			} else {
				EntityTargetLivingEntityEvent event = CraftEventFactory.callEntityTargetLivingEvent(this, (EntityLiving) entity, EntityTargetEvent.TargetReason.TARGET_ATTACKED_ENTITY);
				if (!event.isCancelled()) {
					if (event.getTarget() == null)
						setGoalTarget(null);
					else {
						setGoalTarget(((CraftLivingEntity) event.getTarget()).getHandle());
					}
				}
			}

		}

		super.o(entity);
	}
	
	
	
}