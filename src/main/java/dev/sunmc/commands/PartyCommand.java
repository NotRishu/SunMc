package dev.sunmc.commands;

import dev.sunmc.SunMc;
import dev.sunmc.gui.PartyMenuGUI;
import dev.sunmc.party.Party;
import dev.sunmc.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PartyCommand implements CommandExecutor, TabCompleter {

    private final SunMc plugin;

    public PartyCommand(SunMc plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { MessageUtil.send(sender, "player-only"); return true; }
        if (!player.hasPermission("sunmc.use")) { MessageUtil.send(player, "no-permission"); return true; }

        if (args.length == 0) {
            PartyMenuGUI.open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> plugin.getPartyManager().createParty(player);
            case "invite" -> {
                if (args.length < 2) { player.sendMessage("Usage: /party invite <player>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { MessageUtil.sendRaw(player, "&cPlayer not found."); return true; }
                plugin.getPartyManager().invite(player, target);
            }
            case "accept" -> plugin.getPartyManager().acceptInvite(player);
            case "leave" -> plugin.getPartyManager().leave(player);
            case "menu", "gui" -> PartyMenuGUI.open(player);
            case "info" -> {
                Optional<Party> opt = plugin.getPartyManager().getParty(player.getUniqueId());
                if (opt.isEmpty()) { MessageUtil.send(player, "party-not-found"); return true; }
                Party party = opt.get();
                MessageUtil.sendRaw(player, "&6Party Info &8| &7" + party.size() + " members:");
                party.getMembers().forEach(u -> {
                    Player p = Bukkit.getPlayer(u);
                    String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(u).getName();
                    String role = party.isLeader(u) ? " &c[Leader]" : "";
                    player.sendMessage(dev.sunmc.utils.ColorUtil.component("  &e" + name + role));
                });
            }
            case "disband" -> {
                Optional<Party> opt = plugin.getPartyManager().getParty(player.getUniqueId());
                if (opt.isEmpty()) { MessageUtil.send(player, "party-not-found"); return true; }
                if (!opt.get().isLeader(player.getUniqueId())) {
                    MessageUtil.sendRaw(player, "&cOnly the party leader can disband."); return true;
                }
                plugin.getPartyManager().dissolveParty(opt.get());
            }
            case "vs" -> {
                if (args.length < 2) { player.sendMessage("Usage: /party vs <player>"); return true; }
                Optional<Party> myParty = plugin.getPartyManager().getParty(player.getUniqueId());
                if (myParty.isEmpty()) { MessageUtil.send(player, "party-not-found"); return true; }
                if (!myParty.get().isLeader(player.getUniqueId())) {
                    MessageUtil.sendRaw(player, "&cOnly the party leader can challenge other parties."); return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { MessageUtil.sendRaw(player, "&cPlayer not found."); return true; }
                Optional<Party> theirParty = plugin.getPartyManager().getParty(target.getUniqueId());
                if (theirParty.isEmpty()) { MessageUtil.sendRaw(player, "&c" + target.getName() + " is not in a party."); return true; }
                plugin.getPartyManager().broadcastToParty(myParty.get(), "&eChallenged &c" + target.getName() + "&e's party!");
                plugin.getPartyManager().broadcastToParty(theirParty.get(),
                        "&e" + player.getName() + "&a's party has challenged you! &7(/party accept)");
                // TODO: Full party vs party match flow
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(dev.sunmc.utils.ColorUtil.component("&6&lParty Commands:"));
        List.of(
                "&e/party &7- Open party menu",
                "&e/party create &7- Create a new party",
                "&e/party invite <player> &7- Invite a player",
                "&e/party accept &7- Accept a party invite",
                "&e/party leave &7- Leave your party",
                "&e/party info &7- View party members",
                "&e/party disband &7- Disband the party (leader only)",
                "&e/party vs <player> &7- Challenge another party"
        ).forEach(l -> player.sendMessage(dev.sunmc.utils.ColorUtil.component(l)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1)
            return List.of("create", "invite", "accept", "leave", "menu", "info", "disband", "vs")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("vs")))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        return List.of();
    }
}
