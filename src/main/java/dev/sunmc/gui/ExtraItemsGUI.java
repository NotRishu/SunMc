package dev.sunmc.gui;

import dev.sunmc.SunMc;
import dev.sunmc.kit.Kit;
import dev.sunmc.utils.ColorUtil;
import dev.sunmc.utils.ItemBuilder;
import dev.sunmc.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ExtraItemsGUI {

    public static void open(Player player, Kit kit) {
        SunMc plugin = SunMc.getInstance();
        String title = ColorUtil.color(
                plugin.getConfig().getString("gui.extra-items.title", "&6&lExtra Items &8| &e{kit}")
                        .replace("{kit}", kit.getDisplayName()));
        int rows = plugin.getConfig().getInt("gui.extra-items.rows", 4);
        Inventory inv = Bukkit.createInventory(null, rows * 9, ColorUtil.component(title));

        // Bottom border row
        ItemStack border = new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).name("&8").build();
        for (int i = (rows - 1) * 9; i < rows * 9; i++) inv.setItem(i, border);

        // Load existing extra items
        List<ItemStack> extras = kit.getExtraItems();
        for (int i = 0; i < extras.size() && i < (rows - 1) * 9; i++) {
            inv.setItem(i, extras.get(i));
        }

        // Save button
        inv.setItem((rows - 1) * 9 + 4, new ItemBuilder(Material.LIME_DYE)
                .name("&a&lSave Extra Items").glow()
                .lore("&7Save the current extra items to the kit.").build());

        // Clear button
        inv.setItem((rows - 1) * 9 + 8, new ItemBuilder(Material.RED_DYE)
                .name("&c&lClear All").lore("&7Remove all extra items.").build());

        // Info
        inv.setItem((rows - 1) * 9, new ItemBuilder(Material.BOOK)
                .name("&6&lInfo")
                .lore("&7Add any items to the slots above.",
                        "&7These items will be droppable in-game.",
                        "&7Core kit items are &cnot &7droppable.").build());

        player.openInventory(inv);
    }

    public static void handleClick(Player player, Kit kit, int slot, Inventory inv) {
        SunMc plugin = SunMc.getInstance();
        int rows = plugin.getConfig().getInt("gui.extra-items.rows", 4);
        int saveSlot = (rows - 1) * 9 + 4;
        int clearSlot = (rows - 1) * 9 + 8;

        if (slot == saveSlot) {
            List<ItemStack> extras = new ArrayList<>();
            for (int i = 0; i < (rows - 1) * 9; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType() != Material.AIR) extras.add(item.clone());
            }
            kit.setExtraItems(extras);
            plugin.getKitManager().saveKits();
            MessageUtil.sendRaw(player, "&aExtra items saved for kit &e" + kit.getDisplayName() + "&a!");
            player.closeInventory();
        } else if (slot == clearSlot) {
            for (int i = 0; i < (rows - 1) * 9; i++) inv.setItem(i, null);
            kit.setExtraItems(new ArrayList<>());
            plugin.getKitManager().saveKits();
            MessageUtil.sendRaw(player, "&cExtra items cleared.");
        }
    }
}
