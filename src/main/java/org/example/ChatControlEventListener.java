package org.example;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatControlEventListener implements Listener {
    private final ChatControlCore plugin;
    private final ChatControlCommandExecutor commandExecutor;
    private final WorldMessages worldMessages;
    private FileConfiguration config;
    private FileConfiguration playersConfig;
    private final Map<UUID, LastMessageData> playerMessageData = new HashMap<>();
    private final Set<String> exemptPlayers = new HashSet<>();
    private final Map<UUID, Long> mutedPlayers = new HashMap<>();
    private final Map<UUID, Long> playerLogoutTimes = new HashMap<>();

    public ChatControlEventListener(ChatControlCore plugin, ChatControlCommandExecutor commandExecutor) {
        this.plugin = plugin;
        this.commandExecutor = commandExecutor;
        this.worldMessages = new WorldMessages(plugin);
        reloadConfig();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        String message = event.getMessage();

        if (plugin.isPlayerMuted(playerId)) {
            long muteEndTime = plugin.getMutedPlayers().get(playerId);
            long remainingTime = (muteEndTime - System.currentTimeMillis()) / 1000;
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Вам запрещено писать в чат. Вы заблокированы в чате на " + remainingTime + " секунд!");
            return;
        }

        if (message.startsWith(".")) {
            String chatMessage = message.substring(1);
            PrivateChat chat = plugin.getPrivateChats().values().stream()
                    .filter(c -> c.getMembers().contains(playerId))
                    .findFirst()
                    .orElse(null);

            if (chat != null) {
                event.setCancelled(true);

                File playersFile = new File(plugin.getDataFolder(), "group_players.yml");
                FileConfiguration playersConfig = YamlConfiguration.loadConfiguration(playersFile);
                String groupName = playersConfig.getString("Players." + player.getName() + ".Group", "");

                File groupsFile = new File(plugin.getDataFolder(), "groups.yml");
                FileConfiguration groupsConfig = YamlConfiguration.loadConfiguration(groupsFile);
                String prefix = groupsConfig.getString("Groups." + groupName + ".Prefix", "");

                prefix = ChatColor.translateAlternateColorCodes('&', prefix);

                for (UUID memberId : chat.getMembers()) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null) {
                        member.sendMessage(ChatColor.LIGHT_PURPLE + "[Приватный чат] " + prefix + player.getName() + ": " + chatMessage);
                    }
                }
            } else {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Вы не состоите в приватном чате.");
            }
        } else {
            handleSpam(event, message, playerId, System.currentTimeMillis());

            if (!event.isCancelled()) {
                String groupName = plugin.getGroupManager().getPlayerGroup(player);
                String prefix = plugin.getGroupManager().getPrefix(groupName);
                String suffix = plugin.getGroupManager().getSuffix(groupName);
                message = handleUrlBlocking(event, message);
                message = handleTextReplacement(event, message, groupName);
                worldMessages.handleMessageVisibility(event, message, player, prefix, suffix);
            }
        }
    }

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            UUID playerId = player.getUniqueId();
            if (playerLogoutTimes.containsKey(playerId)) {
                long remainingTime = playerLogoutTimes.get(playerId);
                long newMuteEndTime = System.currentTimeMillis() + remainingTime;
                mutedPlayers.put(playerId, newMuteEndTime);
                playerLogoutTimes.remove(playerId);
            }
        }

    private void handleSpam(AsyncPlayerChatEvent event, String message, UUID playerId, long currentTime) {
        Player player = event.getPlayer();
        if (exemptPlayers.contains(player.getName())) {
            return;
        }

        boolean isSpamProtectionEnabled = config.getBoolean("chat.off_spam", true);
        if (!isSpamProtectionEnabled) {
            return;
        }

        int spamTime = config.getInt("chat.spam_time", 5) * 1000;

        if (commandExecutor.isPlayerMuted(playerId)) {
            event.setCancelled(true);
            return;
        }

        LastMessageData lastMessageData = playerMessageData.get(playerId);
        if (lastMessageData != null && currentTime - lastMessageData.getLastTimestamp() < spamTime) {
            long remainingTime = (lastMessageData.getLastTimestamp() + spamTime - currentTime) / 1000;
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Вы можете отправлять сообщения только раз в " + (spamTime / 1000) + " секунд. Осталось: " + remainingTime + " секунд.");
            return;
        }
        playerMessageData.put(playerId, new LastMessageData(message, currentTime, 1));
    }

        private String handleUrlBlocking(AsyncPlayerChatEvent event, String message) {
            Player player = event.getPlayer();
            if (exemptPlayers.contains(player.getName())) {
                return message;
            }

            boolean isUrlBlocked = config.getBoolean("chat.off_chat_url", true);
            if (isUrlBlocked) {
                String linkRegex = "http[s]?://\\S+";
                Pattern pattern = Pattern.compile(linkRegex);
                Matcher matcher = pattern.matcher(message);
                if (matcher.find()) {
                    String replacementText = ChatColor.translateAlternateColorCodes('&', config.getString("chat.url_change_text", "&4&lзапрещено"));
                    message = replacementText;
                }
            }
            return message;
        }

    private String handleTextReplacement(AsyncPlayerChatEvent event, String message, String groupName) {
        Player player = event.getPlayer();
        if (exemptPlayers.contains(player.getName())) {
            return message;
        }
        boolean isForbiddenWordsEnabled = config.getBoolean("chat.forbidden_words", true);
        if (!isForbiddenWordsEnabled) {
            return message;
        }

        String colorCode = config.getString("Code." + groupName, "&f");
        String colorAfterReplacement = ChatColor.translateAlternateColorCodes('&', colorCode);
        String suffix = plugin.getGroupManager().getSuffix(groupName);
        String suffixColor = ChatColor.translateAlternateColorCodes('&', suffix);

        for (String key : config.getConfigurationSection("replace_worlds").getKeys(false)) {
            String replaceKey = "replace_worlds." + key;
            if (config.isList(replaceKey + ".Text")) {
                List<String> textsToReplace = config.getStringList(replaceKey + ".Text");
                String replacement = ChatColor.translateAlternateColorCodes('&', config.getString(replaceKey + ".Replace", "&f"));
                for (String textToReplace : textsToReplace) {
                    String coloredTextToReplace = ChatColor.translateAlternateColorCodes('&', textToReplace);
                    message = message.replaceAll("(?i)" + Pattern.quote(coloredTextToReplace), replacement + colorAfterReplacement + suffixColor);
                }
            }
        }
        return message;
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
