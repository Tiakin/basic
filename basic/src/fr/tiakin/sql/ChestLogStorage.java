package fr.tiakin.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import fr.tiakin.main.main;

public class ChestLogStorage {
    private static final String CREATE = "CREATE TABLE IF NOT EXISTS chest_log (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "time INTEGER, " +
            "chest_world TEXT, " +
            "chest_x INTEGER, " +
            "chest_y INTEGER, " +
            "chest_z INTEGER, " +
            "player TEXT, " +
            "action TEXT, " +
            "slot INTEGER, " +
            "item TEXT, " +
            "amount INTEGER" +
            ")";
        private static final String IDX_CHEST_POS =
            "CREATE INDEX IF NOT EXISTS idx_chest_pos ON chest_log(chest_world, chest_x, chest_y, chest_z)";
    private static final String INSERT = "INSERT INTO chest_log(time,chest_world,chest_x,chest_y,chest_z,player,action,slot,item,amount) VALUES(?,?,?,?,?,?,?,?,?,?)";
    private static final String COUNT = "SELECT COUNT(*) FROM chest_log WHERE chest_world=? AND chest_x=? AND chest_y=? AND chest_z=?";
    private static final String FETCH = "SELECT time,player,action,slot,item,amount FROM chest_log WHERE chest_world=? AND chest_x=? AND chest_y=? AND chest_z=? ORDER BY time DESC, id DESC LIMIT ? OFFSET ?";
    private static final String CLEAR = "DELETE FROM chest_log WHERE chest_world=? AND chest_x=? AND chest_y=? AND chest_z=?";
    private static final String PRUNE = "DELETE FROM chest_log WHERE time<?";

    public static void init() {
        try (Statement st = SQLManager.connection().createStatement()) {
            st.executeUpdate(CREATE);
            st.executeUpdate(IDX_CHEST_POS);
        } catch (Exception ex) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("ChestLog table init failed: " + ex.getMessage());
        }
    }

    public static void insert(long time, String world, int x, int y, int z, String player, String action, int slot, String item, int amount) {
        init();
        try (PreparedStatement ps = SQLManager.connection().prepareStatement(INSERT)) {
            ps.setLong(1, time);
            ps.setString(2, world);
            ps.setInt(3, x);
            ps.setInt(4, y);
            ps.setInt(5, z);
            ps.setString(6, player);
            ps.setString(7, action);
            ps.setInt(8, slot);
            ps.setString(9, item);
            ps.setInt(10, amount);
            ps.executeUpdate();
        } catch (Exception ex) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("ChestLog insert failed: " + ex.getMessage());
        }
    }

    public static void insertAsync(long time, String world, int x, int y, int z, String player, String action, int slot, String item, int amount) {
        Bukkit.getScheduler().runTaskAsynchronously(JavaPlugin.getPlugin(main.class), () -> {
            insert(time, world, x, y, z, player, action, slot, item, amount);
        });
    }

    public static int count(String world, int x, int y, int z) {
        init();
        try (PreparedStatement ps = SQLManager.connection().prepareStatement(COUNT)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception ex) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("ChestLog count failed: " + ex.getMessage());
        }
        return 0;
    }

    public static List<LogRow> fetch(String world, int x, int y, int z, int offset, int limit) {
        init();
        List<LogRow> out = new ArrayList<>();
        try (PreparedStatement ps = SQLManager.connection().prepareStatement(FETCH)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ps.setInt(5, limit);
            ps.setInt(6, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LogRow row = new LogRow();
                    row.time = rs.getLong(1);
                    row.player = rs.getString(2);
                    row.action = rs.getString(3);
                    row.slot = rs.getInt(4);
                    row.itemData = rs.getString(5);
                    row.amount = rs.getInt(6);
                    out.add(row);
                }
            }
        } catch (Exception ex) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("ChestLog fetch failed: " + ex.getMessage());
        }
        return out;
    }

    public static void clear(String world, int x, int y, int z) {
        init();
        try (PreparedStatement ps = SQLManager.connection().prepareStatement(CLEAR)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ps.executeUpdate();
        } catch (Exception ex) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("ChestLog clear failed: " + ex.getMessage());
        }
    }

    public static void pruneOlderThanDays(int days) {
        init();
        long maxAgeMs = days * 24L * 60L * 60L * 1000L;
        long threshold = System.currentTimeMillis() - maxAgeMs;
        try (PreparedStatement ps = SQLManager.connection().prepareStatement(PRUNE)) {
            ps.setLong(1, threshold);
            ps.executeUpdate();
        } catch (Exception ex) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("ChestLog prune failed: " + ex.getMessage());
        }
    }

    public static class LogRow {
        public long time;
        public String player;
        public String action;
        public int slot;
        public String itemData;
        public int amount;
    }
}