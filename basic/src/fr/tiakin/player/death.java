package fr.tiakin.player;



import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
	SimpleDateFormat timestamp = new SimpleDateFormat("EEEE d MMMM yyyy 'à' HH:mm:ss",new Locale("FR","fr"));
    {timestamp.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));}
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
		
        
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
							if(cs1 != null) {
								for(int n = 0; n < 36;) {
					    			if(cs1.getString(n+"") != null)
					    				inv[n] = IS.deserialize(cs1.getConfigurationSection(""+n).getValues(false));
					    			else
					    				inv[n] = null;
					    			n++;	
					    			}
								
					    		p.getInventory().setStorageContents(inv);
							}
				    		z = new folder("death", p.getName()+"."+time+".armorslot",true);
							ConfigurationSection cs2 = z.getConfiguration();
							if(cs2 != null) {
					    		for(int n = 0; n < 4;) {
					    			if(cs2.getString(n+"") != null)
					    				armor[n] = IS.deserialize(cs2.getConfigurationSection(""+n).getValues(false));
					    			else
					    				armor[n] = null;
							        n++;
							        }
					    		p.getInventory().setArmorContents(armor);
							}
				    		z = new folder("death", p.getName()+"."+time+".offslot");
							p.getInventory().setItemInOffHand(IS.deserialize(z.getValues()));
				    		
							p.setTotalExperience(xp);
							}
			            });
			}else if(args[0].equalsIgnoreCase("view")) {
				//go faire un truc
				if(sender instanceof Player) {
					Player p = (Player) sender;
					String time = args[2];
					Inventory inv = Bukkit.createInventory(null, 54, "§4view : §5"+args[1]);
					z = new folder("death", args[1]+"."+time+".slot");
					ConfigurationSection cs1 = z.getConfiguration();
					if(cs1 != null) {
						for(int n = 0; n < 36;) {
			    			if(cs1.getString(n+"") != null)
			    				inv.setItem(n, IS.deserialize(cs1.getConfigurationSection(""+n).getValues(false)));
			    			n++;	
			    			}
					}
					z = new folder("death", args[1]+"."+time+".armorslot");
					ConfigurationSection cs2 = z.getConfiguration();
					if(cs2 != null) {
			    		for(int n = 0; n < 4;) {
			    			if(cs2.getString(n+"") != null)
			    				inv.setItem(n+45, IS.deserialize(cs2.getConfigurationSection(""+n).getValues(false)));
					        n++;
					        }
					}
		    		z = new folder("death", args[1]+"."+time+".offslot");
		    		inv.setItem(40, IS.deserialize(z.getValues()));
		    		
		    		
		    		z = new folder("death", args[1]+"."+time+".cause");
		    		ItemStack is = new ItemStack(Material.ANVIL, 1);
		    		ItemMeta im = is.getItemMeta();
		    		im.setDisplayName(z.readfolder());
		    		im.setLore(Arrays.asList(ChatColor.DARK_GRAY+args[1]+ChatColor.GRAY+" est mort le "+ChatColor.DARK_GRAY+timestamp.format(Long.parseLong(time)*100)));
		    		is.setItemMeta(im);
		    		inv.setItem(50, is);
		    		
		    		z = new folder("death", args[1]+"."+time+".xp");
		    		is = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);
		    		im = is.getItemMeta();
		    		im.setDisplayName(z.readfolder());
		    		is.setItemMeta(im);
		    		inv.setItem(51, is);
		    		
		    		z = new folder("death", args[1]+"."+time+".location");
		    		is = new ItemStack(Material.MINECART, 1);
		    		im = is.getItemMeta();
		    		im.setDisplayName(z.readfolder());
		    		is.setItemMeta(im);
		    		inv.setItem(52, is);
		    		
		    		is = new ItemStack(Material.EMERALD_BLOCK, 1);
		    		im = is.getItemMeta();
		    		im.setDisplayName("§8Clique ici pour revive !");
		    		im.setLore(Arrays.asList("death restore "+args[1]+" "+time));
		    		is.setItemMeta(im);
		    		inv.setItem(53, is);
		    		
		    		is = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
		    		im = is.getItemMeta();
		    		im.setDisplayName(" ");
		    		is.setItemMeta(im);
		    		inv.setItem(36, is);
		    		inv.setItem(37, is);
		    		inv.setItem(38, is);
		    		inv.setItem(39, is);
		    		inv.setItem(41, is);
		    		inv.setItem(42, is);
		    		inv.setItem(43, is);
		    		inv.setItem(44, is);
		    		inv.setItem(49, is);
		    		p.openInventory(inv);
				}
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
	
	Object sync = true;
	@EventHandler
	 public void ondeath(PlayerDeathEvent e) {
		final Player p = e.getEntity();
		final ItemStack[] inv = p.getInventory().getStorageContents();
        final ItemStack[] armor = p.getInventory().getArmorContents();
        final ItemStack offhand = p.getInventory().getItemInOffHand();
        
        final Location loc = p.getLocation();
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
	        		
	        		z = new folder("death", p.getName()+"."+time+".location", loc.toString());
	        		z.addinfolder();
	            }
            }
        });
	}
	
	 @EventHandler 
	  public void onInventoryClick(InventoryClickEvent e)
	 {
		 ItemStack clicked = e.getCurrentItem();
		 InventoryView name = e.getView();
		 if (name.getTitle().startsWith("§4view : §5")) {
			 if (clicked != null && clicked.getType() == Material.EMERALD_BLOCK) {
				 Bukkit.dispatchCommand(Bukkit.getConsoleSender(), clicked.getItemMeta().getLore().get(0));
			 }
			 e.setCancelled(true);
		 }
	 }
}
