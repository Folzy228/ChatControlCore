package org.example;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AntiAFK implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, Long> playerLastMoveTime = new HashMap<>();
    private final Map<UUID, BukkitRunnable> afkTasks = new HashMap<>();
    private final List<String> whitelist;

    public AntiAFK(JavaPlugin plugin) {
        this.plugin = plugin;
        File playersFile = new File(plugin.getDataFolder(), "players.yml");
        FileConfiguration playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        this.whitelist = playersConfig.getStringList("Players");
        startAFKCheckTask();
    }

    private void startAFKCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfig().getBoolean("afk.anti_afk", true)) {
                    return;
                }

                long afkTime = plugin.getConfig().getLong("afk.afk_time", 60) * 1000;
                long afkOffTime = plugin.getConfig().getLong("afk.afk_off_time", 30) * 1000;
                String afkMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("afk.afk_message", "&f&l[&4&lАНТИ-АФК&f&l] : Ты уже довольно продолжительно стоишь в афк, пошеевелись если ты тут."));
                String kickMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("afk.kick_afk_message", "&f&l[&4&lАНТИ-АФК&f&l] : Игрок %s был кикнут за АФК!"));
                String goodNoAfkMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("afk.good_no_afk_message", "&f&l[&4&lАНТИ-АФК&f&l] : Вижу ты тут!"));

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (whitelist.contains(player.getName())) {
                        continue;
                    }

                    UUID playerId = player.getUniqueId();
                    long currentTime = System.currentTimeMillis();

                    if (!playerLastMoveTime.containsKey(playerId)) {
                        playerLastMoveTime.put(playerId, currentTime);
                    }

                    long lastMoveTime = playerLastMoveTime.get(playerId);

                    if (currentTime - lastMoveTime >= afkTime) {
                        if (!afkTasks.containsKey(playerId)) {
                            player.sendMessage(afkMessage);
                            BukkitRunnable afkTask = new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (System.currentTimeMillis() - playerLastMoveTime.get(playerId) >= afkOffTime) {
                                        Bukkit.broadcastMessage(String.format(kickMessage, player.getName()));
                                        player.kickPlayer("");
                                    }
                                }
                            };
                            afkTask.runTaskLater(plugin, afkOffTime / 50);
                            afkTasks.put(playerId, afkTask);
                        }
                    } else {
                        if (afkTasks.containsKey(playerId)) {
                            afkTasks.get(playerId).cancel();
                            afkTasks.remove(playerId);
                            player.sendMessage(goodNoAfkMessage);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20, 20);
    }

    public void updatePlayerMoveTime(Player player) {
        UUID playerId = player.getUniqueId();
        playerLastMoveTime.put(playerId, System.currentTimeMillis());
        if (afkTasks.containsKey(playerId)) {
            afkTasks.get(playerId).cancel();
            afkTasks.remove(playerId);
            String goodNoAfkMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("afk.good_no_afk_message", "&f&l[&4&lАНТИ-АФК&f&l] : Вижу ты тут!"));
            player.sendMessage(goodNoAfkMessage);
        }
    }

    public void updateWhitelist(List<String> newWhitelist) {
        this.whitelist.clear();
        this.whitelist.addAll(newWhitelist);
    }
}
