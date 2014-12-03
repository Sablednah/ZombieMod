package me.sablednah.zombiemod;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PluginCommandExecutor implements CommandExecutor {
	public ZombieMod	plugin;

	public PluginCommandExecutor(ZombieMod instance) {
		this.plugin = instance;
	}

	@SuppressWarnings("deprecation")
    @Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase(this.plugin.myName.toLowerCase())) {
			if (args.length > 0 && args[0].toLowerCase().equals("reload")) {
				Boolean doReload = false;

				if (sender instanceof Player) {
					if (sender.hasPermission(this.plugin.myName.toLowerCase() + ".reload")) {
						doReload = true;
					} else {
						sender.sendMessage("You do not have permission to reload.");
						return true;
					}
				} else {
					doReload = true;
				}

				if (doReload) {
					plugin.loadConfiguration(false);
					return true;
				}
			}

			if (args.length > 0 && args[0].toLowerCase().equals("setspawn")) {
				if (sender instanceof Player) {
					if (sender.hasPermission(this.plugin.myName.toLowerCase() + ".setspawn")) {
						ZombieMod.spawnLoc = ((Player) sender).getLocation();
						plugin.getConfig().set("spawnX", ZombieMod.spawnLoc.getX());
						plugin.getConfig().set("spawnY", ZombieMod.spawnLoc.getY());
						plugin.getConfig().set("spawnZ", ZombieMod.spawnLoc.getZ());
						plugin.saveConfig();

						sender.sendMessage("Spawn origin reset.");
						return true;
					} else {
						sender.sendMessage("You do not have permission to set spawn origin.");
						return true;
					}
				} else {
					ZombieMod.logger.info("[" + this.plugin.myName + "] Command not valid from Consolse.");
					return true;
				}
			}

			if (args.length > 0 && args[0].toLowerCase().equals("stomp")) {
                            if (sender instanceof Player) {
                                    if (sender.hasPermission(this.plugin.myName.toLowerCase() + ".stomp")) {
                                            ZombieMod.spawnLoc = ((Player) sender).getLocation();
                                            Utils.stomp(((Player) sender).getTargetBlock(null, 16).getLocation(), ((Player) sender), 6, 10);
                                            return true;
                                    } else {
                                            sender.sendMessage("You do not have permission to set spawn origin.");
                                            return true;
                                    }
                            } else {
                                    ZombieMod.logger.info("[" + this.plugin.myName + "] Command not valid from Consolse.");
                                    return true;
                            }
                    }
			
			if (args.length > 0 && args[0].toLowerCase().equals("soundtest")) {
				if (sender instanceof Player) {
					if (sender.hasPermission(this.plugin.myName.toLowerCase() + ".soundtest")) {
						Player pl = (Player) sender;

						float a1, a2;
						Sound s = Sound.GHAST_SCREAM2;
						a1 = 1;
						a2 = 1;

						if (args.length > 2) {
							a1 = new Float(args[1]);
							a2 = new Float(args[2]);
							s = Sound.valueOf(args[3]);
						}

						pl.getWorld().playSound(pl.getLocation(), s, a1, a2);

						sender.sendMessage("sound played.");
						return true;
					} else {
						sender.sendMessage("You do not have permission to do that.");
						return true;
					}
				} else {
					ZombieMod.logger.info("[" + this.plugin.myName + "] Command not valid from Consolse.");
					return true;
				}
			}

			if (args.length > 0 && args[0].toLowerCase().equals("stats")) {
				ZombieMod.logger.info("[" + this.plugin.myName + "] Intervals - " + ZombieMod.intervals + ".");
				for (World w : plugin.getServer().getWorlds()) {
					Collection<Entity> zombies = w.getEntities();
					for (Entity e : zombies) {
						PutredineImmortui zomb = Utils.getZombie(e);
						if (zomb != null) {
							ZombieMod.logger.info("[" + this.plugin.myName + "] " + zomb.species + " | " + zomb.commonName + "  [" + Math.floor(e.getLocation().getX()) + " X - " + Math.floor(e.getLocation().getY()) + " Y - "
									+ Math.floor(e.getLocation().getZ()) + " Z]");
						}
					}
				}
				ZombieMod.logger.info("[" + this.plugin.myName + "] ------ Player zombies ----------------");
				Iterator<Map.Entry<UUID, PutredineImmortui>> it = ZombieMod.playerZombies.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<UUID, PutredineImmortui> entry = it.next();
					System.out.println(entry.getKey() + " = " + entry.getValue().commonName + " | " + entry.getValue().cid + " [" + Math.floor(entry.getValue().lastLoc.getX()) + " X - " + Math.floor(entry.getValue().lastLoc.getY())
							+ " Y - " + Math.floor(entry.getValue().lastLoc.getZ()) + " Z]");

				}
			}
			if (args.length > 0 && args[0].toLowerCase().equals("spawn")) {
				if (sender instanceof Player) {
					if (sender.hasPermission("zombiemod.spawn")) {
						if (args.length > 1) {
							String genus = (args[1].toLowerCase());
							if (plugin.genera.configs.containsKey(genus)) {
								Block lookAt = ((Player) sender).getTargetBlock(null, 20);
								Block safeNewBlock = Utils.getNearestEmptySpace(lookAt, 5);
								if (safeNewBlock != null) {
									Location sqawnLoc = safeNewBlock.getLocation();

									PutredineImmortui zomb = new PutredineImmortui(plugin, genus);
									
									Utils.spawnZombie(zomb, sqawnLoc, plugin);
                                                                        
									if (ZombieMod.debugMode) {
                                                                            ZombieMod.logger.info("[" + this.plugin.myName + "] " + zomb.commonName + " spawned via command");
                                                                        }
                                                                        sender.sendMessage("Zombie '" + zomb.commonName + "' spawned.");

									return true;
								}
							} else {
								sender.sendMessage("Zombie type [" + args[1].toLowerCase() + "] not found.");
								return true;
							}
						} else {
							Block lookAt = ((Player) sender).getTargetBlock(null, 20);
							Block safeNewBlock = Utils.getNearestEmptySpace(lookAt, 5);
							if (safeNewBlock != null) {

								Location sqawnLoc = safeNewBlock.getLocation();

								PutredineImmortui zomb = new PutredineImmortui(plugin);
								if (ZombieMod.debugMode) {
									ZombieMod.logger.info("[" + this.plugin.myName + "] " + zomb.commonName + " spawned via command");
								}
								sender.sendMessage("Zombie '" + zomb.commonName + "' spawned.");

								Utils.spawnZombie(zomb, sqawnLoc, plugin);
								
								return true;
							}
						}
					} else {
						sender.sendMessage("I'm sorry " + sender.getName() + ", I can't let you spawn that.");
						return true;
					}
				} else {
					ZombieMod.logger.info("[" + this.plugin.myName + "] Command not valid from Consolse.");
					return true;
				}
			}
		}
		return false;
	}
}