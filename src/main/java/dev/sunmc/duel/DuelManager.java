package dev.sunmc.duel;

import dev.sunmc.SunMc;
import dev.sunmc.arena.Arena;
import dev.sunmc.arena.ArenaState;
import dev.sunmc.kit.Kit;
import dev.sunmc.utils.ColorUtil;
import dev.sunmc.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class DuelManager {

    private final SunMc plugin;
    private final Map<UUID, DuelMatch> activeMatches = new HashMap<>();      // player -> match
    private final Map<UUID, DuelMatch> matchById = new HashMap<>();           // matchId -> match
    private final Map<UUID, DuelRequest> pendingRequests = new HashMap<>();  // target -> request
    private final Map<UUID, BukkitTask> countdownTasks = new HashMap<>();
    private final Map<UUID, org.bukkit.inventory.ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, org.bukkit.Location> savedLocations = new HashMap<>();

    public DuelManager(SunMc plugin) {
        this.plugin = plugin;
        // Expire requests periodically
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanExpiredRequests, 20L, 20L * 5);
    }

    public void sendRequest(Player sender, Player target, String kitName) {
        if (isInMatch(sender.getUniqueId())) { MessageUtil.send(sender, "already-in-match"); return; }
        if (isInMatch(target.getUniqueId())) { MessageUtil.sendRaw(sender, "&c" + target.getName() + " is already in a match."); return; }

        DuelRequest req = new DuelRequest(sender.getUniqueId(), target.getUniqueId(), kitName,
                plugin.getConfig().getInt("duel.request-timeout", 30));
        pendingRequests.put(target.getUniqueId(), req);

        MessageUtil.send(sender, "duel-sent", "{player}", target.getName());
        MessageUtil.send(target, "duel-received", "{player}", sender.getName());

        // Auto-expire
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DuelRequest existing = pendingRequests.get(target.getUniqueId());
            if (existing != null && existing.isExpired()) {
                pendingRequests.remove(target.getUniqueId());
                if (sender.isOnline()) MessageUtil.send(sender, "duel-expired", "{player}", target.getName());
            }
        }, 20L * plugin.getConfig().getInt("duel.request-timeout", 30));
    }

    public void acceptRequest(Player target) {
        DuelRequest req = pendingRequests.remove(target.getUniqueId());
        if (req == null || req.isExpired()) { MessageUtil.sendRaw(target, "&cNo pending duel request or it has expired."); return; }

        Player sender = Bukkit.getPlayer(req.getSender());
        if (sender == null || !sender.isOnline()) { MessageUtil.sendRaw(target, "&cThe player who challenged you is offline."); return; }

        String kitName = req.getKitName();
        Kit kit = kitName != null ? plugin.getKitManager().getKit(kitName).orElse(null) : null;

        Optional<Arena> arenaOpt = plugin.getArenaManager().getAvailableArena(kitName);
        if (arenaOpt.isEmpty()) { MessageUtil.sendRaw(target, "&cNo available arenas. Please try again shortly."); return; }

        Arena arena = arenaOpt.get();
        DuelMatch match = new DuelMatch(sender.getUniqueId(), target.getUniqueId());
        match.setArena(arena);
        match.setKit(kit);
        match.setState(MatchState.COUNTDOWN);
        arena.setState(ArenaState.IN_USE);
        arena.setMatchId(match.getMatchId());

        activeMatches.put(sender.getUniqueId(), match);
        activeMatches.put(target.getUniqueId(), match);
        matchById.put(match.getMatchId(), match);

        MessageUtil.send(sender, "duel-accepted");
        MessageUtil.send(target, "duel-accepted");

        saveAndTeleport(sender, arena.getSpawn1());
        saveAndTeleport(target, arena.getSpawn2());

        startCountdown(match, sender, target);
    }

    public void declineRequest(Player target) {
        DuelRequest req = pendingRequests.remove(target.getUniqueId());
        if (req == null) { MessageUtil.sendRaw(target, "&cNo pending duel request."); return; }
        Player sender = Bukkit.getPlayer(req.getSender());
        MessageUtil.send(target, "duel-declined");
        if (sender != null) MessageUtil.sendRaw(sender, "&c" + target.getName() + " declined your duel request.");
    }

    private void startCountdown(DuelMatch match, Player p1, Player p2) {
        int seconds = plugin.getConfig().getInt("duel.countdown", 5);
        final int[] remaining = {seconds};

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (remaining[0] <= 0) {
                beginMatch(match, p1, p2);
                BukkitTask t = countdownTasks.remove(match.getMatchId());
                if (t != null) t.cancel();
                return;
            }
            String msg = ColorUtil.color("&aDuel starting in &e" + remaining[0] + "&a...");
            if (p1.isOnline()) p1.sendActionBar(ColorUtil.component(msg));
            if (p2.isOnline()) p2.sendActionBar(ColorUtil.component(msg));
            remaining[0]--;
        }, 0L, 20L);
        countdownTasks.put(match.getMatchId(), task);
    }

    private void beginMatch(DuelMatch match, Player p1, Player p2) {
        match.setState(MatchState.IN_PROGRESS);
        match.setStartTime(System.currentTimeMillis());

        // Apply kits / loadouts
        String kitName = match.getKit() != null ? match.getKit().getName() : null;
        if (kitName != null) {
            int slot = plugin.getKitManager().getActiveLoadoutSlot(p1.getUniqueId());
            plugin.getKitManager().applyLoadout(p1, kitName, slot);
            slot = plugin.getKitManager().getActiveLoadoutSlot(p2.getUniqueId());
            plugin.getKitManager().applyLoadout(p2, kitName, slot);
        }

        String msg = plugin.getConfig().getString("messages.match-start", "&a&lFight!");
        if (p1.isOnline()) p1.sendTitle(ColorUtil.color(msg), "", 5, 20, 10);
        if (p2.isOnline()) p2.sendTitle(ColorUtil.color(msg), "", 5, 20, 10);
    }

    public void handleDeath(Player dead) {
        DuelMatch match = activeMatches.get(dead.getUniqueId());
        if (match == null || match.getState() != MatchState.IN_PROGRESS) return;

        match.setState(MatchState.ENDING);
        UUID winnerUuid = match.getOpponent(dead.getUniqueId());
        match.setWinner(winnerUuid);

        Player winner = Bukkit.getPlayer(winnerUuid);
        Player loser = dead;

        if (winner != null) {
            MessageUtil.send(winner, "match-end-win");
            plugin.getStatsManager().addKill(winnerUuid);
            plugin.getStatsManager().addKillStreak(winnerUuid);
        }
        plugin.getStatsManager().addDeath(dead.getUniqueId());
        plugin.getStatsManager().resetKillStreak(dead.getUniqueId());
        MessageUtil.send(loser, "match-end-lose");

        int endDelay = plugin.getConfig().getInt("duel.end-delay", 3);
        Bukkit.getScheduler().runTaskLater(plugin, () -> endMatch(match), 20L * endDelay);
    }

    public void endMatch(DuelMatch match) {
        if (match == null) return;
        Arena arena = match.getArena();

        for (UUID uuid : List.of(match.getPlayer1(), match.getPlayer2())) {
            activeMatches.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) restorePlayer(p);
        }
        matchById.remove(match.getMatchId());

        long regenDelay = plugin.getConfig().getLong("arena.regen-delay-ticks", 20L);
        if (arena != null && arena.isAutoRegen()) {
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    plugin.getArenaManager().regenerateArena(arena, () ->
                            MessageUtil.broadcast("arena-ready", "{arena}", arena.getName())
                    ), regenDelay);
        } else if (arena != null) {
            arena.setState(ArenaState.AVAILABLE);
            arena.setMatchId(null);
        }
    }

    public void endAllMatches() {
        new HashSet<>(matchById.values()).forEach(m -> endMatch(m));
    }

    private void saveAndTeleport(Player player, org.bukkit.Location loc) {
        savedInventories.put(player.getUniqueId(), player.getInventory().getContents().clone());
        savedLocations.put(player.getUniqueId(), player.getLocation().clone());
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.teleport(loc);
    }

    private void restorePlayer(Player player) {
        org.bukkit.inventory.ItemStack[] saved = savedInventories.remove(player.getUniqueId());
        org.bukkit.Location loc = savedLocations.remove(player.getUniqueId());
        player.getInventory().clear();
        if (saved != null) player.getInventory().setContents(saved);
        if (loc != null) player.teleport(loc);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
    }

    private void cleanExpiredRequests() {
        pendingRequests.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    public boolean isInMatch(UUID uuid) { return activeMatches.containsKey(uuid); }
    public DuelMatch getMatch(UUID uuid) { return activeMatches.get(uuid); }
    public boolean hasPendingRequest(UUID uuid) { return pendingRequests.containsKey(uuid) && !pendingRequests.get(uuid).isExpired(); }
    public DuelRequest getPendingRequest(UUID uuid) { return pendingRequests.get(uuid); }
    public Collection<DuelMatch> getAllMatches() { return matchById.values(); }
}
