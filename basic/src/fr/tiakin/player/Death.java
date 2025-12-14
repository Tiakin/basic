package fr.tiakin.player;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
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
import org.bukkit.plugin.java.JavaPlugin;

import fr.tiakin.main.IS;
import fr.tiakin.main.main;
import fr.tiakin.main.Folder;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;


public class Death implements CommandExecutor, Listener {
    SimpleDateFormat timestamp = new SimpleDateFormat("EEEE d MMMM yyyy '§' HH:mm:ss", new Locale("FR", "fr"));
    
    public Death() {
        timestamp.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
        if(sender.isOp()) {
            if(args.length == 0) {
                sender.sendMessage("/death log (pseudo)");
                sender.sendMessage("/death restore [pseudo] (date) (option)");
            } else {
                if(args[0].equalsIgnoreCase("log")) {
                    if(args.length == 2) {
                        String pseudo = args[1];
                        sender.sendMessage(ChatColor.of(new Color(20,20,20)) + "------- " + ChatColor.of(new Color(50,50,50)) + pseudo + ChatColor.of(new Color(20,20,20)) + " -------");
                        
                        Set<String> dates = Folder.getKeys("death", pseudo);
                        if(dates != null) {
                            for (String date : dates) {
                                TextComponent message = new TextComponent(ChatColor.DARK_GRAY + pseudo + ChatColor.GRAY + " est mort le " + ChatColor.DARK_GRAY + timestamp.format(Long.parseLong(date) * 100));
                                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7Clique pour §6regarder son inventaire §7!  §4" + date)));
                                message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/death view " + pseudo + " " + date));
                                sender.spigot().sendMessage(message);
                            }
                        }
                        sender.sendMessage(ChatColor.of(new Color(20,20,20)) + "---------------------");
                    } else {
                        Set<String> players = Folder.getKeys("death");
                        if(players != null) {
                            for (String pseudo : players) {
                                System.out.println(pseudo);
                                sender.sendMessage(ChatColor.of(new Color(20,20,20)) + "------- " + ChatColor.of(new Color(50,50,50)) + pseudo + ChatColor.of(new Color(20,20,20)) + " -------");
                                
                                Set<String> dates = Folder.getKeys("death", pseudo);
                                if(dates != null) {
                                    for (String date : dates) {
                                        TextComponent message = new TextComponent(ChatColor.DARK_GRAY + pseudo + ChatColor.GRAY + " est mort le " + ChatColor.DARK_GRAY + timestamp.format(Long.parseLong(date) * 100));
                                        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7Clique pour §6regarder son inventaire §7!")));
                                        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/death view " + pseudo + " " + date));
                                        sender.spigot().sendMessage(message);
                                    }
                                }
                                sender.sendMessage(ChatColor.of(new Color(20,20,20)) + "---------------------");
                            }
                        }
                    }
                } else if(args[0].equalsIgnoreCase("restore")) {
                    final Player p = Bukkit.getPlayer(args[1]);
                    
                    String gettime;
                    if(args.length == 3) {
                        gettime = args[2];
                    } else {
                        Set<String> keys = Folder.getKeys("death", p.getName());
                        gettime = keys.toArray()[keys.size() - 1].toString();
                    }
                    final String time = gettime;
                    
                    Bukkit.getServer().getScheduler().runTaskAsynchronously(JavaPlugin.getPlugin(main.class), new Runnable() {
                        @Override
                        public void run() {
                            ItemStack[] inv = new ItemStack[36];
                            ItemStack[] armor = new ItemStack[4];
                            
                            String xpStr = Folder.get("death", p.getName() + "." + time + ".xp");
                            int xp = Integer.parseInt(xpStr != null ? xpStr : "0");
                            
                            ConfigurationSection cs1 = Folder.getSection("death", p.getName() + "." + time + ".slot");
                            if(cs1 != null) {
                                for(int n = 0; n < 36; n++) {
                                    if(cs1.getString(n + "") != null) {
                                        inv[n] = IS.deserialize(cs1.getConfigurationSection("" + n).getValues(false));
                                    } else {
                                        inv[n] = null;
                                    }
                                }
                                p.getInventory().setStorageContents(inv);
                            }
                            
                            ConfigurationSection cs2 = Folder.getSection("death", p.getName() + "." + time + ".armorslot");
                            if(cs2 != null) {
                                for(int n = 0; n < 4; n++) {
                                    if(cs2.getString(n + "") != null) {
                                        armor[n] = IS.deserialize(cs2.getConfigurationSection("" + n).getValues(false));
                                    } else {
                                        armor[n] = null;
                                    }
                                }
                                p.getInventory().setArmorContents(armor);
                            }
                            
                            Map<String, Object> offhandData = Folder.getValues("death", p.getName() + "." + time + ".offslot");
                            if(offhandData != null) {
                                p.getInventory().setItemInOffHand(IS.deserialize(offhandData));
                            }
                            
                            p.setTotalExperience(xp);
                        }
                    });
                } else if(args[0].equalsIgnoreCase("view")) {
                    if(sender instanceof Player) {
                        Player p = (Player) sender;
                        String time = args[2];
                        Inventory inv = Bukkit.createInventory(null, 54, "§4view : §5" + args[1]);
                        
                        ConfigurationSection cs1 = Folder.getSection("death", args[1] + "." + time + ".slot");
                        if(cs1 != null) {
                            for(int n = 0; n < 36; n++) {
                                if(cs1.getString(n + "") != null) {
                                    inv.setItem(n, IS.deserialize(cs1.getConfigurationSection("" + n).getValues(false)));
                                }
                            }
                        }
                        
                        ConfigurationSection cs2 = Folder.getSection("death", args[1] + "." + time + ".armorslot");
                        if(cs2 != null) {
                            for(int n = 0; n < 4; n++) {
                                if(cs2.getString(n + "") != null) {
                                    inv.setItem(n + 45, IS.deserialize(cs2.getConfigurationSection("" + n).getValues(false)));
                                }
                            }
                        }
                        
                        Map<String, Object> offhandData = Folder.getValues("death", args[1] + "." + time + ".offslot");
                        if(offhandData != null) {
                            inv.setItem(40, IS.deserialize(offhandData));
                        }
                        
                        String cause = Folder.get("death", args[1] + "." + time + ".cause");
                        ItemStack is = new ItemStack(Material.ANVIL, 1);
                        ItemMeta im = is.getItemMeta();
                        im.setDisplayName(cause);
                        im.setLore(Arrays.asList(ChatColor.DARK_GRAY + args[1] + ChatColor.GRAY + " est mort le " + ChatColor.DARK_GRAY + timestamp.format(Long.parseLong(time) * 100)));
                        is.setItemMeta(im);
                        inv.setItem(50, is);
                        
                        String xpStr = Folder.get("death", args[1] + "." + time + ".xp");
                        is = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);
                        im = is.getItemMeta();
                        im.setDisplayName(xpStr);
                        is.setItemMeta(im);
                        inv.setItem(51, is);
                        
                        String location = Folder.get("death", args[1] + "." + time + ".location");
                        is = new ItemStack(Material.MINECART, 1);
                        im = is.getItemMeta();
                        im.setDisplayName(location);
                        is.setItemMeta(im);
                        inv.setItem(52, is);
                        
                        is = new ItemStack(Material.EMERALD_BLOCK, 1);
                        im = is.getItemMeta();
                        im.setDisplayName("§8Clique ici pour revive !");
                        im.setLore(Arrays.asList("death restore " + args[1] + " " + time));
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
                } else if(args[0].equalsIgnoreCase("multikill")) {
                    for(String player : args) {
                        if(!player.equalsIgnoreCase("multikill") && Bukkit.getPlayer(player) != null) {
                            Bukkit.getPlayer(player).setHealth(0);
                        }
                    }
                    Bukkit.broadcastMessage(ChatColor.of(new Color(92, 114, 207)) + "Plusieurs ");
                }
            }
        }
        return false;
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
        
        Bukkit.getServer().getScheduler().runTaskAsynchronously(JavaPlugin.getPlugin(main.class), new Runnable() {
            @Override
            public void run() {
                synchronized (sync) {
                    for(int n = 0; n < 36; n++) {
                        if(inv[n] != null) {
                            Folder.setSection("death", p.getName() + "." + time + ".slot." + n, IS.serialize(inv[n]));
                        }
                    }
                    
                    for(int n = 0; n < 4; n++) {
                        if(armor[n] != null) {
                            Folder.setSection("death", p.getName() + "." + time + ".armorslot." + n, IS.serialize(armor[n]));
                        }
                    }
                    
                    Folder.setSection("death", p.getName() + "." + time + ".offslot", IS.serialize(offhand));
                    Folder.set("death", p.getName() + "." + time + ".cause", e.getDeathMessage());
                    Folder.set("death", p.getName() + "." + time + ".xp", xp);
                    Folder.set("death", p.getName() + "." + time + ".location", loc.toString());
                }
            }
        });
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
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
