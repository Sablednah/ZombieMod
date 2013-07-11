package me.sablednah.zombiemod;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * @author sable
 */

public class ZombieMod extends JavaPlugin {
    
    public static Logger logger;
    public EntityListener EntityListener;
    public SkillScheduler SkillScheduler = new SkillScheduler(this);
    public static long intervals = 0;
    
    public static int chunklimit;
    public static int zombiespawnratio;
    public static int zombiespawnerratio;
    public static int spawnmultiplier;
    public List<Material> allowedbreaks = new ArrayList<Material>();
    public List<Material> allowedpermbreaks = new ArrayList<Material>();
    public static Map<UUID, PutredineImmortui> playerZombies = new ConcurrentHashMap<UUID, PutredineImmortui>();
    public static Boolean debugMode;
    public static Boolean dayspawner;
    public static Boolean proximityspawner;
    public static Boolean blocknaturalspawns;
    public static Boolean givezombieplayeritems;
    public static String factionsWildName;
    public static String factionsWarZone;
    public static String factionsSafeName;
    public static Location spawnLoc;
    public static String exitMessage;
    
    public Economy economy = null;
    private PluginCommandExecutor myExecutor;
    public PluginDescriptionFile pdfFile;
    public String myName;
    
    public static FileConfiguration LangConfig = null;
    public static volatile File LangConfigurationFile = null;
    
