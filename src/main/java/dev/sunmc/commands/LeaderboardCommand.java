package dev.sunmc.commands;

import dev.sunmc.SunMc;
import dev.sunmc.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class LeaderboardCommand implements CommandExecutor, TabCompleter {

    private final SunMc plugin;
    private final List<String> types = List.of("kills", "deaths", "killstreak", "max_killstreak", "matches");

    public LeaderboardCommand(SunMc plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { MessageUtil.send(sender, "player-only"); return true; }
        if (!player.hasPermission("sunmc.admin")) { MessageUtil.send(player, "no-permission"); return true; }

        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "place", "create" -> {
                if (args.length < 3) {
                    player.sendMessage("Usage: /leaderboard place <n> <type>");
                    player.sendMessage("Types: " + String.join(", ", types));
                    return true;
                }
                String name = args[1];
                String type = args[2].toLowerCase();
                if (!types.contains(type)) {
                    MessageUtil.sendRaw(player, "&cInvalid type. Valid: " + String.join(", ", types));
                    return true;
                }
                plugin.getLeaderboardManager().placeLeaderboard(name, type, player.getLocation());
                MessageUtil.sendRaw(player, "&aLeaderboard &e" + name + " &a(" + type + ") placed at your location.");
            }
            case "remove", "delete" -> {
                if (args.length < 2) { player.sendMessage("Usage: /leaderboard remove <n>"); return true; }
                plugin.getLeaderboardManager().removeLeaderboard(args[1]);
                MessageUtil.sendRaw(player, "&cLeaderboard &e" + args[1] + " &cremoved.");
            }
            case "list" -> {
                var lbs = plugin.getLeaderboardManager().getAll();
                if (lbs.isEmpty()) { MessageUtil.sendRaw(player, "&7No leaderboards placed."); return true; }
                MessageUtil.sendRaw(player, "&6Leaderboards &7(" + lbs.size() + "):");
                lbs.forEach(lb -> player.sendMessage(dev.sunmc.utils.ColorUtil.component(
                        "&e" + lb.getName() + " &8| &7Type: &f" + lb.getType())));
            }
            case "refresh" -> {
                plugin.getLeaderboardManager().refreshAll();
                MessageUtil.sendRaw(player, "&aAll leaderboards refreshed.");
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(dev.sunmc.utils.ColorUtil.component("&6&lLeaderboard Commands:"));
        List.of(
                "&e/leaderboard place <n> <type> &7- Place leaderboard at your location",
                "&e/leaderboard remove <n> &7- Remove a leaderboard",
                "&e/leaderboard list &7- List all leaderboards",
                "&e/leaderboard refresh &7- Refresh all leaderboard data",
                "&7Types: &fkills, deaths, killstreak, max_killstreak, matches"
        ).forEach(l -> player.sendMessage(dev.sunmc.utils.ColorUtil.component(l)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1)
            return List.of("place", "remove", "list", "refresh").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 3 && args[0].equalsIgnoreCase("place"))
            return types.stream().filter(s -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        return List.of();
    }
}
