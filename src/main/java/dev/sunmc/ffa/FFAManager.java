package dev.sunmc.ffa;

import dev.sunmc.SunMc;
import dev.sunmc.arena.Arena;
import dev.sunmc.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.*;

public class FFAManager {

    private final SunMc plugin;
    private final Map<UUID, String> playerArena = new HashMap<>(); // player -> arena name
    private final Map<UUID, org.bukkit.inventory.ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, org.bukkit.Location> savedLocations = new HashMap<>();

    public FFAManager(SunMc plugin) {
        this.plugin = plugin;
    }

    public void joinFFA(Player player, Arena arena) {
        if (plugin.getDuelManager().isInMatch(player.getUniqueId())) {
            MessageUtil.send(player, "already-in-match"); return;
        }
        if (playerArena.containsKey(player.getUniqueId())) {
            MessageUtil.sendRaw(player, "&cYou are already in an FFA arena."); return;
        }
        if (!arena.isFfaMode() || arena.getFfaSpawn() == null) {
            MessageUtil.sendRaw(player, "&cThat is not a valid FFA arena."); return;
        }

        savedInventories.put(player.getUniqueId(), player.getInventory().getContents().clone());
        savedLocations.put(player.getUniqueId(), player.getLocation().clone());

        playerArena.put(player.getUniqueId(), arena.getName().toLowerCase());
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);

        // Apply spawn protection
        int protection = plugin.getConfig().getInt("ffa.spawn-protection-seconds", 3);
        player.teleport(arena.getFfaSpawn());
        MessageUtil.send(player, "ffa-joined", "{arena}", arena.getName());

        // Give invincibility briefly
        player.setInvulnerable(true);
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.setInvulnerable(false), 20L * protection);
    }

    public void leaveFFA(Player player) {
        String arenaName = playerArena.remove(player.getUniqueId());
        if (arenaName == null) { MessageUtil.sendRaw(player, "&cYou are not in an FFA arena."); return; }
        restorePlayer(player);
        MessageUtil.send(player, "ffa-left");
    }

    public void handleDeath(Player dead) {
        if (!playerArena.containsKey(dead.getUniqueId())) return;
        String arenaName = playerArena.get(dead.getUniqueId());
        plugin.getStatsManager().addDeath(dead.getUniqueId());
        plugin.getStatsManager().resetKillStreak(dead.getUniqueId());

        // Respawn at FFA spawn
        Arena arena = plugin.getArenaManager().getArena(arenaName).orElse(null);
        if (arena != null && arena.getFfaSpawn() != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                dead.teleport(arena.getFfaSpawn());
                dead.setHealth(dead.getMaxHealth());
                dead.setFoodLevel(20);
                dead.setInvulnerable(true);
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> dead.setInvulnerable(false),
                        20L * plugin.getConfig().getInt("ffa.spawn-protection-seconds", 3));
            }, 20L);
        }
    }

    public void handleKill(Player killer, Player victim) {
        if (!playerArena.containsKey(killer.getUniqueId())) return;
        plugin.getStatsManager().addKill(killer.getUniqueId());
        plugin.getStatsManager().addKillStreak(killer.getUniqueId());
    }

    public void evacuateAll() {
        new HashSet<>(playerArena.keySet()).forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            playerArena.remove(uuid);
            if (p != null) restorePlayer(p);
        });
    }

    private void restorePlayer(Player player) {
        org.bukkit.inventory.ItemStack[] saved = savedInventories.remove(player.getUniqueId());
        org.bukkit.Location loc = savedLocations.remove(player.getUniqueId());
        player.getInventory().clear();
        if (saved != null) player.getInventory().setContents(saved);
        if (loc != null) player.teleport(loc);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setInvulnerable(false);
    }

    public boolean isInFFA(UUID uuid) { return playerArena.containsKey(uuid); }
    public Optional<String> getPlayerArena(UUID uuid) { return Optional.ofNullable(playerArena.get(uuid)); }
    public List<UUID> getPlayersInArena(String arenaName) {
        List<UUID> result = new ArrayList<>();
        playerArena.forEach((u, a) -> { if (a.equalsIgnoreCase(arenaName)) result.add(u); });
        return result;
    }
}
