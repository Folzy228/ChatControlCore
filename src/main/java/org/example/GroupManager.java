package org.example;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class GroupManager {
    private final ChatControlCore plugin;
    private final Map<Player, PermissionAttachment> playerPermissions = new HashMap<>();
    private FileConfiguration groupConfig;
    private File groupFile;
    private FileConfiguration playersConfig;
    private File playersFile;

    public GroupManager(ChatControlCore plugin) {
        this.plugin = plugin;
        loadGroupConfig();
        loadPlayerConfig();
    }

    private void loadGroupConfig() {
        groupFile = new File(plugin.getDataFolder(), "groups.yml");
        if (!groupFile.exists()) {
            plugin.saveResource("groups.yml", false);
        }
        groupConfig = YamlConfiguration.loadConfiguration(groupFile);
    }

    private void loadPlayerConfig() {
        playersFile = new File(plugin.getDataFolder(), "group_players.yml");
        if (!playersFile.exists()) {
            plugin.saveResource("group_players.yml", false);
        }
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }

    public void savePlayerConfig() {
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save group_players.yml", e);
        }
    }

    public String getPrefix(String groupName) {
        return groupConfig.getString("Groups." + groupName + ".Prefix", "");
    }

    public String getSuffix(String groupName) {
        return groupConfig.getString("Groups." + groupName + ".Suffix", "");
    }

    public void addGroupToPlayer(Player player, String groupName) {
        String playerName = player.getName();
        playersConfig.set("Players." + playerName + ".Group", groupName);
        savePlayerConfig();
        applyPermissions(player, groupName);
        player.sendMessage("Вы добавлены в группу " + groupName);
    }

    private void applyPermissions(Player player, String groupName) {
        List<String> permissionsList = groupConfig.getStringList("Groups." + groupName + ".Permissions");
        PermissionAttachment attachment = playerPermissions.computeIfAbsent(player, p -> p.addAttachment(plugin));
        for (String permission : permissionsList) {
            attachment.setPermission(permission, true);
        }
    }

    public void removePermissions(Player player) {
        PermissionAttachment attachment = playerPermissions.remove(player);
        if (attachment != null) {
            player.removeAttachment(attachment);
        }
    }

    public String getDefaultGroup() {
        for (String group : groupConfig.getConfigurationSection("Groups").getKeys(false)) {
            if (groupConfig.getBoolean("Groups." + group + ".Default", false)) {
                return group;
            }
        }
        return null;
    }

    public void addDefaultGroupToPlayer(Player player) {
        String defaultGroup = getDefaultGroup();
        if (defaultGroup != null) {
            addGroupToPlayer(player, defaultGroup);
        } else {
            plugin.getLogger().warning("Дефолтная группа не найдена.");
        }
    }

    public String getPlayerGroup(Player player) {
        String playerName = player.getName();
        return playersConfig.getString("Players." + playerName + ".Group", getDefaultGroup());
    }

    public void loadPlayerData(Player player) {
        String playerName = player.getName();
        if (!playersConfig.contains("Players." + playerName)) {
            addDefaultGroupToPlayer(player);
        } else {
            String groupName = getPlayerGroup(player);
            applyPermissions(player, groupName);
        }
    }

    public boolean hasPermission(Player player, String permission) {
        if (player.isOp()) {
            return true;
        }
        String groupName = getPlayerGroup(player);
        List<String> permissionsList = groupConfig.getStringList("Groups." + groupName + ".Permissions");
        return permissionsList.contains(permission) || player.hasPermission(permission);
    }


    public boolean groupExists(String groupName) {
        return groupConfig.contains("Groups." + groupName);
    }

    public boolean hasPlayerGroup(Player player) {
        String playerName = player.getName();
        return playersConfig.contains("Players." + playerName + ".Group");
    }


    public void savePlayerData(Player player) {
        savePlayerConfig();
    }

    public void reloadConfig() {
        loadGroupConfig();
        loadPlayerConfig();
    }
}
