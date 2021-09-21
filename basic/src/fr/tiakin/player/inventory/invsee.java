package fr.tiakin.player.inventory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class invsee implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
		if(sender.isOp() || Bukkit.getPlayer(sender.getName()).getGameMode()==GameMode.SPECTATOR) {
			if(args.length == 0) {
				Player p = Bukkit.getPlayer(sender.getName());
				p.openInventory(p.getInventory());
			}else if(Bukkit.getPlayer(args[0]) != null) {
			Bukkit.getPlayer(sender.getName()).openInventory(Bukkit.getPlayer(args[0]).getInventory());
			}else {
				for(OfflinePlayer offplayer : Bukkit.getOfflinePlayers()) {
					if(offplayer.getName().equalsIgnoreCase(args[0])) {
						File player = new File("world/playerdata/"+offplayer.getUniqueId()+".dat");
						try {
							  NBTTagCompound nbt = NBTCompressedStreamTools.a((InputStream) (new FileInputStream(player)));
							  NBTTagList inventory = (NBTTagList) nbt.get("Inventory");
							  Inventory inv = new CraftInventoryCustom(null, 45);
							  for (int i = 0; i < inventory.size(); i++) {
							    NBTTagCompound compound = (NBTTagCompound) inventory.get(i);
							    if (!compound.isEmpty()) {
							      ItemStack stack = CraftItemStack.asBukkitCopy(net.minecraft.world.item.ItemStack.a(compound));
							      
							      Integer slot = compound.getInt("Slot");
							      if(slot == 100) {
							    	  slot = 36; 
							      }else if(slot == 101) {
							    	  slot = 37;
							      }else if(slot == 102) {
							    	  slot = 38;
							      }else if(slot ==103) {
							    	  slot = 39;
							      }else if(slot == -106) {
							    	  slot = 44;
							      }
							      inv.setItem(slot, stack);
							    }
							  }
							  Bukkit.getPlayer(sender.getName()).openInventory(inv);
							} catch (IOException e) {
							  e.printStackTrace();
							}
					}
				}
			}
		}
		
		return false;
	}

}
