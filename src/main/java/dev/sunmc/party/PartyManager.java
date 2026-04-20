package dev.sunmc.party;

import dev.sunmc.SunMc;
import dev.sunmc.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class PartyManager {

    private final SunMc plugin;
    private final Map<UUID, Party> playerParty = new HashMap<>(); // player -> party

    public PartyManager(SunMc plugin) {
        this.plugin = plugin;
    }

    public Party createParty(Player leader) {
        if (playerParty.containsKey(leader.getUniqueId())) {
            MessageUtil.sendRaw(leader, "&cYou are already in a party.");
            return null;
        }
        Party party = new Party(leader.getUniqueId());
        playerParty.put(leader.getUniqueId(), party);
        MessageUtil.send(leader, "party-created");
        return party;
    }

    public void invite(Player leader, Player target) {
        Party party = playerParty.get(leader.getUniqueId());
        if (party == null) { MessageUtil.sendRaw(leader, "&cYou are not in a party."); return; }
        if (!party.isLeader(leader.getUniqueId())) { MessageUtil.sendRaw(leader, "&cOnly the party leader can invite players."); return; }
        if (party.isMember(target.getUniqueId())) { MessageUtil.sendRaw(leader, "&c" + target.getName() + " is already in your party."); return; }
        int maxSize = plugin.getConfig().getInt("party.max-size", 10);
        if (party.size() >= maxSize) { MessageUtil.send(leader, "party-full"); return; }
        party.invite(target.getUniqueId());
        MessageUtil.sendRaw(leader, "&aInvited &e" + target.getName() + " &ato the party.");
        MessageUtil.sendRaw(target, "&e" + leader.getName() + " &ainvited you to their party! &7(/party accept)");
    }

    public void acceptInvite(Player player) {
        Party party = findPartyWithInvite(player.getUniqueId());
        if (party == null) { MessageUtil.sendRaw(player, "&cYou have no pending party invite."); return; }
        party.addMember(player.getUniqueId());
        playerParty.put(player.getUniqueId(), party);
        broadcastToParty(party, "&e" + player.getName() + " &ajoined the party!");
    }

    public void leave(Player player) {
        Party party = playerParty.get(player.getUniqueId());
        if (party == null) { MessageUtil.send(player, "party-not-found"); return; }
        playerParty.remove(player.getUniqueId());
        party.removeMember(player.getUniqueId());
        broadcastToParty(party, "&e" + player.getName() + " &cleft the party.");
        if (party.size() == 0) dissolveParty(party);
        MessageUtil.sendRaw(player, "&cYou left the party.");
    }

    public void dissolveParty(Party party) {
        for (UUID uuid : party.getMembers()) {
            playerParty.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) MessageUtil.sendRaw(p, "&cThe party has been dissolved.");
        }
    }

    public void broadcastToParty(Party party, String message) {
        for (UUID uuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) MessageUtil.sendRaw(p, message);
        }
    }

    private Party findPartyWithInvite(UUID uuid) {
        return playerParty.values().stream()
                .distinct()
                .filter(p -> p.isInvited(uuid))
                .findFirst()
                .orElse(null);
    }

    public Optional<Party> getParty(UUID uuid) {
        return Optional.ofNullable(playerParty.get(uuid));
    }

    public boolean isInParty(UUID uuid) { return playerParty.containsKey(uuid); }
}
