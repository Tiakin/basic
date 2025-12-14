package fr.tiakin.chest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
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
import fr.tiakin.main.IS;
import fr.tiakin.sql.ChestLogStorage;
import fr.tiakin.sql.SQLManager;
import net.md_5.bungee.api.ChatColor;

public class ChestLog implements CommandExecutor, Listener {
    public static final String CHESTLOG_FILE = "chestlog";
    private static final int PAGE_SIZE = 10;

    private Map<String, ItemStack[]> previousInventories = new HashMap<>();
    private SimpleDateFormat timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", new Locale("FR", "fr"));

    public ChestLog() {
        timestamp.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        ChestLogStorage.init();
        ChestLogStorage.pruneOlderThanDays(7);
    }

    public static void initTable() {
        ChestLogStorage.init();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player)) return;

        Inventory topInv = e.getView().getTopInventory();
        if(topInv == null) return;
        InventoryHolder holder = topInv.getHolder();
        ChestRef ref = getChestRef(holder);
        if(ref != null) {
            String mapKey = e.getWhoClicked().getName() + "_" + ref.key();
            if(!previousInventories.containsKey(mapKey)) {
                previousInventories.put(mapKey, cloneContents(topInv));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if(!(e.getPlayer() instanceof Player)) return;

        Player player = (Player) e.getPlayer();
        Inventory inv = e.getInventory();
        InventoryHolder holder = inv.getHolder();
        ChestRef ref = getChestRef(holder);

        if(ref != null) {
            String mapKey = player.getName() + "_" + ref.key();
            if(previousInventories.containsKey(mapKey)) {
                ItemStack[] before = previousInventories.get(mapKey);
                ItemStack[] after = cloneContents(inv);
                logChanges(player, ref, before, after);
                previousInventories.remove(mapKey);
            }
        }
    }

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

    private ChestRef getChestRef(InventoryHolder holder) {
        Location loc;

        if(holder instanceof DoubleChest) {
            loc = ((DoubleChest) holder).getLocation();
        } else if(holder instanceof Chest) {
            loc = ((Chest) holder).getLocation();
        } else if(holder instanceof Barrel) {
            loc = ((Barrel) holder).getLocation();
        } else if(holder instanceof ShulkerBox) {
            loc = ((ShulkerBox) holder).getLocation();
        } else {
            return null;
        }

        return new ChestRef(sanitize(loc.getWorld().getName()), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace(' ', '_').replace('.', '_');
    }

    private void logChanges(Player player, ChestRef ref, ItemStack[] before, ItemStack[] after) {
        long time = System.currentTimeMillis();

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
            if(!beforeEmpty && afterEmpty) {
                ItemStack removed = beforeItem.clone();
                removals.add(new Change(i, removed));
            }
            else if(beforeEmpty && !afterEmpty) {
                ItemStack added = afterItem.clone();
                deposits.add(new Change(i, added));
            }
            else if(!beforeEmpty && !afterEmpty) {
                if(!beforeItem.getType().equals(afterItem.getType())) {
                    logSwitchAction(player.getName(), ref, time, beforeItem.clone(), afterItem.clone(), i);
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

        boolean[] remUsed = new boolean[removals.size()];
        boolean[] depUsed = new boolean[deposits.size()];

        for(int r = 0; r < removals.size(); r++) {
            Change rem = removals.get(r);
            for(int d = 0; d < deposits.size(); d++) {
                if(depUsed[d]) continue;
                Change dep = deposits.get(d);
                if(rem.item.isSimilar(dep.item) && rem.item.getAmount() == dep.item.getAmount()) {
                    logMoveAction(player.getName(), ref, time, rem.item, rem.slot, dep.slot);
                    remUsed[r] = true;
                    depUsed[d] = true;
                    break;
                }
            }
        }

        for(int r = 0; r < removals.size(); r++) {
            if(remUsed[r]) continue;
            Change rem = removals.get(r);
            logAction(player.getName(), ref, time, "RETRAIT", rem.item, rem.slot);
        }
        for(int d = 0; d < deposits.size(); d++) {
            if(depUsed[d]) continue;
            Change dep = deposits.get(d);
            logAction(player.getName(), ref, time, "DEPOT", dep.item, dep.slot);
        }
    }

    private void logAction(String playerName, ChestRef ref, long time, String action, ItemStack item, int slot) {
        if (item == null || item.getType().isAir()) return;

        ChestLogStorage.insertAsync(time, ref.world, ref.x, ref.y, ref.z, playerName, action, slot, serialize(item), item.getAmount());
    }

    private void logMoveAction(String playerName, ChestRef ref, long time, ItemStack item, int fromSlot, int toSlot) {
        if (item == null || item.getType().isAir()) return;
        ChestLogStorage.insertAsync(time, ref.world, ref.x, ref.y, ref.z, playerName, "MOVE->" + toSlot, fromSlot, serialize(item), item.getAmount());
    }

    private void logSwitchAction(String playerName, ChestRef ref, long time, ItemStack fromItem, ItemStack toItem, int slot) {
        if (fromItem == null || fromItem.getType().isAir()) return;
        if (toItem == null || toItem.getType().isAir()) return;

        ItemStack[] items = new ItemStack[] { fromItem.clone(), toItem.clone() };
        String serialized = serializeArray(items);
        ChestLogStorage.insertAsync(time, ref.world, ref.x, ref.y, ref.z, playerName, "SWITCH", slot, serialized, -1);
    }

    private static class Change {
        int slot;
        ItemStack item;
        Change(int slot, ItemStack item) {
            this.slot = slot;
            this.item = item;
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
            ViewArgs parsed = parseViewArgs(sender, args);
            if(parsed == null || parsed.ref == null) return false;

            int total = ChestLogStorage.count(parsed.ref.world, parsed.ref.x, parsed.ref.y, parsed.ref.z);
            if(total == 0) {
                sender.sendMessage(ChatColor.GRAY + "Aucun log trouvé pour ce coffre.");
                return true;
            }

            int totalPages = (int) Math.ceil(total / (double) PAGE_SIZE);
            int page = Math.max(1, Math.min(parsed.page, totalPages));
            int offset = (page - 1) * PAGE_SIZE;

            sender.sendMessage(ChatColor.GOLD + "=== Logs du coffre " + parsed.ref.key() + " (page " + page + "/" + totalPages + ") ===");

            List<ChestLogStorage.LogRow> rows = ChestLogStorage.fetch(parsed.ref.world, parsed.ref.x, parsed.ref.y, parsed.ref.z, offset, PAGE_SIZE);
            for (ChestLogStorage.LogRow row : rows) {
                ChatColor color = colorForAction(row.action);
                String label = labelForAction(row.action, row.slot);
                String itemName;

                if ("SWITCH".equals(row.action)) {
                    ItemStack[] items = deserializeArray(row.itemData);
                    if (items != null && items.length == 2) {
                        String fromName = items[0] != null ? items[0].getType() + " x" + items[0].getAmount() : "Unknown";
                        String toName = items[1] != null ? items[1].getType() + " x" + items[1].getAmount() : "Unknown";
                        itemName = fromName + " -> " + toName;
                    } else {
                        itemName = "Unknown";
                    }
                } else {
                    ItemStack item = deserialize(row.itemData);
                    itemName = (item != null ? item.getType() + " x" + row.amount : "Unknown");
                }

                sender.sendMessage(color + "[" + timestamp.format(row.time) + "] " + row.player + " - " + label + " - " + itemName + " (slot " + row.slot + ")");
            }

            sender.sendMessage(ChatColor.GRAY + "Utilise /chestlog view ... <page> [monde] pour naviguer.");
            return true;
        }

        if(args[0].equalsIgnoreCase("clear")) {
            ViewArgs parsed = parseViewArgs(sender, args);
            if(parsed == null || parsed.ref == null) return false;

            ChestLogStorage.clear(parsed.ref.world, parsed.ref.x, parsed.ref.y, parsed.ref.z);
            sender.sendMessage(ChatColor.GREEN + "Logs du coffre " + parsed.ref.key() + " supprimés avec succès.");
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

    private boolean canParseInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private ViewArgs parseViewArgs(CommandSender sender, String[] args) {
        Integer x = null, y = null, z = null;
        String worldArg = null;
        int argIdx = 1;

        boolean hasCoords = false;
        if (args.length - argIdx >= 3 && canParseInt(args[argIdx]) && canParseInt(args[argIdx+1]) && canParseInt(args[argIdx+2])) {
            x = Integer.parseInt(args[argIdx]);
            y = Integer.parseInt(args[argIdx+1]);
            z = Integer.parseInt(args[argIdx+2]);
            argIdx += 3;
            hasCoords = true;
        }

        int page = 1;
        if (args.length > argIdx && canParseInt(args[argIdx])) {
            page = Math.max(1, Integer.parseInt(args[argIdx]));
            argIdx++;
        }

        if (hasCoords && args.length > argIdx) {
            StringBuilder sb = new StringBuilder();
            for (int i = argIdx; i < args.length; i++) {
                if (i > argIdx) sb.append(' ');
                sb.append(args[i]);
            }
            worldArg = sb.toString();
        }

        ChestRef ref;
        if(hasCoords) {
            String worldName;
            if(worldArg != null) {
                worldName = worldArg;
            } else if(sender instanceof Player) {
                worldName = ((Player)sender).getWorld().getName();
            } else {
                worldName = Bukkit.getWorlds().get(0).getWorldFolder().getName();
            }
            ref = new ChestRef(sanitize(worldName), x, y, z);
        } else {
            if(!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Vous devez spécifier les coordonnées depuis la console.");
                return null;
            }

            Player player = (Player) sender;
            Block targetBlock = player.getTargetBlock(null, 5);

            if(targetBlock == null || !isContainer(targetBlock.getType())) {
                sender.sendMessage(ChatColor.RED + "Vous devez regarder un conteneur (coffre, barrel, shulker box) !");
                return null;
            }

            Location loc = targetBlock.getLocation();
            ref = new ChestRef(sanitize(loc.getWorld().getName()), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }

        ViewArgs out = new ViewArgs();
        out.ref = ref;
        out.page = page;
        return out;
    }

    private ChatColor colorForAction(String action) {
        if (action == null) return ChatColor.GRAY;
        if (action.startsWith("MOVE")) return ChatColor.AQUA;
        if (action.equals("SWITCH")) return ChatColor.YELLOW;
        if (action.equalsIgnoreCase("DEPOT")) return ChatColor.GREEN;
        if (action.equalsIgnoreCase("RETRAIT")) return ChatColor.RED;
        return ChatColor.GRAY;
    }

    private String labelForAction(String action, int slot) {
        if (action == null) return "UNKNOWN";
        if (action.startsWith("MOVE->")) {
            return "MOVE " + slot + " -> " + action.substring("MOVE->".length());
        }
        if (action.equals("SWITCH")) return "SWITCH slot " + slot;
        return action + " slot " + slot;
    }

    private String serialize(ItemStack item) {
        return SQLManager.mapToString(IS.serialize(item));
    }

    private ItemStack deserialize(String data) {
        if (data == null) return null;
        Map<String, Object> map = SQLManager.stringToMap(data);
        if (map == null || map.isEmpty()) return null;
        return IS.deserialize(map);
    }

    private String serializeArray(ItemStack[] items) {
        try {
            return SQLManager.serializeArray(items);
        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack[] deserializeArray(String data) {
        try {
            return SQLManager.deserializeArray(data);
        } catch (Exception e) {
            return null;
        }
    }

    static class ChestRef {
        final String world;
        final int x;
        final int y;
        final int z;
        ChestRef(String world, int x, int y, int z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }
        String key() {
            return world + "_" + x + "_" + y + "_" + z;
        }
    }

    static class ViewArgs {
        ChestRef ref;
        int page;
    }

}
