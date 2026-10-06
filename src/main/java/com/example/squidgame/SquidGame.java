package com.example.squidgame;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;

public class SquidGame extends JavaPlugin implements CommandExecutor {

    private RedLightGreenLight game;
    private LeaderboardManager leaderboardManager;
    private MenuManager menuManager;
    private PointsManager pointsManager;
    private PetManager petManager;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        leaderboardManager = new LeaderboardManager(this);
        pointsManager = new PointsManager(this);
        petManager = new PetManager(this, pointsManager);
        game = new RedLightGreenLight(this, leaderboardManager, pointsManager, petManager);
        menuManager = new MenuManager(this, game);
        
        getServer().getPluginManager().registerEvents(game, this);
        getServer().getPluginManager().registerEvents(menuManager, this);
        
        getCommand("squidgame").setExecutor(this);
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SquidGameExpansion(game, leaderboardManager).register();
        }
        
        getLogger().info("SquidGame plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.stopGame();
        }
        if (petManager != null) {
            petManager.despawnAll();
        }
        getLogger().info("SquidGame plugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cKullanım: /squidgame [baslat|liste|durdur|setspawn|addstart|clearstarts|npc]");
            return true;
        }

        if (args[0].equalsIgnoreCase("baslat")) {
            if (!sender.hasPermission("squidgame.admin") && !sender.isOp()) return true;
            if (sender instanceof Player) {
                if (game.getState() == RedLightGreenLight.GameState.IDLE) {
                    game.createRoom((Player) sender);
                } else if (game.getState() == RedLightGreenLight.GameState.WAITING || game.getState() == RedLightGreenLight.GameState.STARTING) {
                    game.forceStartLobby();
                    sender.sendMessage("§aOyun zorla başlatılıyor...");
                } else {
                    sender.sendMessage("§cOyun zaten başlamış durumda!");
                }
            } else {
                sender.sendMessage("Bu komut sadece oyundan kullanılabilir.");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("liste")) {
            leaderboardManager.showLeaderboard(sender);
            return true;
        } else if (args[0].equalsIgnoreCase("durdur")) {
            if (!sender.hasPermission("squidgame.admin") && !sender.isOp()) return true;
            game.stopGame();
            sender.sendMessage("§cOyun durduruldu.");
            return true;
        } else if (args[0].equalsIgnoreCase("setwaitinglobby")) {
            if (!sender.hasPermission("squidgame.admin") && !sender.isOp()) return true;
            if (sender instanceof Player) {
                Player p = (Player) sender;
                getConfig().set("waitingLobby.world", p.getLocation().getWorld().getName());
                getConfig().set("waitingLobby.x", p.getLocation().getX());
                getConfig().set("waitingLobby.y", p.getLocation().getY());
                getConfig().set("waitingLobby.z", p.getLocation().getZ());
                getConfig().set("waitingLobby.yaw", p.getLocation().getYaw());
                getConfig().set("waitingLobby.pitch", p.getLocation().getPitch());
                saveConfig();
                p.sendMessage("§a[SquidGame] Oyun öncesi bekleme lobisi başarıyla ayarlandı!");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("setspawn")) {
            if (!sender.hasPermission("squidgame.admin") && !sender.isOp()) return true;
            if (sender instanceof Player) {
                Player p = (Player) sender;
                getConfig().set("spawn.world", p.getLocation().getWorld().getName());
                getConfig().set("spawn.x", p.getLocation().getX());
                getConfig().set("spawn.y", p.getLocation().getY());
                getConfig().set("spawn.z", p.getLocation().getZ());
                getConfig().set("spawn.yaw", p.getLocation().getYaw());
                getConfig().set("spawn.pitch", p.getLocation().getPitch());
                saveConfig();
                p.sendMessage("§a[SquidGame] Lobi/Ölüm noktası (Spawn) başarıyla ayarlandı!");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("addstart")) {
            if (!sender.hasPermission("squidgame.admin") && !sender.isOp()) return true;
            if (sender instanceof Player) {
                Player p = (Player) sender;
                int count = getConfig().getInt("arena.spawnCount", 0) + 1;
                String path = "arena.spawns." + count + ".";
                getConfig().set(path + "world", p.getLocation().getWorld().getName());
                getConfig().set(path + "x", p.getLocation().getX());
                getConfig().set(path + "y", p.getLocation().getY());
                getConfig().set(path + "z", p.getLocation().getZ());
                getConfig().set(path + "yaw", p.getLocation().getYaw());
                getConfig().set(path + "pitch", p.getLocation().getPitch());
                getConfig().set("arena.spawnCount", count);
                saveConfig();
                p.sendMessage("§a[SquidGame] Başlangıç noktası #" + count + " başarıyla eklendi!");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("clearstarts")) {
            if (!sender.hasPermission("squidgame.admin") && !sender.isOp()) return true;
            getConfig().set("arena.spawnCount", 0);
            getConfig().set("arena.spawns", null);
            saveConfig();
            sender.sendMessage("§a[SquidGame] Tüm başlangıç noktaları silindi. Lütfen /squidgame addstart ile yenilerini ekleyin.");
            return true;
        } else if (args[0].equalsIgnoreCase("katil")) {
            if (sender instanceof Player) {
                game.joinRoom((Player) sender);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("menüaç")) {
            if (sender instanceof Player) {
                menuManager.openMenu((Player) sender);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("pet")) {
            if (sender instanceof Player) {
                petManager.openPetMenu((Player) sender);
            }
            return true;
        }

        return false;
    }
}
