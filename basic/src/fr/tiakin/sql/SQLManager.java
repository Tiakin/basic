package fr.tiakin.sql;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import fr.tiakin.main.main;

public class SQLManager {
    private static final String STORAGE_PATH = "plugins/basic/";
    private static final String DB_PATH = STORAGE_PATH + "basic.db";
    private static volatile Connection conn = null;

    private SQLManager() {}

    // -------- SQLite bootstrap ---------
    private static synchronized void ensureInit() {
        if (conn != null) return;
        try {
            Class.forName("org.sqlite.JDBC");
            File dir = new File(STORAGE_PATH);
            dir.mkdirs();
            conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            JavaPlugin.getPlugin(main.class).getLogger().info("SQLite activé pour le stockage (" + DB_PATH + ")");
        } catch (Throwable t) {
            throw new RuntimeException("SQLite doit être disponible", t);
        }
    }

    // -------- Public API ---------
    public static Connection connection() {
        ensureInit();
        return conn;
    }

    // -------- Helper (YAML string serialization) ---------
    public static String mapToString(Map<String, Object> data) {
        if (data == null) return null;
        YamlConfiguration yml = new YamlConfiguration();
        yml.createSection("d", data);
        return yml.saveToString();
    }

    // -------- Binary serialization (for ItemStack arrays) ---------
    public static String serializeArray(ItemStack[] items) {
        if (items == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {
            boos.writeObject(items);
            boos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("ItemStack serialization error: " + e.getMessage());
            return null;
        }
    }

    public static ItemStack[] deserializeArray(String data) {
        if (data == null) return new ItemStack[0];
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream bois = new BukkitObjectInputStream(bais);
            ItemStack[] items = (ItemStack[]) bois.readObject();
            bois.close();
            return items;
        } catch (Exception e) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("ItemStack deserialization error: " + e.getMessage());
            return new ItemStack[0];
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> stringToMap(String data) {
        if (data == null) return Collections.emptyMap();
        YamlConfiguration yml = new YamlConfiguration();
        try {
            yml.loadFromString(data);
            if (yml.getConfigurationSection("d") == null) return Collections.emptyMap();
            return yml.getConfigurationSection("d").getValues(false);
        } catch (InvalidConfigurationException e) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("Invalid YAML data: " + e.getMessage());
            return Collections.emptyMap();
        }
    }
}