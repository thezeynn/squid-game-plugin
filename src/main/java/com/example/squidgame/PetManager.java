package com.example.squidgame;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Location;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PetManager implements Listener {

    private final SquidGame plugin;
    private final PointsManager pointsManager;
    private File petsFile;
    private FileConfiguration petsConfig;

    private Map<UUID, ArmorStand> activePets = new HashMap<>();

    public PetManager(SquidGame plugin, PointsManager pointsManager) {
        this.plugin = plugin;
        this.pointsManager = pointsManager;
        createPetsFile();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startPetFollowTask();
    }

    private void createPetsFile() {
        petsFile = new File(plugin.getDataFolder(), "pets.yml");
        if (!petsFile.exists()) {
            petsFile.getParentFile().mkdirs();
            try {
                petsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        petsConfig = YamlConfiguration.loadConfiguration(petsFile);
    }

    private void savePetsFile() {
        try {
            petsConfig.save(petsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasPet(UUID uuid, String petName) {
        List<String> pets = petsConfig.getStringList(uuid.toString());
        return pets != null && pets.contains(petName);
    }

    public void addPet(UUID uuid, String petName) {
        List<String> pets = petsConfig.getStringList(uuid.toString());
        if (pets == null) pets = new ArrayList<>();
        if (!pets.contains(petName)) {
            pets.add(petName);
            petsConfig.set(uuid.toString(), pets);
            savePetsFile();
        }
    }

    public ItemStack getCustomSkull(String value) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        if (value == null || value.isEmpty()) return head;

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (value.length() <= 16) {
            meta.setOwner(value);
        } else {
            try {
                com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "");
                profile.getProperties().put("textures", new com.mojang.authlib.properties.Property("textures", value));
                java.lang.reflect.Field profileField = meta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(meta, profile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        head.setItemMeta(meta);
        return head;
    }

    public void openPetMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§aPet Menüsü");

        List<String> petSkins = plugin.getConfig().getStringList("pets.skins");
        if (petSkins == null || petSkins.isEmpty()) {
            petSkins = Arrays.asList(
                "MHF_Pig", "MHF_Cow", "MHF_Chicken", "MHF_Sheep", "MHF_Spider",
                "MHF_Enderman", "MHF_Creeper", "MHF_Skeleton", "MHF_Zombie", "MHF_Slime",
                "Pikachu", "Batman", "Spiderman", "Superman", "IronMan"
            );
        }
        
        int cost = plugin.getConfig().getInt("pets.default_cost", 100);

        for (int i = 0; i < petSkins.size() && i < 27; i++) {
            String entry = petSkins.get(i);
            String name = entry;
            String texture = entry;
            if (entry.contains(";")) {
                String[] parts = entry.split(";", 2);
                name = parts[0];
                texture = parts[1];
            }
            
            ItemStack head = getCustomSkull(texture);
            ItemMeta meta = head.getItemMeta();
            meta.setDisplayName("§ePet: " + name);

            List<String> lore = new ArrayList<>();
            lore.add("§8Texture:" + texture);
            if (hasPet(player.getUniqueId(), name)) {
                lore.add("§aSahipsin!");
                lore.add("§7Çağırmak için tıkla.");
            } else {
                lore.add("§cSahip Değilsin!");
                lore.add("§eFiyat: §6" + cost + " Puan");
                lore.add("§7Satın almak için tıkla.");
            }
            meta.setLore(lore);
            head.setItemMeta(meta);
            inv.addItem(head);
        }

        player.openInventory(inv);
    }

    public void openConfirmMenu(Player player, String name, String texture, int cost) {
        Inventory inv = Bukkit.createInventory(null, 27, "§cSatın Almayı Onayla");
        
        ItemStack confirm = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);
        ItemMeta cMeta = confirm.getItemMeta();
        cMeta.setDisplayName("§a§lONAYLA");
        List<String> cLore = new ArrayList<>();
        cLore.add("§7Pet: §e" + name);
        cLore.add("§7Fiyat: §6" + cost + " Puan");
        cLore.add("§8Texture:" + texture);
        cMeta.setLore(cLore);
        confirm.setItemMeta(cMeta);
        
        ItemStack cancel = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
        ItemMeta xMeta = cancel.getItemMeta();
        xMeta.setDisplayName("§c§lİPTAL");
        cancel.setItemMeta(xMeta);
        
        inv.setItem(11, confirm);
        inv.setItem(15, cancel);
        
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§aPet Menüsü")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

            Player p = (Player) e.getWhoClicked();
            if (e.getCurrentItem().getType() == Material.SKULL_ITEM) {
                String name = org.bukkit.ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).replace("Pet: ", "");
                String texture = name;
                if (e.getCurrentItem().getItemMeta().hasLore()) {
                    String firstLore = org.bukkit.ChatColor.stripColor(e.getCurrentItem().getItemMeta().getLore().get(0));
                    if (firstLore.startsWith("Texture:")) {
                        texture = firstLore.replace("Texture:", "");
                    }
                }
                int cost = plugin.getConfig().getInt("pets.default_cost", 100);

                if (hasPet(p.getUniqueId(), name)) {
                    spawnPet(p, texture);
                    p.closeInventory();
                    p.sendMessage("§aPetin çağrıldı!");
                } else {
                    int points = pointsManager.getPoints(p.getUniqueId());
                    if (points >= cost) {
                        openConfirmMenu(p, name, texture, cost);
                    } else {
                        p.sendMessage("§cYeterli puanın yok!");
                    }
                }
            }
        } else if (e.getView().getTitle().equals("§cSatın Almayı Onayla")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
            
            Player p = (Player) e.getWhoClicked();
            if (e.getCurrentItem().getType() == Material.STAINED_GLASS_PANE) {
                short damage = e.getCurrentItem().getDurability();
                if (damage == 5) { // Green (Confirm)
                    String name = org.bukkit.ChatColor.stripColor(e.getCurrentItem().getItemMeta().getLore().get(0)).replace("Pet: ", "");
                    String texture = org.bukkit.ChatColor.stripColor(e.getCurrentItem().getItemMeta().getLore().get(2)).replace("Texture:", "");
                    int cost = plugin.getConfig().getInt("pets.default_cost", 100);
                    
                    int points = pointsManager.getPoints(p.getUniqueId());
                    if (points >= cost) {
                        pointsManager.removePoints(p.getUniqueId(), cost);
                        addPet(p.getUniqueId(), name);
                        p.sendMessage("§aPeti başarıyla satın aldın!");
                        openPetMenu(p);
                    } else {
                        p.sendMessage("§cYeterli puanın yok!");
                        p.closeInventory();
                    }
                } else if (damage == 14) { // Red (Cancel)
                    openPetMenu(p);
                }
            }
        }
    }

    public void spawnPet(Player player, String skin) {
        despawnPet(player);

        Location loc = player.getLocation().add(0, 1, 0);
        ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        as.setVisible(false);
        as.setSmall(true);
        as.setGravity(false);
        as.setMarker(true);

        ItemStack head = getCustomSkull(skin);
        as.setHelmet(head);

        activePets.put(player.getUniqueId(), as);
    }

    public void despawnPet(Player player) {
        ArmorStand as = activePets.remove(player.getUniqueId());
        if (as != null) {
            as.remove();
        }
    }

    public void despawnAll() {
        for (ArmorStand as : activePets.values()) {
            as.remove();
        }
        activePets.clear();
    }

    private void startPetFollowTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, ArmorStand> entry : activePets.entrySet()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    ArmorStand as = entry.getValue();

                    if (p != null && p.isOnline() && !as.isDead()) {
                        Location target = p.getLocation();
                        // Put the pet behind the player slightly and up
                        org.bukkit.util.Vector dir = target.getDirection().normalize();
                        target.subtract(dir.multiply(1.5));
                        target.add(0, 1.2, 0);
                        
                        Location asLoc = as.getLocation();
                        asLoc.add(target.toVector().subtract(asLoc.toVector()).multiply(0.2));
                        asLoc.setYaw(p.getLocation().getYaw());
                        
                        as.teleport(asLoc);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        despawnPet(e.getPlayer());
    }
}
