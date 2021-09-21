package fr.tiakin.player;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import fr.tiakin.main.folder;
import fr.tiakin.main.main;


public class vanish implements CommandExecutor {
	public folder z;
	Plugin main;
	public vanish(main main) {
		this.main = main;
	}
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
		if(sender.isOp()) {
			Player e = null;
			if(args.length == 0) {
				e = Bukkit.getPlayer(sender.getName());
			}else if(args.length == 1){
				e = Bukkit.getPlayer(args[0]);
			}
				z = new folder("vanish", e.getName(), "false");
				if(z.readfolder() == null) {
					z.addinfolder();
				}
					
				if(z.readfolder().equalsIgnoreCase("false")) {
				for(Player p : Bukkit.getOnlinePlayers())
					if(!p.getName().equalsIgnoreCase(e.getName()))
						p.hidePlayer(main,e);
				z = new folder("vanish", e.getName(), "true");
				z.editfolder();
				e.setCanPickupItems(false);
				Bukkit.broadcastMessage(ChatColor.YELLOW+e.getName()+" left the game");
				}else {
	                 for(Player p : Bukkit.getOnlinePlayers())
	                	 if(!p.getName().equalsIgnoreCase(e.getName()))
	                		 p.showPlayer(main,e);
	                z = new folder("vanish", e.getName(), "false");
	 				z.editfolder();
	 				e.setCanPickupItems(true);
	 				Bukkit.broadcastMessage(ChatColor.YELLOW+e.getName()+" joined the game");
				}
			}
		return false;
	}
}
