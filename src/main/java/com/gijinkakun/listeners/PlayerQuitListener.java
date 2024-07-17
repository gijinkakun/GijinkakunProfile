package com.gijinkakun.listeners;

import com.gijinkakun.ProfileManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final ProfileManager profileManager;

    public PlayerQuitListener(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        profileManager.savePlayerDataToYAML(player);
    }
}
