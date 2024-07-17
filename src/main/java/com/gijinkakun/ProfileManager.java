package com.gijinkakun;

import com.gijinkakun.utils.ProfileUtils;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatColor;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Date;

public class ProfileManager {

    private final GijinkakunProfile plugin;
    private final LuckPerms luckPerms;
    private final String errorColor = ChatColor.of("#AB3428").toString();
    private final String labelColor;
    private final String valueColor;
    private final String noValueColor;
    private final String messageColor;

    public ProfileManager(GijinkakunProfile plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        FileConfiguration config = plugin.getConfig();
        this.labelColor = ChatColor.of(config.getString("colors.Label-Color", "#F7E733")).toString();
        this.valueColor = ChatColor.of(config.getString("colors.Stat-Color", "#F7F7F9")).toString();
        this.noValueColor = ChatColor.of(config.getString("colors.No-Stat-Color", "#545E56")).toString();
        this.messageColor = ChatColor.of(config.getString("colors.Player-Name-Color", "#F7F7F9")).toString();
    }

    public void savePlayerDataToYAML(OfflinePlayer offlinePlayer) {
        File playerDataFile = new File(plugin.getDataFolder() + File.separator + "profiles", offlinePlayer.getUniqueId() + ".yml");
        plugin.logToConsole("Saving data to: " + playerDataFile.getAbsolutePath(), ChatColor.YELLOW);

        FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerDataFile);

        playerData.set("name", offlinePlayer.getName());
        playerData.set("firstJoined", ProfileUtils.formatDate(new Date(offlinePlayer.getFirstPlayed())));
        playerData.set("timePlayed", offlinePlayer.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20 / 60);
        playerData.set("deaths", offlinePlayer.getStatistic(Statistic.DEATHS));
        playerData.set("mobKills", ProfileUtils.getTotalMobKills(offlinePlayer));
        playerData.set("blocksMined", ProfileUtils.getTotalBlocksMined(offlinePlayer));
        playerData.set("blocksWalked", ProfileUtils.getTotalBlocksWalked(offlinePlayer));
        playerData.set("animalsBred", offlinePlayer.getStatistic(Statistic.ANIMALS_BRED));
        playerData.set("playerKills", offlinePlayer.getStatistic(Statistic.PLAYER_KILLS));

