package fr.tiakin.player;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class gm implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
			if(sender.isOp()) {
			if(args.length == 1) {
				if(args[0].equalsIgnoreCase("1") ||args[0].equalsIgnoreCase("c") || args[0].equalsIgnoreCase("creative") || args[0].equalsIgnoreCase("creatif"))
				Bukkit.getPlayer(sender.getName()).setGameMode(GameMode.CREATIVE);
				if(args[0].equalsIgnoreCase("0") ||args[0].equalsIgnoreCase("s") || args[0].equalsIgnoreCase("survie") || args[0].equalsIgnoreCase("survival"))
				Bukkit.getPlayer(sender.getName()).setGameMode(GameMode.SURVIVAL);
				if(args[0].equalsIgnoreCase("2") ||args[0].equalsIgnoreCase("a") || args[0].equalsIgnoreCase("aventure") || args[0].equalsIgnoreCase("adventure"))
					Bukkit.getPlayer(sender.getName()).setGameMode(GameMode.ADVENTURE);
				if(args[0].equalsIgnoreCase("3") ||args[0].equalsIgnoreCase("spec") || args[0].equalsIgnoreCase("spectateur") || args[0].equalsIgnoreCase("spectator"))
					Bukkit.getPlayer(sender.getName()).setGameMode(GameMode.SPECTATOR);
			}else {
				if(sender.isOp()) {
					if(args.length == 2) {
						if(args[0].equalsIgnoreCase("1") ||args[0].equalsIgnoreCase("c") || args[0].equalsIgnoreCase("creative") || args[0].equalsIgnoreCase("creatif"))
						Bukkit.getPlayer(args[1]).setGameMode(GameMode.CREATIVE);
						if(args[0].equalsIgnoreCase("0") ||args[0].equalsIgnoreCase("s") || args[0].equalsIgnoreCase("survie") || args[0].equalsIgnoreCase("survival"))
						Bukkit.getPlayer(args[1]).setGameMode(GameMode.SURVIVAL);
						if(args[0].equalsIgnoreCase("2") ||args[0].equalsIgnoreCase("a") || args[0].equalsIgnoreCase("aventure") || args[0].equalsIgnoreCase("adventure"))
							Bukkit.getPlayer(args[1]).setGameMode(GameMode.ADVENTURE);
						if(args[0].equalsIgnoreCase("3") ||args[0].equalsIgnoreCase("spec") || args[0].equalsIgnoreCase("spectateur") || args[0].equalsIgnoreCase("spectator"))
							Bukkit.getPlayer(args[1]).setGameMode(GameMode.SPECTATOR);
					}
			}
			}
			}
			return false;
		}

}
