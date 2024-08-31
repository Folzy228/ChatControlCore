package org.example;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class AutoMessage {
    private final JavaPlugin plugin;
    private final Map<String, AutoMessageTask> tasks = new HashMap<>();

    public AutoMessage(JavaPlugin plugin) {
        this.plugin = plugin;
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("auto_message");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String message = ChatColor.translateAlternateColorCodes('&', section.getString(key + ".text_automessage", "&f&l[&4&lSERVER&f&l] : &f&lПриветствуем тебя на сервере!"));
                long interval = section.getLong(key + ".message_time", 60) * 20;
                boolean isEnabled = section.getBoolean(key + ".on_automessage", true);

                if (isEnabled) {
                    AutoMessageTask task = new AutoMessageTask(message, interval);
                    task.runTaskTimer(plugin, 0, interval);
                    tasks.put(key, task);
                }
            }
        }
    }

    private static class AutoMessageTask extends BukkitRunnable {
        private final String message;
        private final long interval;

        public AutoMessageTask(String message, long interval) {
            this.message = message;
            this.interval = interval;
        }

        @Override
        public void run() {
            Bukkit.broadcastMessage(message);
        }
    }
}
