package org.example;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

public class ChatControlCore extends JavaPlugin implements Listener {
    private final Map<String, PrivateChat> privateChats = new HashMap<>();
    private final Map<UUID, PrivateChatRequest> chatRequests = new HashMap<>();
    private ChatControlCommandExecutor commandExecutor;
    private ChatControlEventListener eventListener;
    static final String VERSION_FILE_URL = "https://raw.githubusercontent.com/Folzy228/ChatControlCore/main/latest_version_ccc.txt";
    private String latestVersion;
    private List<String> chatHistory = new ArrayList<>();
    private Map<UUID, Long> mutedPlayers = new HashMap<>();
    private GroupManager groupManager;
    private AutoMessage autoMessage;
    private AntiAFK antiAFK;

    public void addChatMessage(String message) {
        chatHistory.add(message);
    }

    public ChatControlCommandExecutor getCommandExecutor() {
        return commandExecutor;
    }


    @Override
    public void onEnable() {
        saveDefaultConfig();
        copyResource("players.yml");
        copyResource("groups.yml");
        copyResource("group_players.yml");
        getServer().getPluginManager().registerEvents(this, this);
        groupManager = new GroupManager(this);
        this.groupManager = new GroupManager(this);
        autoMessage = new AutoMessage(this);
        antiAFK = new AntiAFK(this);
        getServer().getPluginManager().registerEvents(antiAFK, this);
        getServer().getPluginManager().registerEvents(this, this);

        groupManager = new GroupManager(this);
        commandExecutor = new ChatControlCommandExecutor(this, groupManager);
        getCommand("ccc").setExecutor(commandExecutor);

        eventListener = new ChatControlEventListener(this, commandExecutor);
        getServer().getPluginManager().registerEvents(eventListener, this);

        reloadConfig();

        checkForUpdates();
    }

    public Map<String, PrivateChat> getPrivateChats() {
        return privateChats;
    }

    public Map<UUID, PrivateChatRequest> getChatRequests() {
        return chatRequests;
    }

    public Map<UUID, Long> getMutedPlayers() {
        return mutedPlayers;
    }

    public boolean isPlayerMuted(UUID playerId) {
        Long muteEndTime = mutedPlayers.get(playerId);
        return muteEndTime != null && System.currentTimeMillis() < muteEndTime;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (eventListener != null) {
            eventListener.reloadConfig();
        } else {
            getLogger().warning("EventListener не инициализирован.");
        }
        if (groupManager != null) {
            groupManager.reloadConfig();
            for (Player player : getServer().getOnlinePlayers()) {
                groupManager.removePermissions(player);
                groupManager.loadPlayerData(player);
            }
        } else {
            getLogger().warning("GroupManager не инициализирован.");
        }
        reloadPlayers();
    }

    private void reloadPlayers() {
        File playersFile = new File(getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            saveResource("players.yml", false);
        }
        FileConfiguration playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        if (antiAFK != null) {
            antiAFK.updateWhitelist(playersConfig.getStringList("Players"));
        } else {
            getLogger().warning("AntiAFK не инициализирован.");
        }
    }


    public GroupManager getGroupManager() {
        return groupManager;
    }

    private void copyResource(String resourceName) {
        File file = new File(getDataFolder(), resourceName);
        if (!file.exists()) {
            getDataFolder().mkdirs();
            try (InputStream in = getResource(resourceName); FileOutputStream out = new FileOutputStream(file)) {
                if (in == null) {
                    getLogger().warning("Ресурс " + resourceName + " не найден в JAR.");
                    return;
                }
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            } catch (IOException e) {
                getLogger().severe("Не удалось скопировать ресурс " + resourceName);
                e.printStackTrace();
            }
        }
    }

    private void checkForUpdates() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(VERSION_FILE_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    latestVersion = reader.readLine().trim();
                    reader.close();
                    String currentVersion = getDescription().getVersion();
                    getServer().getScheduler().runTask(ChatControlCore.this, () -> {
                        if (!latestVersion.equals(currentVersion)) {
                            getLogger().info("A new version of the plugin is available! Current version: " + currentVersion + ", latest version: " + latestVersion);
                        }
                    });
                } catch (IOException e) {
                    getLogger().log(Level.SEVERE, "Error while checking for updates", e);
                }
            }
        }.runTaskAsynchronously(this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        groupManager.loadPlayerData(player);
        if (!groupManager.hasPlayerGroup(player)) {
            groupManager.addDefaultGroupToPlayer(player);
        }
        if (player.isOp() && latestVersion != null) {
            String currentVersion = getDescription().getVersion();
            if (!latestVersion.equals(currentVersion)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        String message = "Доступна новая версия плагина! Текущая версия: " + currentVersion + ", последняя версия: " + latestVersion;
                        player.sendMessage(message);
                    }
                }.runTaskLater(ChatControlCore.this, 100L);
            }
        }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        groupManager.savePlayerData(player);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        antiAFK.updatePlayerMoveTime(event.getPlayer());
    }
}
