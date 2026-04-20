package dev.sunmc.commands;

import dev.sunmc.SunMc;
import dev.sunmc.arena.Arena;
import dev.sunmc.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FFACommand implements CommandExecutor, TabCompleter {

    private final SunMc plugin;

    public FFACommand(SunMc plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { MessageUtil.send(sender, "player-only"); return true; }
        if (!player.hasPermission("sunmc.use")) { MessageUtil.send(player, "no-permission"); return true; }

        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "join" -> {
                if (args.length < 2) {
                    // List available FFA arenas
                    var ffaArenas = plugin.getArenaManager().getAllArenas().stream()
                            .filter(Arena::isFfaMode).collect(Collectors.toList());
                    if (ffaArenas.isEmpty()) {
                        MessageUtil.sendRaw(player, "&cNo FFA arenas available.");
                        return true;
                    }
                    MessageUtil.sendRaw(player, "&6FFA Arenas: &7" +
                            ffaArenas.stream().map(Arena::getName).collect(Collectors.joining(", ")));
                    return true;
                }
                Optional<Arena> opt = plugin.getArenaManager().getArena(args[1]);
                if (opt.isEmpty()) { MessageUtil.send(player, "arena-not-found", "{arena}", args[1]); return true; }
                plugin.getFFAManager().joinFFA(player, opt.get());
            }
            case "leave" -> plugin.getFFAManager().leaveFFA(player);
            case "list" -> {
                var ffaArenas = plugin.getArenaManager().getAllArenas().stream()
                        .filter(Arena::isFfaMode).collect(Collectors.toList());
                if (ffaArenas.isEmpty()) { MessageUtil.sendRaw(player, "&7No FFA arenas."); return true; }
                MessageUtil.sendRaw(player, "&6FFA Arenas:");
                ffaArenas.forEach(a -> {
                    int count = plugin.getFFAManager().getPlayersInArena(a.getName()).size();
                    player.sendMessage(dev.sunmc.utils.ColorUtil.component(
                            "&e" + a.getName() + " &8| &7Players: &f" + count));
                });
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(dev.sunmc.utils.ColorUtil.component("&6&lFFA Commands:"));
        List.of(
                "&e/ffa join <arena> &7- Join an FFA arena",
                "&e/ffa leave &7- Leave your current FFA arena",
                "&e/ffa list &7- List all FFA arenas"
        ).forEach(l -> player.sendMessage(dev.sunmc.utils.ColorUtil.component(l)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1)
            return List.of("join", "leave", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2 && args[0].equalsIgnoreCase("join"))
            return plugin.getArenaManager().getAllArenas().stream()
                    .filter(Arena::isFfaMode).map(Arena::getName)
                    .filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        return List.of();
    }
}
