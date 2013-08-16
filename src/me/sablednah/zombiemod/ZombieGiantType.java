package me.sablednah.zombiemod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import net.minecraft.server.v1_6_R2.EntityGiantZombie;
import net.minecraft.server.v1_6_R2.EntityHuman;
import net.minecraft.server.v1_6_R2.PathfinderGoalFleeSun;
import net.minecraft.server.v1_6_R2.PathfinderGoalFloat;
import net.minecraft.server.v1_6_R2.PathfinderGoalHurtByTarget;
import net.minecraft.server.v1_6_R2.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_6_R2.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_6_R2.PathfinderGoalNearestAttackableTarget;
import net.minecraft.server.v1_6_R2.PathfinderGoalRandomLookaround;
import net.minecraft.server.v1_6_R2.PathfinderGoalRandomStroll;
import net.minecraft.server.v1_6_R2.PathfinderGoalRestrictSun;
//import net.minecraft.server.v1_5_R3.PathfinderGoalSelector;
import net.minecraft.server.v1_6_R2.World;

public class ZombieGiantType extends EntityGiantZombie {
	public PutredineImmortui	genus		= null;
	public List<Material>		borgResist	= new ArrayList<Material>();

	@SuppressWarnings("rawtypes")
	public ZombieGiantType(World paramWorld, PutredineImmortui genus) {
		super(paramWorld);
		this.texture = "/mob/char.png";

		this.genus = genus;

		Boolean noBurn = true;
		float attackSpeed = 0.5F;
		int aggro = 24;
		if (genus != null) {
			this.bI = (float) (genus.speed * 0.5);
			attackSpeed = (float) (genus.attackSpeed * 0.5);
			noBurn = genus.noBurn;
			aggro = genus.agro;
			genus.size = 6.0F;
			this.height *= genus.size;
			a(this.width * genus.size, this.length * genus.size);

		} else {
			this.bI = 0.5F;
			noBurn = false;
			this.height *= 6.0F;
			a(this.width * 6.0F, this.length * 6.0F);
		}

		try {
			/*
			 * gsa.setAccessible(true); gsa.set(this.goalSelector, new UnsafeList()); gsa.set(this.targetSelector, new
			 * UnsafeList());
			 */

			Field goala = this.goalSelector.getClass().getDeclaredField("a");
			goala.setAccessible(true);
			((List) goala.get(this.goalSelector)).clear();

			Field targeta = this.targetSelector.getClass().getDeclaredField("a");
			targeta.setAccessible(true);
			((List) targeta.get(this.targetSelector)).clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		this.getNavigation().b(true);
		this.goalSelector.a(1, new PathfinderGoalFloat(this));
		this.goalSelector.a(5, new PathfinderGoalRandomStroll(this, this.bI));
		this.goalSelector.a(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, aggro));
		this.goalSelector.a(6, new PathfinderGoalRandomLookaround(this));
		this.goalSelector.a(4, new PathfinderGoalMeleeAttack(this, EntityHuman.class, attackSpeed, false));
		if (!noBurn) {
			this.goalSelector.a(2, new PathfinderGoalRestrictSun(this));
			this.goalSelector.a(3, new PathfinderGoalFleeSun(this, attackSpeed));
			this.fireProof = false;
		} else {
			this.fireProof = true;
		}
		this.targetSelector.a(1, new PathfinderGoalHurtByTarget(this, false));
		this.targetSelector.a(2, new PathfinderGoalNearestAttackableTarget(this, EntityHuman.class, aggro, 0, true));
	}

	public ZombieGiantType(World paramWorld) {
		this(paramWorld, null); // move speed, normal zombie's 0.23F
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

	@Override
	public float a(int paramInt1, int paramInt2, int paramInt3) {
		if (this.genus != null && this.genus.noBurn) {
			return 15.0F;
		} else {
			return 0.5F - this.world.q(paramInt1, paramInt2, paramInt3);
		}
	}
}
