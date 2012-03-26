package se.xtralarge.remotechest;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class SetChestCommand {
	private final RemoteChest rc;
	
	public SetChestCommand(RemoteChest rc) {
		this.rc = rc;
	}
	
	public boolean setChest(Player player) {
		// CHECK IF THE SLOT HAS DATA
		if(rc.userdata.isSet(player.getName() +".chest"+ rc.selectedSlots.get(player.getName()))) {
			player.sendMessage(ChatColor.RED + rc.parseMessage(rc.config.getString("messages.slottaken"),player.getName()));
			player.sendMessage(ChatColor.RED + rc.parseMessage(rc.config.getString("messages.takenabort"),player.getName()));
		}
		
		if(!rc.chestSetQueue.contains(player)) {
			rc.chestSetQueue.add(player);
		}
		
		player.sendMessage(rc.parseMessage(rc.config.getString("messages.clickchest"),player.getName()));
		
		return true;
	}
}
