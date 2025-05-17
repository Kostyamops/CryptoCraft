package org.kostyamops.cryptocraft;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CryptoGUI implements Listener {

    private final Main plugin;
    private final Map<UUID, String> action = new HashMap<>();
    private final Map<UUID, String> selectedCrypto = new HashMap<>();
    private final Map<UUID, Integer> selectedAmount = new HashMap<>();

    public CryptoGUI(Main plugin) {
        this.plugin = plugin;
    }

    private Material getCurrencyMaterial() {
        return Material.valueOf(plugin.getConfig().getString("currency", "DIAMOND"));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getCryptos() {
        List<Map<String, Object>> list = (List<Map<String, Object>>) plugin.getConfig().getList("cryptocurrencies");
        return list == null ? Collections.emptyList() : list;
    }

    public void openMainMenu(Player player) {
        List<Map<String, Object>> cryptos = getCryptos();
        Inventory inv = Bukkit.createInventory(null, 27, "CryptoCraft - Действие");

        int slot = 0;

        for (Map<String, Object> crypto : cryptos) {
            if (slot >= 18) break;

            String name = (String) crypto.get("name");
            String id = ((String) crypto.get("id")).toLowerCase();
            String displayItemStr = (String) crypto.get("display_item");

            Material mat = Material.matchMaterial(displayItemStr);
            if (mat == null) mat = Material.BARRIER;

            double balance = plugin.getPlayerData()
                    .getDouble("players." + player.getUniqueId()
                            + "." + id, 0.0);
            double price = plugin.getCryptoAPI().getCachedPrice(id);
            double equiv = balance * price;

            String itemName = String.format("§6%s §7| §a$%.2f §7| §e%.6f §7| ⛏ %.2f", name.toUpperCase(), price, balance, equiv);
            inv.setItem(slot++, createItem(mat, itemName));
        }

        inv.setItem(21, createItem(Material.LIME_CONCRETE, "§aКупить"));
        inv.setItem(23, createItem(Material.RED_CONCRETE, "§cПродать"));

        player.openInventory(inv);
    }

    public void openCryptoSelectMenu(Player player, String mode) {
        action.put(player.getUniqueId(), mode);

        List<Map<String, Object>> cryptos = getCryptos();
        Inventory inv = Bukkit.createInventory(null, 9, "Выберите валюту");

        int slot = 0;
        for (Map<String, Object> crypto : cryptos) {
            if (slot >= 9) break;

            String name = (String) crypto.get("name");
            String displayItemStr = (String) crypto.get("display_item");

            Material mat = Material.matchMaterial(displayItemStr);
            if (mat == null) mat = Material.PAPER;

            inv.setItem(slot++, createItem(mat, name));
        }

        player.openInventory(inv);
    }

    public void openAmountMenu(Player player, String crypto) {
        selectedCrypto.put(player.getUniqueId(), crypto);
        selectedAmount.put(player.getUniqueId(), 1);

        Inventory inv = Bukkit.createInventory(null, 27, "Выберите количество");
        updateAmountMenu(player, inv);
        player.openInventory(inv);
    }

    private void updateAmountMenu(Player player, Inventory inv) {
        UUID uuid = player.getUniqueId();
        int amount = selectedAmount.getOrDefault(uuid, 1);
        String crypto = selectedCrypto.get(uuid);

        Material currencyMat = getCurrencyMaterial();

        inv.clear();

        double price = plugin.getCryptoAPI().getCachedPrice(crypto);
        double balance = plugin.getPlayerData().getDouble("players." + uuid + "." + crypto, 0.0);
        double equiv = balance * price;

        String infoName = String.format("§6%s §7| §a$%.2f §7| §eБаланс: %.6f §7| ⛏ %.2f",
                crypto.toUpperCase(), price, balance, equiv);
        inv.setItem(22, createItem(currencyMat, infoName));

        inv.setItem(1, createItem(Material.GREEN_DYE, "§r+1"));
        inv.setItem(2, createItem(Material.GREEN_DYE, "§r+10"));
        inv.setItem(3, createItem(Material.GREEN_DYE, "§r+64"));
        inv.setItem(4, createItem(Material.PAPER, "§rКол-во: §e" + amount));
        inv.setItem(5, createItem(Material.RED_DYE, "§r-64"));
        inv.setItem(6, createItem(Material.RED_DYE, "§r-10"));
        inv.setItem(7, createItem(Material.RED_DYE, "§r-1"));
        inv.setItem(21, createItem(Material.LIME_CONCRETE, "§aПодтвердить"));
        inv.setItem(23, createItem(Material.RED_CONCRETE, "§cНазад"));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory().equals(player.getInventory())) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getItemMeta() == null) return;

        String title = e.getView().getTitle();
        e.setCancelled(true);

        switch (title) {
            case "CryptoCraft - Действие" -> {
                if (clicked.getType() == Material.LIME_CONCRETE) {
                    openCryptoSelectMenu(player, "buy");
                } else if (clicked.getType() == Material.RED_CONCRETE) {
                    openCryptoSelectMenu(player, "sell");
                }
            }

            case "Выберите валюту" -> {
                String cryptoName = clicked.getItemMeta().getDisplayName();
                String cryptoId = null;

                for (Map<String, Object> c : getCryptos()) {
                    if (cryptoName.equals(c.get("name"))) {
                        cryptoId = ((String) c.get("id")).toLowerCase();
                        break;
                    }
                }

                if (cryptoId != null) {
                    openAmountMenu(player, cryptoId);
                }
            }

            case "Выберите количество" -> {
                UUID uuid = player.getUniqueId();
                int amount = selectedAmount.getOrDefault(uuid, 1);
                String name = clicked.getItemMeta().getDisplayName();

                if (clicked.getType() == Material.GREEN_DYE) {
                    if (name.equals("§r+64")) amount += 64;
                    else if (name.equals("§r+10")) amount += 10;
                    else if (name.equals("§r+1")) amount += 1;
                } else if (clicked.getType() == Material.RED_DYE) {
                    if (name.equals("§r-64")) amount = Math.max(1, amount - 64);
                    else if (name.equals("§r-10")) amount = Math.max(1, amount - 10);
                    else if (name.equals("§r-1")) amount = Math.max(1, amount - 1);
                } else if (clicked.getType() == Material.LIME_CONCRETE) {
                    String act = action.get(uuid);
                    String crypto = selectedCrypto.get(uuid);
                    plugin.getCryptoAPI().processTransaction(player, act, crypto, amount);
                    player.closeInventory();
                    return;
                } else if (clicked.getType() == Material.RED_CONCRETE) {
                    String act = action.get(uuid);
                    openCryptoSelectMenu(player, act);
                    return;
                }

                selectedAmount.put(uuid, amount);
                updateAmountMenu(player, e.getInventory());
            }
        }
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
