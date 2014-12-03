package me.sablednah.zombiemod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R4.event.CraftEventFactory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityShootBowEvent;

import me.sablednah.zombiemod.AI.PathfinderGoalFollowZombOwner;
import me.sablednah.zombiemod.AI.PathfinderGoalZombOwnerHurtByTarget;
import me.sablednah.zombiemod.AI.PathfinderGoalZombOwnerHurtTarget;

import net.minecraft.server.v1_7_R4.DamageSource;
import net.minecraft.server.v1_7_R4.Enchantment;
import net.minecraft.server.v1_7_R4.EnchantmentManager;
import net.minecraft.server.v1_7_R4.EntityArrow;
import net.minecraft.server.v1_7_R4.EntityHuman;
import net.minecraft.server.v1_7_R4.EntityLiving;
import net.minecraft.server.v1_7_R4.EntityOcelot;
import net.minecraft.server.v1_7_R4.EntityWolf;
import net.minecraft.server.v1_7_R4.EntityZombie;
import net.minecraft.server.v1_7_R4.GenericAttributes;
import net.minecraft.server.v1_7_R4.IRangedEntity;
import net.minecraft.server.v1_7_R4.PathfinderGoalArrowAttack;
import net.minecraft.server.v1_7_R4.PathfinderGoalAvoidPlayer;
import net.minecraft.server.v1_7_R4.PathfinderGoalFleeSun;
//import net.minecraft.server.v1_6R3.PathfinderGoalFloat;
import net.minecraft.server.v1_7_R4.PathfinderGoalHurtByTarget;
import net.minecraft.server.v1_7_R4.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_7_R4.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_7_R4.PathfinderGoalNearestAttackableTarget;
import net.minecraft.server.v1_7_R4.PathfinderGoalRandomLookaround;
import net.minecraft.server.v1_7_R4.PathfinderGoalRandomStroll;
import net.minecraft.server.v1_7_R4.PathfinderGoalRestrictSun;
import net.minecraft.server.v1_7_R4.World;


public class ZombieType extends EntityZombie implements IRangedEntity {

	public PutredineImmortui	genus		= null;
	public List<Material>		borgResist	= new ArrayList<Material>();
	public float				size		= 1.0F;
	public float				speed		= 1.0F;
	public float				attackSpeed	= 1.0F;
	public double				damage		= 3.0D;
	public double				maxHealth	= 40.0D;
	public int					aggro		= 24;
	public boolean				noBurn		= true;
	public boolean				passive		= false;
	public boolean				melee		= true;
	public boolean				isCoward	= false;

