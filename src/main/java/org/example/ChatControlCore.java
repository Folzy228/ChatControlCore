package org.example;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
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
import java.util.logging.Level;

public class ChatControlCore extends JavaPlugin implements Listener {
    private ChatControlCommandExecutor commandExecutor;
    private ChatControlEventListener eventListener;
    static final String VERSION_FILE_URL = "https://raw.githubusercontent.com/Folzy228/ChatControlCore/main/latest_version_ccc.txt";
    private String latestVersion;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        copyResource("players.yml");

        commandExecutor = new ChatControlCommandExecutor(this);
        eventListener = new ChatControlEventListener(this);
        getCommand("ccc").setExecutor(commandExecutor);
        getServer().getPluginManager().registerEvents(eventListener, this);
        getServer().getPluginManager().registerEvents(this, this);
        reloadConfig();
        checkForUpdates();
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (eventListener != null) {
            eventListener.reloadConfig();
        } else {
            getLogger().warning("EventListener не инициализирован.");
        }
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
        if (player.isOp() && latestVersion != null) {
            String currentVersion = getDescription().getVersion();
            if (!latestVersion.equals(currentVersion)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        String message = "Доступна новая версия плагина! Текущая версия: " + currentVersion + ", последняя версия: " + latestVersion;
                        player.sendMessage(message);
                    }
                }.runTaskLater(ChatControlCore.this, 100L); // 100 тиков = 5 секунд (20 тиков в секунду)
            }
        }
    }
}
