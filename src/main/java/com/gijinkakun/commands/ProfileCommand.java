package com.gijinkakun.commands;

import com.gijinkakun.ProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProfileCommand implements CommandExecutor, TabCompleter {

    private final ProfileManager profileManager;
    private final String errorColor = ChatColor.of("#AB3428").toString();

    public ProfileCommand(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("pf") && args.length > 0) {
            if (sender instanceof Player) {
                profileManager.resolvePlayerInfo((Player) sender, args[0]);
                return true;
            } else {
                sender.sendMessage(errorColor + "This command can only be run by a player.");
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (cmd.getName().equalsIgnoreCase("pf")) {
            if (args.length == 1) {
                List<String> playerNames = new ArrayList<>();
                String partialName = args[0].toLowerCase();
                for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                    if (offlinePlayer.getName() != null && offlinePlayer.getName().toLowerCase().startsWith(partialName)) {
                        playerNames.add(offlinePlayer.getName());
                    }
                }
                return playerNames;
            }
        }
        return Collections.emptyList();
    }
}
