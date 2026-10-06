package com.example.squidgame;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import com.example.squidgame.RedLightGreenLight.GameState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MenuManager implements Listener {
    private final SquidGame plugin;
    private final RedLightGreenLight game;

    public MenuManager(SquidGame plugin, RedLightGreenLight game) {
        this.plugin = plugin;
        this.game = game;
    }

    public void spawnNPC(Player player, String skinName) {
        ArmorStand npc = player.getWorld().spawn(player.getLocation(), ArmorStand.class);
        npc.setCustomName("§d§lSquid Game Yöneticisi");
        npc.setCustomNameVisible(true);
        npc.setGravity(false); // İtilemez, düşmez
        npc.setBasePlate(false);
        npc.setArms(true);
        npc.setVisible(true);

        org.bukkit.inventory.EntityEquipment eq = npc.getEquipment();
        
        ItemStack pinkChest = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) pinkChest.getItemMeta();
        meta.setColor(Color.FUCHSIA);
        pinkChest.setItemMeta(meta);
        eq.setChestplate(pinkChest);
        
        ItemStack pinkLegs = new ItemStack(Material.LEATHER_LEGGINGS);
        pinkLegs.setItemMeta(meta);
        eq.setLeggings(pinkLegs);
        
        ItemStack pinkBoots = new ItemStack(Material.LEATHER_BOOTS);
        pinkBoots.setItemMeta(meta);
        eq.setBoots(pinkBoots);

        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        skullMeta.setOwner(skinName);
        skull.setItemMeta(skullMeta);
        eq.setHelmet(skull);

        player.sendMessage("§aNPC başarıyla oluşturuldu! (" + skinName + " skini ile) Yönü tamamen senin baktığın yöne kilitlendi ve artık ittirilemez.");
    }



    public void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8§l» §4§lSQUID GAME §8§l«");

        ItemStack bg = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta bgMeta = bg.getItemMeta();
        bgMeta.setDisplayName(" ");
        bg.setItemMeta(bgMeta);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, bg);
        }

        GameState state = game.getState();

        if (state == GameState.IDLE) {
            ItemStack create = new ItemStack(Material.REDSTONE_BLOCK);
            ItemMeta meta = create.getItemMeta();
            meta.setDisplayName("§4§l▶ §c§lYENİ ODA KUR §4§l◀");
            meta.setLore(Arrays.asList("", "§7Tıklayarak yeni bir ölüm oyunu", "§7başlat ve diğerlerini bekle.", "", "§eTıkla ve Kur!"));
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            create.setItemMeta(meta);
            inv.setItem(13, create);
        } else if (state == GameState.WAITING || state == GameState.STARTING) {
            ItemStack join = new ItemStack(Material.EMERALD_BLOCK);
            ItemMeta meta = join.getItemMeta();
            meta.setDisplayName("§2§l▶ §a§lODAYA KATIL §2§l◀");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Durum: " + (state == GameState.STARTING ? "§6§lBaşlıyor..." : "§e§lOyuncu Bekleniyor..."));
            lore.add("§7Mevcut Oyuncu: §b" + game.getActivePlayerCount());
            lore.add("");
            lore.add("§aTıkla ve Hayatta Kalmaya Çalış!");
            meta.setLore(lore);
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            join.setItemMeta(meta);
            inv.setItem(13, join);

            if (player.hasPermission("squidgame.admin") || player.isOp()) {
                ItemStack force = new ItemStack(Material.DIAMOND_SWORD);
                ItemMeta fMeta = force.getItemMeta();
                fMeta.setDisplayName("§b§l⚡ Zorla Başlat (OP)");
                fMeta.setLore(Arrays.asList("§7Oyuncu sayısını beklemeden", "§7hemen başlatır."));
                force.setItemMeta(fMeta);
                inv.setItem(22, force);
            }
        } else if (state == GameState.PLAYING) {
            ItemStack barrier = new ItemStack(Material.BARRIER);
            ItemMeta meta = barrier.getItemMeta();
            meta.setDisplayName("§4§l✖ §c§lOYUN DEVAM EDİYOR §4§l✖");
            meta.setLore(Arrays.asList("", "§7Şu anda devam eden bir oyun var.", "§7Lütfen bitmesini bekle."));
            barrier.setItemMeta(meta);
            inv.setItem(13, barrier);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8§l» §4§lSQUID GAME §8§l«")) {
            e.setCancelled(true);
            Player p = (Player) e.getWhoClicked();
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR || e.getCurrentItem().getType() == Material.STAINED_GLASS_PANE) return;

            Material clicked = e.getCurrentItem().getType();
            p.closeInventory();

            if (clicked == Material.REDSTONE_BLOCK) {
                game.createRoom(p);
            } else if (clicked == Material.EMERALD_BLOCK) {
                game.joinRoom(p);
            } else if (clicked == Material.DIAMOND_SWORD) {
                if (p.hasPermission("squidgame.admin") || p.isOp()) {
                    game.forceStartLobby();
                }
            }
        }
    }
}
