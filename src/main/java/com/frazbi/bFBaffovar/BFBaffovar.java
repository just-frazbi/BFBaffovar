package com.frazbi.bFBaffovar;

import com.frazbi.bFBaffovar.commands.BFBCommand;
import com.frazbi.bFBaffovar.gui.PotionGUI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class BFBaffovar extends JavaPlugin {

    private Economy economy;
    private PotionGUI potionGUI;
    private boolean vaultEnabled = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (setupEconomy()) {
            vaultEnabled = true;
            getLogger().info("Vault hooked: " + economy.getName());
        } else {
            getLogger().warning("Vault not found. Economy features disabled.");
        }

        potionGUI = new PotionGUI(this);
        getServer().getPluginManager().registerEvents(potionGUI, this);

        BFBCommand bfbCommand = new BFBCommand(this);
        PluginCommand cmd = getCommand("bfbaffovar");
        if (cmd != null) {
            cmd.setExecutor(bfbCommand);
            cmd.setTabCompleter(bfbCommand);
        } else {
            getLogger().severe("Command 'bfbaffovar' not found in plugin.yml!");
        }

        getLogger().info("BFBaffovar enabled!");
    }

    @Override
    public void onDisable() {
        if (potionGUI != null) {
            potionGUI.closeAllGUIs();
        }
        getLogger().info("BFBaffovar disabled.");
    }

    public void reloadPlugin() {
        reloadConfig();
        potionGUI.reload();
        getLogger().info("BFBaffovar config reloaded.");
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean isVaultEnabled() {
        return vaultEnabled;
    }

    public PotionGUI getPotionGUI() {
        return potionGUI;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }
}