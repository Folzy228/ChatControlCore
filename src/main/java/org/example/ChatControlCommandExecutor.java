package org.example;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class ChatControlCommandExecutor implements CommandExecutor {
    private final ChatControlCore plugin;

    public ChatControlCommandExecutor(ChatControlCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("ccc")) {
            if (args.length > 0) {
                switch (args[0].toLowerCase()) {
                    case "reload":
                        if (!sender.hasPermission("chatcontrolcore.command.reload")) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : У вас нет разрешения на использование этой команды!");
                            return true;
                        }
                        try {
                            plugin.reloadConfig();
                            sender.sendMessage(ChatColor.GREEN + "[ChatControlCore] : Плагин успешно перезагружен!");
                        } catch (Exception e) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Плагин не перезагружен из-за ошибки!");
                            e.printStackTrace();
                        }
                        return true;

                    case "clear":
                        if (!sender.hasPermission("chatcontrolcore.command.clear")) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : У вас нет разрешения на использование этой команды!");
                            return true;
                        }
                        clearChat(sender);
                        return true;

                    case "whitelist":
                        if (!sender.hasPermission("chatcontrolcore.command.whitelist")) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : У вас нет разрешения на использование этой команды!");
                            return true;
                        }
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Используйте /ccc whitelist <add|remove> <ник игрока>");
                            return true;
                        }
                        String action = args[1].toLowerCase();
                        String playerName = args[2];
                        if (action.equals("add")) {
                            addPlayerToWhitelist(sender, playerName);
                        } else if (action.equals("remove")) {
                            removePlayerFromWhitelist(sender, playerName);
                        } else {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Неизвестное действие. Используйте add или remove.");
                        }
                        return true;

                    case "update":
                        if (!sender.hasPermission("chatcontrolcore.command.update")) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : У вас нет разрешения на использование этой команды!");
                            return true;
                        }
                        checkForUpdates(sender);
                        return true;

                    default:
                        sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Неизвестная команда. Используйте /ccc <reload|clear|whitelist|update>");
                        return true;
                }
            }
        }
        return false;
    }
    private void clearChat(CommandSender sender) {
        int linesToClear = plugin.getConfig().getInt("clear_chat.clear_lines", -1);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            for (int i = 0; i < (linesToClear == -1 ? 100 : linesToClear); i++) {
                player.sendMessage("");
            }
        }
        sender.sendMessage(ChatColor.GREEN + "[ChatControlCore] : Чат был очищен!");
    }

    private void addPlayerToWhitelist(CommandSender sender, String playerName) {
        File playersFile = new File(plugin.getDataFolder(), "players.yml");
        FileConfiguration playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        List<String> players = playersConfig.getStringList("Players");

        if (players.contains(playerName)) {
            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Игрок " + playerName + " уже находится в белом списке!");
            return;
        }

        players.add(playerName);
        playersConfig.set("Players", players);
        try {
            playersConfig.save(playersFile);
            sender.sendMessage(ChatColor.GREEN + "[ChatControlCore] : Игрок " + playerName + " добавлен в белый список!");
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Не удалось сохранить изменения в players.yml!");
            e.printStackTrace();
        }
    }

    private void removePlayerFromWhitelist(CommandSender sender, String playerName) {
        File playersFile = new File(plugin.getDataFolder(), "players.yml");
        FileConfiguration playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        List<String> players = playersConfig.getStringList("Players");

        if (!players.contains(playerName)) {
            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Игрок " + playerName + " не найден в белом списке!");
            return;
        }

        players.remove(playerName);
        playersConfig.set("Players", players);
        try {
            playersConfig.save(playersFile);
            sender.sendMessage(ChatColor.GREEN + "[ChatControlCore] : Игрок " + playerName + " удалён из белого списка!");
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Не удалось сохранить изменения в players.yml!");
            e.printStackTrace();
        }
    }

    private void checkForUpdates(CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(ChatControlCore.VERSION_FILE_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String latestVersion = reader.readLine().trim();
                reader.close();

                String currentVersion = plugin.getDescription().getVersion();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!latestVersion.equals(currentVersion)) {
                        sender.sendMessage(ChatColor.GREEN + "[ChatControlCore] : Доступна новая версия плагина! Текущая версия: " + currentVersion + ", последняя версия: " + latestVersion);
                    } else {
                        sender.sendMessage(ChatColor.GREEN + "[ChatControlCore] : Плагин обновлён до последней версии.");
                    }
                });

            } catch (IOException e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Ошибка при проверке обновлений!");
                });
                e.printStackTrace();
            }
        });
    }
}
