package me.sablednah.zombiemod;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class PutredineImmortui {
	public String uniqueid;
	Date spawnTime;
	public int health;
	public int maxHealth;
	public int damage;
	public int armour;
	public int agro;
	public double speed = 1.0;
	public double attackSpeed = 1.0;
	public double bounty = 0.0;
	public int xp = 0;
	public String spawnReason;
	public Boolean noBurn;
	public Boolean passive;
	public List<Effect> effects;
	public List<PotionEffect> potions;
	public List<String> abilities;
	public List<ItemStack> items;
	public List<Double> dropRates;
	public String species;
	public String genus;
	public String commonName;
	public String skin;
	public String cid = null;
	public UUID ID = null;
	public Location lastLoc;
	
	public PutredineImmortui(ZombieMod plugin, String g, Config conf) {
		// constructor to spawn specified genus of zombie.
		spawnTime = Calendar.getInstance().getTime();

		Random rnd = new Random();
		int num = rnd.nextInt(8999) + 1000;
		uniqueid = (spawnTime + "-" +  num);
	
		if (g != null) {
			genus = g;
		} else {
			genus = "generic.yml";
		}
		
		if (conf == null) {
			conf = plugin.genera.configs.get(genus);
		}
		
		health=conf.maxHealth;
		maxHealth = health;
		damage = conf.damage;
		noBurn = conf.noBurn;
		passive = conf.passive;
		effects = conf.effects;
		potions = conf.potions;
		speed = conf.speed;
		attackSpeed = conf.attackSpeed;
		species = conf.species;
		skin = conf.skin;
		bounty = conf.bounty;
		items = conf.items;
		commonName = conf.commonName;
		xp = conf.xp;
		abilities = conf.abilities;
		dropRates = conf.dropRates;
		agro = conf.agro;
		armour = conf.armour;
		
		if (abilities!= null && abilities.contains("GHOST")) {
			OfflinePlayer[] names = plugin.getServer().getOfflinePlayers();
			if (names != null) {
				if (names.length > 0) {
					Random generator = new Random();
					int rndx = generator.nextInt( names.length );
					OfflinePlayer plyr = names[rndx];
					String name = plyr.getName();
					commonName = "Zombie " + name;
				}
			}
		}
	}
	
	public PutredineImmortui(ZombieMod plugin, Config config) {
		// constructor to choose random weighted genus of zombie.
		this(plugin, (plugin.wpm.nextElt()), config);
	}

	public PutredineImmortui(ZombieMod plugin, String g) {
		// constructor to choose random weighted genus of zombie.
		this(plugin, g, null);
	}
	
	public PutredineImmortui(ZombieMod plugin) {
		// constructor to choose random weighted genus of zombie.
		this(plugin, (plugin.wpm.nextElt()), null);
	}	
}
