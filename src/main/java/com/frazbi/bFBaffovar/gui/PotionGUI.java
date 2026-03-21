package com.frazbi.bFBaffovar.gui;

import com.frazbi.bFBaffovar.BFBaffovar;
import com.frazbi.bFBaffovar.utils.ColorUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PotionGUI implements Listener {

    private final BFBaffovar plugin;

    private final Map<UUID, Inventory> openInventories = new ConcurrentHashMap<>();
    private final Set<UUID> merging = Collections.synchronizedSet(new HashSet<>());

    private int guiSize;
    private String guiTitle;
    private List<Integer> potionSlots;
    private int mergeButtonSlot;
    private int clearButtonSlot;
    private int infoButtonSlot;
    private List<Integer> fillerSlots;
    private boolean fillerEnabled;
    private String fillerMaterial;
    private String fillerName;
    private int maxPotions;
    private boolean requireSameType;
    private String pricingMode;
    private double cost;
    private boolean economyEnabled;

    public PotionGUI(BFBaffovar plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        guiSize         = plugin.getConfig().getInt("gui.size", 27);
        guiTitle        = ColorUtil.color(plugin.getConfig().getString("gui.title", "&5Объединение зелий"));
        potionSlots     = plugin.getConfig().getIntegerList("gui.potion-slots");
        mergeButtonSlot = plugin.getConfig().getInt("gui.merge-button.slot", 22);
        clearButtonSlot = plugin.getConfig().getInt("gui.clear-button.slot", 18);
        infoButtonSlot  = plugin.getConfig().getInt("gui.info-button.slot", 26);
        fillerEnabled   = plugin.getConfig().getBoolean("gui.filler.enabled", true);
        fillerMaterial  = plugin.getConfig().getString("gui.filler.material", "GRAY_STAINED_GLASS_PANE");
        fillerName      = plugin.getConfig().getString("gui.filler.name", " ");
        fillerSlots     = plugin.getConfig().getIntegerList("gui.filler.slots");
        maxPotions      = Math.min(plugin.getConfig().getInt("merge.max-potions", 7), 7);
        requireSameType = plugin.getConfig().getBoolean("merge.require-same-type", true);
        pricingMode     = plugin.getConfig().getString("economy.pricing-mode", "PER_POTION").toUpperCase();
        cost            = plugin.getConfig().getDouble("economy.cost", 50.0);
        economyEnabled  = plugin.getConfig().getBoolean("economy.enabled", true);
    }

    // -------------------------------------------------------------------------
    // Open GUI
    // -------------------------------------------------------------------------

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, guiSize, guiTitle);

        if (fillerEnabled) {
            ItemStack filler = buildItem(fillerMaterial, fillerName, null);
            for (int slot : fillerSlots) {
                if (slot >= 0 && slot < guiSize) {
                    inv.setItem(slot, filler);
                }
            }
        }

        inv.setItem(mergeButtonSlot, buildMergeButton(player, 0));
        inv.setItem(clearButtonSlot, buildClearButton());
        inv.setItem(infoButtonSlot,  buildInfoButton());

        openInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
        playSound(player, "open-gui");
    }

    // -------------------------------------------------------------------------
    // Click
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Inventory tracked = openInventories.get(player.getUniqueId());
        if (tracked == null) return;
        if (!tracked.equals(event.getView().getTopInventory())) return;

        // Bottom inventory — обрабатываем только shift+click
        if (event.getClickedInventory() != null
                && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
                ItemStack item = event.getCurrentItem();
                if (item == null || item.getType() == Material.AIR) return;
                if (!isPotion(item)) {
                    sendMessage(player, "not-a-potion");
                    return;
                }
                // Проверка совместимости с уже лежащими зельями
                if (!isCompatibleWithSlots(item, tracked)) {
                    sendMessage(player, "not-same-type");
                    playSound(player, "merge-fail");
                    return;
                }
                for (int slot : potionSlots) {
                    ItemStack existing = tracked.getItem(slot);
                    if (existing == null || existing.getType() == Material.AIR) {
                        tracked.setItem(slot, item.clone());
                        event.setCurrentItem(new ItemStack(Material.AIR));
                        refreshMergeButton(player, tracked);
                        return;
                    }
                }
            }
            return;
        }

        // Top inventory — отменяем всё, обрабатываем вручную
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= guiSize) return;

        if (slot == mergeButtonSlot) { handleMerge(player, tracked); return; }
        if (slot == clearButtonSlot) { handleClear(player, tracked); return; }
        if (slot == infoButtonSlot)  return;
        if (!potionSlots.contains(slot)) return;

        handlePotionSlotClick(event, player, tracked, slot);
    }

    private void handlePotionSlotClick(InventoryClickEvent event, Player player, Inventory inv, int slot) {
        ItemStack cursor   = player.getItemOnCursor();
        ItemStack slotItem = inv.getItem(slot);

        boolean slotEmpty   = slotItem == null || slotItem.getType() == Material.AIR;
        boolean cursorEmpty = cursor   == null || cursor.getType()   == Material.AIR;

        InventoryAction action = event.getAction();

        // Взять из слота
        if (!slotEmpty && cursorEmpty &&
                (action == InventoryAction.PICKUP_ALL || action == InventoryAction.PICKUP_HALF
                        || action == InventoryAction.PICKUP_ONE || action == InventoryAction.PICKUP_SOME)) {

            if (action == InventoryAction.PICKUP_HALF) {
                int half = (int) Math.ceil(slotItem.getAmount() / 2.0);
                ItemStack toPickup = slotItem.clone();
                toPickup.setAmount(half);
                int remaining = slotItem.getAmount() - half;
                if (remaining <= 0) {
                    inv.setItem(slot, null);
                } else {
                    slotItem.setAmount(remaining);
                    inv.setItem(slot, slotItem);
                }
                player.setItemOnCursor(toPickup);
            } else {
                player.setItemOnCursor(slotItem.clone());
                inv.setItem(slot, null);
            }
            refreshMergeButton(player, inv);
            return;
        }

        // Положить в слот
        if (!cursorEmpty && slotEmpty &&
                (action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE
                        || action == InventoryAction.PLACE_SOME)) {

            if (!isPotion(cursor)) {
                sendMessage(player, "not-a-potion");
                playSound(player, "merge-fail");
                return;
            }
            // Проверяем совместимость с другими зельями в слотах (исключая текущий слот — он пустой)
            if (!isCompatibleWithSlots(cursor, inv)) {
                sendMessage(player, "not-same-type");
                playSound(player, "merge-fail");
                return;
            }
            ItemStack toPlace = cursor.clone();
            if (action == InventoryAction.PLACE_ONE) toPlace.setAmount(1);
            inv.setItem(slot, toPlace);
            int remaining = cursor.getAmount() - toPlace.getAmount();
            if (remaining <= 0) {
                player.setItemOnCursor(new ItemStack(Material.AIR));
            } else {
                cursor.setAmount(remaining);
                player.setItemOnCursor(cursor);
            }
            refreshMergeButton(player, inv);
            return;
        }

        // Поменять местами курсор и слот
        if (!cursorEmpty && !slotEmpty && action == InventoryAction.SWAP_WITH_CURSOR) {
            if (!isPotion(cursor)) {
                sendMessage(player, "not-a-potion");
                playSound(player, "merge-fail");
                return;
            }
            // Проверяем совместимость: курсор должен совпадать с остальными зельями (кроме этого слота)
            if (!isCompatibleWithSlotsExcluding(cursor, inv, slot)) {
                sendMessage(player, "not-same-type");
                playSound(player, "merge-fail");
                return;
            }
            ItemStack old = slotItem.clone();
            inv.setItem(slot, cursor.clone());
            player.setItemOnCursor(old);
            refreshMergeButton(player, inv);
        }
    }

    // -------------------------------------------------------------------------
    // Drag
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Inventory tracked = openInventories.get(player.getUniqueId());
        if (tracked == null) return;
        if (!tracked.equals(event.getView().getTopInventory())) return;

        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Close
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        Inventory tracked = openInventories.remove(player.getUniqueId());
        if (tracked == null) return;

        returnPotions(player, tracked);
    }

    // -------------------------------------------------------------------------
    // Quit
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Inventory tracked = openInventories.remove(player.getUniqueId());
        if (tracked == null) return;
        returnPotions(player, tracked);
    }

    // -------------------------------------------------------------------------
    // Merge
    // -------------------------------------------------------------------------

    private void handleMerge(Player player, Inventory inv) {
        UUID uuid = player.getUniqueId();
        if (merging.contains(uuid)) return;
        merging.add(uuid);

        try {
            List<ItemStack> potions = new ArrayList<>();
            List<Integer> usedSlots = new ArrayList<>();

            for (int slot : potionSlots) {
                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    if (!isPotion(item)) {
                        sendMessage(player, "not-a-potion");
                        playSound(player, "merge-fail");
                        return;
                    }
                    potions.add(item.clone());
                    usedSlots.add(slot);
                }
            }

            if (potions.isEmpty()) {
                sendMessage(player, "no-potions");
                playSound(player, "merge-fail");
                return;
            }

            // Финальная проверка одинаковости (защита от обхода)
            if (!allSameType(potions)) {
                sendMessage(player, "not-same-type");
                playSound(player, "merge-fail");
                return;
            }

            int count = potions.size();
            String freePerm = plugin.getConfig().getString("permissions.free", "bfbaffovar.free");
            boolean isFree = !economyEnabled || !plugin.isVaultEnabled() || player.hasPermission(freePerm);

            if (!isFree) {
                Economy eco = plugin.getEconomy();
                double totalCost = pricingMode.equals("PER_POTION") ? cost * count : cost;
                double balance = eco.getBalance(player);

                if (balance < totalCost) {
                    String msg = getMessage("not-enough-money")
                            .replace("{cost}", String.format("%.2f", totalCost))
                            .replace("{balance}", String.format("%.2f", balance))
                            .replace("{currency}", eco.currencyNamePlural());
                    player.sendMessage(msg);
                    playSound(player, "no-money");
                    return;
                }

                EconomyResponse response = eco.withdrawPlayer(player, totalCost);
                if (!response.transactionSuccess()) {
                    sendMessage(player, "not-enough-money");
                    playSound(player, "no-money");
                    return;
                }

                for (int s : usedSlots) inv.setItem(s, null);
                giveOrDrop(player, buildMergedPotion(potions));

                String msg = getMessage("merge-success")
                        .replace("{count}", String.valueOf(count))
                        .replace("{cost}", String.format("%.2f", totalCost))
                        .replace("{currency}", eco.currencyNamePlural());
                player.sendMessage(msg);

            } else {
                for (int s : usedSlots) inv.setItem(s, null);
                giveOrDrop(player, buildMergedPotion(potions));

                player.sendMessage(getMessage("merge-free").replace("{count}", String.valueOf(count)));
            }

            playSound(player, "merge-success");
            refreshMergeButton(player, inv);

        } finally {
            merging.remove(uuid);
        }
    }

    private ItemStack buildMergedPotion(List<ItemStack> potions) {
        ItemStack base = potions.get(0).clone();
        base.setAmount(potions.size());
        return base;
    }

    // -------------------------------------------------------------------------
    // Clear
    // -------------------------------------------------------------------------

    private void handleClear(Player player, Inventory inv) {
        for (int slot : potionSlots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                giveOrDrop(player, item.clone());
                inv.setItem(slot, null);
            }
        }
        refreshMergeButton(player, inv);
    }

    // -------------------------------------------------------------------------
    // Item builders
    // -------------------------------------------------------------------------

    private ItemStack buildMergeButton(Player player, int potionCount) {
        String mat  = plugin.getConfig().getString("gui.merge-button.material", "PURPLE_STAINED_GLASS_PANE");
        String name = plugin.getConfig().getString("gui.merge-button.name", "&aОбъединить");
        List<String> loreTemplate = plugin.getConfig().getStringList("gui.merge-button.lore");

        Economy eco = plugin.isVaultEnabled() ? plugin.getEconomy() : null;
        double totalCost = pricingMode.equals("PER_POTION") ? cost * Math.max(potionCount, 1) : cost;
        String balance  = eco != null ? String.format("%.2f", eco.getBalance(player)) : "N/A";
        String currency = eco != null ? eco.currencyNamePlural() : "монет";

        List<String> lore = new ArrayList<>();
        for (String line : loreTemplate) {
            lore.add(ColorUtil.color(line
                    .replace("{max}", String.valueOf(maxPotions))
                    .replace("{cost}", String.format("%.2f", totalCost))
                    .replace("{balance}", balance)
                    .replace("{currency}", currency)));
        }
        return buildItem(mat, name, lore);
    }

    private ItemStack buildClearButton() {
        String mat  = plugin.getConfig().getString("gui.clear-button.material", "RED_STAINED_GLASS_PANE");
        String name = plugin.getConfig().getString("gui.clear-button.name", "&cВернуть всё");
        List<String> rawLore = plugin.getConfig().getStringList("gui.clear-button.lore");
        List<String> lore = new ArrayList<>();
        for (String l : rawLore) lore.add(ColorUtil.color(l));
        return buildItem(mat, name, lore);
    }

    private ItemStack buildInfoButton() {
        String mat  = plugin.getConfig().getString("gui.info-button.material", "LIGHT_BLUE_STAINED_GLASS_PANE");
        String name = plugin.getConfig().getString("gui.info-button.name", "&bИнфо");
        List<String> rawLore = plugin.getConfig().getStringList("gui.info-button.lore");
        String pricingLabel = pricingMode.equals("PER_POTION") ? "за зелье" : "фикс.";
        String currency = plugin.isVaultEnabled() ? plugin.getEconomy().currencyNamePlural() : "монет";

        List<String> lore = new ArrayList<>();
        for (String l : rawLore) {
            lore.add(ColorUtil.color(l
                    .replace("{max}", String.valueOf(maxPotions))
                    .replace("{cost}", String.format("%.2f", cost))
                    .replace("{currency}", currency)
                    .replace("{pricing_mode}", pricingMode)
                    .replace("{pricing_label}", pricingLabel)));
        }
        return buildItem(mat, name, lore);
    }

    private ItemStack buildItem(String materialName, String displayName, List<String> lore) {
        Material mat;
        try {
            mat = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Неверный материал: " + materialName);
            mat = Material.STONE;
        }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(displayName));
            if (lore != null && !lore.isEmpty()) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void refreshMergeButton(Player player, Inventory inv) {
        int count = 0;
        for (int slot : potionSlots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) count++;
        }
        inv.setItem(mergeButtonSlot, buildMergeButton(player, count));
    }

    private void returnPotions(Player player, Inventory inv) {
        for (int slot : potionSlots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                giveOrDrop(player, item.clone());
                inv.setItem(slot, null);
            }
        }
    }

    private boolean isPotion(ItemStack item) {
        if (item == null) return false;
        Material t = item.getType();
        return t == Material.POTION
                || t == Material.SPLASH_POTION
                || t == Material.LINGERING_POTION
                || t == Material.TIPPED_ARROW;
    }

    /**
     * Полная проверка одинаковости зелий:
     * сравниваем Material (обычное/плеск/летящее) + PotionType (тип) + усиление + продление
     */
    private boolean allSameType(List<ItemStack> potions) {
        if (potions.size() <= 1) return true;
        ItemStack first = potions.get(0);
        String firstKey = getPotionKey(first);
        for (int i = 1; i < potions.size(); i++) {
            if (!firstKey.equals(getPotionKey(potions.get(i)))) return false;
        }
        return true;
    }

    /**
     * Проверяет, совместим ли item с зельями, уже лежащими в слотах инвентаря.
     * Если слоты пустые — всегда совместим.
     */
    private boolean isCompatibleWithSlots(ItemStack item, Inventory inv) {
        String itemKey = getPotionKey(item);
        for (int slot : potionSlots) {
            ItemStack existing = inv.getItem(slot);
            if (existing != null && existing.getType() != Material.AIR) {
                if (!itemKey.equals(getPotionKey(existing))) return false;
            }
        }
        return true;
    }

    /**
     * Как isCompatibleWithSlots, но игнорирует конкретный слот (для swap).
     */
    private boolean isCompatibleWithSlotsExcluding(ItemStack item, Inventory inv, int excludeSlot) {
        String itemKey = getPotionKey(item);
        for (int slot : potionSlots) {
            if (slot == excludeSlot) continue;
            ItemStack existing = inv.getItem(slot);
            if (existing != null && existing.getType() != Material.AIR) {
                if (!itemKey.equals(getPotionKey(existing))) return false;
            }
        }
        return true;
    }

    /**
     * Уникальный ключ зелья — работает на 1.20.1 и 1.20.5+/1.21+.
     *
     * До 1.20.4: используем старый API PotionData (getBasePotionData).
     * С 1.20.5+:  используем новый API getBasePotionType() через reflection,
     *             чтобы код компилировался под 1.20.1 без ошибок.
     *
     * Ключ: "MATERIAL:POTION_TYPE[:upgraded][:extended]"
     */
    private String getPotionKey(ItemStack item) {
        if (item == null) return "null";
        StringBuilder key = new StringBuilder(item.getType().name());
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof PotionMeta)) return key.toString();

        PotionMeta pm = (PotionMeta) meta;

        // Определяем версию сервера один раз через Bukkit.getBukkitVersion()
        // Формат: "1.20.1-R0.1-SNAPSHOT", "1.21-R0.1-SNAPSHOT" и т.д.
        try {
            String version = Bukkit.getBukkitVersion(); // e.g. "1.21-R0.1-SNAPSHOT"
            int[] ver = parseVersion(version);
            boolean isNewApi = ver[0] > 1 || (ver[0] == 1 && ver[1] > 20) || (ver[0] == 1 && ver[1] == 20 && ver[2] >= 5);

            if (isNewApi) {
                // 1.20.5+ API: getBasePotionType() возвращает PotionType напрямую
                // Используем reflection чтобы не сломать компиляцию под 1.20.1
                java.lang.reflect.Method method = PotionMeta.class.getMethod("getBasePotionType");
                Object potionType = method.invoke(pm);
                if (potionType != null) {
                    key.append(":").append(potionType.toString());
                }
            } else {
                // 1.20.1-1.20.4 API: старый PotionData
                java.lang.reflect.Method method = PotionMeta.class.getMethod("getBasePotionData");
                Object data = method.invoke(pm);
                if (data != null) {
                    java.lang.reflect.Method getType     = data.getClass().getMethod("getType");
                    java.lang.reflect.Method isUpgraded  = data.getClass().getMethod("isUpgraded");
                    java.lang.reflect.Method isExtended  = data.getClass().getMethod("isExtended");
                    key.append(":").append(getType.invoke(data).toString());
                    key.append(":").append(isUpgraded.invoke(data));
                    key.append(":").append(isExtended.invoke(data));
                }
            }
        } catch (Exception ex) {
            // Fallback: если что-то пошло не так — сравниваем только по типу материала
            plugin.getLogger().warning("getPotionKey reflection error: " + ex.getMessage());
        }

        return key.toString();
    }

    /** Парсит версию из строки вида "1.21-R0.1-SNAPSHOT" → [1, 21, 0] */
    private int[] parseVersion(String raw) {
        try {
            String clean = raw.split("-")[0]; // "1.21"
            String[] parts = clean.split("\\.");
            int major = parts.length > 0 ? Integer.parseInt(parts[0]) : 1;
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return new int[]{major, minor, patch};
        } catch (Exception e) {
            return new int[]{1, 20, 1};
        }
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            sendMessage(player, "inventory-full");
        }
    }

    private void playSound(Player player, String soundKey) {
        String soundName = plugin.getConfig().getString("sounds." + soundKey, "");
        if (soundName == null || soundName.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Неверный звук: " + soundName);
        }
    }

    private String getMessage(String key) {
        String prefix = ColorUtil.color(plugin.getConfig().getString("messages.prefix", "&5BFB >> "));
        String msg = plugin.getConfig().getString("messages." + key, key);
        return ColorUtil.color(msg.replace("{prefix}", prefix));
    }

    private void sendMessage(Player player, String key) {
        player.sendMessage(getMessage(key));
    }

    public void closeAllGUIs() {
        for (Map.Entry<UUID, Inventory> entry : openInventories.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                returnPotions(player, entry.getValue());
                player.closeInventory();
            }
        }
        openInventories.clear();
    }
}