package se.xtralarge.remotechest;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class SetChestCommand {
	private final RemoteChest rc;
	
	public SetChestCommand(RemoteChest rc) {
		this.rc = rc;
	}
	
	public boolean setChest(Player player) {
		// Check to see if slot is set
		if(rc.userdata.isSet(player.getName() +".chest"+ rc.selectedSlots.get(player.getName()))) {
			// Inform that a chest is already saved in the slot
			player.sendMessage(ChatColor.RED + rc.parseMessage(rc.config.getString("messages.slottaken"),player.getName()));
			player.sendMessage(ChatColor.RED + rc.parseMessage(rc.config.getString("messages.takenabort"),player.getName()));
		}
		
		// Check if player is not in "queue"
		if(!rc.chestSetQueue.contains(player)) { rc.chestSetQueue.add(player); /* Add player to "queue" */ }
		
		// Send information on how to set chest
		player.sendMessage(rc.parseMessage(rc.config.getString("messages.clickchest"),player.getName()));
		
		return true;
	}
}
