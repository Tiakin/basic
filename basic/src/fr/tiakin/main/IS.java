package fr.tiakin.main;

import java.util.Map;

import org.bukkit.inventory.ItemStack;

public class IS {
	
	 public static Map<String, Object> serialize(ItemStack item){
		 return item.serialize();
	 }
	 
	 public static ItemStack deserialize(Map<String, Object> serializedItem){
		 ItemStack item = ItemStack.deserialize(serializedItem);
		 return item;
	 }
	 
}
