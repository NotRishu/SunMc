package dev.sunmc.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class LocationUtil {

    public static String serialize(Location loc) {
        if (loc == null) return null;
        return loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ()
                + ":" + loc.getYaw() + ":" + loc.getPitch();
    }

    public static Location deserialize(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] parts = s.split(":");
        if (parts.length < 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0f;
        float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0f;
        return new Location(world, x, y, z, yaw, pitch);
    }

    public static Location findSafe(Location loc) {
        if (loc == null) return null;
        World world = loc.getWorld();
        if (world == null) return loc;

        int searchRadius = 5;
        for (int dy = 0; dy <= searchRadius; dy++) {
            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    Location candidate = loc.clone().add(dx, dy, dz);
                    if (isSafe(candidate)) return candidate.add(0.5, 0, 0.5);
                }
            }
        }
        return loc;
    }

    public static boolean isSafe(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;
        Block feet = world.getBlockAt(loc);
        Block head = world.getBlockAt(loc.clone().add(0, 1, 0));
        Block floor = world.getBlockAt(loc.clone().add(0, -1, 0));
        return feet.isPassable() && head.isPassable() && floor.getType().isSolid();
    }

    public static boolean isInsideBlock(Location loc) {
        if (loc == null) return false;
        Block feet = loc.getWorld().getBlockAt(loc);
        Block head = loc.getWorld().getBlockAt(loc.clone().add(0, 1, 0));
        return !feet.isPassable() || !head.isPassable();
    }
}
