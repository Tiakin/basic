package fr.tiakin.player;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import fr.tiakin.main.Folder;
import fr.tiakin.main.main;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;


public class Vanish implements CommandExecutor {
        
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
        if(sender.isOp()) {
            Player e = null;
            if(args.length == 0) {
                e = Bukkit.getPlayer(sender.getName());
            } else if(args.length == 1) {
                e = Bukkit.getPlayer(args[0]);
            }
            
            String vanishStatus = Folder.get("vanish", e.getName());
            
            if(vanishStatus == null) {
                Folder.set("vanish", e.getName(), "false");
                vanishStatus = "false";
            }
            
            if(vanishStatus.equalsIgnoreCase("false")) {
                for(Player p : Bukkit.getOnlinePlayers()) {
                    if(!p.getName().equalsIgnoreCase(e.getName())) {
                        p.hidePlayer(JavaPlugin.getPlugin(main.class), e);
                    }
                }
                Folder.set("vanish", e.getName(), "true");
                e.setCanPickupItems(false);
                
                TranslatableComponent message = new TranslatableComponent("multiplayer.player.left");
                message.addWith(e.getDisplayName());
                message.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                Bukkit.spigot().broadcast(message);
            } else {
                for(Player p : Bukkit.getOnlinePlayers()) {
                    if(!p.getName().equalsIgnoreCase(e.getName())) {
                        p.showPlayer(JavaPlugin.getPlugin(main.class), e);
                    }
                }
                Folder.set("vanish", e.getName(), "false");
                e.setCanPickupItems(true);
                
                TranslatableComponent message = new TranslatableComponent("multiplayer.player.joined");
                message.addWith(e.getDisplayName());
                message.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                Bukkit.spigot().broadcast(message);
            }
        }
        return false;
    }
}
