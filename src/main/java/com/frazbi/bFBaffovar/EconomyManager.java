package com.frazbi.bFBaffovar;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyManager {

    public enum Provider { VAULT, PLAYERPOINTS, NONE }

    private final JavaPlugin plugin;
    private Provider activeProvider = Provider.NONE;
    private Economy vaultEconomy;
    private Object ppApi;

    public EconomyManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        String providerName = plugin.getConfig().getString("economy.provider", "VAULT").toUpperCase();
        if (providerName.equals("PLAYERPOINTS")) {
            if (setupPlayerPoints()) {
                activeProvider = Provider.PLAYERPOINTS;
                plugin.getLogger().info("Economy: PlayerPoints hooked.");
            } else {
                plugin.getLogger().warning("Economy: PlayerPoints not found, falling back to Vault.");
                if (setupVault()) {
                    activeProvider = Provider.VAULT;
                    plugin.getLogger().info("Economy: Vault hooked as fallback: " + vaultEconomy.getName());
                } else {
                    activeProvider = Provider.NONE;
                    plugin.getLogger().warning("Economy: No economy plugin found. Economy disabled.");
                }
            }
        } else {
            if (setupVault()) {
                activeProvider = Provider.VAULT;
                plugin.getLogger().info("Economy: Vault hooked: " + vaultEconomy.getName());
            } else {
                plugin.getLogger().warning("Economy: Vault not found. Economy disabled.");
                activeProvider = Provider.NONE;
            }
        }
    }

    public boolean isEnabled() {
        return activeProvider != Provider.NONE;
    }

    public Provider getActiveProvider() {
        return activeProvider;
    }

    public String getCurrencyName() {
        if (activeProvider == Provider.VAULT && vaultEconomy != null) return vaultEconomy.currencyNamePlural();
        if (activeProvider == Provider.PLAYERPOINTS) return plugin.getConfig().getString("economy.playerpoints-currency-name", "points");
        return "coins";
    }

    public double getBalance(Player player) {
        if (activeProvider == Provider.VAULT && vaultEconomy != null) return vaultEconomy.getBalance(player);
        if (activeProvider == Provider.PLAYERPOINTS && ppApi != null) {
            try {
                java.lang.reflect.Method m = ppApi.getClass().getMethod("look", java.util.UUID.class);
                return ((Number) m.invoke(ppApi, player.getUniqueId())).doubleValue();
            } catch (Exception e) {
                plugin.getLogger().warning("PlayerPoints getBalance error: " + e.getMessage());
            }
        }
        return 0;
    }

    public boolean withdraw(Player player, double amount) {
        if (activeProvider == Provider.VAULT && vaultEconomy != null) {
            EconomyResponse resp = vaultEconomy.withdrawPlayer(player, amount);
            return resp.transactionSuccess();
        }
        if (activeProvider == Provider.PLAYERPOINTS && ppApi != null) {
            try {
                int points = (int) Math.ceil(amount);
                if ((int) getBalance(player) < points) return false;
                java.lang.reflect.Method m = ppApi.getClass().getMethod("take", java.util.UUID.class, int.class);
                return Boolean.TRUE.equals(m.invoke(ppApi, player.getUniqueId(), points));
            } catch (Exception e) {
                plugin.getLogger().warning("PlayerPoints withdraw error: " + e.getMessage());
            }
        }
        return false;
    }

    public boolean has(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    private boolean setupVault() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        vaultEconomy = rsp.getProvider();
        return vaultEconomy != null;
    }

    private boolean setupPlayerPoints() {
        org.bukkit.plugin.Plugin pp = plugin.getServer().getPluginManager().getPlugin("PlayerPoints");
        if (pp == null) return false;
        try {
            java.lang.reflect.Method getApi = pp.getClass().getMethod("getAPI");
            ppApi = getApi.invoke(pp);
            return ppApi != null;
        } catch (Exception e) {
            plugin.getLogger().warning("PlayerPoints hook error: " + e.getMessage());
            return false;
        }
    }
}