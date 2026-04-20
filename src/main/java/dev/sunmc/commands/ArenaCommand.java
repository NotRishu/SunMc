package dev.sunmc.commands;

import dev.sunmc.SunMc;
import dev.sunmc.arena.Arena;
import dev.sunmc.gui.ArenaCreatorGUI;
import dev.sunmc.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ArenaCommand implements CommandExecutor, TabCompleter {

    private final SunMc plugin;

    public ArenaCommand(SunMc plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "player-only");
            return true;
        }
        if (!player.hasPermission("sunmc.admin")) {
            MessageUtil.send(player, "no-permission");
            return true;
        }
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 2) { player.sendMessage("Usage: /arena create <name>"); return true; }
                String name = args[1];
                Arena arena = plugin.getArenaManager().createArena(name);
                MessageUtil.sendRaw(player, "&aArena &e" + name + " &acreated. Opening editor...");
                ArenaCreatorGUI.open(player, arena);
            }
            case "edit" -> {
                if (args.length < 2) { player.sendMessage("Usage: /arena edit <name>"); return true; }
                Optional<Arena> opt = plugin.getArenaManager().getArena(args[1]);
                if (opt.isEmpty()) { MessageUtil.send(player, "arena-not-found", "{arena}", args[1]); return true; }
                ArenaCreatorGUI.open(player, opt.get());
            }
            case "delete" -> {
                if (args.length < 2) { player.sendMessage("Usage: /arena delete <name>"); return true; }
                boolean deleted = plugin.getArenaManager().deleteArena(args[1]);
                if (deleted) MessageUtil.sendRaw(player, "&cArena &e" + args[1] + " &cdeleted.");
                else MessageUtil.send(player, "arena-not-found", "{arena}", args[1]);
            }
            case "list" -> {
                var arenas = plugin.getArenaManager().getAllArenas();
                if (arenas.isEmpty()) { MessageUtil.sendRaw(player, "&7No arenas found."); return true; }
                MessageUtil.sendRaw(player, "&6Arenas &7(" + arenas.size() + "):");
                arenas.forEach(a -> player.sendMessage(
                        dev.sunmc.utils.ColorUtil.component(
                                "&e" + a.getName() + " &8| &7State: &f" + a.getState().name()
                                        + " &8| &7AutoRegen: &f" + a.isAutoRegen()
                                        + " &8| &7FFA: &f" + a.isFfaMode())));
            }
            case "snapshot" -> {
                if (args.length < 2) { player.sendMessage("Usage: /arena snapshot <name>"); return true; }
                Optional<Arena> opt = plugin.getArenaManager().getArena(args[1]);
                if (opt.isEmpty()) { MessageUtil.send(player, "arena-not-found", "{arena}", args[1]); return true; }
                Arena arena = opt.get();
                if (arena.getBoundaryMin() == null || arena.getBoundaryMax() == null) {
                    MessageUtil.sendRaw(player, "&cSet arena boundaries before taking a snapshot.");
                    return true;
                }
                plugin.getArenaManager().takeSnapshot(arena);
                MessageUtil.sendRaw(player, "&aSnapshot saved for arena &e" + arena.getName() + "&a.");
            }
            case "regen" -> {
                if (args.length < 2) { player.sendMessage("Usage: /arena regen <name>"); return true; }
                Optional<Arena> opt = plugin.getArenaManager().getArena(args[1]);
                if (opt.isEmpty()) { MessageUtil.send(player, "arena-not-found", "{arena}", args[1]); return true; }
                MessageUtil.send(player, "arena-regen", "{arena}", args[1]);
                plugin.getArenaManager().regenerateArena(opt.get(), () ->
                        MessageUtil.send(player, "arena-ready", "{arena}", args[1]));
            }
            case "setregen" -> {
                if (args.length < 3) { player.sendMessage("Usage: /arena setregen <name> <true|false>"); return true; }
                Optional<Arena> opt = plugin.getArenaManager().getArena(args[1]);
                if (opt.isEmpty()) { MessageUtil.send(player, "arena-not-found", "{arena}", args[1]); return true; }
                opt.get().setAutoRegen(Boolean.parseBoolean(args[2]));
                plugin.getArenaManager().saveAll();
                MessageUtil.sendRaw(player, "&aAuto-regen for &e" + args[1] + " &aset to &f" + args[2] + "&a.");
            }
            case "setkit" -> {
                if (args.length < 3) { player.sendMessage("Usage: /arena setkit <arena> <kit>"); return true; }
                Optional<Arena> opt = plugin.getArenaManager().getArena(args[1]);
                if (opt.isEmpty()) { MessageUtil.send(player, "arena-not-found", "{arena}", args[1]); return true; }
                opt.get().setAssignedKit(args[2].toLowerCase());
                plugin.getArenaManager().saveAll();
                MessageUtil.sendRaw(player, "&aKit for arena &e" + args[1] + " &aset to &f" + args[2] + "&a.");
            }
            case "duplicate" -> {
                if (args.length < 5) {
                    player.sendMessage("Usage: /arena duplicate <name> <south|east|west> <count> <spacing>");
                    return true;
                }
                Optional<Arena> opt = plugin.getArenaManager().getArena(args[1]);
                if (opt.isEmpty()) { MessageUtil.send(player, "arena-not-found", "{arena}", args[1]); return true; }
                String dir = args[2];
                if (!List.of("south", "east", "west").contains(dir.toLowerCase())) {
                    player.sendMessage("&cDirection must be south, east, or west.");
                    return true;
                }
                int count, spacing;
                try {
                    count = Integer.parseInt(args[3]);
                    spacing = Integer.parseInt(args[4]);
                } catch (NumberFormatException ex) {
                    player.sendMessage("&cCount and spacing must be numbers.");
                    return true;
                }
                var created = plugin.getArenaManager().duplicateArena(opt.get(), dir, count, spacing);
                MessageUtil.sendRaw(player, "&aDuplicated &e" + args[1] + " &a→ created &e" + created.size() + " &anew arenas.");
                created.forEach(a -> MessageUtil.sendRaw(player, "&7  - &e" + a.getName()));
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(dev.sunmc.utils.ColorUtil.component("&6&lArena Commands:"));
        List.of(
                "&e/arena create <name> &7- Create a new arena",
                "&e/arena edit <name> &7- Open arena editor GUI",
                "&e/arena delete <name> &7- Delete an arena",
                "&e/arena list &7- List all arenas",
                "&e/arena snapshot <name> &7- Save arena snapshot",
                "&e/arena regen <name> &7- Restore arena from snapshot",
                "&e/arena setregen <name> <true|false> &7- Toggle auto-regen",
                "&e/arena setkit <arena> <kit> &7- Assign kit to arena",
                "&e/arena duplicate <name> <south|east|west> <count> <spacing> &7- Duplicate arena"
        ).forEach(l -> player.sendMessage(dev.sunmc.utils.ColorUtil.component(l)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1)
            return List.of("create", "edit", "delete", "list", "snapshot", "regen", "setregen", "setkit", "duplicate")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2 && !args[0].equalsIgnoreCase("create"))
            return plugin.getArenaManager().getAllArenas().stream()
                    .map(Arena::getName).filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        if (args.length == 3 && args[0].equalsIgnoreCase("duplicate"))
            return List.of("south", "east", "west");
        return List.of();
    }
}
