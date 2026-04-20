package dev.sunmc.gui;

import dev.sunmc.SunMc;
import dev.sunmc.kit.KitLoadout;
import dev.sunmc.utils.ColorUtil;
import dev.sunmc.utils.ItemBuilder;
import dev.sunmc.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class LoadoutSelectorGUI {

    public static void open(Player player, String kitName) {
        SunMc plugin = SunMc.getInstance();
        String title = ColorUtil.color(plugin.getConfig().getString("gui.loadout-selector.title", "&e&lSelect Loadout"));
        int maxLoadouts = plugin.getConfig().getInt("kit.default-loadouts", 3);
        Inventory inv = Bukkit.createInventory(null, 9, ColorUtil.component(title));

        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("&8").build();
        for (int i = 0; i < 9; i++) inv.setItem(i, filler);

        List<KitLoadout> loadouts = plugin.getKitManager().getPlayerLoadouts(player.getUniqueId(), kitName);
        int currentSlot = plugin.getKitManager().getActiveLoadoutSlot(player.getUniqueId());

        for (int i = 1; i <= maxLoadouts; i++) {
            final int slot = i;
            boolean hasSave = loadouts.stream().anyMatch(l -> l.getSlot() == slot);
            boolean active = currentSlot == slot;

            ItemBuilder builder = new ItemBuilder(hasSave ? Material.BOOK : Material.PAPER)
                    .name((active ? "&a&l" : "&e") + "Loadout #" + slot)
                    .lore(hasSave ? "&7Saved loadout" : "&7Empty — will use defaults",
                            active ? "&aCurrently active" : "&7Click to select");
            if (active) builder.glow();

            inv.setItem(i - 1, builder.build());
        }

        player.openInventory(inv);
    }

    public static void handleClick(Player player, int slot, String kitName) {
        SunMc plugin = SunMc.getInstance();
        int maxLoadouts = plugin.getConfig().getInt("kit.default-loadouts", 3);
        if (slot < 0 || slot >= maxLoadouts) return;

        int loadoutSlot = slot + 1;
        plugin.getKitManager().applyLoadout(player, kitName, loadoutSlot);
        MessageUtil.send(player, "loadout-loaded", "{loadout}", String.valueOf(loadoutSlot));
        player.closeInventory();
    }
}
