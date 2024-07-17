package com.gijinkakun;

import com.gijinkakun.commands.ProfileCommand;
import com.gijinkakun.listeners.PlayerQuitListener;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;

import java.io.File;

public class GijinkakunProfile extends JavaPlugin {

    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        // Ensure the profiles directory exists
        File profilesDir = new File(getDataFolder(), "profiles");
        if (!profilesDir.exists()) {
            boolean dirCreated = profilesDir.mkdirs();
            logToConsole("Profiles directory created: " + dirCreated, ChatColor.GREEN);
        } else {
            logToConsole("Profiles directory already exists", ChatColor.GREEN);
        }

        // Initialize LuckPerms API
        luckPerms = LuckPermsProvider.get();
        if (luckPerms == null) {
            logToConsole("LuckPerms API could not be loaded!", ChatColor.RED);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        ProfileManager profileManager = new ProfileManager(this, luckPerms);

        // Register the event listener
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(profileManager), this);
        // Register commands
        ProfileCommand profileCommand = new ProfileCommand(profileManager);
        getCommand("pf").setExecutor(profileCommand);
        getCommand("pf").setTabCompleter(profileCommand);
        // Log to console that the plugin has been enabled
        logToConsole("Gijinkakun Profile has been enabled!", ChatColor.GREEN);
    }

    @Override
    public void onDisable() {
        // Log to console that the plugin has been disabled
        logToConsole("Gijinkakun Profile has been disabled!", ChatColor.RED);
    }

    /**
     * Logs a message to the console with a specific color.
     *
     * @param message The message to log.
     * @param color   The color to use.
     */
    public void logToConsole(String message, ChatColor color) {
        Bukkit.getConsoleSender().sendMessage(color + message);
    }
}
