package me.sablednah.zombiemod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;

import me.sablednah.zombiemod.AI.PathfinderGoalFollowZombOwner;
import me.sablednah.zombiemod.AI.PathfinderGoalZombOwnerHurtByTarget;
import me.sablednah.zombiemod.AI.PathfinderGoalZombOwnerHurtTarget;

import net.minecraft.server.v1_5_R3.DamageSource;
import net.minecraft.server.v1_5_R3.Enchantment;
import net.minecraft.server.v1_5_R3.EnchantmentManager;
import net.minecraft.server.v1_5_R3.EntityArrow;
import net.minecraft.server.v1_5_R3.EntityHuman;
import net.minecraft.server.v1_5_R3.EntityLiving;
import net.minecraft.server.v1_5_R3.EntityOcelot;
import net.minecraft.server.v1_5_R3.EntityWolf;
import net.minecraft.server.v1_5_R3.EntityZombie;
import net.minecraft.server.v1_5_R3.IRangedEntity;
import net.minecraft.server.v1_5_R3.PathfinderGoalArrowAttack;
import net.minecraft.server.v1_5_R3.PathfinderGoalAvoidPlayer;
import net.minecraft.server.v1_5_R3.PathfinderGoalFleeSun;
//import net.minecraft.server.v1_5_R3.PathfinderGoalFloat;
import net.minecraft.server.v1_5_R3.PathfinderGoalHurtByTarget;
import net.minecraft.server.v1_5_R3.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_5_R3.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_5_R3.PathfinderGoalNearestAttackableTarget;
import net.minecraft.server.v1_5_R3.PathfinderGoalRandomLookaround;
import net.minecraft.server.v1_5_R3.PathfinderGoalRandomStroll;
import net.minecraft.server.v1_5_R3.PathfinderGoalRestrictSun;
import net.minecraft.server.v1_5_R3.World;

public class ZombieType extends EntityZombie implements IRangedEntity {
    
    public PutredineImmortui genus = null;
    public List<Material> borgResist = new ArrayList<Material>();
    public float size = 1.0F;
    
    @SuppressWarnings("rawtypes")
    public ZombieType(World world, PutredineImmortui genus) {
        super(world);
        this.texture = "/mob/char.png";
        this.canPickUpLoot = false;
        this.genus = genus;
        
        
        
        Boolean passive = false;
        Boolean melee = true;
        Boolean noBurn = true;
        Boolean isCoward = false;
        
        float attackSpeed = 0.23F;
        int aggro = 16;
        if (genus != null) {
            if (genus.effects.contains(Effect.BOW_FIRE) || genus.effects.contains(Effect.BLAZE_SHOOT) || genus.effects.contains(Effect.GHAST_SHOOT)) {
                melee = false;
            }
            this.bI = (float) (genus.speed * 0.23);
            attackSpeed = (float) (genus.attackSpeed * 0.23);
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
            this.bI = 0.23F;
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
            this.bG();
            this.setVillager(false);
        }
        
        try {
            /* gsa.setAccessible(true); gsa.set(this.goalSelector, new UnsafeList()); gsa.set(this.targetSelector, new
             * UnsafeList()); */
            
            Field goala = this.goalSelector.getClass().getDeclaredField("a");
            goala.setAccessible(true);
            ((List) goala.get(this.goalSelector)).clear();
            
            Field targeta = this.targetSelector.getClass().getDeclaredField("a");
            targeta.setAccessible(true);
            ((List) targeta.get(this.targetSelector)).clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        //this.goalSelector.a(1, new PathfinderGoalFloat(this));
        if (isCoward) {
            this.goalSelector.a(1, new PathfinderGoalAvoidPlayer(this, EntityOcelot.class, 6.0F, 0.25F, 0.3F));
            this.goalSelector.a(1, new PathfinderGoalAvoidPlayer(this, EntityWolf.class, 6.0F, 0.25F, 0.3F));
        }
        this.goalSelector.a(6, new PathfinderGoalFollowZombOwner(this, this.bI, 10.0F, 2.0F));
        this.goalSelector.a(7, new PathfinderGoalRandomStroll(this, this.bI));
        this.goalSelector.a(8, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, (aggro / 2)));
        this.goalSelector.a(8, new PathfinderGoalRandomLookaround(this));
        
        if (!passive) {
            if (melee) {
                this.goalSelector.a(6, new PathfinderGoalMeleeAttack(this, EntityHuman.class, attackSpeed, false));
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
            this.targetSelector.a(5, new PathfinderGoalNearestAttackableTarget(this, EntityHuman.class, aggro, 0, true));
            if (genus != null && genus.abilities != null && genus.abilities.contains("HUNTER")) {
                this.targetSelector.a(4, new PathfinderGoalNearestAttackableTarget(this, EntityOcelot.class, aggro, 0, false));
                this.targetSelector.a(4, new PathfinderGoalNearestAttackableTarget(this, EntityWolf.class, aggro, 0, false));
            }
        }
        if (!noBurn) {
            this.goalSelector.a(3, new PathfinderGoalRestrictSun(this));
            this.goalSelector.a(4, new PathfinderGoalFleeSun(this, attackSpeed));
            this.fireProof = false;
        } else {
            this.fireProof = true;
        }
        
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
    public float a(int paramInt1, int paramInt2, int paramInt3) {
        if (this.genus != null && this.genus.noBurn) {
            return 0.5F;
        } else {
            return 0.5F - this.world.q(paramInt1, paramInt2, paramInt3);
        }
    }
    
    @Override
    public void a(EntityLiving entityliving, float f)
    {
        EntityArrow entityarrow = new EntityArrow(this.world, this, entityliving, 1.6F, 14 - this.world.difficulty * 4);
        int i = EnchantmentManager.getEnchantmentLevel(Enchantment.ARROW_DAMAGE.id, bG());
        int j = EnchantmentManager.getEnchantmentLevel(Enchantment.ARROW_KNOCKBACK.id, bG());
        
        entityarrow.b(f * 2.0F + this.random.nextGaussian() * 0.25D + this.world.difficulty * 0.11F);
        if (i > 0) {
            entityarrow.b(entityarrow.c() + i * 0.5D + 0.5D);
        }
        
        if (j > 0) {
            entityarrow.a(j);
          }

          if ((EnchantmentManager.getEnchantmentLevel(Enchantment.ARROW_FIRE.id, bG()) > 0)) {
            entityarrow.setOnFire(100);
          }

          makeSound("random.bow", 1.0F, 1.0F / (aE().nextFloat() * 0.4F + 0.8F));
          this.world.addEntity(entityarrow);
    }
    
    //prevent drowning - from EntityWaterAnimal
    @Override
    public boolean bf(){
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
    
    
    @Override
    protected int b(DamageSource damagesource, int i) {
        return i;
      }
}
