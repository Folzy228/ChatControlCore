package org.example;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatControlEventListener implements Listener {

    private final ChatControlCore plugin;
    private FileConfiguration config;
    private FileConfiguration playersConfig;
    private final Map<UUID, LastMessageData> playerMessageData = new HashMap<>();
    private final Set<String> exemptPlayers;
    private final LuckPerms luckPermsApi;

    public ChatControlEventListener(ChatControlCore plugin) {
        this.plugin = plugin;
        this.exemptPlayers = new HashSet<>();
        this.luckPermsApi = Bukkit.getServicesManager().getRegistration(LuckPerms.class).getProvider();
        reloadConfig();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        UUID playerId = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getName();
        long currentTime = System.currentTimeMillis();

        if (exemptPlayers.contains(playerName)) {
            return;
        }

        User user = luckPermsApi.getUserManager().getUser(playerId);
        if (user != null && user.getCachedData().getMetaData().getMetaValue("muted") != null) {
            return;
        }

        boolean isSpamBlocked = config.getBoolean("off_spam", true);
        if (isSpamBlocked) {
            int maxMessages = config.getInt("chat.max_messages");
            long timeWindowMillis = config.getInt("chat.time_window") * 1000;
            long muteDurationMillis = config.getInt("chat.mute_duration") * 1000;
            LastMessageData lastMessageData = playerMessageData.getOrDefault(playerId, new LastMessageData("", 0L, 0));
            if (message.equals(lastMessageData.getMessage()) && (currentTime - lastMessageData.getLastTimestamp() <= timeWindowMillis)) {
                int repeatCount = lastMessageData.getRepeatCount() + 1;
                if (repeatCount >= maxMessages) {
                    String muteCommand = "mute " + event.getPlayer().getName() + " " + (muteDurationMillis / 1000);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), muteCommand);
                    event.getPlayer().sendMessage(ChatColor.RED + "Вы были заблокированы в чате за повторение сообщения!");
                    playerMessageData.put(playerId, new LastMessageData(message, currentTime, 0));
                    return;
                }
                playerMessageData.put(playerId, new LastMessageData(message, currentTime, repeatCount));
            } else {
                playerMessageData.put(playerId, new LastMessageData(message, currentTime, 1));
            }
        }

        boolean isUrlBlocked = config.getBoolean("off_chat_url", true);
        if (isUrlBlocked) {
            String linkRegex = "http[s]?://\\S+";
            Pattern pattern = Pattern.compile(linkRegex);
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String replacementText = ChatColor.translateAlternateColorCodes('&', config.getString("url_change_text", "&4&lзапрещено"));
                event.setMessage(replacementText);
                return;
            }
        }

        String groupName = user != null ? user.getPrimaryGroup() : "default";
        String colorCode = config.getString("Code." + groupName);
        String colorAfterReplacement = colorCode != null ? ChatColor.translateAlternateColorCodes('&', colorCode) : "";
        for (String key : config.getConfigurationSection("Test").getKeys(false)) {
            String replaceKey = "Test." + key;
            if (config.isList(replaceKey + ".Text")) {
                List<String> textsToReplace = config.getStringList(replaceKey + ".Text");
                String replacement = ChatColor.translateAlternateColorCodes('&', config.getString(replaceKey + ".Replace"));
                for (String textToReplace : textsToReplace) {
                    String coloredTextToReplace = ChatColor.translateAlternateColorCodes('&', textToReplace);
                    message = message.replaceAll("(?i)" + Pattern.quote(coloredTextToReplace), replacement + colorAfterReplacement);
                }
            }
        }
        event.setMessage(message);
    }

    public void reloadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
        File playersFile = new File(plugin.getDataFolder(), "players.yml");
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        exemptPlayers.clear();
        exemptPlayers.addAll(playersConfig.getStringList("Players"));
    }

    private static class LastMessageData {
        private final String message;
        private final long lastTimestamp;
        private final int repeatCount;

        public LastMessageData(String message, long lastTimestamp, int repeatCount) {
            this.message = message;
            this.lastTimestamp = lastTimestamp;
            this.repeatCount = repeatCount;
        }

        public String getMessage() {
            return message;
        }

        public long getLastTimestamp() {
            return lastTimestamp;
        }

        public int getRepeatCount() {
            return repeatCount;
        }
    }
}