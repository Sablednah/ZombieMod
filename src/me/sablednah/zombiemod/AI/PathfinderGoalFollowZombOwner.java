package me.sablednah.zombiemod.AI;

import me.sablednah.zombiemod.ZombieType;
import net.minecraft.server.v1_8_R1.BlockPosition;
import net.minecraft.server.v1_8_R1.EntityLiving;
import net.minecraft.server.v1_8_R1.MathHelper;
import net.minecraft.server.v1_8_R1.Navigation;
import net.minecraft.server.v1_8_R1.NavigationAbstract;
import net.minecraft.server.v1_8_R1.PathfinderGoal;
import net.minecraft.server.v1_8_R1.World;

public class PathfinderGoalFollowZombOwner extends PathfinderGoal {

	private ZombieType			d;
	private EntityLiving		e;
	World						a;
	private double				f;
	private NavigationAbstract	g;
	private int					h;
	float						b;
	float						c;
	public boolean				i;

	private boolean				herobrine	= false;

	public PathfinderGoalFollowZombOwner(ZombieType paramEntityTameableAnimal, double paramDouble, float paramFloat1, float paramFloat2, boolean herobrine) {
		this.d = paramEntityTameableAnimal;
		this.a = paramEntityTameableAnimal.world;
		this.f = paramDouble;
		this.g = paramEntityTameableAnimal.getNavigation();
		this.c = paramFloat1;
		this.b = paramFloat2;
		a(3);
		if (!(paramEntityTameableAnimal.getNavigation() instanceof Navigation)) {
			throw new IllegalArgumentException("Unsupported mob type for FollowOwnerGoal");
		}
	}

	public boolean a() {
		EntityLiving localEntityLiving = this.d.getOwner();
		if (localEntityLiving == null) {
			return false;
		}
		if (this.d.isSitting()) {
			return false;
		}
		if (this.d.h(localEntityLiving) < this.c * this.c) {
			return false;
		}
		this.e = localEntityLiving;
		return true;
	}

	public boolean b() {

		if (herobrine) {
			return (!this.g.m()) && ((this.d.h(this.e) > this.b * this.b) || (this.d.h(this.e) < this.c * this.c)) && (!this.d.isSitting());
		} else {
			return (!this.g.m()) && (this.d.h(this.e) > this.b * this.b) && (!this.d.isSitting());
		}

	}

	public void c() {
		this.h = 0;
		this.i = ((Navigation) this.d.getNavigation()).e();
		((Navigation) this.d.getNavigation()).a(false);
	}

	public void d() {
		this.e = null;
		this.g.n();
		((Navigation) this.d.getNavigation()).a(true);
	}

	public void e() {
		this.d.getControllerLook().a(this.e, 10.0F, this.d.bP());
		if (this.d.isSitting()) {
			return;
		}
		if (--this.h > 0) {
			return;
		}
		this.h = 10;
		if (this.g.a(this.e, this.f)) {
			return;
		}
		if (this.d.cb()) {
			return;
		}

		if (!this.herobrine) {

			if (this.d.h(this.e) < 144.0D) {
				return;
			}
			int j = MathHelper.floor(this.e.locX) - 2;
			int k = MathHelper.floor(this.e.locZ) - 2;
			int m = MathHelper.floor(this.e.getBoundingBox().b);
			for (int n = 0; n <= 4; n++) {
				for (int i1 = 0; i1 <= 4; i1++) {
					if ((n < 1) || (i1 < 1) || (n > 3) || (i1 > 3)) {
						if ((World.a(this.a, new BlockPosition(j + n, m - 1, k + i1))) && (!this.a.getType(new BlockPosition(j + n, m, k + i1)).getBlock().d()) && (!this.a.getType(new BlockPosition(j + n, m + 1, k + i1)).getBlock().d())) {
							this.d.setPositionRotation(j + n + 0.5F, m, k + i1 + 0.5F, this.d.yaw, this.d.pitch);
							this.g.n();
							return;
						}
					}
				}
			}
		} else {
			float dst = (float) this.d.h(this.e);
			float min = this.b;
			float max = this.c;
			// System.out.print("dst: "+dst+" - min: " + (min*min) + " - max: "+(max*max));
			if (dst >= (min * min) && dst <= (max * max)) {
				return;
			}

			int x; // j
			int y; // m
			int z; // k

			// herobrine loc
			int x1 = MathHelper.floor(this.d.locX);
			int y1 = MathHelper.floor(this.d.locY);
			int z1 = MathHelper.floor(this.d.locZ);

			// owner loc
			int x2 = MathHelper.floor(this.e.locX);
			int y2 = MathHelper.floor(this.e.locY);
			int z2 = MathHelper.floor(this.e.locZ);

			int xDiff = x2 - x1;
			int zDiff = z2 - z1;

			double angle = (Math.atan2(xDiff, zDiff));

			int magnitude = MathHelper.floor(max);

			double xOffset = (Math.sin(angle)) * magnitude;
			double zOffset = (Math.cos(angle)) * magnitude;

			x2 -= xOffset;
			z2 -= zOffset;

			x = x2;
			z = z2;
			y = y1 + ((y2 - y1) / 2);

			for (int n = 0; n <= 4; n++) {
				for (int i1 = 0; i1 <= 4; i1++) {
					if ((n < 1) || (i1 < 1) || (n > 3) || (i1 > 3)) {
						if ((World.a(this.a, new BlockPosition(x + n, y - 1, z + i1))) && (!this.a.getType(new BlockPosition(x + n, y, z + i1)).getBlock().d()) && (!this.a.getType(new BlockPosition(x + n, y + 1, z + i1)).getBlock().d())) {
							this.d.setPositionRotation(x + n + 0.5F, y, z + i1 + 0.5F, this.d.yaw, this.d.pitch);
							this.g.n();
							return;
						}
					}
				}
			}
		}
	}
}
