package com.example.squidgame;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LeaderboardManager {

    private final SquidGame plugin;
    private File file;
    private FileConfiguration config;

    public LeaderboardManager(SquidGame plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        file = new File(plugin.getDataFolder(), "leaderboard.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("leaderboard.yml oluşturulamadı!");
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void addWin(String playerName) {
        int wins = config.getInt("players." + playerName + ".wins", 0);
        config.set("players." + playerName + ".wins", wins + 1);
        addTotalGame(playerName);
        save();
    }

    public void addLoss(String playerName) {
        int losses = config.getInt("players." + playerName + ".losses", 0);
        config.set("players." + playerName + ".losses", losses + 1);
        addTotalGame(playerName);
        save();
    }

    private void addTotalGame(String playerName) {
        int played = config.getInt("players." + playerName + ".played", 0);
        config.set("players." + playerName + ".played", played + 1);
    }

    public void recordFinishTime(String playerName, int seconds) {
        int best = config.getInt("players." + playerName + ".fastestTime", -1);
        if (best == -1 || seconds < best) {
            config.set("players." + playerName + ".fastestTime", seconds);
            save();
        }
    }

    public int getWins(String playerName) {
        return config.getInt("players." + playerName + ".wins", 0);
    }

    public int getLosses(String playerName) {
        return config.getInt("players." + playerName + ".losses", 0);
    }

    public int getPlayed(String playerName) {
        return config.getInt("players." + playerName + ".played", 0);
    }

    public int getFastestTime(String playerName) {
        return config.getInt("players." + playerName + ".fastestTime", -1);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("leaderboard.yml kaydedilemedi!");
        }
    }

    public void showLeaderboard(CommandSender sender) {
        if (config.getConfigurationSection("players") == null) {
            sender.sendMessage("§cHenüz kimse kazanmadı.");
            return;
        }

        Map<String, Integer> wins = new HashMap<>();
        for (String key : config.getConfigurationSection("players").getKeys(false)) {
            wins.put(key, config.getInt("players." + key + ".wins", 0));
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<>(wins.entrySet());
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        sender.sendMessage("§6--- §eSquid Game Liderlik Tablosu §6---");
        int count = 1;
        for (Map.Entry<String, Integer> entry : list) {
            if (count > 10) break;
            sender.sendMessage("§e" + count + ". §a" + entry.getKey() + " §7- §b" + entry.getValue() + " Galibiyet");
            count++;
        }
    }

    public String getTopWinnerName() {
        String topPlayer = "Yok";
        int maxWins = -1;
        if (config.contains("players")) {
            for (String p : config.getConfigurationSection("players").getKeys(false)) {
                int w = config.getInt("players." + p + ".wins", 0);
                if (w > maxWins) {
                    maxWins = w;
                    topPlayer = p;
                }
            }
        }
        return topPlayer;
    }
    
    public int getTopWinnerScore() {
        int maxWins = 0;
        if (config.contains("players")) {
            for (String p : config.getConfigurationSection("players").getKeys(false)) {
                int w = config.getInt("players." + p + ".wins", 0);
                if (w > maxWins) {
                    maxWins = w;
                }
            }
        }
        return maxWins;
    }
}
