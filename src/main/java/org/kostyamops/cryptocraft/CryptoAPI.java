package org.kostyamops.cryptocraft;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.io.IOException;
import java.util.*;

public class CryptoAPI {

    private final Main plugin;
    private final OkHttpClient client = new OkHttpClient();

    private final Map<String, Double> cachedPrices = new HashMap<>();

    public CryptoAPI(Main plugin) {
        this.plugin = plugin;
        updatePrices();
    }

    private String buildApiUrl() {
        List<Map<String, Object>> cryptos = getCryptosConfig();
        if (cryptos.isEmpty()) return "";

        StringJoiner joiner = new StringJoiner(",");
        for (Map<String, Object> crypto : cryptos) {
            String id = ((String) crypto.get("id")).toLowerCase();
            joiner.add(id);
        }

        return "https://api.coingecko.com/api/v3/simple/price?ids=" + joiner + "&vs_currencies=usd";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getCryptosConfig() {
        List<Map<String, Object>> list = (List<Map<String, Object>>) plugin.getConfig().getList("cryptocurrencies");
        return list == null ? Collections.emptyList() : list;
    }

    public void updatePrices() {
        String url = buildApiUrl();
        if (url.isEmpty()) return;

        try {
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                plugin.getLogger().warning("API request failed: " + response);
                return;
            }

            String responseBody = response.body().string();

            plugin.getLogger().info("API response: " + responseBody);

            Gson gson = new Gson();
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);

            for (Map<String, Object> crypto : getCryptosConfig()) {
                String name = ((String) crypto.get("name")).toLowerCase();
                String id = ((String) crypto.get("id")).toLowerCase();

                if (json.has(id)) {
                    double price = json.getAsJsonObject(id).get("usd").getAsDouble();
                    cachedPrices.put(id, price);
                }
            }

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update prices from API:");
            e.printStackTrace();
        }
    }

    public double getCachedPrice(String cryptoName) {
        return cachedPrices.getOrDefault(cryptoName.toLowerCase(), -1.0);
    }

    public void processTransaction(Player player, String type, String crypto, int amount) {
        switch (type.toLowerCase()) {
            case "buy" -> buyCrypto(player, crypto.toLowerCase(), amount);
            case "sell" -> sellCrypto(player, crypto.toLowerCase(), amount);
        }
    }

    public void buyCrypto(Player player, String crypto, int itemAmount) {
        double price = getCachedPrice(crypto);
        if (price <= 0) {
            player.sendMessage("§cОшибка при получении цены для криптовалюты: " + crypto.toUpperCase());
            return;
        }

        String currencyName = plugin.getConfig().getString("currency", "DIAMOND");
        Material currencyMaterial = Material.matchMaterial(currencyName);
        if (currencyMaterial == null) {
            player.sendMessage("§cВалюта указана неверно в config.yml: " + currencyName);
            return;
        }

        double usdPerItem = 1.0;
        double totalUsd = itemAmount * usdPerItem;
        double cryptoAmount = totalUsd / price;

        ItemStack paymentItem = new ItemStack(currencyMaterial, itemAmount);
        if (!player.getInventory().containsAtLeast(paymentItem, itemAmount)) {
            player.sendMessage("§cУ вас недостаточно " + currencyMaterial.name() + " для покупки.");
            return;
        }

        player.getInventory().removeItem(paymentItem);

        FileConfiguration data = plugin.getPlayerData();
        String path = "players." + player.getUniqueId() + "." + crypto;
        double oldAmount = data.getDouble(path, 0.0);
        data.set(path, oldAmount + cryptoAmount);
        plugin.savePlayerData();

        player.sendMessage(String.format("§aВы купили %.6f %s за %d %s.",
                cryptoAmount, crypto.toUpperCase(), itemAmount, currencyMaterial.name()));
    }


    public void sellCrypto(Player player, String crypto, double amount) {
        FileConfiguration data = plugin.getPlayerData();
        String path = "players." + player.getUniqueId() + "." + crypto;
        double stored = data.getDouble(path, 0.0);

        if (stored < amount) {
            player.sendMessage("§cУ вас недостаточно " + crypto.toUpperCase() + " для продажи.");
            return;
        }

        double price = getCachedPrice(crypto);
        if (price <= 0) {
            player.sendMessage("§cОшибка при получении цены на " + crypto.toUpperCase());
            return;
        }

        String currencyName = plugin.getConfig().getString("currency", "DIAMOND");
        Material currencyMaterial = Material.matchMaterial(currencyName);
        if (currencyMaterial == null) {
            player.sendMessage("§cНеверная валюта в config.yml: " + currencyName);
            return;
        }

        double totalUsd = amount * price;

        int itemsToGive = (int) Math.floor(totalUsd);
        if (itemsToGive <= 0) {
            player.sendMessage("§eСлишком маленькое количество для продажи: получите менее 1 " + currencyMaterial.name());
            return;
        }

        ItemStack rewardItem = new ItemStack(currencyMaterial, itemsToGive);
        player.getInventory().addItem(rewardItem);

        data.set(path, stored - amount);
        plugin.savePlayerData();

        player.sendMessage(String.format("§aВы продали %.6f %s и получили %d %s.",
                amount, crypto.toUpperCase(), itemsToGive, currencyMaterial.name()));
    }

}
