package fr.tiakin.main;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import fr.tiakin.chat.ChatClear;
import fr.tiakin.player.Death;
import fr.tiakin.player.Gm;
import fr.tiakin.player.Revive;
import fr.tiakin.player.Vanish;
import fr.tiakin.player.inventory.EnderSee;
import fr.tiakin.player.inventory.InvSee;
import fr.tiakin.player.life.Feed;
import fr.tiakin.player.life.Heal;
import fr.tiakin.chest.ChestLog;


public class main extends JavaPlugin implements Listener {
    
    private boolean deathModuleEnabled = getConfig().getBoolean("modules.death.enabled", false);
    private boolean chestlogModuleEnabled = getConfig().getBoolean("modules.chestlog.enabled", true);
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        Bukkit.getPluginManager().registerEvents(this, this);
        
        if(deathModuleEnabled) {
            Bukkit.getPluginManager().registerEvents(new Death(), this);
            getCommand("death").setExecutor(new Death());
            getCommand("revive").setExecutor(new Revive());
            Folder.create("death");
            getLogger().info("Module Death activé");
        } else {
            getLogger().info("Module Death désactivé");
        }
        
        getCommand("feed").setExecutor(new Feed());
        getCommand("heal").setExecutor(new Heal());
        getCommand("gm").setExecutor(new Gm());
        getCommand("vanish").setExecutor(new Vanish());
        getCommand("invsee").setExecutor(new InvSee());
        getCommand("chatclear").setExecutor(new ChatClear());
        getCommand("endersee").setExecutor(new EnderSee());
        
        // Module ChestLog
        if(chestlogModuleEnabled) {
            ChestLog chestLog = new ChestLog();
            Bukkit.getPluginManager().registerEvents(chestLog, this);
            getCommand(ChestLog.CHESTLOG_FILE).setExecutor(chestLog);
            Folder.create(ChestLog.CHESTLOG_FILE);
            getLogger().info("Module ChestLog activé");
        } else {
            getLogger().info("Module ChestLog désactivé");
        }
        
        Folder.create("vanish");
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        Map<String, Object> all = Folder.getValues("vanish");
        if(all != null) {
            for(Map.Entry<String, Object> entry : all.entrySet()) {
                if(entry.getValue().toString().equalsIgnoreCase("true")) {
                    e.getPlayer().hidePlayer(this, Bukkit.getPlayer(entry.getKey()));
                }
            }
        }
    }

    @EventHandler
    public void leave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        String status = Folder.get("vanish", p.getName());
        if(status == null) return;
        if(status.equalsIgnoreCase("true")) {
            Folder.set("vanish", p.getName(), "false");
            p.setCanPickupItems(true);
            e.setQuitMessage(null);
        }
    }
    
    @EventHandler
    public static void chat(AsyncPlayerChatEvent e) {
        if(e.getPlayer().isOp() && e.getMessage().contains(".&"))
            e.setMessage(e.getMessage().replace(".&", "§"));
    }
    
    @EventHandler
    public void redstoneChanges(BlockRedstoneEvent e) {
        Block block = e.getBlock();
        BlockState state = block.getState();
        
        if(state instanceof CommandBlock) {
            CommandBlock cb = (CommandBlock) state;
            if(cb.getCommand().contains("/&")) {
                cb.setCommand(cb.getCommand().replace("/&", "§"));
                cb.update();
            }
        }
    }
    
    public static Class<?> getNMSClass(String nmsClassString) throws ClassNotFoundException {
        String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
        String name = "net.minecraft.server." + version + nmsClassString;
        return Class.forName(name);
    }
    
    public boolean isDeathModuleEnabled() {
        return deathModuleEnabled;
    }
    
    public boolean isChestlogModuleEnabled() {
        return chestlogModuleEnabled;
    }
}
