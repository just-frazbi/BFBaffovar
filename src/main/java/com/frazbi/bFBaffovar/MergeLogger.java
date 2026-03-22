package com.frazbi.bFBaffovar;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class MergeLogger {

    private final BFBaffovar plugin;
    private final Logger console;
    private final File dbFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private boolean debugConsole;
    private boolean debugDatabase;

    public MergeLogger(BFBaffovar plugin) {
        this.plugin = plugin;
        this.console = plugin.getLogger();

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.dbFile = new File(dataFolder, "database.log");

        loadSettings();

        if (debugDatabase && !dbFile.exists()) {
            try {
                dbFile.createNewFile();
                writeLine("# BFBaffovar Merge Database");
                writeLine("# Format: [DATE] | PLAYER | UUID | WORLD | X Y Z | POTION | COUNT | COST | PROVIDER");
                writeLine("# -----------------------------------------------------------------------");
            } catch (IOException e) {
                console.warning("Could not create database.log: " + e.getMessage());
            }
        }

        console.info("Debug console: " + (debugConsole ? "enabled" : "disabled")
                + " | Debug database: " + (debugDatabase ? "enabled" : "disabled"));
    }

    public void reload() {
        loadSettings();
    }

    private void loadSettings() {
        debugConsole  = plugin.getConfig().getBoolean("debug.console", true);
        debugDatabase = plugin.getConfig().getBoolean("debug.database", true);
    }

    public void logMerge(Player player, List<ItemStack> potions, double cost, String provider) {
        String potionName = getPotionDisplayName(potions.get(0));
        int count = potions.size();
        String location = String.format("%s %.0f %.0f %.0f",
                player.getWorld().getName(),
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ());

        if (debugConsole) {
            console.info(String.format("[Debug] MERGE | Player: %s | Potion: %s x%d | Cost: %.2f | Provider: %s | Location: %s",
                    player.getName(), potionName, count, cost, provider, location));
        }

        if (debugDatabase) {
            writeLine(String.format("[%s] | %s | %s | %s | %s x%d | %.2f | %s",
                    dateFormat.format(new Date()),
                    player.getName(), player.getUniqueId(),
                    location, potionName, count, cost, provider));
        }
    }

    public void logMergeFree(Player player, List<ItemStack> potions) {
        logMerge(player, potions, 0.0, "FREE");
    }

    public void logFailure(Player player, String reason) {
        if (debugConsole) {
            console.info(String.format("[Debug] FAIL | Player: %s | Reason: %s", player.getName(), reason));
        }
        if (debugDatabase) {
            writeLine(String.format("[%s] | %s | %s | FAILED: %s",
                    dateFormat.format(new Date()), player.getName(), player.getUniqueId(), reason));
        }
    }

    public void logGuiOpen(Player player) {
        if (!debugConsole) return;
        console.info(String.format("[Debug] GUI_OPEN | Player: %s | Location: %s %.0f %.0f %.0f",
                player.getName(),
                player.getWorld().getName(),
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ()));
    }

    public void logGuiClose(Player player, int returnedCount) {
        if (!debugConsole) return;
        console.info(String.format("[Debug] GUI_CLOSE | Player: %s | Returned: %d potions",
                player.getName(), returnedCount));
    }

    private String getPotionDisplayName(ItemStack item) {
        if (item == null) return "UNKNOWN";
        if (item.getItemMeta() instanceof PotionMeta) {
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            try {
                java.lang.reflect.Method m = PotionMeta.class.getMethod("getBasePotionType");
                Object type = m.invoke(meta);
                if (type != null) return item.getType().name() + ":" + type;
            } catch (Exception ignored) {
                try {
                    java.lang.reflect.Method m = PotionMeta.class.getMethod("getBasePotionData");
                    Object data = m.invoke(meta);
                    if (data != null) {
                        Object type = data.getClass().getMethod("getType").invoke(data);
                        boolean upgraded = (boolean) data.getClass().getMethod("isUpgraded").invoke(data);
                        boolean extended = (boolean) data.getClass().getMethod("isExtended").invoke(data);
                        String suffix = upgraded ? " II" : extended ? " (extended)" : "";
                        return item.getType().name() + ":" + type + suffix;
                    }
                } catch (Exception ex) {
                    console.warning("[Debug] getPotionDisplayName error: " + ex.getMessage());
                }
            }
        }
        return item.getType().name();
    }

    private void writeLine(String line) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            console.warning("[Debug] Failed to write to database.log: " + e.getMessage());
        }
    }
}