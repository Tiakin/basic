package fr.tiakin.chest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.bukkit.Bukkit;
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
        pruneOlderThanDays(7);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player)) return;
        
        // On s'intéresse toujours à l'inventaire du haut (coffre/barrel/shulker),
        // même si le clic se fait dans l'inventaire du joueur (shift-click).
        Inventory topInv = e.getView().getTopInventory();
        if(topInv == null) return;
        InventoryHolder holder = topInv.getHolder();
        if(holder instanceof Chest || holder instanceof DoubleChest || holder instanceof Barrel || holder instanceof ShulkerBox) {
            String playerName = e.getWhoClicked().getName();
            String chestKey = getChestKey(holder);
            
            // Sauvegarde l'état actuel avant le premier clic
            if(!previousInventories.containsKey(playerName + "_" + chestKey)) {
                previousInventories.put(playerName + "_" + chestKey, cloneContents(topInv));
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
                ItemStack[] after = cloneContents(inv);
                
                // Compare et log les changements
                logChanges(player, chestKey, before, after);
                
                previousInventories.remove(mapKey);
            }
        }
    }

    // Deep clone to avoid shared ItemStack references
    private ItemStack[] cloneContents(Inventory inv) {
        ItemStack[] src = inv.getContents();
        ItemStack[] copy = new ItemStack[src.length];
        for (int i = 0; i < src.length; i++) {
            if (src[i] != null) {
                copy[i] = src[i].clone();
            }
        }
        return copy;
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
        
        return sanitize(loc.getWorld().getName()) + "_" + 
               loc.getBlockX() + "_" + 
               loc.getBlockY() + "_" + 
               loc.getBlockZ();
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace(' ', '_').replace('.', '_');
    }
    
    private void logChanges(Player player, String chestKey, ItemStack[] before, ItemStack[] after) {
        String time = String.valueOf(System.currentTimeMillis());

        List<Change> removals = new ArrayList<>();
        List<Change> deposits = new ArrayList<>();

        int len = Math.min(before.length, after.length);
        for(int i = 0; i < len; i++) {
            ItemStack beforeItem = before[i];
            ItemStack afterItem = after[i];
            
            boolean beforeEmpty = beforeItem == null || beforeItem.getType().isAir();
            boolean afterEmpty = afterItem == null || afterItem.getType().isAir();
            
            if(beforeEmpty && afterEmpty) {
                continue;
            }
            // Retrait
            if(!beforeEmpty && afterEmpty) {
                ItemStack removed = beforeItem.clone();
                removals.add(new Change(i, removed));
            }
            // Dépôt
            else if(beforeEmpty && !afterEmpty) {
                ItemStack added = afterItem.clone();
                deposits.add(new Change(i, added));
            }
            // Modification
            else if(!beforeEmpty && !afterEmpty) {
                if(!beforeItem.getType().equals(afterItem.getType())) {
                    // Changement de type : log SWITCH (un seul event)
                    logSwitchAction(player.getName(), chestKey, time, beforeItem.clone(), afterItem.clone(), i);
                }
                else if(afterItem.getAmount() < beforeItem.getAmount()) {
                    int amount = beforeItem.getAmount() - afterItem.getAmount();
                    ItemStack removed = beforeItem.clone();
                    removed.setAmount(amount);
                    removals.add(new Change(i, removed));
                }
                else if(afterItem.getAmount() > beforeItem.getAmount()) {
                    int amount = afterItem.getAmount() - beforeItem.getAmount();
                    ItemStack added = afterItem.clone();
                    added.setAmount(amount);
                    deposits.add(new Change(i, added));
                }
            }
        }

        // Détection de déplacement : match retrait et dépôt même type/amount dans le même tick
        boolean[] remUsed = new boolean[removals.size()];
        boolean[] depUsed = new boolean[deposits.size()];

        for(int r = 0; r < removals.size(); r++) {
            Change rem = removals.get(r);
            for(int d = 0; d < deposits.size(); d++) {
                if(depUsed[d]) continue;
                Change dep = deposits.get(d);
                if(rem.item.isSimilar(dep.item) && rem.item.getAmount() == dep.item.getAmount()) {
                    logMoveAction(player.getName(), chestKey, time, rem.item, rem.slot, dep.slot);
                    remUsed[r] = true;
                    depUsed[d] = true;
                    break;
                }
            }
        }

        // Reste : retraits et dépôts non appariés
        for(int r = 0; r < removals.size(); r++) {
            if(remUsed[r]) continue;
            Change rem = removals.get(r);
            logAction(player.getName(), chestKey, time, "RETRAIT", rem.item, rem.slot);
        }
        for(int d = 0; d < deposits.size(); d++) {
            if(depUsed[d]) continue;
            Change dep = deposits.get(d);
            logAction(player.getName(), chestKey, time, "DEPOT", dep.item, dep.slot);
        }
    }
    
    private void logAction(String playerName, String chestKey, String time, String action, ItemStack item, int slot) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        
        String logKey = CHESTLOG_FILE + "." + chestKey + "." + time + "." + slot;
        
        Folder.set(CHESTLOG_FILE, logKey + ".player", playerName);
        Folder.set(CHESTLOG_FILE, logKey + ".action", action);
        Folder.set(CHESTLOG_FILE, logKey + ".timestamp", timestamp.format(new Date()));
        Folder.setSection(CHESTLOG_FILE, logKey + ".item", IS.serialize(item));
    }

    private void logMoveAction(String playerName, String chestKey, String time, ItemStack item, int fromSlot, int toSlot) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        String logKey = CHESTLOG_FILE + "." + chestKey + "." + time + "." + fromSlot;

        Folder.set(CHESTLOG_FILE, logKey + ".player", playerName);
        Folder.set(CHESTLOG_FILE, logKey + ".action", "MOVE");
        Folder.set(CHESTLOG_FILE, logKey + ".timestamp", timestamp.format(new Date()));
        Folder.set(CHESTLOG_FILE, logKey + ".toSlot", String.valueOf(toSlot));
        Folder.setSection(CHESTLOG_FILE, logKey + ".item", IS.serialize(item));
    }

    private void logSwitchAction(String playerName, String chestKey, String time, ItemStack fromItem, ItemStack toItem, int slot) {
        if (fromItem == null || fromItem.getType().isAir()) return;
        if (toItem == null || toItem.getType().isAir()) return;

        String logKey = CHESTLOG_FILE + "." + chestKey + "." + time + "." + slot;

        Folder.set(CHESTLOG_FILE, logKey + ".player", playerName);
        Folder.set(CHESTLOG_FILE, logKey + ".action", "SWITCH");
        Folder.set(CHESTLOG_FILE, logKey + ".timestamp", timestamp.format(new Date()));
        Folder.setSection(CHESTLOG_FILE, logKey + ".from", IS.serialize(fromItem));
        Folder.setSection(CHESTLOG_FILE, logKey + ".to", IS.serialize(toItem));
    }

    private static class Change {
        int slot;
        ItemStack item;
        Change(int slot, ItemStack item) {
            this.slot = slot;
            this.item = item;
        }
    }

    private static class LogEntry {
        String time;
        String slot;
        LogEntry(String time, String slot) {
            this.time = time;
            this.slot = slot;
        }
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
            int page = 1;
            Integer x = null, y = null, z = null;
            String worldArg = null;

            int argIdx = 1;
            // Coords optionnelles
            if (args.length - argIdx >= 3 && canParseInt(args[argIdx]) && canParseInt(args[argIdx+1]) && canParseInt(args[argIdx+2])) {
                x = Integer.parseInt(args[argIdx]);
                y = Integer.parseInt(args[argIdx+1]);
                z = Integer.parseInt(args[argIdx+2]);
                argIdx += 3;
            }
            // Page optionnelle
            if (args.length > argIdx && canParseInt(args[argIdx])) {
                page = Math.max(1, Integer.parseInt(args[argIdx]));
                argIdx++;
            }
            // Monde optionnel (reste des args)
            if (args.length > argIdx) {
                StringBuilder sb = new StringBuilder();
                for (int i = argIdx; i < args.length; i++) {
                    if (i > argIdx) sb.append(' ');
                    sb.append(args[i]);
                }
                worldArg = sb.toString();
            }

            if(x != null && y != null && z != null) {
                String worldName;
                if(worldArg != null) {
                    worldName = worldArg;
                } else if(sender instanceof Player) {
                    worldName = ((Player)sender).getWorld().getName();
                } else {
                    worldName = Bukkit.getWorlds().get(0).getWorldFolder().getName();
                }
                chestKey = sanitize(worldName) + "_" + x + "_" + y + "_" + z;
            } else if(args.length >= 1) {
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
                chestKey = sanitize(loc.getWorld().getName()) + "_" + 
                          loc.getBlockX() + "_" + 
                          loc.getBlockY() + "_" + 
                          loc.getBlockZ();
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /chestlog view [<x> <y> <z>] [page] [monde...]");
                return false;
            }

            Set<String> logs = Folder.getKeys(CHESTLOG_FILE, "chestlog." + chestKey);
            if(logs == null || logs.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "Aucun log trouvé pour ce coffre.");
                return true;
            }

            // Tri des timestamps décroissant (plus récent en premier)
            List<String> sortedTimes = new ArrayList<>(logs);
            sortedTimes.sort((a, b) -> Long.compare(parseLongSafe(b), parseLongSafe(a)));

            List<LogEntry> entries = new ArrayList<>();
            for(String logTime : sortedTimes) {
                Set<String> slots = Folder.getKeys(CHESTLOG_FILE, "chestlog." + chestKey + "." + logTime);
                if (slots == null || slots.isEmpty()) continue;
                for (String slotKey : slots) {
                    entries.add(new LogEntry(logTime, slotKey));
                }
            }

            if(entries.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "Aucun log trouvé pour ce coffre.");
                return true;
            }

            int pageSize = 10;
            int totalPages = (int) Math.ceil(entries.size() / (double) pageSize);
            page = Math.min(page, totalPages);
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, entries.size());

            sender.sendMessage(ChatColor.GOLD + "=== Logs du coffre " + chestKey + " (page " + page + "/" + totalPages + ") ===");

            for (int idx = start; idx < end; idx++) {
                LogEntry entry = entries.get(idx);
                String basePath = "chestlog." + chestKey + "." + entry.time + "." + entry.slot;
                String player = Folder.get(CHESTLOG_FILE, basePath + ".player");
                String action = Folder.get(CHESTLOG_FILE, basePath + ".action");
                String time = Folder.get(CHESTLOG_FILE, basePath + ".timestamp");
                String toSlot = Folder.get(CHESTLOG_FILE, basePath + ".toSlot");

                if (action == null) action = "UNKNOWN";
                if (player == null) player = "?";
                if (time == null) time = "?";
                
                Map<String, Object> itemData = Folder.getValues(CHESTLOG_FILE, basePath + ".item");
                ItemStack item = itemData != null ? IS.deserialize(itemData) : null;
                Map<String, Object> fromData = Folder.getValues(CHESTLOG_FILE, basePath + ".from");
                Map<String, Object> toData = Folder.getValues(CHESTLOG_FILE, basePath + ".to");
                ItemStack fromItem = fromData != null && !fromData.isEmpty() ? IS.deserialize(fromData) : null;
                ItemStack toItem = toData != null && !toData.isEmpty() ? IS.deserialize(toData) : null;

                String itemName;
                if (action.equalsIgnoreCase("SWITCH") && fromItem != null && toItem != null) {
                    itemName = fromItem.getType() + " x" + fromItem.getAmount() + " -> " + toItem.getType() + " x" + toItem.getAmount();
                } else {
                    itemName = item != null ? item.getType() + " x" + item.getAmount() : "Unknown";
                }

                ChatColor color;
                if (action.equalsIgnoreCase("MOVE")) {
                    color = ChatColor.AQUA;
                } else if (action.equalsIgnoreCase("SWITCH")) {
                    color = ChatColor.YELLOW;
                } else if (action.equalsIgnoreCase("DEPOT")) {
                    color = ChatColor.GREEN;
                } else if (action.equalsIgnoreCase("RETRAIT")) {
                    color = ChatColor.RED;
                } else {
                    color = ChatColor.GRAY;
                }

                String actionLabel = action;
                if (action.equalsIgnoreCase("MOVE") && toSlot != null) {
                    actionLabel = "MOVE " + entry.slot + " -> " + toSlot;
                } else if (action.equalsIgnoreCase("SWITCH")) {
                    actionLabel = "SWITCH slot " + entry.slot;
                }

                sender.sendMessage(color + "[" + time + "] " + player + " - " + actionLabel + " - " + itemName + " (slot " + entry.slot + ")");
            }
            
            sender.sendMessage(ChatColor.GRAY + "Utilise /chestlog view ... <page> [monde] pour naviguer.");
            return true;
        }
        
        if(args[0].equalsIgnoreCase("clear")) {
            String chestKey = null;
            
            if(args.length >= 4) {
                String world;
                if(args.length >= 5) {
                    // Recompose le nom du monde qui peut contenir des espaces
                    StringBuilder sb = new StringBuilder();
                    for (int i = 4; i < args.length; i++) {
                        if (i > 4) sb.append(' ');
                        sb.append(args[i]);
                    }
                    world = sb.toString();
                } else if(sender instanceof Player) {
                    world = ((Player)sender).getWorld().getName();
                } else {
                    world = "world";
                }
                chestKey = sanitize(world) + "_" + args[1] + "_" + args[2] + "_" + args[3];
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
                chestKey = sanitize(loc.getWorld().getName()) + "_" + 
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

    private long parseLongSafe(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private boolean canParseInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void pruneOlderThanDays(int days) {
        long now = System.currentTimeMillis();
        long maxAgeMs = days * 24L * 60L * 60L * 1000L;

        Set<String> chests = Folder.getKeys(CHESTLOG_FILE, "chestlog");
        if (chests == null || chests.isEmpty()) return;

        for (String chestKey : chests) {
            Set<String> times = Folder.getKeys(CHESTLOG_FILE, "chestlog." + chestKey);
            if (times == null || times.isEmpty()) continue;

            for (String timeKey : times) {
                try {
                    long entryTime = Long.parseLong(timeKey);
                    if (now - entryTime > maxAgeMs) {
                        Folder.delete(CHESTLOG_FILE, "chestlog." + chestKey + "." + timeKey);
                    }
                } catch (NumberFormatException ignored) {
                    // ne peut pas arriver
                }
            }
        }
    }
}
