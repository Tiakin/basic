package fr.tiakin.chest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Barrel;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import fr.tiakin.main.Folder;
import fr.tiakin.main.IS;
import net.md_5.bungee.api.ChatColor;

public class ChestLog implements CommandExecutor, Listener {
    
    public static final String CHESTLOG_FILE = "chestlog";
    private Map<String, ItemStack[]> previousInventories = new HashMap<>();
    private SimpleDateFormat timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", new Locale("FR", "fr"));
    
    public ChestLog() {
        timestamp.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player)) return;
        
        Inventory inv = e.getClickedInventory();
        if(inv == null) return;
        
        InventoryHolder holder = inv.getHolder();
        if(holder instanceof Chest || holder instanceof DoubleChest || holder instanceof Barrel || holder instanceof ShulkerBox) {
            String playerName = e.getWhoClicked().getName();
            String chestKey = getChestKey(holder);
            
            // Sauvegarde l'état actuel avant le clic
            if(!previousInventories.containsKey(playerName + "_" + chestKey)) {
                previousInventories.put(playerName + "_" + chestKey, inv.getContents().clone());
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if(!(e.getPlayer() instanceof Player)) return;
        
        Player player = (Player) e.getPlayer();
        Inventory inv = e.getInventory();
        InventoryHolder holder = inv.getHolder();
        
        if(holder instanceof Chest || holder instanceof DoubleChest || holder instanceof Barrel || holder instanceof ShulkerBox) {
            String playerName = player.getName();
            String chestKey = getChestKey(holder);
            String mapKey = playerName + "_" + chestKey;
            
            if(previousInventories.containsKey(mapKey)) {
                ItemStack[] before = previousInventories.get(mapKey);
                ItemStack[] after = inv.getContents();
                
                // Compare et log les changements
                logChanges(player, chestKey, before, after);
                
                previousInventories.remove(mapKey);
            }
        }
    }
    
    private String getChestKey(InventoryHolder holder) {
        Location loc;
        
        if(holder instanceof DoubleChest) {
            DoubleChest dc = (DoubleChest) holder;
            loc = dc.getLocation();
        } else if(holder instanceof Chest) {
            Chest chest = (Chest) holder;
            loc = chest.getLocation();
        } else if(holder instanceof Barrel) {
            Barrel barrel = (Barrel) holder;
            loc = barrel.getLocation();
        } else if(holder instanceof ShulkerBox) {
            ShulkerBox shulker = (ShulkerBox) holder;
            loc = shulker.getLocation();
        } else {
            return null;
        }
        
        return loc.getWorld().getName() + "_" + 
               loc.getBlockX() + "_" + 
               loc.getBlockY() + "_" + 
               loc.getBlockZ();
    }
    
    private void logChanges(Player player, String chestKey, ItemStack[] before, ItemStack[] after) {
        String time = String.valueOf(System.currentTimeMillis());
        
        for(int i = 0; i < before.length && i < after.length; i++) {
            ItemStack beforeItem = before[i];
            ItemStack afterItem = after[i];
            
            // Retrait
            if(beforeItem != null && (afterItem == null || afterItem.getAmount() < beforeItem.getAmount())) {
                int amount = beforeItem.getAmount() - (afterItem != null ? afterItem.getAmount() : 0);
                ItemStack removed = beforeItem.clone();
                removed.setAmount(amount);
                
                logAction(player.getName(), chestKey, time, "RETRAIT", removed, i);
            }
            
            // Dépôt
            if(afterItem != null && (beforeItem == null || afterItem.getAmount() > beforeItem.getAmount())) {
                int amount = afterItem.getAmount() - (beforeItem != null ? beforeItem.getAmount() : 0);
                ItemStack added = afterItem.clone();
                added.setAmount(amount);
                
                logAction(player.getName(), chestKey, time, "DEPOT", added, i);
            }
        }
    }
    
    private void logAction(String playerName, String chestKey, String time, String action, ItemStack item, int slot) {
        String logKey = CHESTLOG_FILE + "." + chestKey + "." + time + "." + System.nanoTime();
        
        Folder.set(CHESTLOG_FILE, logKey + ".player", playerName);
        Folder.set(CHESTLOG_FILE, logKey + ".action", action);
        Folder.set(CHESTLOG_FILE, logKey + ".slot", String.valueOf(slot));
        Folder.set(CHESTLOG_FILE, logKey + ".timestamp", timestamp.format(new Date()));
        Folder.setSection(CHESTLOG_FILE, logKey + ".item", IS.serialize(item));
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
        if(!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
            return false;
        }
        
        if(args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/chestlog view [<x> <y> <z> [monde]]");
            sender.sendMessage(ChatColor.YELLOW + "/chestlog clear [<x> <y> <z> [monde]]");
            return true;
        }
        
        if(args[0].equalsIgnoreCase("view")) {
            String chestKey = null;
            
            if(args.length >= 4) {
                String world;
                if(args.length >= 5) {
                    world = args[4];
                } else if(sender instanceof Player) {
                    world = ((Player)sender).getWorld().getName();
                } else {
                    world = "world";
                }
                chestKey = world + "_" + args[1] + "_" + args[2] + "_" + args[3];
            } else if(args.length == 1) {
                if(!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Vous devez spécifier les coordonnées depuis la console.");
                    return false;
                }
                
                Player player = (Player) sender;
                Block targetBlock = player.getTargetBlock(null, 5);
                
                if(targetBlock == null || !isContainer(targetBlock.getType())) {
                    sender.sendMessage(ChatColor.RED + "Vous devez regarder un conteneur (coffre, barrel, shulker box) !");
                    return false;
                }
                
                Location loc = targetBlock.getLocation();
                chestKey = loc.getWorld().getName() + "_" + 
                          loc.getBlockX() + "_" + 
                          loc.getBlockY() + "_" + 
                          loc.getBlockZ();
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /chestlog view [<x> <y> <z> [monde]]");
                return false;
            }
            
            sender.sendMessage(ChatColor.GOLD + "=== Logs du coffre " + chestKey + " ===");
            
            Set<String> logs = Folder.getKeys(CHESTLOG_FILE, "chestlog." + chestKey);
            if(logs == null || logs.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "Aucun log trouvé pour ce coffre.");
                return true;
            }
            
            for(String logTime : logs) {
                String basePath = "chestlog." + chestKey + "." + logTime;
                String player = Folder.get(CHESTLOG_FILE, basePath + ".player");
                String action = Folder.get(CHESTLOG_FILE, basePath + ".action");
                String time = Folder.get(CHESTLOG_FILE, basePath + ".timestamp");
                String slot = Folder.get(CHESTLOG_FILE, basePath + ".slot");
                
                Map<String, Object> itemData = Folder.getValues(CHESTLOG_FILE, basePath + ".item");
                ItemStack item = itemData != null ? IS.deserialize(itemData) : null;
                
                String itemName = item != null ? item.getType() + " x" + item.getAmount() : "Unknown";
                ChatColor color = action.equals("DEPOT") ? ChatColor.GREEN : ChatColor.RED;
                
                sender.sendMessage(color + "[" + time + "] " + player + " - " + action + " - " + itemName + " (slot " + slot + ")");
            }
            
            return true;
        }
        
        if(args[0].equalsIgnoreCase("clear")) {
            String chestKey = null;
            
            if(args.length >= 4) {
                String world;
                if(args.length >= 5) {
                    world = args[4];
                } else if(sender instanceof Player) {
                    world = ((Player)sender).getWorld().getName();
                } else {
                    world = "world";
                }
                chestKey = world + "_" + args[1] + "_" + args[2] + "_" + args[3];
            } else if(args.length == 1) {
                if(!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Vous devez spécifier les coordonnées depuis la console.");
                    return false;
                }
                
                Player player = (Player) sender;
                Block targetBlock = player.getTargetBlock(null, 5);
                
                if(targetBlock == null || !isContainer(targetBlock.getType())) {
                    sender.sendMessage(ChatColor.RED + "Vous devez regarder un conteneur (coffre, barrel, shulker box) !");
                    return false;
                }
                
                Location loc = targetBlock.getLocation();
                chestKey = loc.getWorld().getName() + "_" + 
                          loc.getBlockX() + "_" + 
                          loc.getBlockY() + "_" + 
                          loc.getBlockZ();
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /chestlog clear [<x> <y> <z> [monde]]");
                return false;
            }
            
            Folder.delete(CHESTLOG_FILE, "chestlog." + chestKey);
            sender.sendMessage(ChatColor.GREEN + "Logs du coffre " + chestKey + " supprimés avec succès.");
            
            return true;
        }
        
        return false;
    }
    
    private boolean isContainer(Material material) {
        return material == Material.CHEST || 
               material == Material.TRAPPED_CHEST || 
               material == Material.BARREL ||
               material.name().contains("SHULKER_BOX");
        }
}
