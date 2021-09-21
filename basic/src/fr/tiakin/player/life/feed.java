package fr.tiakin.player.life;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class feed implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
		if(sender.isOp()) {
		if(args.length == 0) {
			Bukkit.getPlayer(sender.getName()).setFoodLevel(20);
		}else {
			Bukkit.getPlayer(args[0]).setFoodLevel(20);
		}
		}
		return false;
	}

}
