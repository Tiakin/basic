package fr.tiakin.chat;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ChatClear implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
		if(sender.isOp()) {
			for(int i=0; i<200; i++)
            {
                Bukkit.broadcastMessage("");
            }
		}
		return false;
	}

}
