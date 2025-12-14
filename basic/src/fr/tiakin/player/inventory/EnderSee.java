package fr.tiakin.player.inventory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import fr.tiakin.main.main;

public class EnderSee implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
		if(sender.isOp() || Bukkit.getPlayer(sender.getName()).getGameMode()==GameMode.SPECTATOR) {
			if(args.length == 0) {
				Player p = Bukkit.getPlayer(sender.getName());
				p.openInventory(p.getEnderChest());
			}else if(Bukkit.getPlayer(args[0]) != null) {
			Bukkit.getPlayer(sender.getName()).openInventory(Bukkit.getPlayer(args[0]).getEnderChest());
			}else {
				try {
					for(OfflinePlayer offplayer : Bukkit.getOfflinePlayers()) {
						if(offplayer.getName().equalsIgnoreCase(args[0])) {
							File worldFolder = Bukkit.getWorlds().get(0).getWorldFolder();
							File player = new File(worldFolder, "playerdata" + File.separator + offplayer.getUniqueId() + ".dat");
							Object nbt = main.readNBTFromFile(player);
							Object inventory = main.getList(nbt, "EnderItems", 10);
							Inventory inv = Bukkit.createInventory(null, 27, offplayer.getName());
							for (int i = 0; i < main.size(inventory); i++) {
							    Object compound = main.get(inventory, i);
							    if (!main.isEmpty(compound)) {
							    	Method m = main.getNMSClass("inventory.CraftItemStack").getMethod("asBukkitCopy",net.minecraft.world.item.ItemStack.class);
							    	Object nmsItemStack = main.nbtToItemStack(compound);
							    	ItemStack stack = (ItemStack) m.invoke(null, nmsItemStack);
							    	Integer slot = main.getInt(compound, "Slot");
							    	inv.setItem(slot, stack);
							    }
							  }
							  Bukkit.getPlayer(sender.getName()).openInventory(inv);
							}
					}
				} catch (Exception e) {
					  e.printStackTrace();
				}
			}
		}
		
		return false;
	}

}