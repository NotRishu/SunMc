package dev.sunmc;

import dev.sunmc.arena.ArenaManager;
import dev.sunmc.commands.*;
import dev.sunmc.duel.DuelManager;
import dev.sunmc.ffa.FFAManager;
import dev.sunmc.kit.KitManager;
import dev.sunmc.leaderboard.LeaderboardManager;
import dev.sunmc.listeners.*;
import dev.sunmc.managers.DataManager;
import dev.sunmc.managers.StatsManager;
import dev.sunmc.party.PartyManager;
import dev.sunmc.scoreboard.ScoreboardManager;
import dev.sunmc.utils.ColorUtil;
import org.bukkit.plugin.java.JavaPlugin;

public final class SunMc extends JavaPlugin {

    private static SunMc instance;

    private DataManager dataManager;
    private ArenaManager arenaManager;
    private KitManager kitManager;
    private DuelManager duelManager;
    private FFAManager ffaManager;
    private PartyManager partyManager;
    private ScoreboardManager scoreboardManager;
    private LeaderboardManager leaderboardManager;
    private StatsManager statsManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getLogger().info(ColorUtil.strip("&6Loading SunMc v" + getDescription().getVersion() + "..."));

        // Managers
        this.dataManager = new DataManager(this);
        this.statsManager = new StatsManager(this);
        this.arenaManager = new ArenaManager(this);
        this.kitManager = new KitManager(this);
        this.duelManager = new DuelManager(this);
        this.ffaManager = new FFAManager(this);
        this.partyManager = new PartyManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.leaderboardManager = new LeaderboardManager(this);

        registerListeners();
        registerCommands();

        getLogger().info("SunMc enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (duelManager != null) duelManager.endAllMatches();
        if (ffaManager != null) ffaManager.evacuateAll();
        if (arenaManager != null) arenaManager.saveAll();
        if (dataManager != null) dataManager.saveAll();
        getLogger().info("SunMc disabled.");
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new InventoryListener(this), this);
        pm.registerEvents(new DuelListener(this), this);
        pm.registerEvents(new ArenaListener(this), this);
        pm.registerEvents(new PartyListener(this), this);
        pm.registerEvents(new FFAListener(this), this);
    }

    private void registerCommands() {
        getCommand("arena").setExecutor(new ArenaCommand(this));
        getCommand("kit").setExecutor(new KitCommand(this));
        getCommand("duel").setExecutor(new DuelCommand(this));
        getCommand("ffa").setExecutor(new FFACommand(this));
        getCommand("party").setExecutor(new PartyCommand(this));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand(this));
        getCommand("sunmc").setExecutor(new SunMcCommand(this));
    }

    public void reload() {
        reloadConfig();
        arenaManager.reload();
        kitManager.reload();
        scoreboardManager.reload();
    }

    public static SunMc getInstance() { return instance; }
    public DataManager getDataManager() { return dataManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public KitManager getKitManager() { return kitManager; }
    public DuelManager getDuelManager() { return duelManager; }
    public FFAManager getFFAManager() { return ffaManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public StatsManager getStatsManager() { return statsManager; }
}
