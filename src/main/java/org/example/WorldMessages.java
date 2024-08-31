package org.example;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class WorldMessages {

    private final ChatControlCore plugin;

    public WorldMessages(ChatControlCore plugin) {
        this.plugin = plugin;
    }

    public void handleMessageVisibility(AsyncPlayerChatEvent event, String message, Player player, String prefix, String suffix) {
        String localPrefix = ChatColor.WHITE + "[" + ChatColor.AQUA + "L" + ChatColor.WHITE + "] ";
        String globalPrefix = ChatColor.WHITE + "[" + ChatColor.RED + "G" + ChatColor.WHITE + "] ";
        String formattedMessage;

        if (message.startsWith("!")) {
            message = message.substring(1);
            formattedMessage = globalPrefix + ChatColor.translateAlternateColorCodes('&', prefix) + player.getDisplayName() + ChatColor.translateAlternateColorCodes('&', suffix) + ": " + message;

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player worldPlayer : player.getWorld().getPlayers()) {
                    worldPlayer.sendMessage(formattedMessage);
                }
            });
            event.setCancelled(true);
        } else {
            formattedMessage = localPrefix + ChatColor.translateAlternateColorCodes('&', prefix) + player.getDisplayName() + ChatColor.translateAlternateColorCodes('&', suffix) + ": " + message;

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                    if (nearbyPlayer.getLocation().distance(player.getLocation()) <= 30) {
                        nearbyPlayer.sendMessage(formattedMessage);
                    }
                }
            });
            event.setCancelled(true);
        }
    }
}