	@SuppressWarnings("rawtypes")
	public ZombieType(World world, PutredineImmortui genus) {
		super(world);

		this.canPickUpLoot = false;
		this.genus = genus;

		if (genus != null) {
			if (genus.effects.contains(Effect.BOW_FIRE) || genus.effects.contains(Effect.BLAZE_SHOOT) || genus.effects.contains(Effect.GHAST_SHOOT)) {
				melee = false;
			}
			this.speed = (float) (genus.speed);
			this.attackSpeed = (float) (genus.attackSpeed);
			this.damage = (float) (genus.damage);
			this.maxHealth = (float) (genus.maxHealth);

			noBurn = genus.noBurn;
			aggro = genus.agro;
			passive = genus.passive;
			this.size = genus.size;
			isCoward = genus.coward;
			if (genus.equip != null) {
				for (int cntr = 0; cntr < genus.equip.length; cntr++) {
					if (genus.equip[cntr] != null) {
						Utils.setEquip((LivingEntity) this.getBukkitEntity(), genus.equip[cntr], cntr);
					}
				}
			}

		} else {
			noBurn = false;
			this.size = 1;
		}

		if (this.size > 1) {
			if (this.size == 2) {
				a(0.72F, 2.16F);
			} else {
				this.height *= this.size;
				a(this.width * this.size, this.length * this.size);
			}
		} else if (this.size < 1) {
			this.setBaby(true);
		}

		try {
			/*
			 * gsa.setAccessible(true); gsa.set(this.goalSelector, new UnsafeList()); gsa.set(this.targetSelector, new
			 * UnsafeList());
			 */

			Field goala = this.goalSelector.getClass().getDeclaredField("b");
			goala.setAccessible(true);

			((List) goala.get(this.goalSelector)).clear();

			Field targeta = this.targetSelector.getClass().getDeclaredField("b");
			targeta.setAccessible(true);
			((List) targeta.get(this.targetSelector)).clear();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// this.goalSelector.a(1, new PathfinderGoalFloat(this));
		if (isCoward) {
			this.goalSelector.a(1, new PathfinderGoalAvoidPlayer(this, EntityOcelot.class, 6.0F, 0.25F, 0.3F));
			this.goalSelector.a(1, new PathfinderGoalAvoidPlayer(this, EntityWolf.class, 6.0F, 0.25F, 0.3F));
		}
		if (genus != null && genus.abilities != null && genus.abilities.contains("HEROBRINE")) {
			this.goalSelector.a(7, new PathfinderGoalFollowZombOwner(this, 1.0F, 32.0F, 16.0F , true));
			this.goalSelector.a(9, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 32));
		} else {
			this.goalSelector.a(7, new PathfinderGoalFollowZombOwner(this, 1.0F, 10.0F, 2.0F, false));
			this.goalSelector.a(9, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, (aggro / 2)));
		}
		this.goalSelector.a(8, new PathfinderGoalRandomStroll(this, 1.0F));
		this.goalSelector.a(9, new PathfinderGoalRandomLookaround(this));

