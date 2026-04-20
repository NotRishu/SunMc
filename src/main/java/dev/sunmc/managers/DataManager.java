package dev.sunmc.managers;

import dev.sunmc.SunMc;

import java.io.File;

public class DataManager {

    private final SunMc plugin;

    public DataManager(SunMc plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
    }

    public void saveAll() {
        plugin.getStatsManager().saveAll();
        plugin.getArenaManager().saveAll();
        plugin.getKitManager().saveKits();
    }
}
