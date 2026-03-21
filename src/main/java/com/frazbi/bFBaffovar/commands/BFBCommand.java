package com.frazbi.bFBaffovar.commands;

import com.frazbi.bFBaffovar.BFBaffovar;
import com.frazbi.bFBaffovar.utils.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BFBCommand implements CommandExecutor, TabCompleter {

    private final BFBaffovar plugin;

    public BFBCommand(BFBaffovar plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            String reloadPerm = plugin.getConfig().getString("permissions.reload", "bfbaffovar.reload");
            if (!sender.hasPermission(reloadPerm)) {
                sender.sendMessage(getMsg("no-permission"));
                return true;
            }
            plugin.reloadPlugin();
            sender.sendMessage(getMsg("reload-success"));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ColorUtil.color("&cOnly players can use this command."));
                return true;
            }
            Player player = (Player) sender;
            String usePerm = plugin.getConfig().getString("permissions.use", "bfbaffovar.use");
            if (!player.hasPermission(usePerm)) {
                player.sendMessage(getMsg("no-permission"));
                return true;
            }
            plugin.getPotionGUI().openGUI(player);
            return true;
        }

        sender.sendMessage(getMsg("invalid-command"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String reloadPerm = plugin.getConfig().getString("permissions.reload", "bfbaffovar.reload");
            if (sender.hasPermission(reloadPerm) && "reload".startsWith(args[0].toLowerCase())) {
                List<String> list = new ArrayList<>();
                list.add("reload");
                return list;
            }
        }
        return Collections.emptyList();
    }

    private String getMsg(String key) {
        String prefix = ColorUtil.color(plugin.getConfig().getString("messages.prefix", "&5BFB &8>> &r"));
        String raw = plugin.getConfig().getString("messages." + key, key);
        return ColorUtil.color(raw.replace("{prefix}", prefix));
    }
}