package com.example.squidgame;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PointsManager {

    private final JavaPlugin plugin;
    private File pointsFile;
    private FileConfiguration pointsConfig;

    public PointsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        createPointsFile();
    }

    private void createPointsFile() {
        pointsFile = new File(plugin.getDataFolder(), "points.yml");
        if (!pointsFile.exists()) {
            pointsFile.getParentFile().mkdirs();
            try {
                pointsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        pointsConfig = YamlConfiguration.loadConfiguration(pointsFile);
    }

    private java.util.Map<UUID, Long> pointAnimations = new java.util.HashMap<>();
    private java.util.Map<UUID, Integer> pointChanges = new java.util.HashMap<>();

    public int getPoints(UUID uuid) {
        return pointsConfig.getInt(uuid.toString(), 0);
    }

    public void addPoints(UUID uuid, int amount) {
        int current = getPoints(uuid);
        pointsConfig.set(uuid.toString(), current + amount);
        pointAnimations.put(uuid, System.currentTimeMillis() + 5000);
        pointChanges.put(uuid, amount);
        savePointsFile();
    }

    public void removePoints(UUID uuid, int amount) {
        int current = getPoints(uuid);
        pointsConfig.set(uuid.toString(), Math.max(0, current - amount));
        pointAnimations.put(uuid, System.currentTimeMillis() + 5000);
        pointChanges.put(uuid, -amount);
        savePointsFile();
    }
    
    public boolean hasAnimation(UUID uuid) {
        if (pointAnimations.containsKey(uuid)) {
            if (System.currentTimeMillis() < pointAnimations.get(uuid)) {
                return true;
            } else {
                pointAnimations.remove(uuid);
                pointChanges.remove(uuid);
            }
        }
        return false;
    }
    
    public int getAnimationChange(UUID uuid) {
        return pointChanges.getOrDefault(uuid, 0);
    }

    private void savePointsFile() {
        try {
            pointsConfig.save(pointsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
