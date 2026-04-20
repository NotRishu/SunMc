package dev.sunmc.listeners;

import dev.sunmc.SunMc;
import dev.sunmc.duel.DuelMatch;
import dev.sunmc.duel.MatchState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class DuelListener implements Listener {

    private final SunMc plugin;

    public DuelListener(SunMc plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player attacker)) return;

        boolean victimInDuel = plugin.getDuelManager().isInMatch(victim.getUniqueId());
        boolean attackerInDuel = plugin.getDuelManager().isInMatch(attacker.getUniqueId());

        if (victimInDuel && attackerInDuel) {
            DuelMatch matchV = plugin.getDuelManager().getMatch(victim.getUniqueId());
            DuelMatch matchA = plugin.getDuelManager().getMatch(attacker.getUniqueId());
            // Only allow damage within the same match
            if (matchV == null || !matchV.getMatchId().equals(matchA != null ? matchA.getMatchId() : null)) {
                e.setCancelled(true);
            } else if (matchV.getState() != MatchState.IN_PROGRESS) {
                e.setCancelled(true);
            }
            return;
        }

        // Prevent attacking players in different contexts
        if (victimInDuel || attackerInDuel) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        DuelMatch match = plugin.getDuelManager().getMatch(player.getUniqueId());
        if (match == null) return;
        if (match.getKit() == null) return;

        // Extra items can be dropped; core kit items cannot
        ItemStack dropped = e.getItemDrop().getItemStack();
        boolean isExtra = match.getKit().getExtraItems().stream()
                .anyMatch(item -> item != null && item.isSimilar(dropped));

        if (!isExtra) {
            e.setCancelled(true);
        }
    }
}
