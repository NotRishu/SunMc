package dev.sunmc.commands;

import dev.sunmc.SunMc;
import dev.sunmc.managers.StatsManager;
import dev.sunmc.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SunMcCommand implements CommandExecutor, TabCompleter {

    private final SunMc plugin;

    public SunMcCommand(SunMc plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("sunmc.admin")) { MessageUtil.send(sender, "no-permission"); return true; }

        if (args.length == 0) {
            sender.sendMessage(dev.sunmc.utils.ColorUtil.component(
                    "&c&lSun&6Mc &7v" + plugin.getDescription().getVersion()
                            + " &8| &7Use &e/sunmc help &7for commands."));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                MessageUtil.sendRaw(sender, "&aPlugin reloaded successfully.");
            }
            case "stats" -> {
                if (args.length < 2) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("Usage: /sunmc stats <player>");
                        return true;
                    }
                    printStats(sender, player.getUniqueId(), player.getName());
                } else {
                    Player target = Bukkit.getPlayerExact(args[1]);
                    UUID uuid = target != null ? target.getUniqueId() : null;
                    if (uuid == null) {
                        // Try offline player
                        var offlinePlayer = Bukkit.getOfflinePlayerIfCached(args[1]);
                        if (offlinePlayer != null) uuid = offlinePlayer.getUniqueId();
                    }
                    if (uuid == null) {
                        MessageUtil.sendRaw(sender, "&cPlayer not found.");
                        return true;
                    }
                    printStats(sender, uuid, args[1]);
                }
            }
            case "version" ->
                sender.sendMessage(dev.sunmc.utils.ColorUtil.component(
                        "&6SunMc &7v" + plugin.getDescription().getVersion()
                                + " &8| &7MC 1.21.1 &8| &7Paper/Spigot"));
            case "arenas" ->
                sender.sendMessage(dev.sunmc.utils.ColorUtil.component(
                        "&6Arenas: &f" + plugin.getArenaManager().getAllArenas().size()
                                + " &8| &6Kits: &f" + plugin.getKitManager().getAllKits().size()
                                + " &8| &6Matches: &f" + plugin.getDuelManager().getAllMatches().size()));
            default -> {
                sender.sendMessage(dev.sunmc.utils.ColorUtil.component("&6SunMc Commands:"));
                List.of(
                        "&e/sunmc reload &7- Reload plugin config",
                        "&e/sunmc stats [player] &7- View player stats",
                        "&e/sunmc version &7- Plugin version info",
                        "&e/sunmc arenas &7- Quick server overview"
                ).forEach(l -> sender.sendMessage(dev.sunmc.utils.ColorUtil.component(l)));
            }
        }
        return true;
    }

    private void printStats(CommandSender sender, UUID uuid, String name) {
        StatsManager.PlayerStats s = plugin.getStatsManager().getStats(uuid);
        sender.sendMessage(dev.sunmc.utils.ColorUtil.component("&6Stats for &e" + name + "&6:"));
        List.of(
                "&7Kills: &f" + s.kills,
                "&7Deaths: &f" + s.deaths,
                "&7Kill Streak: &f" + s.killStreak,
                "&7Max Kill Streak: &f" + s.maxKillStreak,
                "&7Matches: &f" + s.matches,
                "&7K/D: &f" + (s.deaths == 0 ? s.kills : String.format("%.2f", (double) s.kills / s.deaths))
        ).forEach(l -> sender.sendMessage(dev.sunmc.utils.ColorUtil.component("  " + l)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1)
            return List.of("reload", "stats", "version", "arenas").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2 && args[0].equalsIgnoreCase("stats"))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        return List.of();
    }
}
