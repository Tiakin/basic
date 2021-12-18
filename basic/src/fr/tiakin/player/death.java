package fr.tiakin.player;



import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import fr.tiakin.main.IS;
import fr.tiakin.main.folder;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;


public class death implements CommandExecutor,Listener{
	public folder z;
	Plugin main;
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
		SimpleDateFormat timestamp = new SimpleDateFormat("EEEE d MMMM yyyy 'à' HH:mm:ss",new Locale("FR","fr"));
        timestamp.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        
		if(sender.isOp()) {
			if(args.length == 0) {
				sender.sendMessage("/death log (pseudo)");
				sender.sendMessage("/death restore [pseudo] (date) (option)");
			}else {
				if(args[0].equalsIgnoreCase("log")) {
					if(args.length == 2) {
						String pseudo = args[1];
						sender.sendMessage(ChatColor.of(new Color(20,20,20))+"------- "+ChatColor.of(new Color(50,50,50))+pseudo+ChatColor.of(new Color(20,20,20))+" -------");
						z = new folder("death", pseudo);
						for (String date : z.getkey()) {
							
					        TextComponent message = new TextComponent(ChatColor.DARK_GRAY+pseudo+ChatColor.GRAY+" est mort le "+ChatColor.DARK_GRAY+timestamp.format(Long.parseLong(date)*100));
					        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new Text("§7Clique pour §6regarder son inventaire §7!  §4"+date)));
					        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/death view "+pseudo+" "+ date));
					        sender.spigot().sendMessage(message);
							
						}
						sender.sendMessage(ChatColor.of(new Color(20,20,20))+"---------------------");
					}else {
						z = new folder("death", null);
						for (String pseudo : z.getkey()) {
						System.out.println(pseudo);
						sender.sendMessage(ChatColor.of(new Color(20,20,20))+"------- "+ChatColor.of(new Color(50,50,50))+pseudo+ChatColor.of(new Color(20,20,20))+" -------");
						z = new folder("death", pseudo);
						for (String date : z.getkey()) {
							
							
					        TextComponent message = new TextComponent(ChatColor.DARK_GRAY+pseudo+ChatColor.GRAY+" est mort le "+ChatColor.DARK_GRAY+timestamp.format(Long.parseLong(date)*100));
					        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new Text("§7Clique pour §6regarder son inventaire §7!")));
					        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/death view "+pseudo+" "+ date));
					        sender.spigot().sendMessage(message);
							
						}
						sender.sendMessage(ChatColor.of(new Color(20,20,20))+"---------------------");
						}
					}
				}else if(args[0].equalsIgnoreCase("restore")) {
					final Player p = Bukkit.getPlayer(args[1]);
					
					String gettime;
					if(args.length == 3) {
						gettime = args[2];
					}else {
						z = new folder("death", p.getName());
						Set<String> keys = z.getkey();
						gettime = keys.toArray()[keys.size()-1].toString();
					}
					final String time = gettime;
					Bukkit.getServer().getScheduler().runTaskAsynchronously(main,new Runnable() {
			            @Override
			            public void run() {
			            	ItemStack[] inv = new ItemStack[36];
							ItemStack[] armor = new ItemStack[4];
							z = new folder("death", p.getName()+"."+time+".xp", "null");
							int xp = Integer.parseInt(z.readfolder());
							
							z = new folder("death", p.getName()+"."+time+".slot",true);
							ConfigurationSection cs1 = z.getConfiguration();
							for(int n = 0; n < 36;) {
				    			if(cs1.getString(n+"") != null)
				    				inv[n] = IS.deserialize(cs1.getConfigurationSection(""+n).getValues(false));
				    			else
				    				inv[n] = null;
				    			n++;	
				    			}
				    		p.getInventory().setStorageContents(inv);
				    		
				    		z = new folder("death", p.getName()+"."+time+".armorslot",true);
							ConfigurationSection cs2 = z.getConfiguration();
				    		for(int n = 0; n < 4;) {
				    			if(cs2.getString(n+"") != null)
				    				armor[n] = IS.deserialize(cs2.getConfigurationSection(""+n).getValues(false));
				    			else
				    				armor[n] = null;
						        n++;
						        }
				    		p.getInventory().setArmorContents(armor);
				    		
				    		z = new folder("death", p.getName()+"."+time+".offslot");
							p.getInventory().setItemInOffHand(IS.deserialize(z.getValues()));
				    		
							p.setTotalExperience(xp);
							}
			            });
			}else if(args[0].equalsIgnoreCase("view")) {
				
			}else if(args[0].equalsIgnoreCase("multikill")) {
				for(String player : args) {
					if(!player.equalsIgnoreCase("multikill") && Bukkit.getPlayer(player) != null) {
						Bukkit.getPlayer(player).setHealth(0);
					}
				}
				Bukkit.broadcastMessage(ChatColor.of(new Color(92,114,207))+"Plusieurs ");
			}else {
				sender.sendMessage("euh pas capiche");
			}
		}
		}
		return false;
	}
	
	public death(Plugin main) {
		this.main = main;
	}
	public death() {

	}
	Object sync;
	@EventHandler
	 public void ondeath(PlayerDeathEvent e) {
		final Player p = e.getEntity();
		final ItemStack[] inv = p.getInventory().getStorageContents();
        final ItemStack[] armor = p.getInventory().getArmorContents();
        final ItemStack offhand = p.getInventory().getItemInOffHand();
        
        final String xp = Integer.toString(p.getTotalExperience());
        final String time = Long.toString(System.currentTimeMillis() / 100L);
		Bukkit.getServer().getScheduler().runTaskAsynchronously(main,new Runnable() {
            @Override
            public  void run() {
            	synchronized (sync) {
	        		for(int n = 0; n < 36;) {
	        			if(inv[n] != null) {
	        				z = new folder("death", p.getName()+"."+time+".slot."+n, IS.serialize(inv[n]));
	        				z.editfoldermap();
	        			}
	        			n++;
	        		}
	        		
	        		for(int n = 0; n < 4;) {
	        			if(armor[n] != null) {
	        				z = new folder("death", p.getName()+"."+time+".armorslot."+n, IS.serialize(armor[n]));
	        				z.editfoldermap();  
	        			}
	        			n++;
	        	    }
	        		    
	        		
	        			z = new folder("death", p.getName()+"."+time+".offslot", IS.serialize(offhand));
	        		    z.editfoldermap();
	        		
	        		z = new folder("death", p.getName()+"."+time+".cause", e.getDeathMessage());
	        		z.addinfolder();
	        		
	        		z = new folder("death", p.getName()+"."+time+".xp", xp);
	        		z.addinfolder();
	        		
	        		z = new folder("death", p.getName()+"."+time+".location", p.getLocation().toString());
	        		z.addinfolder();
	            }
            }
        });
	}
}
