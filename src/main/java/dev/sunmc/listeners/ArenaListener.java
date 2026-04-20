package dev.sunmc.listeners;

import dev.sunmc.SunMc;
import dev.sunmc.arena.Arena;
import dev.sunmc.duel.DuelMatch;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class ArenaListener implements Listener {

    private final SunMc plugin;

    public ArenaListener(SunMc plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        if (player.hasPermission("sunmc.admin")) return;

        // Protect arena regions unless the player is in a match (Crystal PvP allows it)
        DuelMatch match = plugin.getDuelManager().getMatch(player.getUniqueId());
        if (match == null && plugin.getFFAManager().isInFFA(player.getUniqueId())) return; // FFA: allow

        // Check if block is inside any arena boundary
        for (Arena arena : plugin.getArenaManager().getAllArenas()) {
            if (arena.getBoundaryMin() != null && arena.inBoundary(e.getBlock().getLocation())) {
                if (match == null) {
                    e.setCancelled(true); // Not in match, can't break
                }
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        if (player.hasPermission("sunmc.admin")) return;

        DuelMatch match = plugin.getDuelManager().getMatch(player.getUniqueId());
        if (match == null && plugin.getFFAManager().isInFFA(player.getUniqueId())) return;

        for (Arena arena : plugin.getArenaManager().getAllArenas()) {
            if (arena.getBoundaryMin() != null && arena.inBoundary(e.getBlock().getLocation())) {
                if (match == null) {
                    e.setCancelled(true);
                }
                return;
            }
        }
    }
}
