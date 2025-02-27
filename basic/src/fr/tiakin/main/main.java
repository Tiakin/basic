package fr.tiakin.main;

import java.io.File;
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

import fr.tiakin.chat.chatclear;
import fr.tiakin.player.death;
import fr.tiakin.player.gm;
import fr.tiakin.player.revive;
import fr.tiakin.player.vanish;
import fr.tiakin.player.inventory.enderchest;
import fr.tiakin.player.inventory.invsee;
import fr.tiakin.player.life.feed;
import fr.tiakin.player.life.heal;


public class main extends JavaPlugin implements Listener{
	public folder z;
	public void onEnable(){
		Bukkit.getPluginManager().registerEvents(this, this);
		Bukkit.getPluginManager().registerEvents(new death(this), this);
		 getCommand("feed").setExecutor(new feed());
		 getCommand("heal").setExecutor(new heal());
		 getCommand("gm").setExecutor(new gm());
		 getCommand("vanish").setExecutor(new vanish(this));
		 getCommand("invsee").setExecutor(new invsee());
		 getCommand("revive").setExecutor(new revive());
		 getCommand("chatclear").setExecutor(new chatclear());
		 getCommand("death").setExecutor(new death(this));
		 getCommand("enderchest").setExecutor(new enderchest());
		 
		 File vanishfile = new File("plugins/storage/vanish.yml");
	        if (!vanishfile.exists()){
	        	z = new folder("vanish", null);
	        	z.setfolder();
	        }
	     File deathfile = new File("plugins/storage/death.yml");
	        if (!deathfile.exists()){
	        	z = new folder("death", null);
	        	z.setfolder();
	        }
	}

	@EventHandler
	 public void join(PlayerJoinEvent e) {
		
		z = new folder("vanish");
		Map<String, Object> all = z.getValues();
		 for(String pseudo : all.keySet()) {
			 System.out.println(all.get(pseudo));
			 if(all.get(pseudo).toString().equalsIgnoreCase("true")) {
				 e.getPlayer().hidePlayer(this, Bukkit.getPlayer(pseudo));
			 }
		 }
	}

	
	@EventHandler
	 public void leave(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		z = new folder("vanish", p.getName(),"false");
		if(z.readfolder() == null) {
			
		}else if(z.readfolder().equalsIgnoreCase("true")) {
			z.editfolder();
			p.setCanPickupItems(true);
			
			e.setQuitMessage(null);
		}
	}
	
	@EventHandler
    public static void chat(AsyncPlayerChatEvent e){
    	if(e.getPlayer().isOp())
    		if(e.getMessage().contains(".&"))
    			e.setMessage(e.getMessage().replace(".&", "�"));
    }
	
    @EventHandler
    public void redstoneChanges(BlockRedstoneEvent e) {
        Block block = e.getBlock();
        BlockState state = block.getState();
        
        if(state instanceof CommandBlock) {
        	CommandBlock cb = (CommandBlock)state;
        	if(cb.getCommand().contains("/&")) {
        		cb.setCommand(cb.getCommand().replace("/&", "�"));
        		cb.update();
        		
        	}
        }
    }
    
    public static Class<?> getNMSClass(String nmsClassString) throws ClassNotFoundException {
        String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
        String name = "net.minecraft.server." + version + nmsClassString;
        Class<?> nmsClass = Class.forName(name);
        
        return nmsClass;
    }
    
}
