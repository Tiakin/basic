package fr.tiakin.player;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Revive implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
		if(sender.isOp()) {
		if(args.length == 0) {
			Bukkit.getPlayer(sender.getName()).spigot().respawn();
		}else {
			Bukkit.getPlayer(args[0]).spigot().respawn();
		}
		}
		return false;
	}
	
}
