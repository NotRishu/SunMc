package dev.sunmc.gui;

import dev.sunmc.SunMc;
import dev.sunmc.arena.Arena;
import dev.sunmc.utils.ColorUtil;
import dev.sunmc.utils.ItemBuilder;
import dev.sunmc.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArenaCreatorGUI {

    // Track which arena each admin is editing
    private static final Map<UUID, String> editingArena = new HashMap<>();

    public static void open(Player player, Arena arena) {
        editingArena.put(player.getUniqueId(), arena.getName());
        SunMc plugin = SunMc.getInstance();

        String title = ColorUtil.color("&c&lArena Editor &8| &e" + arena.getName());
        Inventory inv = Bukkit.createInventory(null, 27, ColorUtil.component(title));

        ItemStack bg = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("&8").build();
        for (int i = 0; i < 27; i++) inv.setItem(i, bg);

        boolean hasSpawn1 = arena.getSpawn1() != null;
        boolean hasSpawn2 = arena.getSpawn2() != null;
        boolean hasBoundary = arena.getBoundaryMin() != null && arena.getBoundaryMax() != null;

        inv.setItem(10, new ItemBuilder(Material.LIME_WOOL)
                .name("&aSet Spawn 1")
                .lore(hasSpawn1 ? "&a✔ Set" : "&7Click to set spawn 1 to your location.")
                .build());

        inv.setItem(11, new ItemBuilder(Material.RED_WOOL)
                .name("&cSet Spawn 2")
                .lore(hasSpawn2 ? "&a✔ Set" : "&7Click to set spawn 2 to your location.")
                .build());

        inv.setItem(13, new ItemBuilder(Material.COMPASS)
                .name("&6Set Boundary Min")
                .lore(arena.getBoundaryMin() != null ? "&a✔ Set" : "&7Click to set boundary corner 1.")
                .build());

        inv.setItem(14, new ItemBuilder(Material.COMPASS)
                .name("&eSet Boundary Max")
                .lore(arena.getBoundaryMax() != null ? "&a✔ Set" : "&7Click to set boundary corner 2.")
                .build());

        inv.setItem(15, new ItemBuilder(arena.isAutoRegen() ? Material.GREEN_DYE : Material.RED_DYE)
                .name("&6Auto Regen: " + (arena.isAutoRegen() ? "&aON" : "&cOFF"))
                .lore("&7Click to toggle auto-regen after matches.").build());

        inv.setItem(16, new ItemBuilder(arena.isFfaMode() ? Material.BLAZE_POWDER : Material.GUNPOWDER)
                .name("&dFFA Mode: " + (arena.isFfaMode() ? "&aON" : "&cOFF"))
                .lore("&7Toggle FFA mode for this arena.").build());

        if (hasBoundary) {
            inv.setItem(22, new ItemBuilder(Material.CHEST)
                    .name("&b&lTake Snapshot")
                    .lore("&7Save the current arena blocks for regeneration.").glow().build());
        }

        inv.setItem(26, new ItemBuilder(Material.LIME_DYE)
                .name("&a&lSave & Close").glow()
                .lore("&7Save this arena configuration.").build());

        player.openInventory(inv);
    }

    public static void handleClick(Player player, int slot, Inventory inv) {
        SunMc plugin = SunMc.getInstance();
        String arenaName = editingArena.get(player.getUniqueId());
        if (arenaName == null) { player.closeInventory(); return; }
        Arena arena = plugin.getArenaManager().getArena(arenaName).orElse(null);
        if (arena == null) { player.closeInventory(); return; }

        switch (slot) {
            case 10 -> {
                arena.setSpawn1(player.getLocation().clone());
                MessageUtil.sendRaw(player, "&aSpawn 1 set to your location.");
                open(player, arena);
            }
            case 11 -> {
                arena.setSpawn2(player.getLocation().clone());
                MessageUtil.sendRaw(player, "&aSpawn 2 set to your location.");
                open(player, arena);
            }
            case 13 -> {
                arena.setBoundaryMin(player.getLocation().clone());
                MessageUtil.sendRaw(player, "&6Boundary Min set.");
                open(player, arena);
            }
            case 14 -> {
                arena.setBoundaryMax(player.getLocation().clone());
                MessageUtil.sendRaw(player, "&eBoundary Max set.");
                open(player, arena);
            }
            case 15 -> {
                arena.setAutoRegen(!arena.isAutoRegen());
                open(player, arena);
            }
            case 16 -> {
                arena.setFfaMode(!arena.isFfaMode());
                open(player, arena);
            }
            case 22 -> {
                player.closeInventory();
                plugin.getArenaManager().takeSnapshot(arena);
                MessageUtil.sendRaw(player, "&aSnapshot taken for arena &e" + arena.getName() + "&a!");
            }
            case 26 -> {
                plugin.getArenaManager().saveAll();
                editingArena.remove(player.getUniqueId());
                MessageUtil.send(player, "arena-saved", "{arena}", arena.getName());
                player.closeInventory();
            }
        }
    }

    public static boolean isEditing(UUID uuid) { return editingArena.containsKey(uuid); }
    public static String getEditingArena(UUID uuid) { return editingArena.get(uuid); }
}
