package dev.sunmc.leaderboard;

import dev.sunmc.SunMc;
import dev.sunmc.managers.StatsManager;
import dev.sunmc.utils.ColorUtil;
import dev.sunmc.utils.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LeaderboardManager {

    private final SunMc plugin;
    private final Map<String, LeaderboardEntry> leaderboards = new HashMap<>();
    private final Map<String, List<ArmorStand>> stands = new HashMap<>();
    private File lbFile;
    private FileConfiguration lbConfig;

    public LeaderboardManager(SunMc plugin) {
        this.plugin = plugin;
        load();
        // Update leaderboards periodically
        long interval = plugin.getConfig().getLong("leaderboard.update-interval-seconds", 60L) * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, interval, interval);
    }

    private void load() {
        lbFile = new File(plugin.getDataFolder(), "leaderboards.yml");
        if (!lbFile.exists()) {
            try { lbFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        lbConfig = YamlConfiguration.loadConfiguration(lbFile);
        for (String name : lbConfig.getKeys(false)) {
            String type = lbConfig.getString(name + ".type", "kills");
            Location loc = LocationUtil.deserialize(lbConfig.getString(name + ".location"));
            if (loc != null) {
                LeaderboardEntry entry = new LeaderboardEntry(name, type, loc);
                leaderboards.put(name.toLowerCase(), entry);
            }
        }
        plugin.getLogger().info("Loaded " + leaderboards.size() + " leaderboards.");
    }

    public void placeLeaderboard(String name, String type, Location loc) {
        removeLeaderboard(name);
        LeaderboardEntry entry = new LeaderboardEntry(name, type, loc);
        leaderboards.put(name.toLowerCase(), entry);

        lbConfig.set(name + ".type", type);
        lbConfig.set(name + ".location", LocationUtil.serialize(loc));
        try { lbConfig.save(lbFile); } catch (IOException e) { e.printStackTrace(); }

        spawnStands(entry);
    }

    public void removeLeaderboard(String name) {
        leaderboards.remove(name.toLowerCase());
        List<ArmorStand> oldStands = stands.remove(name.toLowerCase());
        if (oldStands != null) oldStands.forEach(ArmorStand::remove);
    }

    private void spawnStands(LeaderboardEntry entry) {
        List<ArmorStand> list = new ArrayList<>();
        Location base = entry.getLocation().clone();

        // Title stand
        ArmorStand title = spawnStand(base.clone().add(0, 3, 0),
                ColorUtil.color("&6&l⚔ &e" + entry.getName().toUpperCase() + " &6&l⚔"));
        list.add(title);

        // Top 10 rows
        List<Map.Entry<UUID, StatsManager.PlayerStats>> top = getTop(entry.getType(), 10);
        for (int i = 0; i < top.size(); i++) {
            Map.Entry<UUID, StatsManager.PlayerStats> e = top.get(i);
            String playerName = Bukkit.getOfflinePlayer(e.getKey()).getName();
            if (playerName == null) playerName = "Unknown";
            String val = e.getValue().getFormatted(entry.getType());
            String medal = switch (i) {
                case 0 -> "&6#1";
                case 1 -> "&7#2";
                case 2 -> "&c#3";
                default -> "&f#" + (i + 1);
            };
            String line = ColorUtil.color(medal + " &e" + playerName + " &7- &f" + val);
            list.add(spawnStand(base.clone().add(0, 2.7 - (i * 0.3), 0), line));
        }

        stands.put(entry.getName().toLowerCase(), list);
    }

    private ArmorStand spawnStand(Location loc, String name) {
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setCustomName(name);
        stand.setCustomNameVisible(true);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setSmall(true);
        return stand;
    }

    public void refreshAll() {
        for (LeaderboardEntry entry : leaderboards.values()) {
            List<ArmorStand> old = stands.remove(entry.getName().toLowerCase());
            if (old != null) old.forEach(ArmorStand::remove);
            spawnStands(entry);
        }
    }

    private List<Map.Entry<UUID, StatsManager.PlayerStats>> getTop(String type, int limit) {
        return switch (type.toLowerCase()) {
            case "kills" -> plugin.getStatsManager().getTopByKills(limit);
            case "killstreak", "max_killstreak" -> plugin.getStatsManager().getTopByMaxKillStreak(limit);
            default -> plugin.getStatsManager().getTopByKills(limit);
        };
    }

    public Collection<LeaderboardEntry> getAll() { return leaderboards.values(); }

    public static class LeaderboardEntry {
        private final String name;
        private final String type;
        private final Location location;

        public LeaderboardEntry(String name, String type, Location location) {
            this.name = name;
            this.type = type;
            this.location = location;
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public Location getLocation() { return location; }
    }
}
