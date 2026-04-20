package dev.sunmc.gui;

import dev.sunmc.SunMc;
import dev.sunmc.duel.DuelMatch;
import dev.sunmc.duel.MatchState;
import dev.sunmc.utils.ColorUtil;
import dev.sunmc.utils.ItemBuilder;
import dev.sunmc.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class DuelPropsGUI {

    public static void open(Player player) {
        SunMc plugin = SunMc.getInstance();
        DuelMatch match = plugin.getDuelManager().getMatch(player.getUniqueId());
        if (match == null) { MessageUtil.send(player, "not-in-match"); return; }
        if (match.getState() != MatchState.COUNTDOWN && match.getState() != MatchState.WAITING) {
            MessageUtil.sendRaw(player, "&cYou can only change options before the match starts."); return;
        }

        String title = ColorUtil.color(plugin.getConfig().getString("gui.duel-props.title", "&c&lDuel Options"));
        int rows = plugin.getConfig().getInt("gui.duel-props.rows", 1);
        Inventory inv = Bukkit.createInventory(null, rows * 9, ColorUtil.component(title));

        // Fill with glass
        ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("&0").build();
        for (int i = 0; i < rows * 9; i++) inv.setItem(i, filler);

        int leaveSlot = plugin.getConfig().getInt("gui.duel-props.leave-slot", 0);
        Material leaveMat = mat(plugin, "gui.duel-props.leave-item.material", "RED_BED");
        String leaveName = plugin.getConfig().getString("gui.duel-props.leave-item.name", "&c&lLeave Duel");
        inv.setItem(leaveSlot, new ItemBuilder(leaveMat).name(leaveName)
                .lore("&7Click to leave the duel queue.").build());

        int loadoutSlot = plugin.getConfig().getInt("gui.duel-props.loadout-slot", 4);
        Material loadoutMat = mat(plugin, "gui.duel-props.loadout-item.material", "CHEST");
        String loadoutName = plugin.getConfig().getString("gui.duel-props.loadout-item.name", "&e&lChange Loadout");
        String kitName = match.getKit() != null ? match.getKit().getName() : null;
        int currentSlot = plugin.getKitManager().getActiveLoadoutSlot(player.getUniqueId());
        inv.setItem(loadoutSlot, new ItemBuilder(loadoutMat).name(loadoutName)
                .lore("&7Current loadout: &e#" + currentSlot,
                        "&7Click to cycle through your loadouts.").build());

        int kitSlot = plugin.getConfig().getInt("gui.duel-props.kit-slot", 8);
        Material kitMat = mat(plugin, "gui.duel-props.kit-item.material", "DIAMOND_SWORD");
        String kitItemName = plugin.getConfig().getString("gui.duel-props.kit-item.name", "&b&lChange Kit");
        String currentKit = kitName != null ? kitName : "None";
        inv.setItem(kitSlot, new ItemBuilder(kitMat).name(kitItemName)
                .lore("&7Current kit: &b" + currentKit).build());

        player.openInventory(inv);
    }

    public static void handleClick(Player player, int slot, Inventory inv) {
        SunMc plugin = SunMc.getInstance();
        DuelMatch match = plugin.getDuelManager().getMatch(player.getUniqueId());
        if (match == null) { player.closeInventory(); return; }

        int leaveSlot = plugin.getConfig().getInt("gui.duel-props.leave-slot", 0);
        int loadoutSlot = plugin.getConfig().getInt("gui.duel-props.loadout-slot", 4);

        if (slot == leaveSlot) {
            player.closeInventory();
            plugin.getDuelManager().endMatch(match);
            MessageUtil.sendRaw(player, "&cYou left the duel.");
        } else if (slot == loadoutSlot) {
            if (match.getKit() == null) { MessageUtil.sendRaw(player, "&cNo kit assigned to this duel."); return; }
            player.closeInventory();
            LoadoutSelectorGUI.open(player, match.getKit().getName());
        }
    }

    private static Material mat(SunMc plugin, String path, String def) {
        try { return Material.valueOf(plugin.getConfig().getString(path, def).toUpperCase()); }
        catch (Exception e) { return Material.valueOf(def); }
    }
}
