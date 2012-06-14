package me.sablednah.zombiemod;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Effect;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class Config {
	public String species;
	public String commonName;
	public String skin;
	public int maxHealth;
	public int damage;
	public int armour;
	public int agro;
	public double speed = 1.0;
	public double attackSpeed = 1.0;
	public Boolean noBurn;
	public Boolean passive;
	public double bounty = 0.0;
	public int xp = 0;
	public List<Effect> effects = new ArrayList<Effect>();
	public List<String> abilities = new ArrayList<String>();
	public List<ItemStack> items = new ArrayList<ItemStack>();
	public List<Double> dropRates = new ArrayList<Double>();
	public List<PotionEffect> potions = new ArrayList<PotionEffect>();
}
