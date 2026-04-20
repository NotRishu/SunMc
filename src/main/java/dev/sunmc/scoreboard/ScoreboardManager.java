package dev.sunmc.scoreboard;

import dev.sunmc.SunMc;
import dev.sunmc.managers.StatsManager;
import dev.sunmc.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;

public class ScoreboardManager {

    private final SunMc plugin;
    private final Map<UUID, Scoreboard> playerBoards = new HashMap<>();
    private BukkitTask updateTask;

    public ScoreboardManager(SunMc plugin) {
        this.plugin = plugin;
        if (plugin.getConfig().getBoolean("scoreboard.enabled", true)) {
            start();
        }
    }

    private void start() {
        long interval = plugin.getConfig().getLong("scoreboard.update-interval-ticks", 20L);
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () ->
                Bukkit.getOnlinePlayers().forEach(this::update), interval, interval);
    }

    public void reload() {
        if (updateTask != null) updateTask.cancel();
        playerBoards.clear();
        Bukkit.getOnlinePlayers().forEach(p -> p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard()));
        if (plugin.getConfig().getBoolean("scoreboard.enabled", true)) start();
    }

    public void setup(Player player) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        String rawTitle = plugin.getConfig().getString("scoreboard.title", "&c&l⚔ &6SunMC &c&l⚔");
        Objective obj = board.registerNewObjective("sunmc", Criteria.DUMMY,
                ColorUtil.component(rawTitle));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        playerBoards.put(player.getUniqueId(), board);
        player.setScoreboard(board);
        update(player);
    }

    public void remove(Player player) {
        playerBoards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    private void update(Player player) {
        Scoreboard board = playerBoards.get(player.getUniqueId());
        if (board == null) { setup(player); return; }
        Objective obj = board.getObjective("sunmc");
        if (obj == null) return;

        StatsManager.PlayerStats s = plugin.getStatsManager().getStats(player.getUniqueId());
        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");

        // Clear old scores
        for (String entry : new HashSet<>(board.getEntries())) board.resetScores(entry);

        int score = lines.size();
        for (String line : lines) {
            String formatted = applyPlaceholders(line, player, s);
            String coloredLine = ColorUtil.color(formatted);
            // Pad duplicates to ensure uniqueness
            while (board.getEntries().contains(coloredLine)) coloredLine += " ";
            obj.getScore(coloredLine).setScore(score--);
        }
    }

    private String applyPlaceholders(String line, Player player, StatsManager.PlayerStats s) {
        return line
                .replace("{player}", player.getName())
                .replace("{kills}", String.valueOf(s.kills))
                .replace("{deaths}", String.valueOf(s.deaths))
                .replace("{killstreak}", String.valueOf(s.killStreak))
                .replace("{max_killstreak}", String.valueOf(s.maxKillStreak))
                .replace("{matches}", String.valueOf(s.matches));
    }
}
