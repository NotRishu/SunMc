package dev.sunmc.arena;

import dev.sunmc.SunMc;
import dev.sunmc.utils.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager {

    private final SunMc plugin;
    private final Map<String, Arena> arenas = new HashMap<>();
    private final Map<String, ArenaSnapshot> snapshots = new HashMap<>();
    private File arenaFile;
    private FileConfiguration arenaConfig;

    public ArenaManager(SunMc plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        arenaFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!arenaFile.exists()) {
            try { arenaFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);
        for (String name : arenaConfig.getKeys(false)) {
            Arena arena = new Arena(name);
            arena.setSpawn1(LocationUtil.deserialize(arenaConfig.getString(name + ".spawn1")));
            arena.setSpawn2(LocationUtil.deserialize(arenaConfig.getString(name + ".spawn2")));
            arena.setFfaSpawn(LocationUtil.deserialize(arenaConfig.getString(name + ".ffa_spawn")));
            arena.setBoundaryMin(LocationUtil.deserialize(arenaConfig.getString(name + ".boundary_min")));
            arena.setBoundaryMax(LocationUtil.deserialize(arenaConfig.getString(name + ".boundary_max")));
            arena.setAutoRegen(arenaConfig.getBoolean(name + ".auto_regen", true));
            arena.setFfaMode(arenaConfig.getBoolean(name + ".ffa_mode", false));
            arena.setAssignedKit(arenaConfig.getString(name + ".kit", null));
            arenas.put(name.toLowerCase(), arena);

            // Rebuild snapshot if boundary exists
            if (arena.getBoundaryMin() != null && arena.getBoundaryMax() != null) {
                snapshots.put(name.toLowerCase(), new ArenaSnapshot(arena.getBoundaryMin(), arena.getBoundaryMax()));
            }
        }
        plugin.getLogger().info("Loaded " + arenas.size() + " arenas.");
    }

    public void saveAll() {
        arenaFile = new File(plugin.getDataFolder(), "arenas.yml");
        arenaConfig = new YamlConfiguration();
        for (Arena arena : arenas.values()) {
            String n = arena.getName();
            arenaConfig.set(n + ".spawn1", LocationUtil.serialize(arena.getSpawn1()));
            arenaConfig.set(n + ".spawn2", LocationUtil.serialize(arena.getSpawn2()));
            arenaConfig.set(n + ".ffa_spawn", LocationUtil.serialize(arena.getFfaSpawn()));
            arenaConfig.set(n + ".boundary_min", LocationUtil.serialize(arena.getBoundaryMin()));
            arenaConfig.set(n + ".boundary_max", LocationUtil.serialize(arena.getBoundaryMax()));
            arenaConfig.set(n + ".auto_regen", arena.isAutoRegen());
            arenaConfig.set(n + ".ffa_mode", arena.isFfaMode());
            arenaConfig.set(n + ".kit", arena.getAssignedKit());
        }
        try { arenaConfig.save(arenaFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public void reload() {
        arenas.clear();
        snapshots.clear();
        load();
    }

    public Arena createArena(String name) {
        Arena arena = new Arena(name.toLowerCase());
        arenas.put(name.toLowerCase(), arena);
        saveAll();
        return arena;
    }

    public boolean deleteArena(String name) {
        if (arenas.remove(name.toLowerCase()) != null) {
            snapshots.remove(name.toLowerCase());
            saveAll();
            return true;
        }
        return false;
    }

    public Optional<Arena> getArena(String name) {
        return Optional.ofNullable(arenas.get(name.toLowerCase()));
    }

    public Optional<Arena> getAvailableArena(String kit) {
        return arenas.values().stream()
                .filter(a -> !a.isFfaMode())
                .filter(Arena::isAvailable)
                .filter(a -> kit == null || kit.equals(a.getAssignedKit()) || a.getAssignedKit() == null)
                .filter(a -> a.getSpawn1() != null && a.getSpawn2() != null)
                .findFirst();
    }

    public Collection<Arena> getAllArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public void takeSnapshot(Arena arena) {
        if (arena.getBoundaryMin() != null && arena.getBoundaryMax() != null) {
            snapshots.put(arena.getName().toLowerCase(), new ArenaSnapshot(arena.getBoundaryMin(), arena.getBoundaryMax()));
        }
    }

    public void regenerateArena(Arena arena, Runnable onComplete) {
        arena.setState(ArenaState.REGENERATING);
        ArenaSnapshot snapshot = snapshots.get(arena.getName().toLowerCase());
        if (snapshot == null || !snapshot.hasSnapshot()) {
            arena.setState(ArenaState.AVAILABLE);
            if (onComplete != null) onComplete.run();
            return;
        }
        // Run async restore then sync state update
        Bukkit.getScheduler().runTask(plugin, () -> {
            snapshot.restore(() -> {
                arena.setState(ArenaState.AVAILABLE);
                arena.setMatchId(null);
                if (onComplete != null) onComplete.run();
            });
        });
    }

    /**
     * Duplicate an arena in a direction.
     * Direction: south=+Z, east=+X, west=-X
     */
    public List<Arena> duplicateArena(Arena original, String direction, int count, int spacing) {
        List<Arena> created = new ArrayList<>();
        if (original.getBoundaryMin() == null || original.getBoundaryMax() == null) return created;

        double width = Math.abs(original.getBoundaryMax().getX() - original.getBoundaryMin().getX());
        double depth = Math.abs(original.getBoundaryMax().getZ() - original.getBoundaryMin().getZ());

        double offsetX = 0, offsetZ = 0;
        switch (direction.toLowerCase()) {
            case "south" -> offsetZ = depth + spacing;
            case "east"  -> offsetX = width + spacing;
            case "west"  -> offsetX = -(width + spacing);
        }

        int existingCount = (int) arenas.keySet().stream()
                .filter(k -> k.startsWith(original.getName())).count();

        for (int i = 1; i <= count; i++) {
            double dx = offsetX * i;
            double dz = offsetZ * i;
            String newName = original.getName() + (existingCount + i);
            Arena dup = new Arena(newName);

            if (original.getSpawn1() != null)
                dup.setSpawn1(original.getSpawn1().clone().add(dx, 0, dz));
            if (original.getSpawn2() != null)
                dup.setSpawn2(original.getSpawn2().clone().add(dx, 0, dz));
            if (original.getFfaSpawn() != null)
                dup.setFfaSpawn(original.getFfaSpawn().clone().add(dx, 0, dz));
            dup.setBoundaryMin(original.getBoundaryMin().clone().add(dx, 0, dz));
            dup.setBoundaryMax(original.getBoundaryMax().clone().add(dx, 0, dz));
            dup.setAutoRegen(original.isAutoRegen());
            dup.setFfaMode(original.isFfaMode());
            dup.setAssignedKit(original.getAssignedKit());

            arenas.put(newName.toLowerCase(), dup);
            created.add(dup);
        }
        saveAll();
        return created;
    }
}
