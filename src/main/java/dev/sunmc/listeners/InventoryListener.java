package dev.sunmc.listeners;

import dev.sunmc.SunMc;
import dev.sunmc.duel.DuelMatch;
import dev.sunmc.gui.*;
import dev.sunmc.kit.Kit;
import dev.sunmc.utils.ColorUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {

    private final SunMc plugin;

    public InventoryListener(SunMc plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        Inventory inv = e.getInventory();
        String title = getTitle(inv);
        int slot = e.getRawSlot();

        if (isTitle(title, "Kit Editor")) {
            handleKitEditor(e, player, inv, slot);
        } else if (isTitle(title, "Duel Options")) {
            e.setCancelled(true);
            DuelPropsGUI.handleClick(player, slot, inv);
        } else if (isTitle(title, "Select Loadout")) {
            e.setCancelled(true);
            DuelMatch match = plugin.getDuelManager().getMatch(player.getUniqueId());
            String kitName = match != null && match.getKit() != null
                    ? match.getKit().getName()
                    : plugin.getKitManager().getActiveKit(player.getUniqueId()).orElse(null);
            if (kitName != null) LoadoutSelectorGUI.handleClick(player, slot, kitName);
        } else if (isTitle(title, "Party Menu")) {
            e.setCancelled(true);
            PartyMenuGUI.handleClick(player, slot, inv);
        } else if (isTitle(title, "Arena Editor")) {
            e.setCancelled(true);
            ArenaCreatorGUI.handleClick(player, slot, inv);
        } else if (isTitle(title, "Extra Items")) {
            handleExtraItems(e, player, inv, slot);
        }
    }

    private void handleKitEditor(InventoryClickEvent e, Player player, Inventory inv, int slot) {
        String title = getTitle(inv);
        String kitName = extractKitName(title, "Kit Editor");
        Kit kit = plugin.getKitManager().getKit(kitName).orElse(null);
        if (kit == null) return;

        // Block clicking on action buttons in the bottom row
        int saveSlot = plugin.getConfig().getInt("gui.kit-editor.save-slot", 49);
        int resetSlot = plugin.getConfig().getInt("gui.kit-editor.reset-slot", 45);
        int leaveSlot = plugin.getConfig().getInt("gui.kit-editor.leave-slot", 53);
        if (slot == saveSlot || slot == resetSlot || slot == leaveSlot || slot == 41 || slot == 42) {
            e.setCancelled(true);
            KitEditorGUI.handleClick(player, kit, slot, inv);
        }
        // Allow free inventory manipulation in item slots (0-44)
    }

    private void handleExtraItems(InventoryClickEvent e, Player player, Inventory inv, int slot) {
        String title = getTitle(inv);
        String kitName = extractKitName(title, "Extra Items");
        Kit kit = plugin.getKitManager().getKit(kitName).orElse(null);
        if (kit == null) return;

        int rows = plugin.getConfig().getInt("gui.extra-items.rows", 4);
        int bottomStart = (rows - 1) * 9;
        if (slot >= bottomStart) {
            e.setCancelled(true);
            ExtraItemsGUI.handleClick(player, kit, slot, inv);
        }
    }

    // Party menu item — right click to open
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack item = e.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String name = PlainTextComponentSerializer.plainText()
                .serialize(item.getItemMeta().displayName());
        String partyItemName = ColorUtil.strip(
                plugin.getConfig().getString("party.item.name", "&c&lParty Menu"));

        if (name.equals(partyItemName)) {
            e.setCancelled(true);
            PartyMenuGUI.open(player);
        }
    }

    private String getTitle(Inventory inv) {
        if (inv.getViewers().isEmpty()) return "";
        return PlainTextComponentSerializer.plainText().serialize(
                inv.getViewers().get(0).getOpenInventory().title());
    }

    private boolean isTitle(String title, String keyword) {
        return title.contains(keyword);
    }

    private String extractKitName(String title, String prefix) {
        // Title format: "Kit Editor | <kitname>" or similar
        int idx = title.lastIndexOf("|");
        if (idx >= 0 && idx < title.length() - 1) {
            return title.substring(idx + 1).trim().toLowerCase();
        }
        return "";
    }
}
