package fr.tiakin.player;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import fr.tiakin.main.main;
import fr.tiakin.sql.DeathStorage;
import fr.tiakin.sql.SQLManager;
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

    public static void initTable() {
        DeathStorage.init();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
        if(!sender.isOp()) return false;

        if(args.length == 0) {
            sender.sendMessage("/death log (pseudo)");
            sender.sendMessage("/death restore [pseudo] (date) (option)");
            return true;
        }

        if(args[0].equalsIgnoreCase("log")) {
            if(args.length == 2) {
                String pseudo = args[1];
                sender.sendMessage(ChatColor.of(new Color(20,20,20)) + "------- " + ChatColor.of(new Color(50,50,50)) + pseudo + ChatColor.of(new Color(20,20,20)) + " -------");

                List<Long> dates = DeathStorage.listTimes(pseudo);
                for (Long date : dates) {
                    TextComponent message = new TextComponent(ChatColor.DARK_GRAY + pseudo + ChatColor.GRAY + " est mort le " + ChatColor.DARK_GRAY + timestamp.format(date * 100));
                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7Clique pour §6regarder son inventaire §7!  §4" + date)));
                    message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/death view " + pseudo + " " + date));
                    sender.spigot().sendMessage(message);
                }
                sender.sendMessage(ChatColor.of(new Color(20,20,20)) + "---------------------");
            } else {
                Set<String> players = DeathStorage.listPlayers();
                for (String pseudo : players) {
                    sender.sendMessage(ChatColor.of(new Color(20,20,20)) + "------- " + ChatColor.of(new Color(50,50,50)) + pseudo + ChatColor.of(new Color(20,20,20)) + " -------");
                    List<Long> dates = DeathStorage.listTimes(pseudo);
                    for (Long date : dates) {
                        TextComponent message = new TextComponent(ChatColor.DARK_GRAY + pseudo + ChatColor.GRAY + " est mort le " + ChatColor.DARK_GRAY + timestamp.format(date * 100));
                        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7Clique pour §6regarder son inventaire §7!")));
                        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/death view " + pseudo + " " + date));
                        sender.spigot().sendMessage(message);
                    }
                    sender.sendMessage(ChatColor.of(new Color(20,20,20)) + "---------------------");
                }
            }
            return true;
        }

        if(args[0].equalsIgnoreCase("restore")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage invalide pour /death restore.");
                sender.sendMessage(ChatColor.YELLOW + "Correct: /death restore <pseudo> [date]");
                return true;
            }
            final Player p = Bukkit.getPlayer(args[1]);
            if (p == null) return false;

            String gettime;
            if(args.length == 3) {
                gettime = args[2];
            } else {
                List<Long> keys = DeathStorage.listTimes(p.getName());
                if (keys.isEmpty()) return false;
                gettime = keys.get(keys.size() - 1).toString();
            }
            final String time = gettime;

            Bukkit.getServer().getScheduler().runTaskAsynchronously(JavaPlugin.getPlugin(main.class), () -> {
                DeathStorage.LogEntry log = DeathStorage.getLog(p.getName(), Long.parseLong(time));
                if (log == null) return;

                ItemStack[] inv = deserializeContentArray(log.contents, 36);
                ItemStack[] armor = deserializeContentArray(log.armor, 4);
                ItemStack offhand = deserializeItem(log.offhand);

                p.getInventory().setStorageContents(inv);
                p.getInventory().setArmorContents(armor);
                if (offhand != null) p.getInventory().setItemInOffHand(offhand);
                p.setTotalExperience(log.xp);
            });
            return true;
        }

        if(args[0].equalsIgnoreCase("view")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage invalide pour /death view.");
                sender.sendMessage(ChatColor.YELLOW + "Correct: /death view <pseudo> <date>");
                return true;
            }
            if(!(sender instanceof Player)) return false;
            Player p = (Player) sender;
            String playerName = args[1];
            long time = Long.parseLong(args[2]);

            DeathStorage.LogEntry log = DeathStorage.getLog(playerName, time);
            if (log == null) return false;

            Inventory inv = Bukkit.createInventory(null, 54, "§4view : §5" + playerName);

            ItemStack[] contents = deserializeContentArray(log.contents, 36);
            for(int n = 0; n < contents.length; n++) {
                if(contents[n] != null) {
                    inv.setItem(n, contents[n]);
                }
            }

            ItemStack[] armor = deserializeContentArray(log.armor, 4);
            for(int n = 0; n < armor.length; n++) {
                if(armor[n] != null) {
                    inv.setItem(n + 45, armor[n]);
                }
            }

            ItemStack offhand = deserializeItem(log.offhand);
            if(offhand != null) {
                inv.setItem(40, offhand);
            }

            ItemStack is = new ItemStack(Material.ANVIL, 1);
            ItemMeta im = is.getItemMeta();
            im.setDisplayName(log.cause);
            im.setLore(Arrays.asList(ChatColor.DARK_GRAY + playerName + ChatColor.GRAY + " est mort le " + ChatColor.DARK_GRAY + timestamp.format(time * 100)));
            is.setItemMeta(im);
            inv.setItem(50, is);

            is = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);
            im = is.getItemMeta();
            im.setDisplayName(Integer.toString(log.xp));
            is.setItemMeta(im);
            inv.setItem(51, is);

            is = new ItemStack(Material.MINECART, 1);
            im = is.getItemMeta();
            im.setDisplayName(log.location);
            is.setItemMeta(im);
            inv.setItem(52, is);

            is = new ItemStack(Material.EMERALD_BLOCK, 1);
            im = is.getItemMeta();
            im.setDisplayName("§8Clique ici pour revive !");
            im.setLore(Arrays.asList("death restore " + playerName + " " + time));
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
            return true;
        }

        if(args[0].equalsIgnoreCase("multikill")) {
            for(String player : args) {
                if(!player.equalsIgnoreCase("multikill") && Bukkit.getPlayer(player) != null) {
                    Bukkit.getPlayer(player).setHealth(0);
                }
            }
            Bukkit.broadcastMessage(ChatColor.of(new Color(92, 114, 207)) + "[Multikill] " + ChatColor.RESET + sender.getName() + " a tué " + (args.length - 1) + " joueurs !");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "/death log (pseudo)");
        sender.sendMessage(ChatColor.YELLOW + "/death restore [pseudo] (date)");
        sender.sendMessage(ChatColor.YELLOW + "/death view [pseudo] [date]");
        return true;
    }

    @EventHandler
    public void ondeath(PlayerDeathEvent e) {
        final Player p = e.getEntity();
        final ItemStack[] inv = p.getInventory().getStorageContents();
        final ItemStack[] armor = p.getInventory().getArmorContents();
        final ItemStack offhand = p.getInventory().getItemInOffHand();
        final Location loc = p.getLocation();
        final int xp = p.getTotalExperience();
        final long time = System.currentTimeMillis() / 100L;

        DeathStorage.insertLogAsync(
            p.getUniqueId().toString(),
            p.getName(),
            time,
            xp,
            e.getDeathMessage(),
            loc.toString(),
            serializeArray(inv),
            serializeArray(armor),
            serializeItem(offhand)
        );
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

    // ---- serialization helpers ----
    private static String serializeArray(ItemStack[] items) {
        return SQLManager.serializeArray(items);
    }

    private static ItemStack[] deserializeContentArray(String data, int size) {
        ItemStack[] items = SQLManager.deserializeArray(data);
        if (items.length == 0) return new ItemStack[size];
        ItemStack[] out = new ItemStack[size];
        System.arraycopy(items, 0, out, 0, Math.min(items.length, size));
        return out;
    }

    private static String serializeItem(ItemStack item) {
        if (item == null) return null;
        return SQLManager.serializeArray(new ItemStack[]{item});
    }

    private static ItemStack deserializeItem(String data) {
        ItemStack[] items = SQLManager.deserializeArray(data);
        return items.length > 0 ? items[0] : null;
    }

}
