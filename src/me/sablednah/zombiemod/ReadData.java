package me.sablednah.zombiemod;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.FileManager;

public class ReadData {
    
    public ZombieMod plugin;
    public Map<String, Config> configs = new HashMap<String, Config>();
    public ArrayList<Pair<Integer, String>> elts = new ArrayList<Pair<Integer, String>>();
    
    public ReadData(ZombieMod p) {
        this.plugin = p;
        
        File genusDir = new File(plugin.getDataFolder() + File.separator + "genera");
        ZombieMod.logger.info("[" + this.plugin.myName + "] Scanning: " + genusDir);
        
        if (!genusDir.exists()) {
            genusDir.mkdir();
        }
        
        File[] genera = genusDir.listFiles();
        
        for (File genus : genera) {
            if (genus.isFile() && genus.getName().toLowerCase().endsWith(".yml")) {
                // found a config file - load it in time
                ZombieMod.logger.info("[" + this.plugin.myName + "] Found: " + genus.getName());
                Config c = new Config();
                Boolean validConfig = true;
                YamlConfiguration thisConfig = null;
                try {
                    thisConfig = YamlConfiguration.loadConfiguration(genus);
                    
                    c.commonName = thisConfig.getString("name");
                    c.jockey = thisConfig.getString("jockey",null);
                    c.species = thisConfig.getString("species");
                    c.skin = thisConfig.getString("skin", null);
                    if (c.skin != null && ZombieMod.hasSpout) {
                        // SpoutManager sm = new SpoutManager();
                        FileManager fm = SpoutManager.getFileManager();
                        boolean worked = fm.addToCache(plugin, c.skin);
                        if (ZombieMod.debugMode) {
                            if (!worked) {
                                // ZombieMod.logger.info("["+ZombieMod.myName+"] skinload fail: " + c.skin);
                                worked = fm.canCache(c.skin);
                                // ZombieMod.logger.info("["+ZombieMod.myName+"] canCache?: " + worked);
                            }
                        }
                    }
                    
                    c.maxHealth = thisConfig.getInt("maxHealth", 20);
                    c.agro = thisConfig.getInt("agro", 20);
                    c.armour = thisConfig.getInt("armour", 2);
                    c.damage = thisConfig.getInt("damage", 6);
                    c.noBurn = thisConfig.getBoolean("noBurn", false);
                    c.passive = thisConfig.getBoolean("passive", false);
                    c.coward = thisConfig.getBoolean("coward", true);
                    c.isPet = thisConfig.getBoolean("isPet", false);
                    c.size = (float) thisConfig.getDouble("size", 1.0);
                    c.speed = thisConfig.getDouble("speed", 1.0);
                    c.attackSpeed = thisConfig.getDouble("attackSpeed", c.speed);
                    c.bounty = thisConfig.getDouble("bounty", 0.0);
                    c.xp = thisConfig.getInt("xp", 0);
                    
                    int tmp;
                    tmp = thisConfig.getInt("handItem", 0);
                    if (tmp > 0) {
                        c.equip[0] = new ItemStack(Material.getMaterial(tmp), 1);
                    }
                    tmp = thisConfig.getInt("bootsItem", 0);
                    if (tmp > 0) {
                        c.equip[1] = new ItemStack(Material.getMaterial(tmp), 1);
                    }
                    tmp = thisConfig.getInt("leggingsItem", 0);
                    if (tmp > 0) {
                        c.equip[2] = new ItemStack(Material.getMaterial(tmp), 1);
                    }
                    tmp = thisConfig.getInt("chestplateItem", 0);
                    if (tmp > 0) {
                        c.equip[3] = new ItemStack(Material.getMaterial(tmp), 1);
                    }
                    tmp = thisConfig.getInt("helmetItem", 0);
                    if (tmp > 0) {
                        if (tmp==397) {
                            String skullname = thisConfig.getString("skullname","player");
                            
                            if (skullname != null && !skullname.equals("player")) {
                                System.out.print("skullname:"+skullname);
                                c.equip[4] = Utils.getSkullItemStack(1, skullname);
                            } else {
                                c.equip[4] = new ItemStack(Material.getMaterial(tmp), 1);                                
                            }
                        } else {
                            c.equip[4] = new ItemStack(Material.getMaterial(tmp), 1);                            
                        }
                    }
                    
                    @SuppressWarnings("unchecked")
                    List<String> effects = (List<String>) thisConfig.getList("effects");
                    if (effects != null) {
                        for (String thisEffect : effects) {
                            // ZombieMod.logger.info("["+ZombieMod.myName+"] Effect: " + thisEffect);
                            Effect x = Effect.valueOf(thisEffect);
                            // ZombieMod.logger.info("["+ZombieMod.myName+"] x: " + x);
                            c.effects.add(x);
                        }
                    }
                    
                    @SuppressWarnings("unchecked")
                    List<String> potions = (List<String>) thisConfig.getList("potions");
                    if (potions != null) {
                        for (String thisPotion : potions) {
                            c.potions.add(new PotionEffect(PotionEffectType.getByName(thisPotion), 20, 1));
                        }
                    }
                    
                    @SuppressWarnings("unchecked")
                    List<String> items = (List<String>) thisConfig.getList("items");
                    
                    int itemcount =0;
                    if (items != null) {
                        for (String thisItem : items) {
                            c.items.add(new ItemStack(Material.matchMaterial(thisItem), 1));
                            itemcount++;
                        }
                    }
                    
                    
                    
                    @SuppressWarnings("unchecked")
                    List<String> abilities = (List<String>) thisConfig.getList("abilities");
                    c.abilities = abilities;
                    
                    @SuppressWarnings("unchecked")
                    List<String> schematics = (List<String>) thisConfig.getList("schematics");
                    c.schematics = schematics;
                    
                    @SuppressWarnings("unchecked")
                    List<String> contexts = (List<String>) thisConfig.getList("contexts");
                    c.contexts = contexts;
                    
                    @SuppressWarnings("unchecked")
                    List<Double> dropRates = (List<Double>) thisConfig.getList("droprates");
                    c.dropRates = dropRates;
                    int dropCount =0;
                    if (dropRates!=null) {dropCount = dropRates.size();}
                    
                    int afreq = thisConfig.getInt("altfrequency", 0);
                    if (afreq == 0) {
                        afreq = thisConfig.getInt("frequency");
                    }
                    c.altfrequency = afreq;
                    
                    if (dropCount != itemcount ) {
                        ZombieMod.logger.info("[" + this.plugin.myName + "] Dropcount Missmatch: " + dropCount + " drops | " + itemcount + " items" );
                    }
                    
                } catch (Exception e) {
                    validConfig = false;
                    e.printStackTrace();
                }
                
                if (validConfig) {
                    elts.add(new Pair<Integer, String>(thisConfig.getInt("frequency"), c.commonName.toLowerCase()));
                    configs.put(c.commonName.toLowerCase(), c);
                } else {
                    ZombieMod.logger.info("[" + this.plugin.myName + "] Invalid Config: " + genus.getName());
                }
            }
        }
    }
}
