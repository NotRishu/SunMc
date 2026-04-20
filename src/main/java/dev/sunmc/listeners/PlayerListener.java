package dev.sunmc.listeners;

import dev.sunmc.SunMc;
import dev.sunmc.duel.DuelMatch;
import dev.sunmc.duel.MatchState;
import dev.sunmc.utils.LocationUtil;
import dev.sunmc.utils.MessageUtil;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerListener implements Listener {

    private final SunMc plugin;

    public PlayerListener(SunMc plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.getScoreboardManager().setup(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        // End match if in one
        DuelMatch match = plugin.getDuelManager().getMatch(player.getUniqueId());
        if (match != null) plugin.getDuelManager().endMatch(match);
        // Leave FFA
        if (plugin.getFFAManager().isInFFA(player.getUniqueId())) {
            plugin.getFFAManager().leaveFFA(player);
        }
        plugin.getScoreboardManager().remove(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();
        e.setDeathMessage(null); // Suppress vanilla death message

        // Duel death
        DuelMatch match = plugin.getDuelManager().getMatch(dead.getUniqueId());
        if (match != null && match.getState() == MatchState.IN_PROGRESS) {
            e.setKeepInventory(true);
            e.getDrops().clear();
            e.setDroppedExp(0);
            plugin.getDuelManager().handleDeath(dead);
            return;
        }

        // FFA death
        if (plugin.getFFAManager().isInFFA(dead.getUniqueId())) {
            e.setKeepInventory(true);
            Player killer = dead.getKiller();
            if (killer != null) plugin.getFFAManager().handleKill(killer, dead);
            plugin.getFFAManager().handleDeath(dead);
            e.getDrops().clear();
            e.setDroppedExp(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        DuelMatch match = plugin.getDuelManager().getMatch(player.getUniqueId());
        if (match == null || match.getState() != MatchState.IN_PROGRESS) return;

        if (!plugin.getConfig().getBoolean("arena.boundary-enforce", true)) return;

        var arena = match.getArena();
        if (arena == null) return;

        if (!arena.inBoundary(e.getTo())) {
            // Teleport back to nearest boundary point
            e.setTo(e.getFrom());
            player.sendActionBar(dev.sunmc.utils.ColorUtil.component("&cYou cannot leave the battle region!"));
        }
    }

    // ===== Ender Pearl Glitch Fix =====
    @EventHandler(priority = EventPriority.HIGH)
    public void onPearlLand(PlayerTeleportEvent e) {
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        Player player = e.getPlayer();
        var dest = e.getTo();
        if (dest == null) return;

        if (LocationUtil.isInsideBlock(dest)) {
            var safe = LocationUtil.findSafe(dest);
            e.setTo(safe);
            player.sendActionBar(dev.sunmc.utils.ColorUtil.component("&6Pearl glitch prevented!"));
        }
    }
}