		if (!passive) {
			if (melee) {
				this.goalSelector.a(6, new PathfinderGoalMeleeAttack(this, EntityHuman.class, attackSpeed, false));
				this.goalSelector.a(7, new PathfinderGoalMeleeAttack(this, EntityZombie.class, attackSpeed, false));

			} else {
				this.goalSelector.a(6, new PathfinderGoalArrowAttack(this, attackSpeed, 60, 10.0F));
			}

			if (genus != null && genus.abilities != null && genus.abilities.contains("HUNTER")) {
				this.goalSelector.a(5, new PathfinderGoalMeleeAttack(this, EntityOcelot.class, attackSpeed, true));
				this.goalSelector.a(5, new PathfinderGoalMeleeAttack(this, EntityWolf.class, attackSpeed, true));
			}

			this.targetSelector.a(1, new PathfinderGoalZombOwnerHurtByTarget(this));
			this.targetSelector.a(2, new PathfinderGoalZombOwnerHurtTarget(this));
			this.targetSelector.a(3, new PathfinderGoalHurtByTarget(this, false));
			if (genus != null && genus.abilities != null && genus.abilities.contains("HUNTER")) {
				this.targetSelector.a(4, new PathfinderGoalNearestAttackableTarget(this, EntityOcelot.class, 0, false));
				this.targetSelector.a(4, new PathfinderGoalNearestAttackableTarget(this, EntityWolf.class, 0, false));
			}
			this.targetSelector.a(5, new PathfinderGoalNearestAttackableTarget(this, EntityHuman.class, 0, true));
		}
		if (!noBurn) {
			this.goalSelector.a(3, new PathfinderGoalRestrictSun(this));
			this.goalSelector.a(4, new PathfinderGoalFleeSun(this, attackSpeed));
			this.fireProof = false;
		} else {
			this.fireProof = true;
		}
		setatrib();
		this.setHealth((float) this.maxHealth);
	}

	public ZombieType(World world) {
		this(world, null); // move speed, normal zombie's 0.23F
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
	public float a(int i, int j, int k) {
		if (this.genus != null && this.genus.noBurn) {
			return 0.5F;
		} else {
			return 0.5F - this.world.n(i, j, k);
		}
	}
	 public void a(EntityLiving entityliving, float f)
	  {
	    EntityArrow entityarrow = new EntityArrow(this.world, this, entityliving, 1.6F, 14 - this.world.difficulty.a() * 4);
	    int i = EnchantmentManager.getEnchantmentLevel(Enchantment.ARROW_DAMAGE.id, be());
	    int j = EnchantmentManager.getEnchantmentLevel(Enchantment.ARROW_KNOCKBACK.id, be());

	    entityarrow.b(f * 2.0F + this.random.nextGaussian() * 0.25D + this.world.difficulty.a() * 0.11F);
	    if (i > 0) {
	      entityarrow.b(entityarrow.e() + i * 0.5D + 0.5D);
	    }

	    if (j > 0) {
	      entityarrow.setKnockbackStrength(j);
	    }

	    if ((EnchantmentManager.getEnchantmentLevel(Enchantment.ARROW_FIRE.id, be()) > 0) )
	    {
			entityarrow.setOnFire(100);
	    }

	    EntityShootBowEvent event = CraftEventFactory.callEntityShootBowEvent(this, be(), entityarrow, 0.8F);
	    if (event.isCancelled()) {
	      event.getProjectile().remove();
	      return;
	    }

	    if (event.getProjectile() == entityarrow.getBukkitEntity()) {
	      this.world.addEntity(entityarrow);
	    }

	    makeSound("random.bow", 1.0F, 1.0F / (aI().nextFloat() * 0.4F + 0.8F));
	  }

	 // prevent drowning - from EntityWaterAnimal
	@Override
	public boolean aE() {
		return true;
	}

	// pet class's
	public boolean isTamed() {
		if (genus != null && genus.owner != null && genus.owner.length() > 0) {
			return true;
		}
		return false;
	}

	public boolean isSitting() {
		return false;
	}

	public void setSitting(boolean paramBoolean) {}

	public String getOwnerName() {
		if (genus != null) {
			return genus.owner;
		}
		return null;
	}

	public void setOwnerName(String paramString) {
		if (genus != null) {
			genus.owner = paramString;
		}
	}

	public EntityLiving getOwner() {
		String own = getOwnerName();
		if (own != null) {
			return this.world.a(getOwnerName());
		}
		return null;
	}

	public boolean a(EntityLiving paramEntityLiving1, EntityLiving paramEntityLiving2) {
		return true;
	}

	  public boolean damageEntity(DamageSource damagesource, float f) {
		    if (!super.damageEntity(damagesource, f)) {
		      return false;
		    }
		    return true;
		  }

	protected void aD() {
		super.aD();
		setatrib();
	}

	public void setatrib() {
		// base health
		getAttributeInstance(GenericAttributes.maxHealth).setValue((double) maxHealth);
		// agro/follow range
		getAttributeInstance(GenericAttributes.b).setValue((double) aggro);
		if (size > 2) {
			if (size > 4) {
				getAttributeInstance(GenericAttributes.c).setValue(1.0D);
			} else {
				getAttributeInstance(GenericAttributes.c).setValue(0.5D);
			}
		}
		// base speed
		getAttributeInstance(GenericAttributes.d).setValue(0.2300000041723251D * speed);
		// damage
		setDamage((double)damage);
	}
	
	public void setDamage(double damage) {
		getAttributeInstance(GenericAttributes.e).setValue((double) damage);		
	}
	public double getDamage() {
		return getAttributeInstance(GenericAttributes.e).getValue();		
	}

	/*
	 * public static final IAttribute a = new AttributeRanged("generic.maxHealth", 20.0D, 0.0D,
	 * 1.7976931348623157E+308D).a("Max Health").a(true); public static final IAttribute b = new
	 * AttributeRanged("generic.followRange", 32.0D, 0.0D, 2048.0D).a("Follow Range"); public static final IAttribute c
	 * = new AttributeRanged("generic.knockbackResistance", 0.0D, 0.0D, 1.0D).a("Knockback Resistance"); public static
	 * final IAttribute d = new AttributeRanged("generic.movementSpeed", 0.699999988079071D, 0.0D,
	 * 1.7976931348623157E+308D).a("Movement Speed").a(true); public static final IAttribute e = new
	 * AttributeRanged("generic.attackDamage", 2.0D, 0.0D, 1.7976931348623157E+308D);
	 */
}
