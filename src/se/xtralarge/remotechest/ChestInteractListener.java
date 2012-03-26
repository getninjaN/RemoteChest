package se.xtralarge.remotechest;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;


public class ChestInteractListener implements Listener {
	private final RemoteChest rc;
	
	public ChestInteractListener(RemoteChest rc) {
		this.rc = rc;
	}
	
	@EventHandler
	public void chestInteract(PlayerInteractEvent event) {
		if(event.getClickedBlock() == null) { return; }
		if(event.getClickedBlock().getType().getId() != 54) { return; }
		
		if(rc.chestSetQueue.indexOf(event.getPlayer()) > -1) {
			Block block = event.getClickedBlock();
			Player player = event.getPlayer();
			
			
			if(rc.wg.canBuild(player, block.getLocation())) {
				Location blockLoc = block.getLocation();
				String blockInfo = blockLoc.getWorld().getName() +","+ blockLoc.getBlockX() +","+ blockLoc.getBlockY() +","+ blockLoc.getBlockZ();
				
				rc.setChest(player,blockInfo);
			} else {
				String message = "";
				rc.resetChestSet(player,message);
			}
		}
	}
}