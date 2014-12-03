package me.sablednah.zombiemod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import net.minecraft.server.v1_7_R4.EntityGiantZombie;
import net.minecraft.server.v1_7_R4.EntityHuman;
import net.minecraft.server.v1_7_R4.GenericAttributes;
import net.minecraft.server.v1_7_R4.PathfinderGoalFleeSun;
import net.minecraft.server.v1_7_R4.PathfinderGoalFloat;
import net.minecraft.server.v1_7_R4.PathfinderGoalHurtByTarget;
import net.minecraft.server.v1_7_R4.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_7_R4.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_7_R4.PathfinderGoalNearestAttackableTarget;
import net.minecraft.server.v1_7_R4.PathfinderGoalRandomLookaround;
import net.minecraft.server.v1_7_R4.PathfinderGoalRandomStroll;
import net.minecraft.server.v1_7_R4.PathfinderGoalRestrictSun;
//import net.minecraft.server.v1_7_R1.PathfinderGoalSelector;
import net.minecraft.server.v1_7_R4.World;

public class ZombieGiantType extends EntityGiantZombie {
    
    public PutredineImmortui genus = null;
    public List<Material> borgResist = new ArrayList<Material>();
    public float speed = 1.0F;
    public float attackSpeed = 1.0F;
    public double damage = 50.0D;
    public double maxHealth = 100.0D;
    public int aggro = 24;
    public boolean noBurn = true;

    @SuppressWarnings("rawtypes")
    public ZombieGiantType(World paramWorld, PutredineImmortui genus) {
        super(paramWorld);
        
        this.genus = genus;
        
        if (genus != null) {
            this.speed = (float) (genus.speed);
            this.attackSpeed = (float) (genus.attackSpeed);
            this.damage = (float) (genus.damage);
            this.maxHealth = (float) (genus.maxHealth);
            
            this.noBurn = genus.noBurn;
            this.aggro = genus.agro;
            genus.size = 6.0F;
            this.height *= genus.size;
            a(this.width * genus.size, this.length * genus.size);
            
        } else {
            noBurn = false;
            this.height *= 6.0F;
            a(this.width * 6.0F, this.length * 6.0F);
        }
        
        try {
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
        this.goalSelector.a(5, new PathfinderGoalRandomStroll(this, 1.0F));
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
        this.targetSelector.a(2, new PathfinderGoalNearestAttackableTarget(this, EntityHuman.class, 0, true));
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
            return 0.5F - this.world.n(paramInt1, paramInt2, paramInt3);
        }
    }
    
    protected void aD() {
        super.aD();

        // base health
        getAttributeInstance(GenericAttributes.maxHealth).setValue((double) maxHealth);
        // base speed
        getAttributeInstance(GenericAttributes.d).setValue(0.5D * speed);
        // damage
        getAttributeInstance(GenericAttributes.e).setValue((double) damage);
    }
}
