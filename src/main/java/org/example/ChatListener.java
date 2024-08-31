package org.example;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final ChatControlCore plugin;

    public ChatListener(ChatControlCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = "[" + event.getPlayer().getName() + "] " + event.getMessage();
        plugin.addChatMessage(message);
    }
}
