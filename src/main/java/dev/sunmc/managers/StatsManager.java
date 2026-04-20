package dev.sunmc.managers;

import dev.sunmc.SunMc;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StatsManager {

    private final SunMc plugin;
    private final Map<UUID, PlayerStats> stats = new HashMap<>();
    private File statsFile;
    private FileConfiguration statsConfig;

    public StatsManager(SunMc plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            try { statsFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        for (String uuidStr : statsConfig.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            PlayerStats s = new PlayerStats(uuid);
            s.kills = statsConfig.getInt(uuidStr + ".kills", 0);
            s.deaths = statsConfig.getInt(uuidStr + ".deaths", 0);
            s.killStreak = 0;
            s.maxKillStreak = statsConfig.getInt(uuidStr + ".max_killstreak", 0);
            s.matches = statsConfig.getInt(uuidStr + ".matches", 0);
            stats.put(uuid, s);
        }
    }

    public PlayerStats getStats(UUID uuid) {
        return stats.computeIfAbsent(uuid, PlayerStats::new);
    }

    public void addKill(UUID uuid) {
        getStats(uuid).kills++;
        save(uuid);
    }

    public void addDeath(UUID uuid) {
        getStats(uuid).deaths++;
        save(uuid);
    }

    public void addKillStreak(UUID uuid) {
        PlayerStats s = getStats(uuid);
        s.killStreak++;
        if (s.killStreak > s.maxKillStreak) s.maxKillStreak = s.killStreak;
        save(uuid);
    }

    public void resetKillStreak(UUID uuid) {
        getStats(uuid).killStreak = 0;
    }

    public void addMatch(UUID uuid) {
        getStats(uuid).matches++;
        save(uuid);
    }

    private void save(UUID uuid) {
        PlayerStats s = getStats(uuid);
        statsConfig.set(uuid + ".kills", s.kills);
        statsConfig.set(uuid + ".deaths", s.deaths);
        statsConfig.set(uuid + ".max_killstreak", s.maxKillStreak);
        statsConfig.set(uuid + ".matches", s.matches);
        try { statsConfig.save(statsFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public void saveAll() {
        stats.keySet().forEach(this::save);
    }

    public List<Map.Entry<UUID, PlayerStats>> getTopByKills(int limit) {
        return stats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().kills, a.getValue().kills))
                .limit(limit).toList();
    }

    public List<Map.Entry<UUID, PlayerStats>> getTopByMaxKillStreak(int limit) {
        return stats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().maxKillStreak, a.getValue().maxKillStreak))
                .limit(limit).toList();
    }

    public static class PlayerStats {
        public final UUID uuid;
        public int kills = 0;
        public int deaths = 0;
        public int killStreak = 0;
        public int maxKillStreak = 0;
        public int matches = 0;

        public PlayerStats(UUID uuid) { this.uuid = uuid; }

        public String getFormatted(String key) {
            return switch (key.toLowerCase()) {
                case "kills" -> String.valueOf(kills);
                case "deaths" -> String.valueOf(deaths);
                case "killstreak" -> String.valueOf(killStreak);
                case "max_killstreak" -> String.valueOf(maxKillStreak);
                case "matches" -> String.valueOf(matches);
                default -> "0";
            };
        }
    }
}
