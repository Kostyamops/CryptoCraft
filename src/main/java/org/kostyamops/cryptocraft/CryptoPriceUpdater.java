package org.kostyamops.cryptocraft;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CryptoPriceUpdater {

    private final JavaPlugin plugin;
    private final CryptoAPI cryptoAPI;

    private final List<String> cryptoIds;
    private final List<String> cryptoNames;

    private Map<String, Double> prevPrices;
    private Map<String, Double> currentPrices;

    public CryptoPriceUpdater(JavaPlugin plugin, CryptoAPI cryptoAPI) {
        this.plugin = plugin;
        this.cryptoAPI = cryptoAPI;

        List<Map<?, ?>> cryptos = plugin.getConfig().getMapList("cryptocurrencies");

        this.cryptoNames = cryptos.stream()
                .map(m -> ((String) m.get("name")).toUpperCase())
                .collect(Collectors.toList());

        this.cryptoIds = cryptos.stream()
                .map(m -> ((String) m.get("id")).toLowerCase())
                .collect(Collectors.toList());

        prevPrices = cryptoIds.stream().collect(Collectors.toMap(id -> id, id -> 0.0));
        currentPrices = cryptoIds.stream().collect(Collectors.toMap(id -> id, id -> 0.0));

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updatePricesTask, 0L, 1200L);

        Bukkit.getScheduler().runTaskTimer(plugin, this::displayActionBarTask, 0L, 20L);
    }

    private void updatePricesTask() {
        cryptoAPI.updatePrices();

        for (String id : cryptoIds) {
            double newPrice = cryptoAPI.getCachedPrice(id);
            prevPrices.put(id, currentPrices.getOrDefault(id, 0.0));
            currentPrices.put(id, newPrice);
        }
    }

    private void displayActionBarTask() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < cryptoIds.size(); i++) {
            String id = cryptoIds.get(i);
            String name = cryptoNames.get(i);

            double prev = prevPrices.getOrDefault(id, 0.0);
            double current = currentPrices.getOrDefault(id, 0.0);

            String color;
            String arrow;

            if (current > prev) {
                color = "§a";
                arrow = "▲";
            } else if (current < prev) {
                color = "§c";
                arrow = "▼";
            } else {
                color = "§7";
                arrow = "-";
            }

            sb.append(String.format("§r%s: %.2f%s%s ", name, current, color, arrow));
        }

        String message = sb.toString().trim();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        }
    }
}
