package fr.tiakin.player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Nick implements CommandExecutor {

    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([0-9a-fA-F]+)\\\"");
    private static final Pattern VALUE_PATTERN = Pattern.compile("\\\"value\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("\\\"signature\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern NAME_PATTERN = Pattern.compile("\\W{3,16}$");

    private final JavaPlugin plugin;

    public Nick(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est reservée aux joueurs.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("Usage: /nick <pseudo>");
            return true;
        }

        Player player = (Player) sender;
        String newName = args[0];

        if (!NAME_PATTERN.matcher(newName).matches()) {
            player.sendMessage("Pseudo invalide (3-16 caractères).");
            return true;
        }

        player.setDisplayName(newName);
        player.setPlayerListName(newName);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            SkinData skin = null;
            try {
                skin = fetchSkin(newName);
            } catch (IOException e) {
                plugin.getLogger().warning("Nick: erreur fetch skin pour " + newName + ": " + e.getMessage());
            }

            SkinData finalSkin = skin;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalSkin == null) {
                    player.sendMessage("Skin introuvable pour ce pseudo. Pseudo appliqué sans skin.");
                    return;
                }
                try {
                    applySkin(player, finalSkin);
                    player.sendMessage("Pseudo et skin appliqués.");
                } catch (Exception e) {
                    plugin.getLogger().warning("Nick: erreur apply skin pour " + newName + ": " + e.getMessage());
                    player.sendMessage("Erreur lors de l'application du skin.");
                }
            });
        });

        return true;
    }

    private SkinData fetchSkin(String name) throws IOException {
        String profileJson = readUrl("https://api.mojang.com/users/profiles/minecraft/" + name);
        if (profileJson == null || profileJson.isEmpty()) {
            return null;
        }

        Matcher idMatcher = ID_PATTERN.matcher(profileJson);
        if (!idMatcher.find()) {
            return null;
        }

        String uuid = idMatcher.group(1);
        String sessionJson = readUrl("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
        if (sessionJson == null || sessionJson.isEmpty()) {
            return null;
        }

        Matcher valueMatcher = VALUE_PATTERN.matcher(sessionJson);
        Matcher signatureMatcher = SIGNATURE_PATTERN.matcher(sessionJson);
        if (!valueMatcher.find()) {
            return null;
        }

        String value = valueMatcher.group(1);
        String signature = signatureMatcher.find() ? signatureMatcher.group(1) : null;
        if (signature == null) {
            return null;
        }

        return new SkinData(value, signature);
    }

    private String readUrl(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "basic-nick");

        int code = connection.getResponseCode();
        if (code != 200) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private void applySkin(Player player, SkinData skin) throws Exception {
        String cbPackage = Bukkit.getServer().getClass().getPackage().getName();
        Class<?> craftPlayerClass = Class.forName(cbPackage + ".entity.CraftPlayer");
        Object craftPlayer = craftPlayerClass.cast(player);
        Object gameProfile = craftPlayerClass.getMethod("getProfile").invoke(craftPlayer);

        Object propertyMap = gameProfile.getClass().getMethod("getProperties").invoke(gameProfile);
        try {
            propertyMap.getClass().getMethod("removeAll", Object.class).invoke(propertyMap, "textures");
        } catch (NoSuchMethodException e) {
            propertyMap.getClass().getMethod("removeAll", String.class).invoke(propertyMap, "textures");
        }

        Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
        Object textureProperty = propertyClass.getConstructor(String.class, String.class, String.class)
                .newInstance("textures", skin.value, skin.signature);

        try {
            propertyMap.getClass().getMethod("put", Object.class, Object.class).invoke(propertyMap, "textures", textureProperty);
        } catch (NoSuchMethodException e) {
            propertyMap.getClass().getMethod("put", String.class, Object.class).invoke(propertyMap, "textures", textureProperty);
        }

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.hidePlayer(plugin, player);
            viewer.showPlayer(plugin, player);
        }
    }

    private static class SkinData {
        private final String value;
        private final String signature;

        private SkinData(String value, String signature) {
            this.value = value;
            this.signature = signature;
        }
    }
}
