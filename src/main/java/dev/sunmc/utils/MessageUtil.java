package dev.sunmc.utils;

import dev.sunmc.SunMc;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtil {

    private static String prefix() {
        return ColorUtil.color(SunMc.getInstance().getConfig().getString("messages.prefix", "&c&lSun&6Mc &8» &r"));
    }

    public static void send(CommandSender sender, String key, String... replacements) {
        String msg = SunMc.getInstance().getConfig().getString("messages." + key, "&c[SunMc] Message not found: " + key);
        msg = ColorUtil.replace(msg, replacements);
        sender.sendMessage(ColorUtil.component(prefix() + msg));
    }

    public static void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(ColorUtil.component(prefix() + message));
    }

    public static void broadcast(String key, String... replacements) {
        String msg = SunMc.getInstance().getConfig().getString("messages." + key, "&c[SunMc] " + key);
        msg = ColorUtil.replace(msg, replacements);
        for (Player p : SunMc.getInstance().getServer().getOnlinePlayers()) {
            p.sendMessage(ColorUtil.component(prefix() + msg));
        }
    }
}
