package fr.tiakin.main;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import fr.tiakin.chat.ChatClear;
import fr.tiakin.player.Death;
import fr.tiakin.player.Gm;
import fr.tiakin.player.Nick;
import fr.tiakin.player.Revive;
import fr.tiakin.player.Vanish;
import fr.tiakin.player.inventory.EnderSee;
import fr.tiakin.player.inventory.InvSee;
import fr.tiakin.player.life.Feed;
import fr.tiakin.player.life.Heal;
import fr.tiakin.chest.ChestLog;


public class main extends JavaPlugin implements Listener {
    
    private boolean deathModuleEnabled = getConfig().getBoolean("modules.death.enabled", false);
    private boolean chestlogModuleEnabled = getConfig().getBoolean("modules.chestlog.enabled", true);
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        Bukkit.getPluginManager().registerEvents(this, this);
        
        if(deathModuleEnabled) {
            Death death = new Death();
            Bukkit.getPluginManager().registerEvents(death, this);
            getCommand("death").setExecutor(death);
            getCommand("revive").setExecutor(new Revive());
            Death.initTable();
            getLogger().info("Module Death activé");
        } else {
            getLogger().info("Module Death désactivé");
        }
        
        getCommand("feed").setExecutor(new Feed());
        getCommand("heal").setExecutor(new Heal());
        getCommand("gm").setExecutor(new Gm());
        getCommand("vanish").setExecutor(new Vanish());
        getCommand("invsee").setExecutor(new InvSee());
        getCommand("nick").setExecutor(new Nick(this));
        getCommand("chatclear").setExecutor(new ChatClear());
        getCommand("endersee").setExecutor(new EnderSee());
        
        // Module ChestLog
        if(chestlogModuleEnabled) {
            ChestLog chestLog = new ChestLog();
            Bukkit.getPluginManager().registerEvents(chestLog, this);
            getCommand(ChestLog.CHESTLOG_FILE).setExecutor(chestLog);
            ChestLog.initTable();
            getLogger().info("Module ChestLog activé");
        } else {
            getLogger().info("Module ChestLog désactivé");
        }
        
