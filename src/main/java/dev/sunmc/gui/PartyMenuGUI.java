package dev.sunmc.gui;

import dev.sunmc.SunMc;
import dev.sunmc.party.Party;
import dev.sunmc.utils.ColorUtil;
import dev.sunmc.utils.ItemBuilder;
import dev.sunmc.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public class PartyMenuGUI {

    public static void open(Player player) {
        SunMc plugin = SunMc.getInstance();
        Optional<Party> partyOpt = plugin.getPartyManager().getParty(player.getUniqueId());
        if (partyOpt.isEmpty()) {
            // Offer to create party
            MessageUtil.sendRaw(player, "&6You are not in a party. Use &e/party create &6to start one.");
            return;
        }

        Party party = partyOpt.get();
        String title = ColorUtil.color(plugin.getConfig().getString("gui.party-menu.title", "&c&lParty Menu"));
        int rows = plugin.getConfig().getInt("gui.party-menu.rows", 3);
        Inventory inv = Bukkit.createInventory(null, rows * 9, ColorUtil.component(title));

        // Background
        ItemStack bg = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("&0").build();
        for (int i = 0; i < rows * 9; i++) inv.setItem(i, bg);

        // Party info display
        inv.setItem(4, new ItemBuilder(Material.PLAYER_HEAD).name("&6&lParty (" + party.size() + " members)")
                .lore("&7Leader: &e" + Bukkit.getOfflinePlayer(party.getLeader()).getName(),
                        "&7Members: &f" + party.size()).build());

        if (party.isLeader(player.getUniqueId())) {
            // Split
            int splitSlot = plugin.getConfig().getInt("gui.party-menu.split-slot", 10);
            String splitName = plugin.getConfig().getString("gui.party-menu.split-item.name", "&e&lParty Split");
            List<String> splitLore = plugin.getConfig().getStringList("gui.party-menu.split-item.lore");
            inv.setItem(splitSlot, new ItemBuilder(Material.BLAZE_ROD).name(splitName).lore(splitLore).build());

            // Fight
            int fightSlot = plugin.getConfig().getInt("gui.party-menu.fight-slot", 13);
            String fightName = plugin.getConfig().getString("gui.party-menu.fight-item.name", "&c&lParty Fight");
            List<String> fightLore = plugin.getConfig().getStringList("gui.party-menu.fight-item.lore");
            inv.setItem(fightSlot, new ItemBuilder(Material.IRON_SWORD).name(fightName).lore(fightLore).build());

            // vs Party
            int vsSlot = plugin.getConfig().getInt("gui.party-menu.vs-slot", 16);
            String vsName = plugin.getConfig().getString("gui.party-menu.vs-item.name", "&d&lParty vs Party");
            List<String> vsLore = plugin.getConfig().getStringList("gui.party-menu.vs-item.lore");
            inv.setItem(vsSlot, new ItemBuilder(Material.NETHER_STAR).name(vsName).lore(vsLore).build());
        }

        // Leave (bottom row)
        inv.setItem(rows * 9 - 1, new ItemBuilder(Material.BARRIER)
                .name("&c&lLeave Party")
                .lore("&7Click to leave the party.").build());

        player.openInventory(inv);
    }

    public static void handleClick(Player player, int slot, Inventory inv) {
        SunMc plugin = SunMc.getInstance();
        Optional<Party> partyOpt = plugin.getPartyManager().getParty(player.getUniqueId());
        if (partyOpt.isEmpty()) { player.closeInventory(); return; }
        Party party = partyOpt.get();

        int rows = plugin.getConfig().getInt("gui.party-menu.rows", 3);
        int splitSlot = plugin.getConfig().getInt("gui.party-menu.split-slot", 10);
        int fightSlot = plugin.getConfig().getInt("gui.party-menu.fight-slot", 13);
        int vsSlot = plugin.getConfig().getInt("gui.party-menu.vs-slot", 16);
        int leaveSlot = rows * 9 - 1;

        if (slot == splitSlot && party.isLeader(player.getUniqueId())) {
            player.closeInventory();
            handlePartySplit(player, party);
        } else if (slot == fightSlot && party.isLeader(player.getUniqueId())) {
            player.closeInventory();
            MessageUtil.sendRaw(player, "&eSetting up a party fight... (feature: internal party vs)");
        } else if (slot == vsSlot && party.isLeader(player.getUniqueId())) {
            player.closeInventory();
            MessageUtil.sendRaw(player, "&dParty vs Party: Challenge another party leader by running &e/party vs <player>&d.");
        } else if (slot == leaveSlot) {
            player.closeInventory();
            plugin.getPartyManager().leave(player);
        }
    }

    private static void handlePartySplit(Player player, Party party) {
        SunMc plugin = SunMc.getInstance();
        if (party.size() < 2) {
            MessageUtil.sendRaw(player, "&cYou need at least 2 members to split the party.");
            return;
        }
        var teams = party.split();
        plugin.getPartyManager().broadcastToParty(party, "&6Party split into two teams:");
        StringBuilder t1 = new StringBuilder("&eTeam A: ");
        StringBuilder t2 = new StringBuilder("&cTeam B: ");
        teams.get(0).forEach(u -> t1.append(Bukkit.getOfflinePlayer(u).getName()).append(", "));
        teams.get(1).forEach(u -> t2.append(Bukkit.getOfflinePlayer(u).getName()).append(", "));
        plugin.getPartyManager().broadcastToParty(party, t1.toString().replaceAll(", $", ""));
        plugin.getPartyManager().broadcastToParty(party, t2.toString().replaceAll(", $", ""));
    }
}
