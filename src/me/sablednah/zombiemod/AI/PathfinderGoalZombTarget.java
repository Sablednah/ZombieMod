package me.sablednah.zombiemod.AI;

import me.sablednah.zombiemod.ZombieType;
import net.minecraft.server.v1_7_R4.AttributeInstance;
import net.minecraft.server.v1_7_R4.EntityCreature;
import net.minecraft.server.v1_7_R4.EntityLiving;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.EntityHuman;
//import net.minecraft.server.v1_7_R4.EntityOwnable;
import net.minecraft.server.v1_7_R4.GenericAttributes;
import net.minecraft.server.v1_7_R4.MathHelper;
import net.minecraft.server.v1_7_R4.PathEntity;
import net.minecraft.server.v1_7_R4.PathPoint;

import net.minecraft.server.v1_7_R4.PathfinderGoal;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_7_R4.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public abstract class PathfinderGoalZombTarget extends PathfinderGoal
{
  protected EntityCreature c;
  protected boolean d;
  private boolean a;
  private int b;
  private int e;
  private int f;

  public PathfinderGoalZombTarget(EntityCreature entitycreature, boolean flag)
  {
    this(entitycreature, flag, false);
  }

  public PathfinderGoalZombTarget(EntityCreature entitycreature, boolean flag, boolean flag1) {
    this.c = entitycreature;
    this.d = flag;
    this.a = flag1;
  }

  public boolean b() {
    EntityLiving entityliving = this.c.getGoalTarget();

    if (entityliving == null)
      return false;
    if (!entityliving.isAlive()) {
      return false;
    }
    double d0 = f();

    if (this.c.f(entityliving) > d0 * d0) {
      return false;
    }
    if (this.d) {
      if (this.c.getEntitySenses().canSee(entityliving))
        this.f = 0;
      else if (++this.f > 60) {
        return false;
      }
    }

    return (!(entityliving instanceof EntityPlayer)) || (!((EntityPlayer)entityliving).playerInteractManager.isCreative());
  }

  protected double f()
  {
    AttributeInstance attributeinstance = this.c.getAttributeInstance(GenericAttributes.b);

    return attributeinstance == null ? 16.0D : attributeinstance.getValue();
  }

  public void c() {
    this.b = 0;
    this.e = 0;
    this.f = 0;
  }

  public void d() {
    this.c.setGoalTarget((EntityLiving)null);
  }

  protected boolean a(EntityLiving entityliving, boolean flag) {
	  
    if (entityliving == null)
      return false;
    if (entityliving == this.c)
      return false;
    if (!entityliving.isAlive())
      return false;
    if (!this.c.a(entityliving.getClass())) {
      return false;
    }
    
    if (((this.c instanceof ZombieType)) && (((ZombieType)this.c).getOwner()!=null)) {
 //   	if (((this.c instanceof ZombieType)) && (((ZombieType)this.c).getOwner().getUniqueID().equals(entityliving.getUniqueID()))) {
 //  // if (((this.c instanceof EntityOwnable)) && (StringUtils.isNotEmpty(((EntityOwnable)this.c).getOwnerUUID()))) {
 //  //   if (((entityliving instanceof EntityOwnable)) && (((EntityOwnable)this.c).getOwnerUUID().equals(((EntityOwnable)entityliving).getOwnerUUID()))) {
 //       return false;
 //     }

 //       System.out.print("checking tamed targeting: " + entityliving.toString());
 //       System.out.print("checking tamed owner: " + ((ZombieType)this.c).getOwner());
    	
      if (entityliving == ((ZombieType)this.c).getOwner())
        return false;
    }
    else if (((entityliving instanceof EntityHuman)) && (!flag) && (((EntityHuman)entityliving).abilities.isInvulnerable)) {
      return false;
    }

    if (!this.c.b(MathHelper.floor(entityliving.locX), MathHelper.floor(entityliving.locY), MathHelper.floor(entityliving.locZ)))
      return false;
    if ((this.d) && (!this.c.getEntitySenses().canSee(entityliving))) {
      return false;
    }
    if (this.a) {
      if (--this.e <= 0) {
        this.b = 0;
      }

      if (this.b == 0) {
        this.b = (a(entityliving) ? 1 : 2);
      }

      if (this.b == 2) {
        return false;
      }

    }

    EntityTargetEvent.TargetReason reason = EntityTargetEvent.TargetReason.RANDOM_TARGET;

    if ((this instanceof PathfinderGoalZombOwnerHurtByTarget))
      reason = EntityTargetEvent.TargetReason.TARGET_ATTACKED_OWNER;
    else if ((this instanceof PathfinderGoalZombOwnerHurtTarget)) {
      reason = EntityTargetEvent.TargetReason.OWNER_ATTACKED_TARGET;
    }

    EntityTargetLivingEntityEvent event = CraftEventFactory.callEntityTargetLivingEvent(this.c, entityliving, reason);
    if ((event.isCancelled()) || (event.getTarget() == null)) {
      this.c.setGoalTarget(null);
      return false;
    }if (entityliving.getBukkitEntity() != event.getTarget()) {
      this.c.setGoalTarget((EntityLiving)((CraftEntity)event.getTarget()).getHandle());
    }
    if ((this.c instanceof EntityCreature)) {
      this.c.target = ((CraftEntity)event.getTarget()).getHandle();
    }

    return true;
  }

  private boolean a(EntityLiving entityliving)
  {
    this.e = (10 + this.c.aI().nextInt(5));
    PathEntity pathentity = this.c.getNavigation().a(entityliving);

    if (pathentity == null) {
      return false;
    }
    PathPoint pathpoint = pathentity.c();

    if (pathpoint == null) {
      return false;
    }
    int i = pathpoint.a - MathHelper.floor(entityliving.locX);
    int j = pathpoint.c - MathHelper.floor(entityliving.locZ);

    return i * i + j * j <= 2.25D;
  }
}