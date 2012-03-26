package se.xtralarge.remotechest;

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
			String[] chestData = rc.userdata.getString(player.getName() +".chest"+ rc.selectedSlots.get(player.getName())).split(",");
			World chestWorld = rc.getServer().getWorld(chestData[0]);
			double chestX = Double.parseDouble(chestData[1]);
			double chestY = Double.parseDouble(chestData[2]);
			double chestZ = Double.parseDouble(chestData[3]);
			
			Location chestLocation = new Location(chestWorld, chestX, chestY, chestZ);
			Block locationBlock = chestLocation.getBlock();
			
			if(rc.wg.canBuild(player, chestLocation)) {
				if(locationBlock.getType().getId() == 54) {
					Chest chest = (Chest)chestLocation.getBlock().getState();
					player.openInventory(chest.getInventory());
					
					if(rc.economyuse && rc.economyopen) {
						double topay = rc.config.getDouble("economy.opencost");
						
						rc.withdraw(player,true,topay);
					}
				}
			} else {
				player.sendMessage(rc.parseMessage(rc.config.getString("messages.chestprotected"),player.getName()));
			}
		} else {
			player.sendMessage(rc.parseMessage(rc.config.getString("messages.chestnotfound"),player.getName()));
		}
		
		return true;
	}
}
