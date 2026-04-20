package dev.sunmc.commands;

import dev.sunmc.SunMc;
import dev.sunmc.gui.DuelPropsGUI;
import dev.sunmc.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class DuelCommand implements CommandExecutor, TabCompleter {

    private final SunMc plugin;

    public DuelCommand(SunMc plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { MessageUtil.send(sender, "player-only"); return true; }
        if (!player.hasPermission("sunmc.use")) { MessageUtil.send(player, "no-permission"); return true; }

        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "accept" -> plugin.getDuelManager().acceptRequest(player);
            case "decline", "deny" -> plugin.getDuelManager().declineRequest(player);
            case "options", "props" -> DuelPropsGUI.open(player);
            case "list" -> {
                var matches = plugin.getDuelManager().getAllMatches();
                if (matches.isEmpty()) { MessageUtil.sendRaw(player, "&7No active matches."); return true; }
                MessageUtil.sendRaw(player, "&6Active Matches &7(" + matches.size() + "):");
                matches.forEach(m -> {
                    String p1 = nameOf(m.getPlayer1());
                    String p2 = nameOf(m.getPlayer2());
                    String kit = m.getKit() != null ? m.getKit().getName() : "No Kit";
                    player.sendMessage(dev.sunmc.utils.ColorUtil.component(
                            "&e" + p1 + " &7vs &e" + p2 + " &8| &7Kit: &f" + kit + " &8| &7" + m.getState().name()));
                });
            }
            default -> {
                // /duel <player> [kit]
                String targetName = args[0];
                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null || !target.isOnline()) {
                    MessageUtil.sendRaw(player, "&cPlayer &e" + targetName + " &cis not online.");
                    return true;
                }
                if (target.equals(player)) {
                    MessageUtil.sendRaw(player, "&cYou cannot duel yourself.");
                    return true;
                }
                String kitName = args.length > 1 ? args[1].toLowerCase() : null;
                if (kitName != null && plugin.getKitManager().getKit(kitName).isEmpty()) {
                    MessageUtil.send(player, "kit-not-found", "{kit}", kitName);
                    return true;
                }
                plugin.getDuelManager().sendRequest(player, target, kitName);
            }
        }
        return true;
    }

    private String nameOf(java.util.UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        return p != null ? p.getName() : uuid.toString().substring(0, 8);
    }

    private void sendHelp(Player player) {
        player.sendMessage(dev.sunmc.utils.ColorUtil.component("&6&lDuel Commands:"));
        List.of(
                "&e/duel <player> [kit] &7- Challenge a player",
                "&e/duel accept &7- Accept a duel request",
                "&e/duel decline &7- Decline a duel request",
                "&e/duel options &7- Open pre-match options GUI",
                "&e/duel list &7- List active matches"
        ).forEach(l -> player.sendMessage(dev.sunmc.utils.ColorUtil.component(l)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            List<String> sub = List.of("accept", "decline", "options", "list");
            List<String> players = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName).collect(Collectors.toList());
            players.addAll(sub);
            return players.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            return plugin.getKitManager().getAllKits().stream()
                    .map(k -> k.getName())
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
