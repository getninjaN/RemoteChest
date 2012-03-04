package se.xtralarge.remotechest;

import net.milkbowl.vault.economy.Economy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class RemoteChest extends JavaPlugin {
	public Boolean chestDoSet = false; 
	private static Logger log = Logger.getLogger("Minecraft"); // Log
	public static Economy economy = null;
	
	private static Boolean economyuse;
	private static Boolean economyopen;
	private static Boolean economyset;
	private static int opencost = 0;
	private static int setcost = 0;
	private static int maxslots = 0;
	private static String depositto = "";
	
	public static int selectedslot;
	
	private double balance;
	private Server server;
	public FileConfiguration config = null;
	protected PluginManager pm;
	protected WorldGuardPlugin wg;
	
	public static FileConfiguration userdata = null;
	private static File userdataConfigFile = null;
	
	public void onEnable(){
		log.info(this.getDescription().getFullName() +" has been enabled!");
		
		this.server = this.getServer();
		this.pm = this.server.getPluginManager();
		this.wg = getWorldGuard();
		
		this.getConfig().options().copyDefaults(true);
		saveConfig();
		
		userdata = getUserdata();
		
		config = this.getConfig();
		economyuse = config.getBoolean("economy.use");
		economyopen = config.getBoolean("economy.foropen");
		economyset = config.getBoolean("economy.forset");
		opencost = config.getInt("economy.opencost");
		setcost = config.getInt("economy.setcost");
		maxslots = config.getInt("maxslots");
		depositto = config.getString("economy.depositto");
		
		setupEconomy();
		
		pm.registerEvents(new ChestInteractListener(this),this);
	}
	
	public void onDisable(){
		log.info(this.getDescription().getFullName() +" has been disabled.");
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		Player player = null;
		int opencost = config.getInt("economy.opencost");
		int setcost = config.getInt("economy.setcost");
		
		if(cmd.getName().equalsIgnoreCase("remotechest")) {
			if(args.length == 1 && args[0].equalsIgnoreCase("reload")) {
				if(sender.hasPermission("remotechest.reload")) {
					configReload(sender);
						
					return true;
				}
			}
			
			if (sender instanceof Player) {
				player = (Player) sender;
			} else {
				sender.sendMessage("You have to be a player in order to use "+ this.getDescription().getFullName());
				return true;
			}
			
			// GET BALANCE
			if(economyuse && economy != null) { balance = economy.getBalance(player.getName()); }
			
			// CANCEL
			if(args.length >= 1 && (args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("c"))) {
				String message = config.getString("messages.setaborted");
				resetChestSet(player,message);
				
				return true;
			}
			
			// DO SOME SLOT CHECKS
			if(maxslots > 1) {
				if(args.length < 2) {
					player.sendMessage(ChatColor.RED + parseMessage(config.getString("messages.chooseslot")));
					return false;
				} else {
					try {
						selectedslot = Integer.parseInt(args[1]);
					} catch(NumberFormatException nFE) {
						player.sendMessage(parseMessage(config.getString("messages.choosebetween")));
						
						return true;
					}
				}
			} else if(maxslots == 1) {
				selectedslot = 1;
			} else {
				selectedslot = -1;
			}
			
			if(selectedslot == -1) {
				player.sendMessage(parseMessage(config.getString("messages.cantuse")));
			} else if(selectedslot > maxslots) {
				player.sendMessage(parseMessage(config.getString("messages.choosebetween")));
			}
			
			// SET
			if(args.length >= 1 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("s"))) {
				if (((economyuse && economyset) && economy != null) && balance < setcost) {
				    player.sendMessage(ChatColor.RED + parseMessage(config.getString("messages.notaffordset")));
				    //ChatColor.RED + "- Du har inte råd att köpa sol... Det kostar "+ setcost +"c!"
				    return true;
				}
				
				// CHECK IF THE SLOT HAS DATA
				if(userdata.isSet(player.getName() +".chest"+ selectedslot)) {
					player.sendMessage(ChatColor.RED + parseMessage(config.getString("messages.slottaken")));
					player.sendMessage(ChatColor.RED + parseMessage(config.getString("messages.takenabort")));
				}
				
				chestDoSet = true;
				player.sendMessage(parseMessage(config.getString("messages.clickchest")));
				
				return true;
				
			// OPEN
			} else if(args.length >= 1 && (args[0].equalsIgnoreCase("open") || args[0].equalsIgnoreCase("o"))) {
				// CHECK IF ECONOMY AND IF BALANCE
				if (((economyuse && economyopen) && economy != null) && balance < opencost) {
				    player.sendMessage(ChatColor.RED + parseMessage(config.getString("messages.notaffordopen")));
				    return true;
				}
				
				
				// DOES THE CHOSEN SLOT EXIST?
				if(userdata.isSet(player.getName() +".chest"+ selectedslot)) {
					String[] chestData = userdata.getString(player.getName() +".chest"+ selectedslot).split(",");
					World chestWorld = this.server.getWorld(chestData[0]);
					double chestX = Double.parseDouble(chestData[1]);
					double chestY = Double.parseDouble(chestData[2]);
					double chestZ = Double.parseDouble(chestData[3]);
					
					Location chestLocation = new Location(chestWorld, chestX, chestY, chestZ);
					Block locationBlock = chestLocation.getBlock();
					
					if(wg.canBuild(player, chestLocation)) {
						if(locationBlock.getType().getId() == 54) {
							Chest chest = (Chest)chestLocation.getBlock().getState();
							player.openInventory(chest.getInventory());
							
							if(economyuse && economyopen) {
								double topay = config.getDouble("economy.opencost");
								
								economy.withdrawPlayer(player.getName(), topay);
								if(depositto != null) { economy.depositPlayer(depositto, topay); }
								
								player.sendMessage(parseMessage(config.getString("messages.openwithdraw")));
							}
						}
					} else {
						player.sendMessage(parseMessage(config.getString("messages.chestprotected")));
					}
				} else {
					player.sendMessage(parseMessage(config.getString("messages.chestnotfound")));
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	// Economy setup
	private Boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) { economy = economyProvider.getProvider(); }
		return (economy != null);
	}
	
	private WorldGuardPlugin getWorldGuard() {
		Plugin plugin = pm.getPlugin("WorldGuard");
		
		if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
			return null; // Maybe you want throw an exception instead
	    }
		
		return (WorldGuardPlugin) plugin;
	}
	
	public String parseMessage(String message) {
		String parsedString = message;
		
		parsedString = parsedString.replaceAll("%setcost%", setcost +"");
		parsedString = parsedString.replaceAll("%opencost%", opencost +"");
		parsedString = parsedString.replaceAll("%chestslot%",selectedslot +"");
		parsedString = parsedString.replaceAll("%maxslots%", maxslots +"");
		parsedString = parsedString.replaceAll("%plugin%", this.getDescription().getName());
		parsedString = parsedString.replaceAll("%maxslots%", maxslots +"");
		
		return parsedString;
	}
	
	public void setChest(Player player, String chestInfo) {
		Boolean widthdraw = false;
		
		if(economyuse && economyset) {
			if(userdata.getString(player.getName() +".chest"+ selectedslot) == null) {
				widthdraw = true;
			}
		}
		
		String message = config.getString("messages.chestset");
		userdata.set(player.getName() +".chest"+ selectedslot, chestInfo);
		saveUserdata();
		
		if(widthdraw) {
			double topay = config.getDouble("economy.setcost");
			
			economy.withdrawPlayer(player.getName(), topay);
			if(depositto != null) { economy.depositPlayer(depositto, topay); }
			
			player.sendMessage(parseMessage(config.getString("messages.setwithdraw")));
		}
		
		resetChestSet(player,message);
	}
	
	public void resetChestSet(Player player, String message) {
		player.sendMessage(parseMessage(message));
		
		chestDoSet = false;
		selectedslot = -1;
	}
	
	/* CUSTOM CONFIG STUFF */
	public void reloadUserdata() {
    	if (userdataConfigFile == null) {
    		userdataConfigFile = new File(getDataFolder(), "userdata.yml");
    	}
    	
    	userdata = YamlConfiguration.loadConfiguration(userdataConfigFile);
    	InputStream defConfigStream = getResource("userdata.yml");
    	if (defConfigStream != null) {
    		YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
    		userdata.setDefaults(defConfig);
    	}
    }
   
	public void loadUserdata(){
		userdata = getUserdata();
		userdata.options().copyDefaults(true);
		saveUserdata();
	}
   
	public FileConfiguration getUserdata() {
		if (userdata == null) {reloadUserdata();}
		return userdata;
	}
   
	public void saveUserdata() {
		if (userdata == null || userdataConfigFile == null) {
			log.info("userdata or userdataConfigFile == null");
			return;
		}
		try {
			userdata.save(userdataConfigFile);
		} catch (IOException ex) {
			Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE, "Could not save config to " + userdataConfigFile, ex);
		}
	}
	/* CUSTOM CONFIG STUFF */
	
	private void configReload(CommandSender sender) {
		this.reloadConfig();
		config = this.getConfig();
		economyuse = config.getBoolean("economy.use");
		economyopen = config.getBoolean("economy.foropen");
		economyset = config.getBoolean("economy.forset");
		opencost = config.getInt("economy.opencost");
		setcost = config.getInt("economy.setcost");
		depositto = config.getString("economy.depositto");
		sender.sendMessage(this.getDescription().getFullName() +" has been reloaded.");
	}
}
