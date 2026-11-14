package ru.casino;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class CasinoGame implements Listener {

    private static final String GUI_TITLE = ChatColor.DARK_GRAY + "Слот-машина";
    private static final List<Material> SYMBOLS = List.of(
            Material.DIAMOND,
            Material.EMERALD,
            Material.GOLD_INGOT,
            Material.IRON_INGOT,
            Material.APPLE
    );
    private static final Random RANDOM = new Random();

    public static void startSpin(SimpleCasino plugin, Player player, double bet) {
        plugin.getActivePlayers().add(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);

        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack pointer = createItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "▲ Линия выигрыша ▲");

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, glass);
        }
        inv.setItem(3, pointer);
        inv.setItem(4, pointer);
        inv.setItem(5, pointer);
        inv.setItem(21, pointer);
        inv.setItem(22, pointer);
        inv.setItem(23, pointer);

        player.openInventory(inv);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 25;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    plugin.getActivePlayers().remove(player.getUniqueId());
                    cancel();
                    return;
                }

                if (ticks < 15) {
                    inv.setItem(12, new ItemStack(getRandomSymbol()));
                }
                if (ticks < 20) {
                    inv.setItem(13, new ItemStack(getRandomSymbol()));
                }
                if (ticks < maxTicks) {
                    inv.setItem(14, new ItemStack(getRandomSymbol()));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.2f);
                }

                if (ticks >= maxTicks) {
                    finishGame(plugin, player, inv, bet);
                    cancel();
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    private static void finishGame(SimpleCasino plugin, Player player, Inventory inv, double bet) {
        plugin.getActivePlayers().remove(player.getUniqueId());

        ItemStack item1 = inv.getItem(12);
        ItemStack item2 = inv.getItem(13);
        ItemStack item3 = inv.getItem(14);

        if (item1 == null || item2 == null || item3 == null) {
            return;
        }

        Material s1 = item1.getType();
        Material s2 = item2.getType();
        Material s3 = item3.getType();

        double multiplier = 0.0;

        if (s1 == s2 && s2 == s3) {
            multiplier = plugin.getConfig().getDouble("multipliers." + s1.name(), 2.0);
        } else if (s1 == s2 || s2 == s3) {
            multiplier = plugin.getConfig().getDouble("two-match-multiplier", 1.5);
        }

        if (multiplier > 0) {
            double winAmount = bet * multiplier;
            SimpleCasino.getEconomy().depositPlayer(player, winAmount);
            player.sendMessage(plugin.getMsg("win").replace("%amount%", String.format("%.2f", winAmount)));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else {
            player.sendMessage(plugin.getMsg("lose"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private static Material getRandomSymbol() {
        return SYMBOLS.get(RANDOM.nextInt(SYMBOLS.size()));
    }

    private static ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE)) {
            event.setCancelled(true);
        }
    }
                             }
