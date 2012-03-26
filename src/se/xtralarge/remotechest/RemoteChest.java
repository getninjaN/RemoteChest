package se.xtralarge.remotechest;

import net.milkbowl.vault.economy.Economy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.bukkit.GameMode;
import org.bukkit.Server;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class RemoteChest extends JavaPlugin {
	//public Boolean chestDoSet = false;
	public ArrayList<Player> chestSetQueue = new ArrayList<Player>();
	public HashMap<String, Integer> selectedSlots = new HashMap<String, Integer>();
	
	private static Logger log = Logger.getLogger("Minecraft"); // Log
	public static Economy economy = null;
	
	public Boolean economyuse;
	public Boolean economyopen;
	public Boolean economyset;
	private static int opencost = 0;
	private static int setcost = 0;
	private static int maxslots = 0;
	private static String depositto = "";
	
	//public static int selectedslot;
	
	private double balance;
	private Server server;
	public FileConfiguration config = null;
	protected PluginManager pm;
	protected WorldGuardPlugin wg;
	
	public FileConfiguration userdata = null;
	private static File userdataConfigFile = null;
	
	protected SetChestCommand set;
	protected OpenChestCommand open;
	
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
		
		set = new SetChestCommand(this);
		open = new OpenChestCommand(this);
		
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
			
			if(player.getGameMode() == GameMode.CREATIVE) {
				sender.sendMessage(parseMessage(config.getString("messages.notincreative"),player.getName()));
				
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
					player.sendMessage(ChatColor.RED + parseMessage(config.getString("messages.chooseslot"),player.getName()));
					return false;
				} else {
					try {
						selectedSlots.put(player.getName(), Integer.parseInt(args[1]));
						
						//selectedslot = Integer.parseInt(args[1]);
					} catch(NumberFormatException nFE) {
						player.sendMessage(parseMessage(config.getString("messages.choosebetween"),player.getName()));
						
						return true;
					}
				}
			} else if(maxslots == 1) {
				selectedSlots.put(player.getName(), 1);
				//selectedslot = 1;
			} else {
				selectedSlots.remove(player.getName());
				//selectedslot = -1;
			}
			
			if(!selectedSlots.containsKey(player.getName())) {
				player.sendMessage(parseMessage(config.getString("messages.cantuse"),player.getName()));
			} else if(selectedSlots.get(player.getName()) > maxslots) {
				player.sendMessage(parseMessage(config.getString("messages.choosebetween"),player.getName()));
			}
			
			// SET
			if(args.length >= 1 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("s"))) {
				if (((economyuse && economyset) && economy != null) && balance < setcost) {
				    player.sendMessage(ChatColor.RED + parseMessage(config.getString("messages.notaffordset"),player.getName()));
				    return true;
				}
				
				boolean setChest = set.setChest(player);
				
				return setChest;
				
			// OPEN
			} else if(args.length >= 1 && (args[0].equalsIgnoreCase("open") || args[0].equalsIgnoreCase("o"))) {
				// CHECK IF ECONOMY AND IF BALANCE
				if (((economyuse && economyopen) && economy != null) && balance < opencost) {
				    player.sendMessage(ChatColor.RED + parseMessage(config.getString("messages.notaffordopen"),player.getName()));
				    return true;
				}
				
				boolean openChest = open.openChest(player);
				
				
				return openChest;
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
	
	public String parseMessage(String message, String playerName) {
		String parsedString = message;
		
		parsedString = parsedString.replaceAll("%setcost%", setcost +"");
		parsedString = parsedString.replaceAll("%opencost%", opencost +"");
		parsedString = parsedString.replaceAll("%chestslot%",selectedSlots.get(playerName) +"");
		parsedString = parsedString.replaceAll("%maxslots%", maxslots +"");
		parsedString = parsedString.replaceAll("%plugin%", this.getDescription().getName());
		parsedString = parsedString.replaceAll("%maxslots%", maxslots +"");
		
		return parsedString;
	}
	
	public void setChest(Player player, String chestInfo) {
		Boolean widthdraw = false;
		
		if(economyuse && economyset) {
			if(userdata.getString(player.getName() +".chest"+ selectedSlots.get(player.getName())) == null) {
				widthdraw = true;
			}
		}
		
		String message = config.getString("messages.chestset");
		userdata.set(player.getName() +".chest"+ selectedSlots.get(player.getName()), chestInfo);
		saveUserdata();
		
		if(widthdraw) {
			double topay = config.getDouble("economy.setcost");
			
			economy.withdrawPlayer(player.getName(), topay);
			if(depositto != null) { economy.depositPlayer(depositto, topay); }
			
			player.sendMessage(parseMessage(config.getString("messages.setwithdraw"),player.getName()));
		}
		
		resetChestSet(player,message);
	}
	
	public void resetChestSet(Player player, String message) {
		int queuePosition = chestSetQueue.indexOf(player);
		
		player.sendMessage(parseMessage(message,player.getName()));
		
		if(queuePosition > -1) {
			chestSetQueue.remove(queuePosition);
		}
		
		selectedSlots.remove(player.getName());
		//selectedslot = -1;
	}
	
	public void widthdraw(Player player) {
		double topay = config.getDouble("economy.opencost");
		
		economy.withdrawPlayer(player.getName(), topay);
		if(depositto != null) { economy.depositPlayer(depositto, topay); }
		
		player.sendMessage(parseMessage(config.getString("messages.openwithdraw"),player.getName()));
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
