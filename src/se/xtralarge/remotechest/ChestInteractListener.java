package se.xtralarge.remotechest;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;


public class ChestInteractListener implements Listener {
	private final RemoteChest rc;
	
	public ChestInteractListener(RemoteChest rc) {
		this.rc = rc;
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void chestInteract(PlayerInteractEvent event) {
		if(rc.chestDoSet) {
			Block block = event.getClickedBlock();
			Player player = event.getPlayer();
			
			if(block.getType().getId() == 54) {
				Location blockLoc = block.getLocation();
				String blockInfo = blockLoc.getWorld().getName() +","+ blockLoc.getBlockX() +","+ blockLoc.getBlockY() +","+ blockLoc.getBlockZ();
				
				rc.setChest(player,blockInfo);
			} else {
				player.sendMessage("Du måste klicka på en kista");
			}
		}
	}
}