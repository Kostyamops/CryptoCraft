package org.kostyamops.cryptocraft;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CryptoCommand implements CommandExecutor {

    private final Main plugin;
    private final CryptoAPI cryptoAPI;

    public CryptoCommand(Main plugin, CryptoAPI cryptoAPI) {
        this.plugin = plugin;
        this.cryptoAPI = cryptoAPI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            new CryptoGUI(plugin).openMainMenu(player);
            return true;
        }
        return false;
    }
}
