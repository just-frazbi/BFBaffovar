package com.frazbi.bFBaffovar;

import com.frazbi.bFBaffovar.commands.BFBCommand;
import com.frazbi.bFBaffovar.gui.PotionGUI;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class BFBaffovar extends JavaPlugin {

    private EconomyManager economyManager;
    private PotionGUI potionGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        economyManager = new EconomyManager(this);
        economyManager.setup();

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
        if (potionGUI != null) potionGUI.closeAllGUIs();
        getLogger().info("BFBaffovar disabled.");
    }

    public void reloadPlugin() {
        reloadConfig();
        economyManager.setup();
        potionGUI.reload();
        getLogger().info("BFBaffovar config reloaded.");
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public PotionGUI getPotionGUI() {
        return potionGUI;
    }
}