package dev.sunmc.arena;

import dev.sunmc.utils.LocationUtil;
import org.bukkit.Location;

import java.util.UUID;

public class Arena {

    private final String name;
    private Location spawn1;
    private Location spawn2;
    private Location ffaSpawn;
    private Location boundaryMin;
    private Location boundaryMax;
    private boolean autoRegen;
    private boolean ffaMode;
    private ArenaState state;
    private String assignedKit;
    private UUID matchId;

    public Arena(String name) {
        this.name = name;
        this.autoRegen = true;
        this.ffaMode = false;
        this.state = ArenaState.AVAILABLE;
    }

    public boolean inBoundary(Location loc) {
        if (boundaryMin == null || boundaryMax == null) return true;
        if (!loc.getWorld().equals(boundaryMin.getWorld())) return false;
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= Math.min(boundaryMin.getX(), boundaryMax.getX())
                && x <= Math.max(boundaryMin.getX(), boundaryMax.getX())
                && y >= Math.min(boundaryMin.getY(), boundaryMax.getY())
                && y <= Math.max(boundaryMin.getY(), boundaryMax.getY())
                && z >= Math.min(boundaryMin.getZ(), boundaryMax.getZ())
                && z <= Math.max(boundaryMin.getZ(), boundaryMax.getZ());
    }

    public boolean isAvailable() { return state == ArenaState.AVAILABLE; }

    // --- Getters/Setters ---
    public String getName() { return name; }
    public Location getSpawn1() { return spawn1; }
    public void setSpawn1(Location spawn1) { this.spawn1 = spawn1; }
    public Location getSpawn2() { return spawn2; }
    public void setSpawn2(Location spawn2) { this.spawn2 = spawn2; }
    public Location getFfaSpawn() { return ffaSpawn; }
    public void setFfaSpawn(Location ffaSpawn) { this.ffaSpawn = ffaSpawn; }
    public Location getBoundaryMin() { return boundaryMin; }
    public void setBoundaryMin(Location boundaryMin) { this.boundaryMin = boundaryMin; }
    public Location getBoundaryMax() { return boundaryMax; }
    public void setBoundaryMax(Location boundaryMax) { this.boundaryMax = boundaryMax; }
    public boolean isAutoRegen() { return autoRegen; }
    public void setAutoRegen(boolean autoRegen) { this.autoRegen = autoRegen; }
    public boolean isFfaMode() { return ffaMode; }
    public void setFfaMode(boolean ffaMode) { this.ffaMode = ffaMode; }
    public ArenaState getState() { return state; }
    public void setState(ArenaState state) { this.state = state; }
    public String getAssignedKit() { return assignedKit; }
    public void setAssignedKit(String assignedKit) { this.assignedKit = assignedKit; }
    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }
}
