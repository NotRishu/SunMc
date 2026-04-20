package dev.sunmc.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;

public class ArenaSnapshot {

    private final Map<String, BlockData> blocks = new HashMap<>();
    private final Location min;
    private final Location max;

    public ArenaSnapshot(Location min, Location max) {
        this.min = min;
        this.max = max;
        capture();
    }

    private void capture() {
        World world = min.getWorld();
        int minX = (int) Math.min(min.getX(), max.getX());
        int minY = (int) Math.min(min.getY(), max.getY());
        int minZ = (int) Math.min(min.getZ(), max.getZ());
        int maxX = (int) Math.max(min.getX(), max.getX());
        int maxY = (int) Math.max(min.getY(), max.getY());
        int maxZ = (int) Math.max(min.getZ(), max.getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR) {
                        blocks.put(x + "," + y + "," + z, block.getBlockData().clone());
                    }
                }
            }
        }
    }

    public void restore(Runnable onComplete) {
        World world = min.getWorld();
        int minX = (int) Math.min(min.getX(), max.getX());
        int minY = (int) Math.min(min.getY(), max.getY());
        int minZ = (int) Math.min(min.getZ(), max.getZ());
        int maxX = (int) Math.max(min.getX(), max.getX());
        int maxY = (int) Math.max(min.getY(), max.getY());
        int maxZ = (int) Math.max(min.getZ(), max.getZ());

        // Clear first
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }

        // Restore
        for (Map.Entry<String, BlockData> entry : blocks.entrySet()) {
            String[] parts = entry.getKey().split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            Block block = world.getBlockAt(x, y, z);
            block.setBlockData(entry.getValue().clone(), false);
        }

        if (onComplete != null) onComplete.run();
    }

    public boolean hasSnapshot() {
        return !blocks.isEmpty();
    }
}
