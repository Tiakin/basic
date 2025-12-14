package fr.tiakin.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Folder {
    private static final String STORAGE_PATH = "plugins/basic/";
    
    private Folder() {}
    
    private static File getFile(String filename) {
        return new File(STORAGE_PATH + filename + ".yml");
    }
    
    private static YamlConfiguration loadYaml(File file) throws IOException, InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        if (file.exists()) {
            config.load(file);
        }
        return config;
    }
    
    public static void create(String filename) {
        File file = getFile(filename);
        if (!file.exists()) {
            try {
                File dir = file.getParentFile();
                dir.mkdirs();
                if(!file.createNewFile()) {
					JavaPlugin.getPlugin(main.class).getLogger().severe("Erreur lors de la création du fichier: " + file.getAbsolutePath());
				}
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void set(String filename, String key, String value) {
        File file = getFile(filename);
        create(filename);
        try {
            YamlConfiguration config = loadYaml(file);
            config.set(key, value);
            config.save(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }
    
    public static void setSection(String filename, String key, Map<String, Object> values) {
        File file = getFile(filename);
        create(filename);
        try {
            YamlConfiguration config = loadYaml(file);
            config.createSection(key, values);
            config.save(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }
    
    public static void add(String filename, String key, String value) {
        File file = getFile(filename);
        create(filename);
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))	) {
            bw.write(key + ":");
            bw.write(" '" + value + "'");
            bw.newLine();
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static String get(String filename, String key) {
        File file = getFile(filename);
        if (!file.exists()) {
            return null;
        }
        try {
            YamlConfiguration config = loadYaml(file);
            return config.getString(key);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static void delete(String filename, String key) {
        File file = getFile(filename);
        if (!file.exists()) {
            return;
        }
        try {
            YamlConfiguration config = loadYaml(file);
            config.set(key, null);
            config.save(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }
    
    public static Set<String> getKeys(String filename, String section) {
        File file = getFile(filename);
        if (!file.exists()) {
            return Collections.emptySet();
        }
        try {
            YamlConfiguration config = loadYaml(file);
            if (section != null) {
                ConfigurationSection cs = config.getConfigurationSection(section);
                return cs != null ? cs.getKeys(false) : null;
            } else {
                return config.getKeys(false);
            }
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return Collections.emptySet();
    }
    
    public static Set<String> getKeys(String filename) {
        return getKeys(filename, null);
    }
    
    public static Map<String, Object> getValues(String filename, String section) {
        File file = getFile(filename);
        if (!file.exists()) {
            return Collections.emptyMap();
        }
        try {
            YamlConfiguration config = loadYaml(file);
            if (section != null) {
                ConfigurationSection cs = config.getConfigurationSection(section);
                return cs != null ? cs.getValues(false) : null;
            } else {
                return config.getValues(false);
            }
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return Collections.emptyMap();
    }
    
    public static Map<String, Object> getValues(String filename) {
        return getValues(filename, null);
    }
    
    public static ConfigurationSection getSection(String filename, String section) {
        File file = getFile(filename);
        if (!file.exists()) {
            return null;
        }
        try {
            YamlConfiguration config = loadYaml(file);
            return config.getConfigurationSection(section);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }
}
