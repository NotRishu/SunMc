package dev.sunmc.commands;

import dev.sunmc.SunMc;
import dev.sunmc.gui.ExtraItemsGUI;
import dev.sunmc.gui.KitEditorGUI;
import dev.sunmc.kit.Kit;
import dev.sunmc.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class KitCommand implements CommandExecutor, TabCompleter {

    private final SunMc plugin;

    public KitCommand(SunMc plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { MessageUtil.send(sender, "player-only"); return true; }
        if (!player.hasPermission("sunmc.admin")) { MessageUtil.send(player, "no-permission"); return true; }
        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 2) { player.sendMessage("Usage: /kit create <n>"); return true; }
                String name = args[1].toLowerCase();
                if (plugin.getKitManager().getKit(name).isPresent()) {
                    MessageUtil.sendRaw(player, "&cKit &e" + name + " &calready exists.");
                    return true;
                }
                Kit kit = plugin.getKitManager().createKit(name);
                MessageUtil.sendRaw(player, "&aKit &e" + name + " &acreated. Opening editor...");
                KitEditorGUI.open(player, kit);
            }
            case "edit" -> {
                if (args.length < 2) { player.sendMessage("Usage: /kit edit <n>"); return true; }
                Optional<Kit> opt = plugin.getKitManager().getKit(args[1]);
                if (opt.isEmpty()) { MessageUtil.send(player, "kit-not-found", "{kit}", args[1]); return true; }
                KitEditorGUI.open(player, opt.get());
            }
            case "delete" -> {
                if (args.length < 2) { player.sendMessage("Usage: /kit delete <n>"); return true; }
                boolean del = plugin.getKitManager().deleteKit(args[1]);
                if (del) MessageUtil.sendRaw(player, "&cKit &e" + args[1] + " &cdeleted.");
                else MessageUtil.send(player, "kit-not-found", "{kit}", args[1]);
            }
            case "list" -> {
                var kits = plugin.getKitManager().getAllKits();
                if (kits.isEmpty()) { MessageUtil.sendRaw(player, "&7No kits found."); return true; }
                MessageUtil.sendRaw(player, "&6Kits &7(" + kits.size() + "):");
                kits.forEach(k -> player.sendMessage(dev.sunmc.utils.ColorUtil.component(
                        "&e" + k.getName() + " &8| &7Extra Items: &f" + k.isExtraItemsEnabled())));
            }
            case "extraitems", "extras" -> {
                if (args.length < 2) { player.sendMessage("Usage: /kit extraitems <n>"); return true; }
                Optional<Kit> opt = plugin.getKitManager().getKit(args[1]);
                if (opt.isEmpty()) { MessageUtil.send(player, "kit-not-found", "{kit}", args[1]); return true; }
                Kit kit = opt.get();
                if (!kit.isExtraItemsEnabled()) {
                    kit.setExtraItemsEnabled(true);
                    plugin.getKitManager().saveKits();
                }
                ExtraItemsGUI.open(player, kit);
            }
            case "setextra" -> {
                if (args.length < 3) { player.sendMessage("Usage: /kit setextra <n> <true|false>"); return true; }
                Optional<Kit> opt = plugin.getKitManager().getKit(args[1]);
                if (opt.isEmpty()) { MessageUtil.send(player, "kit-not-found", "{kit}", args[1]); return true; }
                opt.get().setExtraItemsEnabled(Boolean.parseBoolean(args[2]));
                plugin.getKitManager().saveKits();
                MessageUtil.sendRaw(player, "&aExtra items for &e" + args[1] + " &aset to &f" + args[2] + "&a.");
            }
            case "loadout" -> {
                if (args.length < 3) { player.sendMessage("Usage: /kit loadout <kitname> <slot>"); return true; }
                Optional<Kit> opt = plugin.getKitManager().getKit(args[1]);
                if (opt.isEmpty()) { MessageUtil.send(player, "kit-not-found", "{kit}", args[1]); return true; }
                try {
                    int slot = Integer.parseInt(args[2]);
                    plugin.getKitManager().applyLoadout(player, args[1], slot);
                    MessageUtil.send(player, "loadout-loaded", "{loadout}", args[2]);
                } catch (NumberFormatException ex) {
                    player.sendMessage("&cSlot must be a number.");
                }
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(dev.sunmc.utils.ColorUtil.component("&6&lKit Commands:"));
        List.of(
                "&e/kit create <n> &7- Create and open kit editor",
                "&e/kit edit <n> &7- Open kit editor GUI",
                "&e/kit delete <n> &7- Delete a kit",
                "&e/kit list &7- List all kits",
                "&e/kit extraitems <n> &7- Open extra items editor",
                "&e/kit setextra <n> <true|false> &7- Toggle extra items",
                "&e/kit loadout <n> <slot> &7- Apply a loadout to yourself"
        ).forEach(l -> player.sendMessage(dev.sunmc.utils.ColorUtil.component(l)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1)
            return List.of("create", "edit", "delete", "list", "extraitems", "setextra", "loadout")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2 && !args[0].equalsIgnoreCase("create"))
            return plugin.getKitManager().getAllKits().stream()
                    .map(Kit::getName).filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        return List.of();
    }
}
