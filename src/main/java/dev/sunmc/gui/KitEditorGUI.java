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
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;

public class KitEditorGUI {

    private static final String IDENTIFIER = "§0§1§2§3"; // hidden color code marker

    public static void open(Player player, Kit kit) {
        SunMc plugin = SunMc.getInstance();
        String title = ColorUtil.color(
                plugin.getConfig().getString("gui.kit-editor.title", "&c&lKit Editor &8| &e{kit}")
                        .replace("{kit}", kit.getDisplayName())) + IDENTIFIER;

        Inventory inv = Bukkit.createInventory(null, 54, ColorUtil.component(title));

        // Fill border with gray glass panes
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("&8").build();
        for (int i = 36; i < 54; i++) inv.setItem(i, border);

        // Load current loadout if any
        int loadoutSlot = plugin.getKitManager().getActiveLoadoutSlot(player.getUniqueId());
        var loadouts = plugin.getKitManager().getPlayerLoadouts(player.getUniqueId(), kit.getName());
        var loadoutOpt = loadouts.stream().filter(l -> l.getSlot() == loadoutSlot).findFirst();

        if (loadoutOpt.isPresent()) {
            var loadout = loadoutOpt.get();
            // Armor row (slots 36-39)
            ItemStack[] armor = loadout.getArmor();
            for (int i = 0; i < 4 && armor != null; i++) {
                if (armor[i] != null) inv.setItem(36 + i, armor[i]);
            }
            // Main inventory (slots 0-35)
            ItemStack[] contents = loadout.getContents();
            if (contents != null) {
                for (int i = 0; i < Math.min(contents.length, 36); i++) {
                    if (contents[i] != null) inv.setItem(i, contents[i]);
                }
            }
            // Offhand at slot 40
            if (loadout.getOffhand() != null) inv.setItem(40, loadout.getOffhand());
        }

        // Action buttons
        int saveSlot = plugin.getConfig().getInt("gui.kit-editor.save-slot", 49);
        int resetSlot = plugin.getConfig().getInt("gui.kit-editor.reset-slot", 45);
        int leaveSlot = plugin.getConfig().getInt("gui.kit-editor.leave-slot", 53);

        Material saveMat = getMaterial(plugin, "gui.kit-editor.save-item.material", "LIME_DYE");
        String saveName = plugin.getConfig().getString("gui.kit-editor.save-item.name", "&a&lSave Kit");
        inv.setItem(saveSlot, new ItemBuilder(saveMat).name(saveName).glow().build());

        Material resetMat = getMaterial(plugin, "gui.kit-editor.reset-item.material", "RED_DYE");
        String resetName = plugin.getConfig().getString("gui.kit-editor.reset-item.name", "&c&lReset Kit");
        inv.setItem(resetSlot, new ItemBuilder(resetMat).name(resetName).build());

        Material leaveMat = getMaterial(plugin, "gui.kit-editor.leave-item.material", "BARRIER");
        String leaveName = plugin.getConfig().getString("gui.kit-editor.leave-item.name", "&c&lLeave Editor");
        inv.setItem(leaveSlot, new ItemBuilder(leaveMat).name(leaveName).build());

        // Labels for armor row
        inv.setItem(41, new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE).name("&bOffhand →").build());
        inv.setItem(42, new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE).name("&9Armor Slots →").build());

        player.openInventory(inv);
    }

    public static boolean isKitEditor(Inventory inv) {
        if (inv == null || inv.getSize() != 54) return false;
        String title = inv.getLocation() != null ? "" : "";
        // Check by size and button presence (reliable)
        SunMc plugin = SunMc.getInstance();
        int saveSlot = plugin.getConfig().getInt("gui.kit-editor.save-slot", 49);
        ItemStack saveItem = inv.getItem(saveSlot);
        if (saveItem == null) return false;
        String saveName = plugin.getConfig().getString("gui.kit-editor.save-item.name", "&a&lSave Kit");
        return saveItem.hasItemMeta() && saveItem.getItemMeta().hasDisplayName();
    }

    public static void handleClick(Player player, Kit kit, int slot, Inventory inv) {
        SunMc plugin = SunMc.getInstance();
        int saveSlot = plugin.getConfig().getInt("gui.kit-editor.save-slot", 49);
        int resetSlot = plugin.getConfig().getInt("gui.kit-editor.reset-slot", 45);
        int leaveSlot = plugin.getConfig().getInt("gui.kit-editor.leave-slot", 53);

        if (slot == saveSlot) {
            saveKit(player, kit, inv);
            MessageUtil.send(player, "loadout-saved");
            player.closeInventory();
        } else if (slot == resetSlot) {
            for (int i = 0; i < 36; i++) inv.setItem(i, null);
            inv.setItem(36, null); inv.setItem(37, null);
            inv.setItem(38, null); inv.setItem(39, null);
            inv.setItem(40, null);
            MessageUtil.sendRaw(player, "&cKit reset.");
        } else if (slot == leaveSlot) {
            player.closeInventory();
        }
    }

    private static void saveKit(Player player, Kit kit, Inventory inv) {
        SunMc plugin = SunMc.getInstance();
        // Collect from GUI
        ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i < 36; i++) contents[i] = inv.getItem(i);

        ItemStack[] armor = new ItemStack[]{
                inv.getItem(39), // helmet
                inv.getItem(38), // chestplate
                inv.getItem(37), // leggings
                inv.getItem(36)  // boots
        };
        ItemStack offhand = inv.getItem(40);

        // Use a temp PlayerInventory by applying and saving
        int slot = plugin.getKitManager().getActiveLoadoutSlot(player.getUniqueId());
        PlayerInventory playerInv = player.getInventory();
        ItemStack[] oldContents = playerInv.getContents().clone();

        playerInv.setStorageContents(contents);
        playerInv.setArmorContents(armor);
        if (offhand != null) playerInv.setItemInOffHand(offhand);

        plugin.getKitManager().saveLoadout(player.getUniqueId(), kit.getName(), slot, playerInv);
        playerInv.setContents(oldContents);
    }

    private static Material getMaterial(SunMc plugin, String path, String def) {
        try {
            return Material.valueOf(plugin.getConfig().getString(path, def).toUpperCase());
        } catch (Exception e) {
            return Material.valueOf(def);
        }
    }
}
