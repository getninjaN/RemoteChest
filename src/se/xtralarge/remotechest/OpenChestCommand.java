package se.xtralarge.remotechest;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;

public class OpenChestCommand {
	private final RemoteChest rc;
	
	public OpenChestCommand(RemoteChest rc) {
		this.rc = rc;
	}
	
	public boolean openChest(Player player) {
		// DOES THE CHOSEN SLOT EXIST?
		if(rc.userdata.isSet(player.getName() +".chest"+ rc.selectedSlots.get(player.getName()))) {
			String[] chestData = rc.userdata.getString(player.getName() +".chest"+ rc.selectedSlots.get(player.getName())).split(","); // Get slot data
			World chestWorld = rc.getServer().getWorld(chestData[0]); // Get world
			double chestX = Double.parseDouble(chestData[1]); // Set chest X
			double chestY = Double.parseDouble(chestData[2]); // Set chest Y
			double chestZ = Double.parseDouble(chestData[3]); // Set chest Z
			
			Location chestLocation = new Location(chestWorld, chestX, chestY, chestZ); // Get chest location
			Block locationBlock = chestLocation.getBlock(); // Get block at location
			
			// Check chest location is air
			if(locationBlock.getType().getId() == 0) {
				//Inform that the chest is not found at location
				player.sendMessage(ChatColor.RED + rc.parseMessage(rc.config.getString("messages.chestgone"), player.getName()));
				
				return true;
			}
			
			// Check if player has the permission to build at chest location
			if(rc.wg.canBuild(player, chestLocation)) {
				if(locationBlock.getType().getId() == 54) {
					Chest chest = (Chest)chestLocation.getBlock().getState(); // Get chest
					player.openInventory(chest.getInventory()); // Open chest inventory
					
					if(rc.economyuse && rc.economyopen) {
						double topay = rc.config.getDouble("economy.opencost"); // Set price to open
						
						rc.withdraw(player,true,topay); // Withdraw
					}
				}
			} else {
				player.sendMessage(ChatColor.RED + rc.parseMessage(rc.config.getString("messages.chestprotected"),player.getName())); // Send protected message
			}
		} else {
			player.sendMessage(ChatColor.YELLOW + rc.parseMessage(rc.config.getString("messages.chestnotfound"),player.getName())); // Send chest not found
		}
		
		return true;
	}
}