        Vanish.initTable();
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        for (java.util.UUID uuid : Vanish.activeVanished()) {
            Player vanished = Bukkit.getPlayer(uuid);
            if (vanished != null) {
                e.getPlayer().hidePlayer(this, vanished);
            }
        }
    }

    @EventHandler
    public void leave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if(Vanish.isVanished(p.getUniqueId())) {
            Vanish.setVanished(p.getUniqueId(), p.getName(), false);
            p.setCanPickupItems(true);
            e.setQuitMessage(null);
        }
    }
    
    @EventHandler
    public static void chat(AsyncPlayerChatEvent e) {
        if(e.getPlayer().isOp() && e.getMessage().contains(".&"))
            e.setMessage(e.getMessage().replace(".&", "§"));
    }
    
    @EventHandler
    public void redstoneChanges(BlockRedstoneEvent e) {
        Block block = e.getBlock();
        BlockState state = block.getState();
        
        if(state instanceof CommandBlock) {
            CommandBlock cb = (CommandBlock) state;
            if(cb.getCommand().contains("/&")) {
                cb.setCommand(cb.getCommand().replace("/&", "§"));
                cb.update();
            }
        }
    }
    
    public static Class<?> getNMSClass(String nmsClassString) throws ClassNotFoundException {
        String cbPackage = Bukkit.getServer().getClass().getPackage().getName();
        String version = "";
        if (cbPackage.startsWith("org.bukkit.craftbukkit")) {
            int lastDot = cbPackage.lastIndexOf('.');
            String tail = cbPackage.substring(lastDot + 1);
            if (!"craftbukkit".equals(tail)) {
                version = tail + ".";
            }
        }
        String name = "org.bukkit.craftbukkit." + version + nmsClassString;
        return Class.forName(name);
    }
    
    public static Object readNBTFromFile(java.io.File file) throws Exception {
        try {
            // Pre-1.21
            Class<?> nbtToolsClass = Class.forName("net.minecraft.nbt.NBTCompressedStreamTools");
            java.lang.reflect.Method readMethod = nbtToolsClass.getMethod("a", java.io.InputStream.class);
            return readMethod.invoke(null, new java.io.FileInputStream(file));
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // 1.21+
            try {
                Class<?> nbtIoClass = Class.forName("net.minecraft.nbt.NbtIo");
                Class<?> nbtAccounterClass = Class.forName("net.minecraft.nbt.NbtAccounter");
                java.lang.reflect.Method unlimitedHeapMethod = nbtAccounterClass.getMethod("unlimitedHeap");
                Object accounter = unlimitedHeapMethod.invoke(null);
                
                java.lang.reflect.Method readMethod = nbtIoClass.getMethod("readCompressed", java.io.InputStream.class, nbtAccounterClass);
                return readMethod.invoke(null, new java.io.FileInputStream(file), accounter);
            } catch (Exception ex) {
                throw new Exception("Impossible de lire le fichier NBT - API non trouvée", ex);
            }
        }
    }
    
    public static Object getList(Object nbtTag, String key, int type) throws Exception {
        // Pre-1.21
        try {
            java.lang.reflect.Method method = nbtTag.getClass().getMethod("getList", String.class, int.class);
            return method.invoke(nbtTag, key, type);
        } catch (NoSuchMethodException ignored) {
            // 1.21+
            try {
                java.lang.reflect.Method method = nbtTag.getClass().getMethod("getList", String.class);
                Object result = method.invoke(nbtTag, key);
                // 1.21+ probablement Optional
                if (result instanceof java.util.Optional) {
                    return ((java.util.Optional<?>) result).orElse(null);
                }
                return result;
            } catch (NoSuchMethodException ignored2) {
                // Fallback: use generic get(String) and expect a ListTag
                Object list = get(nbtTag, key);
                if (list == null) {
                    throw new Exception("NBT list '" + key + "' introuvable");
                }
                return list;
            }
        }
    }
    
    public static Object get(Object nbtTag, String key) throws Exception {
        java.lang.reflect.Method method = nbtTag.getClass().getMethod("get", String.class);
        Object result = method.invoke(nbtTag, key);
        // 1.21+ probablement Optional
        if (result instanceof java.util.Optional) {
            return ((java.util.Optional<?>) result).orElse(null);
        }
        return result;
    }
    
    public static int getInt(Object nbtTag, String key) throws Exception {
        java.lang.reflect.Method method = nbtTag.getClass().getMethod("getInt", String.class);
        Object result = method.invoke(nbtTag, key);
        // 1.21+ probablement Optional
        if (result instanceof java.util.Optional) {
            return ((java.util.Optional<Integer>) result).orElse(0);
        }
        return (int) result;
    }
    
    public static boolean isEmpty(Object nbtTag) throws Exception {
        java.lang.reflect.Method method = nbtTag.getClass().getMethod("isEmpty");
        return (boolean) method.invoke(nbtTag);
    }
    
    public static int size(Object listTag) throws Exception {
        // 1.21+ probablement Optional
        if (listTag instanceof java.util.Optional) {
            listTag = ((java.util.Optional<?>) listTag).orElse(null);
            if (listTag == null) return 0;
        }
        java.lang.reflect.Method method = listTag.getClass().getMethod("size");
        return (int) method.invoke(listTag);
    }
    
    public static Object get(Object listTag, int index) throws Exception {
        java.lang.reflect.Method method = listTag.getClass().getMethod("get", int.class);
        Object result = method.invoke(listTag, index);
        // 1.21+ probablement Optional
        if (result instanceof java.util.Optional) {
            return ((java.util.Optional<?>) result).orElse(null);
        }
        return result;
    }
    
        public static Object nbtToItemStack(Object nbtTag) throws Exception {
            try {
                // Pré-1.21 : ItemStack.a(NBTTagCompound)
                Class<?> itemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
                java.lang.reflect.Method legacy = itemStackClass.getMethod("a", nbtTag.getClass());
                return legacy.invoke(null, nbtTag);
            } catch (NoSuchMethodException e) {
                // 1.21+ : parse via Codec et NbtOps
                try {
                    Class<?> itemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
                    Class<?> nbtOpsClass = Class.forName("net.minecraft.nbt.NbtOps");
                    java.lang.reflect.Field codecField = itemStackClass.getField("CODEC");
                    Object codec = codecField.get(null);
                    java.lang.reflect.Field opsInstanceField = nbtOpsClass.getField("INSTANCE");
                    Object nbtOps = opsInstanceField.get(null);

                    Class<?> dynamicOpsClass = Class.forName("com.mojang.serialization.DynamicOps");
                    java.lang.reflect.Method parse = codec.getClass().getMethod("parse", dynamicOpsClass, Object.class);
                    Object dataResult = parse.invoke(codec, nbtOps, nbtTag);
                    java.lang.reflect.Method resultMethod = dataResult.getClass().getMethod("result");
                    java.util.Optional<?> opt = (java.util.Optional<?>) resultMethod.invoke(dataResult);
                    if (opt.isPresent()) {
                        return opt.get();
                    }
                    java.lang.reflect.Field emptyField = itemStackClass.getField("EMPTY");
                    return emptyField.get(null);
                } catch (Exception ex) {
                    throw new Exception("Impossible de convertir NBT en ItemStack - API non trouvée", ex);
                }
            }
        }
    
    public boolean isDeathModuleEnabled() {
        return deathModuleEnabled;
    }
    
    public boolean isChestlogModuleEnabled() {
        return chestlogModuleEnabled;
    }
}
