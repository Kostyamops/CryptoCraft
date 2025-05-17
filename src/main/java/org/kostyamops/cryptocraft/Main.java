package org.kostyamops.cryptocraft;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class Main extends JavaPlugin {

    private CryptoAPI cryptoAPI;
    private FileConfiguration playerData;
    private File playerDataFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createPlayerDataConfig();

        cryptoAPI = new CryptoAPI(this);
        getCommand("crypto").setExecutor(new CryptoCommand(this, cryptoAPI));
        getServer().getPluginManager().registerEvents(new CryptoGUI(this), this);

        CryptoPriceUpdater updater = new CryptoPriceUpdater(this, cryptoAPI);

        getLogger().info("CryptoCraft плагин включен!");
    }

    @Override
    public void onDisable() {
        savePlayerData();
        getLogger().info("CryptoCraft плагин отключен!");
    }

    private void createPlayerDataConfig() {
        playerDataFile = new File(getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            saveResource("playerdata.yml", false);
        }
        playerData = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(playerDataFile);
    }

    public FileConfiguration getPlayerData() {
        return playerData;
    }

    public void savePlayerData() {
        try {
            playerData.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CryptoAPI getCryptoAPI() {
        return cryptoAPI;
    }
}
