package fr.tiakin.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;

import fr.tiakin.main.main;

public class VanishStorage {
    private static final String CREATE = "CREATE TABLE IF NOT EXISTS vanish (" +
            "uuid TEXT PRIMARY KEY, " +
            "name TEXT, " +
            "status INTEGER NOT NULL" +
            ")";
    private static final String UPSERT = "INSERT INTO vanish(uuid,name,status) VALUES(?,?,?) " +
            "ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, status=excluded.status";
    private static final String GET = "SELECT status FROM vanish WHERE uuid=?";
    private static final String LIST_ACTIVE = "SELECT uuid FROM vanish WHERE status=1";

    public static void init() {
        try (Statement st = SQLManager.connection().createStatement()) {
            st.executeUpdate(CREATE);
        } catch (Exception ex) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("Vanish table init failed: " + ex.getMessage());
        }
    }

    public static boolean isVanished(String uuid) {
        init();
        try (PreparedStatement ps = SQLManager.connection().prepareStatement(GET)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 1;
                }
            }
        } catch (Exception ex) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("Vanish get failed: " + ex.getMessage());
        }
        return false;
    }

    public static void setVanished(String uuid, String name, boolean status) {
        init();
        try (PreparedStatement ps = SQLManager.connection().prepareStatement(UPSERT)) {
            ps.setString(1, uuid);
            ps.setString(2, name);
            ps.setInt(3, status ? 1 : 0);
            ps.executeUpdate();
        } catch (Exception ex) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("Vanish set failed: " + ex.getMessage());
        }
    }

    public static Set<UUID> activeVanished() {
        init();
        Set<UUID> out = new HashSet<>();
        try (Statement st = SQLManager.connection().createStatement();
             ResultSet rs = st.executeQuery(LIST_ACTIVE)) {
            while (rs.next()) {
                try {
                    out.add(UUID.fromString(rs.getString(1)));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (Exception ex) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("Vanish list failed: " + ex.getMessage());
        }
        return out;
    }
}