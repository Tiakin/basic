package fr.tiakin.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import fr.tiakin.main.main;

public class DeathStorage {
    private static final String CREATE = "CREATE TABLE IF NOT EXISTS death_log (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "player_uuid TEXT, " +
            "player_name TEXT, " +
            "time INTEGER, " +
            "xp INTEGER, " +
            "cause TEXT, " +
            "location TEXT, " +
            "contents TEXT, " +
            "armor TEXT, " +
            "offhand TEXT" +
            ")";
    private static final String INSERT = "INSERT INTO death_log(player_uuid,player_name,time,xp,cause,location,contents,armor,offhand) VALUES(?,?,?,?,?,?,?,?,?)";
    private static final String LIST_PLAYERS = "SELECT DISTINCT player_name FROM death_log";
    private static final String LIST_TIMES = "SELECT time FROM death_log WHERE player_name=? ORDER BY time";
    private static final String GET_LOG = "SELECT xp,cause,location,contents,armor,offhand FROM death_log WHERE player_name=? AND time=?";

    public static void init() {
        try (Statement st = SQLManager.connection().createStatement()) {
            st.executeUpdate(CREATE);
        } catch (Exception ex) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("Death table init failed: " + ex.getMessage());
        }
    }

    public static void insertLog(String uuid, String name, long time, int xp, String cause, String location, String contents, String armor, String offhand) {
        init();
        try (PreparedStatement ps = SQLManager.connection().prepareStatement(INSERT)) {
            ps.setString(1, uuid);
            ps.setString(2, name);
            ps.setLong(3, time);
            ps.setInt(4, xp);
            ps.setString(5, cause);
            ps.setString(6, location);
            ps.setString(7, contents);
            ps.setString(8, armor);
            ps.setString(9, offhand);
            ps.executeUpdate();
        } catch (Exception ex) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("Death insert failed: " + ex.getMessage());
        }
    }

    public static void insertLogAsync(String uuid, String name, long time, int xp, String cause, String location, String contents, String armor, String offhand) {
        Bukkit.getScheduler().runTaskAsynchronously(JavaPlugin.getPlugin(main.class), () -> {
            insertLog(uuid, name, time, xp, cause, location, contents, armor, offhand);
        });
    }

    public static Set<String> listPlayers() {
        init();
        Set<String> out = new java.util.HashSet<>();
        try (Statement st = SQLManager.connection().createStatement(); ResultSet rs = st.executeQuery(LIST_PLAYERS)) {
            while (rs.next()) {
                out.add(rs.getString(1));
            }
        } catch (Exception ex) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("Death list players failed: " + ex.getMessage());
        }
        return out;
    }

    public static List<Long> listTimes(String player) {
        init();
        List<Long> out = new ArrayList<>();
        try (PreparedStatement ps = SQLManager.connection().prepareStatement(LIST_TIMES)) {
            ps.setString(1, player);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getLong(1));
            }
        } catch (Exception ex) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("Death list times failed: " + ex.getMessage());
        }
        return out;
    }

    public static LogEntry getLog(String player, long time) {
        init();
        try (PreparedStatement ps = SQLManager.connection().prepareStatement(GET_LOG)) {
            ps.setString(1, player);
            ps.setLong(2, time);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LogEntry log = new LogEntry();
                    log.playerName = player;
                    log.time = time;
                    log.xp = rs.getInt(1);
                    log.cause = rs.getString(2);
                    log.location = rs.getString(3);
                    log.contents = rs.getString(4);
                    log.armor = rs.getString(5);
                    log.offhand = rs.getString(6);
                    return log;
                }
            }
        } catch (Exception ex) {
            JavaPlugin.getPlugin(main.class).getLogger().severe("Death get log failed: " + ex.getMessage());
        }
        return null;
    }

    public static class LogEntry {
        public String playerName;
        public long time;
        public int xp;
        public String cause;
        public String location;
        public String contents;
        public String armor;
        public String offhand;
    }
}