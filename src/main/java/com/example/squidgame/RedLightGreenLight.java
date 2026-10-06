package com.example.squidgame;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Effect;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.util.Vector;
import org.bukkit.Color;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class RedLightGreenLight implements Listener {

    public enum GameState {
        IDLE, WAITING, STARTING, PLAYING
    }

    private final SquidGame plugin;
    private final LeaderboardManager leaderboard;
    private final PointsManager pointsManager;
    private final PetManager petManager;
    
    private GameState state = GameState.IDLE;
    private boolean isRedLight = false;
    
    private BukkitRunnable lobbyTask;
    private BukkitRunnable countdownTask;
    private BukkitRunnable timerTask;
    private BukkitRunnable lobbyScoreboardTask;
    
    private int lobbyTime = 15;
    private int currentDelay = 100;
    private int cycleCount = 0; 
    private int timeLeft = 120; // 2 dakika
    
    private Set<UUID> activePlayers = new HashSet<>();
    private Set<UUID> spectators = new HashSet<>();
    private Map<UUID, Scoreboard> personalScoreboards = new HashMap<>();
    private List<Integer> availableSpawns = new ArrayList<>();
    private Map<UUID, Integer> playerSpawnIds = new HashMap<>();
    private Scoreboard gameScoreboard;
    private Objective objective;
    private Random random = new Random();

    public RedLightGreenLight(SquidGame plugin, LeaderboardManager leaderboard, PointsManager pointsManager, PetManager petManager) {
        this.plugin = plugin;
        this.leaderboard = leaderboard;
        this.pointsManager = pointsManager;
        this.petManager = petManager;
        startLobbyScoreboardTask();
    }
    
    private void startLobbyScoreboardTask() {
        lobbyScoreboardTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state == GameState.IDLE || state == GameState.WAITING || (state == GameState.STARTING && lobbyTime > 0)) {
                    ScoreboardManager manager = Bukkit.getScoreboardManager();
                    
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!spectators.contains(p.getUniqueId())) {
                            Scoreboard board = personalScoreboards.get(p.getUniqueId());
                            Objective obj = null;
                            if (board == null) {
                                board = manager.getNewScoreboard();
                                obj = board.registerNewObjective("lobby", "dummy");
                                obj.setDisplaySlot(DisplaySlot.SIDEBAR);
                                obj.setDisplayName("§6§lSQUID GAME LOBİ");
                                personalScoreboards.put(p.getUniqueId(), board);
                                p.setScoreboard(board);
                            } else {
                                obj = board.getObjective("lobby");
                                if (obj == null) {
                                    obj = board.registerNewObjective("lobby", "dummy");
                                    obj.setDisplaySlot(DisplaySlot.SIDEBAR);
                                    obj.setDisplayName("§6§lSQUID GAME LOBİ");
                                }
                            }
                            
                            if (p.getScoreboard() != board) {
                                p.setScoreboard(board);
                            }
                            
                            for (String entry : board.getEntries()) {
                                board.resetScores(entry);
                            }
                            
                            String name = p.getName();
                            obj.getScore("§1").setScore(10);
                            obj.getScore("§7Oyuncu: §a" + name).setScore(9);
                            obj.getScore("§2").setScore(8);
                            obj.getScore("§7Oynanan Oyun: §e" + leaderboard.getPlayed(name)).setScore(7);
                            obj.getScore("§7Kazanılan: §a" + leaderboard.getWins(name)).setScore(6);
                            obj.getScore("§7Kaybedilen: §c" + leaderboard.getLosses(name)).setScore(5);
                            obj.getScore("§3").setScore(4);
                            
                            int points = pointsManager.getPoints(p.getUniqueId());
                            String pointsStr = String.valueOf(points);
                            if (pointsManager.hasAnimation(p.getUniqueId())) {
                                int change = pointsManager.getAnimationChange(p.getUniqueId());
                                String color = change > 0 ? "§a(+" : "§c(";
                                pointsStr += " " + color + change + ")";
                            }
                            obj.getScore("§7Puan: §b" + pointsStr).setScore(3);
                            obj.getScore("§4").setScore(2);

                            int fast = leaderboard.getFastestTime(name);
                            String fastStr = fast == -1 ? "Yok" : formatTime(fast);
                            obj.getScore("§7En Hızlı Bitiş: §b" + fastStr).setScore(1);
                            
                            if (state == GameState.IDLE) {
                                obj.getScore("§eBaşlamak için NPC'ye sağ tıkla!").setScore(0);
                            } else if (state == GameState.WAITING) {
                                obj.getScore("§eOyuncu Bekleniyor...").setScore(0);
                            } else {
                                obj.getScore("§eBaşlamasına: §c" + lobbyTime + " sn").setScore(0);
                            }
                        }
                    }
                }
            }
        };
        lobbyScoreboardTask.runTaskTimer(plugin, 0L, 40L); // Her 2 saniyede bir günceller
    }
    
    public GameState getState() { return state; }
    public int getActivePlayerCount() { return activePlayers.size(); }

    private Location getSpawnLocation() {
        if (plugin.getConfig().contains("spawn.world")) {
            return new Location(
                Bukkit.getWorld(plugin.getConfig().getString("spawn.world")),
                plugin.getConfig().getDouble("spawn.x"),
                plugin.getConfig().getDouble("spawn.y"),
                plugin.getConfig().getDouble("spawn.z"),
                (float) plugin.getConfig().getDouble("spawn.yaw"),
                (float) plugin.getConfig().getDouble("spawn.pitch")
            );
        }
        return null;
    }

    private Location getSpecificStartLocation(int index) {
        String path = "arena.spawns." + index + ".";
        if (plugin.getConfig().contains(path + "world")) {
            return new Location(
                Bukkit.getWorld(plugin.getConfig().getString(path + "world")),
                plugin.getConfig().getDouble(path + "x"),
                plugin.getConfig().getDouble(path + "y"),
                plugin.getConfig().getDouble(path + "z"),
                (float) plugin.getConfig().getDouble(path + "yaw"),
                (float) plugin.getConfig().getDouble(path + "pitch")
            );
        }
        return null;
    }

    public void createRoom(Player p) {
        if (state != GameState.IDLE) {
            p.sendMessage("§cOda zaten mevcut!");
            return;
        }
        
        int count = plugin.getConfig().getInt("arena.spawnCount", 0);
        if (count == 0) {
            p.sendMessage("§cOyun başlangıç noktası ayarlanmamış! Yetkili /squidgame addstart ile noktalar eklemeli.");
            return;
        }
        
        availableSpawns.clear();
        for (int i = 1; i <= count; i++) {
            availableSpawns.add(i);
        }
        Collections.shuffle(availableSpawns);
        playerSpawnIds.clear();
        
        state = GameState.WAITING;
        activePlayers.clear();
        
        net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent("§8[§cSquidGame§8] §e" + p.getName() + " §byeni bir oyun odası kurdu! ");
        net.md_5.bungee.api.chat.TextComponent click = new net.md_5.bungee.api.chat.TextComponent("§a§l[KATILMAK İÇİN TIKLA]");
        click.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/squidgame katil"));
        click.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.ComponentBuilder("§7Hemen oyuna katılmak için buraya tıkla!").create()));
        msg.addExtra(click);
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.spigot().sendMessage(msg);
        }
        
        joinRoom(p);
    }

    private Location getWaitingLobby() {
        if (plugin.getConfig().contains("waitingLobby.world")) {
            return new Location(
                Bukkit.getWorld(plugin.getConfig().getString("waitingLobby.world")),
                plugin.getConfig().getDouble("waitingLobby.x"),
                plugin.getConfig().getDouble("waitingLobby.y"),
                plugin.getConfig().getDouble("waitingLobby.z"),
                (float) plugin.getConfig().getDouble("waitingLobby.yaw"),
                (float) plugin.getConfig().getDouble("waitingLobby.pitch")
            );
        }
        return getSpawnLocation(); // Fallback to normal spawn
    }

    public void joinRoom(Player p) {
        if (state == GameState.IDLE || state == GameState.PLAYING) {
            p.sendMessage("§cŞu an katılabileceğin bir oda yok.");
            return;
        }
        if (activePlayers.contains(p.getUniqueId())) {
            p.sendMessage("§cZaten odadasın!");
            return;
        }
        if (availableSpawns.isEmpty()) {
            p.sendMessage("§cOyun dolu! Bütün başlangıç noktaları kapılmış.");
            return;
        }
        
        int spawnId = availableSpawns.remove(0);
        
        activePlayers.add(p.getUniqueId());
        playerSpawnIds.put(p.getUniqueId(), spawnId);

        
        Location waiting = getWaitingLobby();
        if (waiting != null) {
            p.teleport(waiting);
        }
        
        p.setGameMode(org.bukkit.GameMode.SURVIVAL);
        p.setHealth(20.0);
        p.setFoodLevel(20);
        for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }
        
        p.getInventory().clear();
        
        ItemStack petMenu = new ItemStack(Material.BONE);
        ItemMeta petMeta = petMenu.getItemMeta();
        petMeta.setDisplayName("§aPet Menüsü");
        petMenu.setItemMeta(petMeta);
        p.getInventory().setItem(4, petMenu);

        ItemStack leave = new ItemStack(Material.BED);
        ItemMeta meta = leave.getItemMeta();
        meta.setDisplayName("§cOdadan Ayrıl");
        leave.setItemMeta(meta);
        p.getInventory().setItem(8, leave);
        
        Bukkit.broadcastMessage("§8[§cSquidGame§8] §e" + p.getName() + " §aodaya katıldı! §7(" + activePlayers.size() + " Oyuncu)");
        
        if (state == GameState.WAITING && activePlayers.size() >= 4) {
            startLobbyCountdown(15);
        }
    }
    
    public void forceStartLobby() {
        if (state == GameState.WAITING || state == GameState.STARTING) {
            startLobbyCountdown(3); // OP zorlarsa 3 saniyede başlasın
        }
    }

    private void startLobbyCountdown(int seconds) {
        lobbyTime = seconds;
        
        if (lobbyTask != null) lobbyTask.cancel();
        
        lobbyTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activePlayers.isEmpty()) {
                    Bukkit.broadcastMessage("§8[§cSquidGame§8] §cOdada kimse kalmadığı için başlatılma iptal edildi.");
                    state = GameState.IDLE;
                    this.cancel();
                    return;
                }
                
                if (lobbyTime <= 0) {
                    startGamePlay();
                    this.cancel();
                    return;
                }
                
                if (lobbyTime % 5 == 0 || lobbyTime <= 5) {
                    for (UUID uuid : activePlayers) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.sendMessage("§8[§cSquidGame§8] §eOyunun başlamasına §c" + lobbyTime + " §esaniye kaldı!");
                            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1.0f, 1.0f);
                        }
                    }
                }
                lobbyTime--;
            }
        };
        lobbyTask.runTaskTimer(plugin, 0L, 20L);
    }

    private void startGamePlay() {
        state = GameState.STARTING; // Geri sayım sırasında hâlâ donuk kalmaları için
        isRedLight = false;
        currentDelay = 100;
        cycleCount = 0;
        timeLeft = 120; 
        
        for (UUID uuid : activePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && playerSpawnIds.containsKey(uuid)) {
                p.getInventory().clear();
                Location loc = getSpecificStartLocation(playerSpawnIds.get(uuid));
                if (loc != null) {
                    p.teleport(loc);
                }
            }
        }
        
        setupScoreboard();
        updateScoreboard("§e§lGERİ SAYIM...");
        
        Bukkit.broadcastMessage("§8[§cSquidGame§8] §eOyun birazdan başlıyor! Çizgiyi geçmek için BAŞLA yazısını bekle!");

        countdownTask = new BukkitRunnable() {
            int count = 3;
            @Override
            public void run() {
                if (state != GameState.STARTING) {
                    this.cancel();
                    return;
                }
                
                if (count > 0) {
                    String color = (count == 3) ? "§c" : ((count == 2) ? "§6" : "§e");
                    for (UUID uuid : activePlayers) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            sendTitle(p, color + "§l" + count, "§7Oyun başlıyor, hazırlık yap...", 0, 25, 5);
                            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1.0f, 1.0f);
                        }
                    }
                    count--;
                } else {
                    // Sayım bittiği an durum PLAYING'e geçer ve hareket kilidi açılır
                    state = GameState.PLAYING;
                    for (UUID uuid : activePlayers) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            sendTitle(p, "§a§lBAŞLA!", "§fHemen koşmaya başla!", 0, 30, 10);
                            p.playSound(p.getLocation(), Sound.NOTE_PIANO, 1.0f, 1.0f);
                        }
                    }
                    updateScoreboard("§a§lYEŞİL IŞIK");
                    startTimer(); 
                    scheduleNextLight();
                    this.cancel();
                }
            }
        };
        countdownTask.runTaskTimer(plugin, 0L, 20L);
    }

    private void startTimer() {
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.PLAYING) {
                    this.cancel();
                    return;
                }
                
                if (timeLeft <= 0) {
                    timeUp();
                    this.cancel();
                    return;
                }
                
                timeLeft--;
                updateScoreboard(isRedLight ? "§c§lKIRMIZI IŞIK" : "§a§lYEŞİL IŞIK");
            }
        };
        timerTask.runTaskTimer(plugin, 0L, 20L);
    }

    private void timeUp() {
        Bukkit.broadcastMessage("§8[§cSquidGame§8] §4SÜRE DOLDU! Bitiş çizgisine ulaşamayan herkes eleniyor!");
        for (UUID uuid : activePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                sendTitle(p, "§4SÜRE DOLDU", "§cElendin!", 10, 60, 20);
                p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 1.0f, 0.2f);
                p.getWorld().strikeLightningEffect(p.getLocation()); // Vurucu detay
            }
        }
        
        Set<UUID> remaining = new HashSet<>(activePlayers);
        for (UUID uuid : remaining) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                eliminatePlayer(p);
            }
        }
    }

    private void scheduleNextLight() {
        if (state != GameState.PLAYING) return;
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.PLAYING) return;
                
                isRedLight = !isRedLight;
                if (isRedLight) {
                    currentDelay = 40 + random.nextInt(51); 
                    updateScoreboard("§c§lKIRMIZI IŞIK");
                    for (UUID uuid : activePlayers) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            sendTitle(p, "§cKIRMIZI IŞIK", "§4Hareket etme!", 5, currentDelay - 10, 10);
                            p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 1.0f, 0.5f);
                        }
                    }
                } else {
                    cycleCount++;
                    if (cycleCount <= 2) {
                        currentDelay = 100 - (cycleCount * 15);
                    } else {
                        currentDelay = 20 + random.nextInt(81);
                    }
                    
                    updateScoreboard("§a§lYEŞİL IŞIK");
                    for (UUID uuid : activePlayers) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            String hizBilgisi = cycleCount <= 2 ? ("§a%" + (100 - currentDelay)) : "§dBelirsiz (Rastgele!)";
                            sendActionBar(p, "§eOyun devam ediyor! Bitişe koş!");
                            sendTitle(p, "§aYEŞİL IŞIK", "§eHız: " + hizBilgisi, 5, currentDelay - 10, 10);
                            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
                        }
                    }
                }
                
                scheduleNextLight();
            }
        }.runTaskLater(plugin, currentDelay);
    }

    public void stopGame() {
        state = GameState.IDLE;
        if (lobbyTask != null) lobbyTask.cancel();
        if (countdownTask != null) countdownTask.cancel();
        if (timerTask != null) timerTask.cancel();
        
        activePlayers.clear();

        
        Set<UUID> specs = new HashSet<>(spectators);
        for (UUID uuid : specs) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                leaveSpectator(p);
            }
        }
        spectators.clear();
        
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard emptyScoreboard = manager.getMainScoreboard();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(emptyScoreboard);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!activePlayers.contains(player.getUniqueId())) return; 
        if (player.isDead()) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        // Sadece başlama (geri sayım) aşamasında oyuncuları dondur
        if (state == GameState.STARTING) {
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                Location lock = from.clone();
                lock.setYaw(to.getYaw());
                lock.setPitch(to.getPitch());
                event.setTo(lock);
            }
            return;
        }

        if (state != GameState.PLAYING) return;

        boolean moved = (from.getX() != to.getX() || from.getZ() != to.getZ());

        if (moved && isRedLight) {
            eliminatePlayer(player);
        } else if (moved) {
            Location blockUnder = to.clone().subtract(0, 1, 0);
            if (to.getBlock().getType() == Material.DIAMOND_BLOCK || blockUnder.getBlock().getType() == Material.DIAMOND_BLOCK) {
                winGame(player);
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (spectators.contains(player.getUniqueId())) {
            leaveSpectator(player);
        }
        
        if (playerSpawnIds.containsKey(player.getUniqueId())) {
            availableSpawns.add(playerSpawnIds.remove(player.getUniqueId()));
        }
        
        if (activePlayers.contains(player.getUniqueId())) {
            if (state == GameState.PLAYING) {
                eliminatePlayer(player);
            } else {
                activePlayers.remove(player.getUniqueId());
        
                if (state == GameState.WAITING || state == GameState.STARTING) {
                    if (activePlayers.isEmpty()) {
                        Bukkit.broadcastMessage("§8[§cSquidGame§8] §cOdada kimse kalmadığı için oyun iptal edildi.");
                        state = GameState.IDLE;
                        if (lobbyTask != null) {
                            lobbyTask.cancel();
                        }
                    }
                }
            }
        }
    }

    private void eliminatePlayer(final Player player) {
        activePlayers.remove(player.getUniqueId());
        petManager.despawnPet(player);

        leaderboard.addLoss(player.getName());
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 255, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1, false, false));
        
        Location pLoc = player.getLocation();
        pLoc.setPitch(0);
        
        Vector dir = pLoc.getDirection().normalize().multiply(3);
        Location npcLoc = pLoc.clone().add(dir);
        npcLoc.setY(pLoc.getY());
        npcLoc.setYaw(pLoc.getYaw() + 180);
        npcLoc.setPitch(0);
        
        final ArmorStand guard = (ArmorStand) pLoc.getWorld().spawnEntity(npcLoc, EntityType.ARMOR_STAND);
        guard.setVisible(true);
        guard.setBasePlate(false);
        guard.setArms(true);
        guard.setGravity(false);
        guard.setCustomName("§cGörevli");
        guard.setCustomNameVisible(true);
        
        EntityEquipment eq = guard.getEquipment();
        ItemStack helmet = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        org.bukkit.inventory.meta.SkullMeta sMeta = (org.bukkit.inventory.meta.SkullMeta) helmet.getItemMeta();
        sMeta.setOwner(plugin.getConfig().getString("npc.killer_skin", "SquidGuard"));
        helmet.setItemMeta(sMeta);
        eq.setHelmet(helmet);
        
        ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta cMeta = (LeatherArmorMeta) chest.getItemMeta();
        cMeta.setColor(org.bukkit.Color.fromRGB(255, 0, 127));
        chest.setItemMeta(cMeta);
        eq.setChestplate(chest);
        
        ItemStack legs = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta lMeta = (LeatherArmorMeta) legs.getItemMeta();
        lMeta.setColor(org.bukkit.Color.fromRGB(255, 0, 127));
        legs.setItemMeta(lMeta);
        eq.setLeggings(legs);
        
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bMeta = (LeatherArmorMeta) boots.getItemMeta();
        bMeta.setColor(org.bukkit.Color.fromRGB(0, 0, 0));
        boots.setItemMeta(bMeta);
        eq.setBoots(boots);
        
        eq.setItemInHand(new ItemStack(Material.BOW));
        
        new BukkitRunnable() {
            @Override
            public void run() {
                Location eyeLoc = guard.getLocation().add(0, 1.5, 0);
                org.bukkit.util.Vector arrowDir = player.getLocation().add(0, 1, 0).toVector().subtract(eyeLoc.toVector()).normalize();
                org.bukkit.entity.Arrow arrow = guard.getWorld().spawnArrow(eyeLoc, arrowDir, 1.5f, 0.0f);
                arrow.setShooter(guard);
                
                player.getWorld().playSound(player.getLocation(), Sound.SHOOT_ARROW, 1.0f, 1.0f);
            }
        }.runTaskLater(plugin, 5L);

        new BukkitRunnable() {
            @Override
            public void run() {
                player.getWorld().playSound(player.getLocation(), Sound.EXPLODE, 1.0f, 2.0f);
                player.getWorld().playEffect(player.getLocation().add(0, 1, 0), org.bukkit.Effect.STEP_SOUND, Material.REDSTONE_BLOCK.getId());
                player.getWorld().strikeLightningEffect(player.getLocation()); // Vurucu Şimşek Efekti!
                
                spectators.add(player.getUniqueId());
                player.setGameMode(org.bukkit.GameMode.ADVENTURE);
                player.setAllowFlight(true);
                player.setFlying(true);
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));
                
                player.getInventory().clear();
                
                ItemStack compass = new ItemStack(Material.COMPASS);
                ItemMeta cMeta = compass.getItemMeta();
                cMeta.setDisplayName("§bİzleyici Menüsü");
                compass.setItemMeta(cMeta);
                player.getInventory().setItem(0, compass);
                
                ItemStack leave = new ItemStack(Material.BED);
                ItemMeta meta = leave.getItemMeta();
                meta.setDisplayName("§cLobiye Dön");
                leave.setItemMeta(meta);
                player.getInventory().setItem(8, leave);
                
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.hidePlayer(player);
                }
                
                sendActionBar(player, "§c§lELENDİN!");
                
                Bukkit.broadcastMessage("§8[§cSquidGame§8] §c☠ §4§l" + player.getName() + " §celendi!");
                updateScoreboard(isRedLight ? "§c§lKIRMIZI IŞIK" : "§a§lYEŞİL IŞIK");
                
                checkGameEnd();
            }
        }.runTaskLater(plugin, 10L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (guard != null) {
                    guard.remove();
                }
            }
        }.runTaskLater(plugin, 60L);
    }

    private void leaveSpectator(Player p) {
        spectators.remove(p.getUniqueId());
        p.setGameMode(org.bukkit.GameMode.SURVIVAL);
        p.setAllowFlight(false);
        p.setFlying(false);
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        p.getInventory().clear();
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(p);
        }
        Location spawn = getSpawnLocation();
        if (spawn != null) {
            p.teleport(spawn);
        }
    }
    
    @EventHandler
    public void onLobbyInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (state == GameState.WAITING && activePlayers.contains(p.getUniqueId())) {
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
                if (e.getItem() != null) {
                    if (e.getItem().getType() == Material.BED) {
                        e.setCancelled(true);
                        activePlayers.remove(p.getUniqueId());
                        if (playerSpawnIds.containsKey(p.getUniqueId())) {
                            availableSpawns.add(playerSpawnIds.remove(p.getUniqueId()));
                        }
                        p.getInventory().clear();
                        Location spawn = getSpawnLocation();
                        if (spawn != null) {
                            p.teleport(spawn);
                        }
                        p.sendMessage("§cOyun lobisinden ayrıldın.");
                        Bukkit.broadcastMessage("§8[§cSquidGame§8] §c" + p.getName() + " §eodadan ayrıldı! §7(" + activePlayers.size() + " Oyuncu)");
                
                        
                        if (activePlayers.isEmpty()) {
                            Bukkit.broadcastMessage("§8[§cSquidGame§8] §cOdada kimse kalmadığı için oyun iptal edildi.");
                            state = GameState.IDLE;
                            if (lobbyTask != null) {
                                lobbyTask.cancel();
                            }
                        }
                    } else if (e.getItem().getType() == Material.BONE && e.getItem().hasItemMeta() && e.getItem().getItemMeta().getDisplayName().equals("§aPet Menüsü")) {
                        e.setCancelled(true);
                        petManager.openPetMenu(p);
                    }
                }
            }
        }
    }

    private void openSpectatorMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8Hayatta Kalan Oyuncular");
        for (UUID uuid : activePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
                meta.setOwner(p.getName());
                meta.setDisplayName("§a" + p.getName());
                head.setItemMeta(meta);
                inv.addItem(head);
            }
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onSpectatorInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (spectators.contains(p.getUniqueId())) {
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
                if (e.getItem() != null) {
                    if (e.getItem().getType() == Material.BED) {
                        leaveSpectator(p);
                    } else if (e.getItem().getType() == Material.COMPASS) {
                        openSpectatorMenu(p);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onItemDrop(org.bukkit.event.player.PlayerDropItemEvent e) {
        ItemStack item = e.getItemDrop().getItemStack();
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = item.getItemMeta().getDisplayName();
            if (name.equals("§aPet Menüsü") || name.equals("§cOdadan Ayrıl") || name.equals("§aOyuncu Menüsü") || name.equals("§aBaşla")) {
                e.setCancelled(true);
                return;
            }
        }
        
        if (activePlayers.contains(e.getPlayer().getUniqueId()) && (state == GameState.WAITING || state == GameState.STARTING)) {
            e.setCancelled(true);
        }
        if (spectators.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        
        if (e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta() && e.getCurrentItem().getItemMeta().hasDisplayName()) {
            String name = e.getCurrentItem().getItemMeta().getDisplayName();
            if (name.equals("§aPet Menüsü") || name.equals("§cOdadan Ayrıl") || name.equals("§aOyuncu Menüsü") || name.equals("§aBaşla")) {
                e.setCancelled(true);
                return;
            }
        }
        
        if (activePlayers.contains(p.getUniqueId()) && (state == GameState.WAITING || state == GameState.STARTING)) {
            e.setCancelled(true);
        }
        if (spectators.contains(p.getUniqueId()) && !e.getView().getTitle().equals("§8Hayatta Kalan Oyuncular")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (activePlayers.contains(p.getUniqueId()) && (state == GameState.WAITING || state == GameState.STARTING)) {
            e.setCancelled(true);
        }
        if (spectators.contains(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpectatorInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8Hayatta Kalan Oyuncular")) {
            e.setCancelled(true);
            if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.SKULL_ITEM) {
                String targetName = org.bukkit.ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
                Player target = Bukkit.getPlayer(targetName);
                if (target != null && target.isOnline() && activePlayers.contains(target.getUniqueId())) {
                    e.getWhoClicked().teleport(target.getLocation());
                    e.getWhoClicked().sendMessage("§a" + targetName + " §eoyuncusuna ışınlandınız.");
                } else {
                    e.getWhoClicked().sendMessage("§cOyuncu bulunamadı veya elenmiş.");
                }
            }
        } else if (spectators.contains(e.getWhoClicked().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    private void winGame(Player player) {
        activePlayers.remove(player.getUniqueId());
        
        Bukkit.broadcastMessage("§8[§cSquidGame§8] §a🏆 §l" + player.getName() + " §eçizgiyi geçti ve KAZANDI!");
        leaderboard.addWin(player.getName());
        leaderboard.recordFinishTime(player.getName(), 120 - timeLeft); // Tamamlama süresini kaydet
        
        int awardedPoints = timeLeft * 10;
        pointsManager.addPoints(player.getUniqueId(), awardedPoints);
        
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 1));
        player.giveExpLevels(5);
        player.sendMessage("§a[SquidGame] Oyunu kazandığın için ödüllendirildin!");
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendTitle(p, "§6§lOYUN BİTTİ", "§aKazanan: " + player.getName(), 10, 80, 20);
            p.playSound(p.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
            
            if (p.equals(player)) {
                for (int i = 0; i < 30; i++) {
                    p.getWorld().playEffect(p.getLocation().add(0, 2, 0), Effect.HEART, 1);
                    p.getWorld().playEffect(p.getLocation(), Effect.FIREWORKS_SPARK, 1);
                }
            }
        }

        if (!activePlayers.isEmpty()) {
            Bukkit.broadcastMessage("§8[§cSquidGame§8] §cİlk kişi kazandığı için geride kalan tüm oyuncular eleniyor!");
            Set<UUID> remaining = new HashSet<>(activePlayers);
            for (UUID uuid : remaining) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    eliminatePlayer(p);
                }
            }
        }

        state = GameState.IDLE;
        if (countdownTask != null) countdownTask.cancel();
        if (timerTask != null) timerTask.cancel();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.setHealth(player.getMaxHealth());
                    player.setFoodLevel(20);
                    Location spawn = getSpawnLocation();
                    if (spawn != null) {
                        player.teleport(spawn);
                    }
                }
                stopGame(); // Oyuncuyu ışınladıktan sonra tüm verileri ve Scoreboard'ı temizle
            }
        }.runTaskLater(plugin, 60L);
    }

    private void checkGameEnd() {
        if (activePlayers.isEmpty() && state != GameState.IDLE) {
            Bukkit.broadcastMessage("§8[§cSquidGame§8] §cHayatta kimse kalmadı. Oyun sona erdi!");
            for (Player p : Bukkit.getOnlinePlayers()) {
                sendTitle(p, "§4OYUN BİTTİ", "§cHerkes elendi...", 10, 60, 20);
            }
            stopGame();
        }
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    private void setupScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        gameScoreboard = manager.getNewScoreboard();
        objective = gameScoreboard.registerNewObjective("squidgame", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName("§6§lSQUID GAME");
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(gameScoreboard);
        }
    }

    private void updateScoreboard(String lightStatus) {
        if (state == GameState.IDLE) return; // Oyun bittiyse tabloyu yenileme (Ekranda kalmasını önler)
        if (gameScoreboard == null || objective == null) return;
        
        for (String entry : gameScoreboard.getEntries()) {
            gameScoreboard.resetScores(entry);
        }
        
        objective.getScore("§1").setScore(6);
        objective.getScore("§7Işık Durumu:").setScore(5);
        objective.getScore(lightStatus).setScore(4);
        objective.getScore("§2").setScore(3);
        objective.getScore("§7Hayatta Kalan: §a" + activePlayers.size()).setScore(2);
        objective.getScore("§3").setScore(1);
        objective.getScore("§7Kalan Süre: §c" + formatTime(timeLeft)).setScore(0);
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(gameScoreboard);
        }
    }

    private void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            Object chatTitle = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\":\"" + title + "\"}");
            Object chatSubtitle = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\":\"" + subtitle + "\"}");
            
            Object titlePacket = getNMSClass("PacketPlayOutTitle").getConstructor(getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent"), int.class, int.class, int.class).newInstance(getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TITLE").get(null), chatTitle, fadeIn, stay, fadeOut);
            Object subtitlePacket = getNMSClass("PacketPlayOutTitle").getConstructor(getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent"), int.class, int.class, int.class).newInstance(getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("SUBTITLE").get(null), chatSubtitle, fadeIn, stay, fadeOut);
            
            sendPacket(player, titlePacket);
            sendPacket(player, subtitlePacket);
        } catch (Exception e) {
            player.sendMessage(title);
            player.sendMessage(subtitle);
        }
    }

    private void sendActionBar(Player player, String message) {
        try {
            Object chat = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\":\"" + message + "\"}");
            Object packet = getNMSClass("PacketPlayOutChat").getConstructor(getNMSClass("IChatBaseComponent"), byte.class).newInstance(chat, (byte) 2);
            sendPacket(player, packet);
        } catch (Exception e) {
        }
    }

    private void sendPacket(Player player, Object packet) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Class<?> getNMSClass(String name) {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        try {
            return Class.forName("net.minecraft.server." + version + "." + name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Skeleton && event.getEntity().getCustomName() != null && event.getEntity().getCustomName().equals("§cGörevli")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Skeleton && event.getDamager().getCustomName() != null && event.getDamager().getCustomName().equals("§cGörevli")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Skeleton && event.getEntity().getCustomName() != null && event.getEntity().getCustomName().equals("§cGörevli")) {
            event.setCancelled(true);
        }
        if (event.getEntity() instanceof ArmorStand && event.getEntity().getCustomName() != null && event.getEntity().getCustomName().equals("§cGörevli")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onArmorManipulate(org.bukkit.event.player.PlayerArmorStandManipulateEvent e) {
        if (e.getRightClicked().getCustomName() != null && e.getRightClicked().getCustomName().equals("§cGörevli")) {
            e.setCancelled(true);
        }
    }
}
