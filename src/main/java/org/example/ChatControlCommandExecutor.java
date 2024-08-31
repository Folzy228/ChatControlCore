package org.example;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

public class ChatControlCommandExecutor implements CommandExecutor, Listener {
    private final ChatControlCore plugin;
    private final Map<UUID, Long> mutedPlayers = new HashMap<>();
    private final GroupManager groupManager;

    public ChatControlCommandExecutor(ChatControlCore plugin, GroupManager groupManager) {
        this.plugin = plugin;
        this.groupManager = groupManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean isPlayer = sender instanceof Player;
        Player player = isPlayer ? (Player) sender : null;

        if (label.equalsIgnoreCase("ccc")) {
            if (args.length > 0) {
                switch (args[0].toLowerCase()) {
                    case "reload":
                        if (isPlayer && !groupManager.hasPermission(player, "chatcontrolcore.command.reload")) {
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
                        if (isPlayer && !groupManager.hasPermission(player, "chatcontrolcore.command.clear")) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : У вас нет разрешения на использование этой команды!");
                            return true;
                        }
                        clearChat(sender);
                        return true;

                    case "update":
                        if (isPlayer && !groupManager.hasPermission(player, "chatcontrolcore.command.update")) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : У вас нет разрешения на использование этой команды!");
                            return true;
                        }
                        checkForUpdates(sender);
                        return true;

                    case "whitelist":
                        if (isPlayer && !groupManager.hasPermission(player, "chatcontrolcore.command.whitelist")) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : У вас нет разрешения на использование этой команды!");
                            return true;
                        }
                        if (args.length < 2) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Используйте /ccc whitelist <add|remove> <ник_игрока>");
                            return true;
                        }
                        switch (args[1].toLowerCase()) {
                            case "add":
                                if (args.length < 3) {
                                    sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Используйте /ccc whitelist add <ник_игрока>");
                                    return true;
                                }
                                addPlayerToWhitelist(sender, args[2]);
                                return true;

                            case "remove":
                                if (args.length < 3) {
                                    sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Используйте /ccc whitelist remove <ник_игрока>");
                                    return true;
                                }
                                removePlayerFromWhitelist(sender, args[2]);
                                return true;

                            default:
                                sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Неизвестная подкоманда. Используйте /ccc whitelist <add|remove>");
                                return true;
                        }

                    case "mute":
                        if (isPlayer && !groupManager.hasPermission(player, "chatcontrolcore.command.mute")) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : У вас нет разрешения на использование этой команды!");
                            return true;
                        }
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Используйте /ccc mute <ник_игрока> <время_в_секундах>");
                            return true;
                        }
                        String playerName = args[1];
                        long muteDuration;
                        try {
                            muteDuration = Long.parseLong(args[2]) * 1000;
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Время должно быть числом!");
                            return true;
                        }
                        mutePlayer(sender, playerName, muteDuration);
                        return true;

                    case "unmute":
                        if (isPlayer && !groupManager.hasPermission(player, "chatcontrolcore.command.mute")) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : У вас нет разрешения на использование этой команды!");
                            return true;
                        }
                        if (args.length < 2) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Используйте /ccc unmute <ник_игрока>");
                            return true;
                        }
                        unmutePlayer(sender, args[1]);
                        return true;

                    case "user":
                        if (isPlayer && !groupManager.hasPermission(player, "chatcontrolcore.command.user")) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : У вас нет разрешения на использование этой команды!");
                            return true;
                        }
                        if (args.length < 4 || !args[2].equalsIgnoreCase("add")) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Используйте /ccc user <ник_игрока> add <название_группы>");
                            return true;
                        }
                        String playerName1 = args[1];
                        String groupName = args[3];
                        if (!groupManager.groupExists(groupName)) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Группа " + groupName + " не найдена!");
                            return true;
                        }
                        addUserToGroup(sender, playerName1, groupName);
                        return true;

                    case "private":
                        if (isPlayer && !groupManager.hasPermission(player, "chatcontrolcore.command.private")) {
                            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : У вас нет разрешения на использование этой команды!");
                            return true;
                        }
                        if (args.length < 2) {
                            sender.sendMessage(ChatColor.RED + "Используйте /ccc private <create|add|accept|decline|off|leave> [аргументы]");
                            return true;
                        }
                        switch (args[1].toLowerCase()) {
                            case "create":
                                if (args.length < 3) {
                                    sender.sendMessage(ChatColor.RED + "Используйте /ccc private create <название_чата>");
                                    return true;
                                }
                                createPrivateChat(player, args[2]);
                                return true;

                            case "add":
                                if (args.length < 3) {
                                    sender.sendMessage(ChatColor.RED + "Используйте /ccc private add <ник_игрока>");
                                    return true;
                                }
                                addPlayerToPrivateChat(player, args[2]);
                                return true;

                            case "accept":
                                if (!groupManager.hasPermission(player, "chatcontrolcore.command.private.acceptdecline")) {
                                    sender.sendMessage(ChatColor.RED + "[ChatControlCore] : У вас нет разрешения на использование этой команды!");
                                    return true;
                                }
                                acceptPrivateChatRequest(player);
                                return true;

                            case "decline":
                                if (!groupManager.hasPermission(player, "chatcontrolcore.command.private.acceptdecline")) {
                                    sender.sendMessage(ChatColor.RED + "[ChatControlCore] : У вас нет разрешения на использование этой команды!");
                                    return true;
                                }
                                declinePrivateChatRequest(player);
                                return true;

                            case "off":
                                disablePrivateChat(player);
                                return true;

                            case "leave":
                                leavePrivateChat(player);
                                return true;

                            default:
                                sender.sendMessage(ChatColor.RED + "Неизвестная подкоманда. Используйте /ccc private <create|add|accept|decline|off|leave>");
                                return true;
                        }

                    default:
                        sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Неизвестная команда. Используйте /ccc <reload|clear|whitelist|update|savemessage|mute>");
                        return true;
                }
            }
        }
        return false;
    }

    private void mutePlayer(CommandSender sender, String playerName, long duration) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Игрок с ником " + playerName + " не найден!");
            return;
        }
        plugin.getMutedPlayers().put(player.getUniqueId(), System.currentTimeMillis() + duration);
        sender.sendMessage(ChatColor.GREEN + "[ChatControlCore] : Игрок " + playerName + " был заблокирован в чате на " + (duration / 1000) + " секунд!");
        player.sendMessage(ChatColor.RED + "Вы заблокированы в чате на " + (duration / 1000) + " секунд!");
    }

    private void unmutePlayer(CommandSender sender, String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Игрок с ником " + playerName + " не найден!");
            return;
        }
        UUID playerId = player.getUniqueId();
        if (plugin.getMutedPlayers().containsKey(playerId)) {
            plugin.getMutedPlayers().remove(playerId);
            player.sendMessage(ChatColor.GREEN + "Вы были разблокированы в чате!");
            sender.sendMessage(ChatColor.GREEN + "[ChatControlCore] : Игрок " + playerName + " был разблокирован в чате!");
        } else {
            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Игрок " + playerName + " не находится в муте!");
        }
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

    private void addUserToGroup(CommandSender sender, String playerName, String groupName) {
        File groupsFile = new File(plugin.getDataFolder(), "groups.yml");
        FileConfiguration groupsConfig = YamlConfiguration.loadConfiguration(groupsFile);
        if (!groupsConfig.contains("Groups." + groupName)) {
            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Группа " + groupName + " не найдена!");
            return;
        }

        File playersFile = new File(plugin.getDataFolder(), "group_players.yml");
        FileConfiguration playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        playersConfig.set("Players." + playerName + ".Group", groupName);

        try {
            playersConfig.save(playersFile);
            sender.sendMessage(ChatColor.GREEN + "[ChatControlCore] : Игрок " + playerName + " добавлен в группу " + groupName + "!");
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "[ChatControlCore] : Не удалось сохранить изменения в group_players.yml!");
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (mutedPlayers.containsKey(playerId)) {
            long muteEndTime = mutedPlayers.get(playerId);
            if (System.currentTimeMillis() > muteEndTime) {
                mutedPlayers.remove(playerId);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (mutedPlayers.containsKey(playerId)) {
        }
    }

    private void createPrivateChat(Player player, String chatName) {
        UUID playerId = player.getUniqueId();
        boolean isInChat = plugin.getPrivateChats().values().stream()
                .anyMatch(c -> c.getOwner().equals(playerId) || c.getMembers().contains(playerId));
        if (isInChat) {
            player.sendMessage(ChatColor.RED + "Вы уже состоите в приватном чате. Выйдите из текущего чата перед созданием нового.");
            return;
        }

        if (plugin.getPrivateChats().containsKey(chatName)) {
            player.sendMessage(ChatColor.RED + "Приватный чат с таким названием уже существует.");
            return;
        }
        PrivateChat chat = new PrivateChat(playerId, chatName);
        chat.addMember(playerId);
        plugin.getPrivateChats().put(chatName, chat);
        player.sendMessage(ChatColor.GREEN + "Приватный чат " + chatName + " создан. Используйте префикс '.' для отправки сообщений в этот чат.");
    }

    private void leavePrivateChat(Player player) {
        UUID playerId = player.getUniqueId();
        PrivateChat chat = plugin.getPrivateChats().values().stream()
                .filter(c -> c.getMembers().contains(playerId))
                .findFirst()
                .orElse(null);
        if (chat == null) {
            player.sendMessage(ChatColor.RED + "Вы не состоите в приватном чате.");
            return;
        }
        chat.getMembers().remove(playerId);
        player.sendMessage(ChatColor.GREEN + "Вы вышли из приватного чата " + chat.getName() + ".");
        for (UUID memberId : chat.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                member.sendMessage(ChatColor.RED + "Игрок " + player.getName() + " вышел из приватного чата " + chat.getName() + ".");
            }
        }
        if (chat.getMembers().isEmpty()) {
            plugin.getPrivateChats().remove(chat.getName());
            player.sendMessage(ChatColor.GREEN + "Приватный чат " + chat.getName() + " был удален, так как в нем больше нет участников.");
        }
    }


    private void addPlayerToPrivateChat(Player sender, String playerName) {
        UUID senderId = sender.getUniqueId();
        PrivateChat chat = plugin.getPrivateChats().values().stream()
                .filter(c -> c.getOwner().equals(senderId))
                .findFirst()
                .orElse(null);

        if (chat == null) {
            sender.sendMessage(ChatColor.RED + "Вы ещё не создали приватный чат.");
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Игрок " + playerName + " не найден.");
            return;
        }

        targetPlayer.sendMessage(ChatColor.GREEN + "Игрок " + sender.getName() + " приглашает вас присоединиться к приватному чату " + chat.getName() + ".");
        targetPlayer.sendMessage(ChatColor.GREEN + "Введите /ccc private accept для присоединения или /ccc private decline для отказа.");

        plugin.getChatRequests().put(targetPlayer.getUniqueId(), new PrivateChatRequest(chat, senderId));
        sender.sendMessage(ChatColor.GREEN + "Запрос на присоединение отправлен игроку " + playerName + ".");
    }

    private void acceptPrivateChatRequest(Player player) {
        UUID playerId = player.getUniqueId();
        PrivateChatRequest request = plugin.getChatRequests().remove(playerId);
        if (request == null) {
            player.sendMessage(ChatColor.RED + "У вас нет активных приглашений в приватный чат.");
            return;
        }
        PrivateChat chat = request.getChat();
        chat.addMember(playerId);
        player.sendMessage(ChatColor.GREEN + "Вы присоединились к приватному чату " + chat.getName() + ".");
        Player owner = Bukkit.getPlayer(request.getOwner());
        if (owner != null) {
            owner.sendMessage(ChatColor.GREEN + "Игрок " + player.getName() + " присоединился к вашему приватному чату.");
        }
    }

    private void declinePrivateChatRequest(Player player) {
        UUID playerId = player.getUniqueId();
        PrivateChatRequest request = plugin.getChatRequests().remove(playerId);
        if (request == null) {
            player.sendMessage(ChatColor.RED + "У вас нет активных приглашений в приватный чат.");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Вы отклонили приглашение в приватный чат " + request.getChat().getName() + ".");
        Player owner = Bukkit.getPlayer(request.getOwner());
        if (owner != null) {
            owner.sendMessage(ChatColor.RED + "Игрок " + player.getName() + " отклонил приглашение в ваш приватный чат.");
        }
    }

    private void disablePrivateChat(Player player) {
        UUID playerId = player.getUniqueId();
        PrivateChat chat = plugin.getPrivateChats().values().stream()
                .filter(c -> c.getOwner().equals(playerId))
                .findFirst()
                .orElse(null);
        if (chat == null) {
            player.sendMessage(ChatColor.RED + "Вы не создали приватный чат.");
            return;
        }
        plugin.getPrivateChats().remove(chat.getName());
        for (UUID memberId : chat.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                if (memberId.equals(playerId)) {
                    member.sendMessage(ChatColor.GREEN + "Приватный чат " + chat.getName() + " отключен.");
                } else {
                    member.sendMessage(ChatColor.RED + "Приватный чат " + chat.getName() + " был отключен.");
                }
            }
        }
    }

    public boolean isPlayerMuted(UUID playerId) {
        Long muteEndTime = plugin.getMutedPlayers().get(playerId);
        return muteEndTime != null && System.currentTimeMillis() < muteEndTime;
    }
}

class PrivateChat {
    private final UUID owner;
    private final String name;
    private final Set<UUID> members;

    public PrivateChat(UUID owner, String name) {
        this.owner = owner;
        this.name = name;
        this.members = new HashSet<>(); // Инициализация Set
    }

    public UUID getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public void addMember(UUID memberId) {
        members.add(memberId);
    }
}

class PrivateChatRequest {
    private final PrivateChat chat;
    private final UUID owner;

    public PrivateChatRequest(PrivateChat chat, UUID owner) {
        this.chat = chat;
        this.owner = owner;
    }

    public PrivateChat getChat() {
        return chat;
    }

    public UUID getOwner() {
        return owner;
    }
}