        fetchUser(offlinePlayer.getUniqueId()).thenAccept(user -> {
            if (user != null) {
                String groupName = ProfileUtils.capitalizeFirstLetter(ProfileUtils.getPrimaryGroupName(user));
                playerData.set("groupName", groupName);
            } else {
                playerData.set("groupName", "Unknown");
            }

            try {
                playerData.save(playerDataFile);
                plugin.logToConsole("Data saved successfully for " + offlinePlayer.getName(), ChatColor.GREEN);
            } catch (IOException e) {
                plugin.logToConsole("Could not save data for " + offlinePlayer.getName(), ChatColor.RED);
                e.printStackTrace();
            }
        }).exceptionally(ex -> {
            plugin.logToConsole("Could not fetch user data for saving: " + ex.getMessage(), ChatColor.RED);
            ex.printStackTrace();
            return null;
        });
    }

    public void loadPlayerDataFromYAML(UUID playerUUID, Player requester) {
        File playerDataFile = new File(plugin.getDataFolder() + File.separator + "profiles", playerUUID + ".yml");
        plugin.logToConsole("Attempting to load data from: " + playerDataFile.getAbsolutePath(), ChatColor.YELLOW);

        if (!playerDataFile.exists()) {
            plugin.logToConsole("Player data file not found: " + playerDataFile.getAbsolutePath(), ChatColor.RED);
            requester.sendMessage(errorColor + "Player data file not found.");
            return;
        }

        FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerDataFile);
        String playerName = playerData.getString("name", "Unknown");
        String firstJoined = playerData.getString("firstJoined", "Unknown");
        int timePlayed = playerData.getInt("timePlayed", 0);
        int deaths = playerData.getInt("deaths", 0);
        int mobKills = playerData.getInt("mobKills", 0);
        int blocksMined = playerData.getInt("blocksMined", 0);
        int blocksWalked = playerData.getInt("blocksWalked", 0);
        int animalsBred = playerData.getInt("animalsBred", 0);
        int playerKills = playerData.getInt("playerKills", 0);
        String groupName = playerData.getString("groupName", "Unknown");

        plugin.logToConsole("Loaded data: Name: " + playerName + ", First Joined: " + firstJoined + ", Group: " + groupName, ChatColor.YELLOW);

        sendPlayerProfile(requester, playerName, groupName, firstJoined, timePlayed, deaths, mobKills, blocksMined,
                blocksWalked, animalsBred, playerKills);
    }

    public void resolvePlayerInfo(Player requester, String playerNameOrUUID) {
        UUID playerUUID = parseUUID(playerNameOrUUID);

        if (playerUUID != null) {
            CompletableFuture.runAsync(() -> {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
                if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> requester.sendMessage(errorColor + "Invalid player name or UUID."));
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (offlinePlayer.isOnline()) {
                            getPlayerInfo(requester, offlinePlayer.getPlayer());
                        } else {
                            loadPlayerDataFromYAML(offlinePlayer.getUniqueId(), requester);
                        }
                    });
                }
            });
        } else {
            CompletableFuture.runAsync(() -> {
                OfflinePlayer offlinePlayer = getOfflinePlayerByName(playerNameOrUUID);
                if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
                    Bukkit.getScheduler().runTask(plugin, () -> getPlayerInfo(requester, offlinePlayer));
                } else {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> requester.sendMessage(errorColor + "Invalid player name or UUID."));
                }
            });
        }
    }

    private UUID parseUUID(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private OfflinePlayer getOfflinePlayerByName(String playerName) {
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(playerName)) {
                return offlinePlayer;
            }
        }
        return null;
    }

    private void getPlayerInfo(Player requester, OfflinePlayer offlinePlayer) {
        if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
            fetchUser(offlinePlayer.getUniqueId()).thenAccept(user -> {
                if (user != null) {
                    String groupName = ProfileUtils.capitalizeFirstLetter(ProfileUtils.getPrimaryGroupName(user));
                    int deathCount = offlinePlayer.getStatistic(Statistic.DEATHS);
                    int mobKillCount = ProfileUtils.getTotalMobKills(offlinePlayer);
                    int blocksMinedCount = ProfileUtils.getTotalBlocksMined(offlinePlayer);
                    int blocksWalkedCount = ProfileUtils.getTotalBlocksWalked(offlinePlayer);
                    int animalsBredCount = offlinePlayer.getStatistic(Statistic.ANIMALS_BRED);
                    int timePlayedMinutes = offlinePlayer.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20 / 60;
                    String firstJoinedFormatted = ProfileUtils.formatDate(new Date(offlinePlayer.getFirstPlayed()));
                    int playersKilledCount = offlinePlayer.getStatistic(Statistic.PLAYER_KILLS);

                    sendPlayerProfile(requester, offlinePlayer.getName(), groupName, firstJoinedFormatted,
                            timePlayedMinutes, deathCount, mobKillCount, blocksMinedCount, blocksWalkedCount,
                            animalsBredCount, playersKilledCount);
                } else {
                    requester.sendMessage(errorColor + "User data not available.");
                }
            }).exceptionally(ex -> {
                requester.sendMessage(errorColor + "User data not available.");
                ex.printStackTrace();
                return null;
            });
        } else {
            requester.sendMessage(errorColor + "Player not found or has never played.");
        }
    }

    private CompletableFuture<User> fetchUser(UUID uuid) {
        return luckPerms.getUserManager().loadUser(uuid);
    }

    private void sendPlayerProfile(Player player, String playerName, String groupName, String firstJoined,
        int timePlayed, int deaths, int mobKills, int blocksMined, int blocksWalked, int animalsBred, int playerKills) {

        player.sendMessage(messageColor + "[" + playerName + "]");
        if (groupName != null)
        player.sendMessage(labelColor + "Rank: " + ChatColor.RESET + valueColor + groupName);
        player.sendMessage(labelColor + "First Joined: " + ChatColor.RESET + valueColor + firstJoined);
        player.sendMessage(labelColor + "Time Played: " + ChatColor.RESET + valueColor + timePlayed + " minutes");
        player.sendMessage(labelColor + "Deaths: " + ChatColor.RESET + (deaths == 0 ? noValueColor : valueColor) + deaths);
        player.sendMessage(labelColor + "Mob Kills: " + ChatColor.RESET + (mobKills == 0 ? noValueColor : valueColor) + mobKills);
        player.sendMessage(labelColor + "Blocks Mined: " + ChatColor.RESET + (blocksMined == 0 ? noValueColor : valueColor) + blocksMined);
        player.sendMessage(labelColor + "Blocks Walked: " + ChatColor.RESET + (blocksWalked == 0 ? noValueColor : valueColor) + blocksWalked);
        player.sendMessage(labelColor + "Animals Bred: " + ChatColor.RESET + (animalsBred == 0 ? noValueColor : valueColor) + animalsBred);
        player.sendMessage(labelColor + "Players Killed: " + ChatColor.RESET + (playerKills == 0 ? noValueColor : valueColor) + playerKills);
    }
}
