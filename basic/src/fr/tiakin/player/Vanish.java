package fr.tiakin.player;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import fr.tiakin.main.main;
import fr.tiakin.sql.VanishStorage;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TranslatableComponent;

public class Vanish implements CommandExecutor {

    public static void initTable() {
        VanishStorage.init();
    }

    public static Set<UUID> activeVanished() {
        return VanishStorage.activeVanished();
    }

    public static boolean isVanished(UUID uuid) {
        return VanishStorage.isVanished(uuid.toString());
    }

    public static void setVanished(UUID uuid, String name, boolean status) {
        VanishStorage.setVanished(uuid.toString(), name, status);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
        if(!sender.isOp()) return false;

        Player target = null;
        if(args.length == 0) {
            target = Bukkit.getPlayer(sender.getName());
        } else if(args.length == 1) {
            target = Bukkit.getPlayer(args[0]);
        }
        if (target == null) return false;

        String uuid = target.getUniqueId().toString();
        boolean vanished = VanishStorage.isVanished(uuid);

        if(!vanished) {
            for(Player p : Bukkit.getOnlinePlayers()) {
                if(!p.getUniqueId().equals(target.getUniqueId())) {
                    p.hidePlayer(JavaPlugin.getPlugin(main.class), target);
                }
            }
            VanishStorage.setVanished(uuid, target.getName(), true);
            target.setCanPickupItems(false);

            TranslatableComponent message = new TranslatableComponent("multiplayer.player.left");
            message.addWith(target.getDisplayName());
            message.setColor(ChatColor.YELLOW);
            Bukkit.spigot().broadcast(message);
        } else {
            for(Player p : Bukkit.getOnlinePlayers()) {
                if(!p.getUniqueId().equals(target.getUniqueId())) {
                    p.showPlayer(JavaPlugin.getPlugin(main.class), target);
                }
            }
            VanishStorage.setVanished(uuid, target.getName(), false);
            target.setCanPickupItems(true);

            TranslatableComponent message = new TranslatableComponent("multiplayer.player.joined");
            message.addWith(target.getDisplayName());
            message.setColor(ChatColor.YELLOW);
            Bukkit.spigot().broadcast(message);
        }
        return true;
    }

}
