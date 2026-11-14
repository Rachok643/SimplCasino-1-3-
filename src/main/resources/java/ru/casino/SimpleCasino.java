package ru.casino;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SimpleCasino extends JavaPlugin implements CommandExecutor {

    private static Economy econ = null;
    private final Set<UUID> activePlayers = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!setupEconomy()) {
            getLogger().severe("Плагин Vault или плагин экономики не найден! Отключение...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (getCommand("casino") != null) {
            getCommand("casino").setExecutor(this);
        }
        getServer().getPluginManager().registerEvents(new CasinoGame(), this);
        getLogger().info("SimpleCasino успешно запущен!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public Set<UUID> getActivePlayers() {
        return activePlayers;
    }

    public String getMsg(String path) {
        String prefix = getConfig().getString("messages.prefix", "");
        String msg = getConfig().getString("messages." + path, "");
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только игрокам.");
            return true;
        }

        if (activePlayers.contains(player.getUniqueId())) {
            player.sendMessage(getMsg("already-playing"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(getMsg("usage"));
            return true;
        }

        double bet;
        try {
            bet = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(getMsg("invalid-number"));
            return true;
        }

        double minBet = getConfig().getDouble("min-bet", 50.0);
        double maxBet = getConfig().getDouble("max-bet", 50000.0);

        if (bet < minBet || bet > maxBet) {
            player.sendMessage(getMsg("bet-limits")
                    .replace("%min%", String.valueOf(minBet))
                    .replace("%max%", String.valueOf(maxBet)));
            return true;
        }

        if (!econ.has(player, bet)) {
            player.sendMessage(getMsg("no-money"));
            return true;
        }

        econ.withdrawPlayer(player, bet);
        CasinoGame.startSpin(this, player, bet);
        return true;
    }
}
