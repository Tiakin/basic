package fr.tiakin.player;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Gm implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
			if(sender.isOp()) {
				Player p = Bukkit.getPlayer(sender.getName());
				if(args.length == 2) {
					p = Bukkit.getPlayer(args[1]);
				}

				if(p == null) {
					sender.sendMessage("Joueur introuvable.");
					return true;
				}
				
				switch (args[0].toLowerCase()) {
					case "0":
					case "s":
					case "survie":
					case "survival":
						p.setGameMode(GameMode.SURVIVAL);
						break;
					case "1":
					case "c":
					case "creative":
					case "creatif":
						p.setGameMode(GameMode.CREATIVE);
						break;
					case "2":
					case "a":
					case "aventure":
					case "adventure":
						p.setGameMode(GameMode.ADVENTURE);
						break;
					case "3":
					case "spec":
					case "spectateur":
					case "spectator":
						p.setGameMode(GameMode.SPECTATOR);
						break;
					default:
						sender.sendMessage("Usage: /gm <mode> [player]");
				}
			}
			return false;
		}

}