    // build a couple of global potion objects now so that checking them every tick is quicker.
    public static PotionEffect speedPotion = new PotionEffect(PotionEffectType.SPEED, 20, 1);
    public static PotionEffect resistPotion = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20, 1);
    
    public static Boolean hasHeroes;
    public static Boolean hasSpout;
    public static Boolean hasFactions;
    public static Boolean hasCityWorld;
    public static Boolean hasBeardStat;
    public static Boolean hasCreeperHeal;
    
    public ReadData genera;
    public WeightedProbMap<String> wpm;
    
    // public static Map<Player, Boolean> pluginEnabled = null;
    
    public static String[] animalList = { "Pig", "Sheep", "Cow", "Chicken", "MushroomCow", "Golem", "IronGolem", "Snowman", "Squid", "Villager", "Wolf", "Ocelot" };
    public static String[] monsterList = { "Blaze", "Zombie", "Creeper", "Skeleton", "Spider", "Ghast", "MagmaCube", "Slime", "CaveSpider", "EnderDragon", "EnderMan", "Giant",
            "PigZombie", "SilverFish",
            "Spider" };
    public static String heatlhPrefix = ChatColor.translateAlternateColorCodes('&', "&r&f&r");
    
    public String[] entityList = Utils.concat(animalList, monsterList);
    
    
    @Override
    public void onDisable() {
        EntityListener = null;
        this.getServer().getScheduler().cancelTasks(this);
        logger.info("[" + myName + "] " + exitMessage);
    }
    
    @Override
    public void onEnable() {
        this.pdfFile = this.getDescription();
        this.myName = pdfFile.getName();
        ZombieMod.logger = this.getLogger();
        
        PluginManager pm = getServer().getPluginManager();
        if (pm.isPluginEnabled("Vault")) {
            RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            if (economyProvider != null) {
                this.economy = economyProvider.getProvider();
                logger.info("[" + myName + "] Ecconomy '" + economy.getName() + "' Found.");
            } else {
                economy = null;
                logger.info("[" + myName + "] No ecconomy found.");
            }
        }
        if (EntityListener == null)
            EntityListener = new EntityListener(this);
        PlayerListener playerListerner = new PlayerListener(this);
        pm.registerEvents(EntityListener, this);
        pm.registerEvents(playerListerner, this);
        
        hasSpout = this.getServer().getPluginManager().isPluginEnabled("Spout");
        if (hasSpout) {
            SpoutListener spoutListener = new SpoutListener(this);
            pm.registerEvents(spoutListener, this);
            
            logger.info("[" + myName + "] Spout features Enabled");
        }
        hasHeroes = this.getServer().getPluginManager().isPluginEnabled("Heroes");
        if (hasHeroes) {
            logger.info("[" + myName + "] Heroes Support Enabled");
            pm.registerEvents(this.SkillScheduler, this);
        }
        hasFactions = this.getServer().getPluginManager().isPluginEnabled("Factions");
        if (hasFactions) {
            logger.info("[" + myName + "] Factions Support Enabled");
        }
        hasCityWorld = this.getServer().getPluginManager().isPluginEnabled("CityWorld");
        if (hasCityWorld) {
            CityListener cityListener = new CityListener(this);
            pm.registerEvents(cityListener, this);
            logger.info("[" + myName + "] CityWorld Support Enabled");
        }
        hasBeardStat = this.getServer().getPluginManager().isPluginEnabled("BeardStat");
        if (hasBeardStat) {
            logger.info("[" + myName + "] BeardStat Support Enabled");
        }
        hasCreeperHeal = this.getServer().getPluginManager().isPluginEnabled("CreeperHeal");
        if (hasCreeperHeal) {
            logger.info("[" + myName + "] Creeper Heal Support Enabled");
        }
        
        // Custom NMS stuff here :/
        try {
            @SuppressWarnings("rawtypes")
            Class[] args = new Class[3];
            args[0] = Class.class;
            args[1] = String.class;
            args[2] = int.class;
            Method a = net.minecraft.server.v1_5_R3.EntityTypes.class.getDeclaredMethod("a", args);
            a.setAccessible(true);
            a.invoke(a, ZombieType.class, "Zombie", 54);
            a.invoke(a, ZombieGiantType.class, "Giant", 53);
            a.invoke(a, AngryGolem.class, "VillagerGolem", 99);
        } catch (Exception e) {
            e.printStackTrace();
            this.setEnabled(false);
        }
        
        myExecutor = new PluginCommandExecutor(this);
        getCommand(myName).setExecutor(myExecutor);
        
        loadConfiguration(true);
        
        if (debugMode) {
            logger.info("[" + myName + "] DebugMode Enabled.");
        }
        if (dayspawner) {
            logger.info("[" + myName + "] Day Spawner Enabled.");
        }
        if (proximityspawner) {
            logger.info("[" + myName + "] Proximity Spawner Enabled.");
        }
        
        // OK we're scheduling alot of stuff here :/
        // Basic Housekeeping (untracking non-existant entities etc)
        // plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Housekeeping(plugin),1200L,1200L);
        
        // Thread safe stuff can go here - this can be bloaty.
        // plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(plugin, new
        // ProcessAsyncedTasks(plugin),200L,200L);
        
        // Sync thread to trigger animations uses "interval" and % devision to execute tasks ever 5 runs etc.
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Animations(this), 20L, 20L);
        
        // Super lightweight task to set speeds vectors frequently
        // plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Throttle(plugin),0,5L);
        
        // dayspawner
        // plugin.getServer().getScheduler().scheduleSyncRepeatingTask(this, new DaySpawner(plugin), 600L, 600L);
        
        // proximityspawner) {
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new ProximitySystems(this), 200L, 100L);
        
       
        logger.info("[" + myName + "] Online.");
    }
    
    public PutredineImmortui getZombie(Entity entity) {
        return Utils.getZombie(entity);
    }
    
    /**
     * Initialise config file
     */
    public void loadConfiguration(Boolean firstrun) {
        getConfig().options().copyDefaults(true);
        if (firstrun) {
            String headertext;
            headertext = "Default " + this.myName + " Config file\r\n";
            headertext += "\r\n";
            headertext += "\r\n";
            headertext += "debugMode: [true|false] Enable extra debug info in logs.\r\n";
            headertext += "chunklimit: [4] Limit to zombies per chunk in spawner.";
            headertext += "zombiespawnratio: [50] Spawn chance %.";
            headertext += "spawnmultiplier: [0] extra zombies to spawn.";
            headertext += "\r\n";
            
            getConfig().options().header(headertext);
            getConfig().options().copyHeader(true);
        } else {
            this.reloadConfig();
        }
        debugMode = getConfig().getBoolean("debugMode", false);
        dayspawner = getConfig().getBoolean("dayspawner", true);
        proximityspawner = getConfig().getBoolean("proximityspawner", true);
        blocknaturalspawns = getConfig().getBoolean("blocknaturalspawns", true);
        givezombieplayeritems = getConfig().getBoolean("givezombieplayeritems", true);
        chunklimit = getConfig().getInt("debugMode", 4);
        zombiespawnratio = getConfig().getInt("zombiespawnratio", 50);
        zombiespawnerratio = getConfig().getInt("zombiespawnerratio", 80);
        
        spawnmultiplier = getConfig().getInt("spawnmultiplier", 0);
        factionsWildName = getConfig().getString("factionsWildName", "wilderness");
        factionsSafeName = getConfig().getString("factionsSafeName", "safezone");
        factionsWarZone = getConfig().getString("factionsWarZone", "warzone");
        
        double sx, sy, sz;
        Location sp = Bukkit.getServer().getWorlds().get(0).getSpawnLocation();
        sx = getConfig().getDouble("spawnX", sp.getX());
        sy = getConfig().getDouble("spawnY", sp.getY());
        sz = getConfig().getDouble("spawnZ", sp.getZ());
        ZombieMod.spawnLoc = new Location(sp.getWorld(), sx, sy, sz);
        
        @SuppressWarnings("unchecked")
        List<String> breaks = (List<String>) getConfig().getList("allowedbreaks");
        if (breaks != null) {
            for (String breakable : breaks) {
                Material blk = Material.getMaterial(breakable);
                allowedbreaks.add(blk);
            }
        }
        
        @SuppressWarnings("unchecked")
        List<String> breaks2 = (List<String>) getConfig().getList("allowedpermbreaks");
        if (breaks2 != null) {
            for (String breakable : breaks2) {
                Material blk = Material.getMaterial(breakable);
                allowedpermbreaks.add(blk);
            }
        }
        
        saveConfig();
        
        if (firstrun) {
            this.getLangConfig();
        } else {
            this.reloadLangConfig();
        }
        exitMessage = getLangConfig().getString("exitMessage");
        saveLangConfig();
        
        genera = new ReadData(this);
        wpm = new WeightedProbMap<String>(genera.elts);
    }
    
    
    public void reloadLangConfig() {
        if (LangConfigurationFile == null) {
            LangConfigurationFile = new File(getDataFolder(), "lang.yml");
        }
        LangConfig = YamlConfiguration.loadConfiguration(LangConfigurationFile);
        LangConfig.options().copyDefaults(true);
        
        // Look for defaults in the jar
        InputStream defConfigStream = getResource("lang.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            LangConfig.setDefaults(defConfig);
        }
    }
    
    public FileConfiguration getLangConfig() {
        if (LangConfig == null) {
            reloadLangConfig();
        }
        return LangConfig;
    }
    
    public void saveLangConfig() {
        if (LangConfig == null || LangConfigurationFile == null) {
            return;
        }
        try {
            LangConfig.save(LangConfigurationFile);
        } catch (IOException ex) {
            logger.severe("Could not save Lang config to " + LangConfigurationFile + " " + ex);
        }
    }
}
