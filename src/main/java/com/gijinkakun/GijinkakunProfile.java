package com.gijinkakun;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class GijinkakunProfile extends JavaPlugin implements Listener {
    private static final String PLAYER_DATA_NOT_FOUND = "Player data file not found.";
    private static final String PLAYER_NOT_FOUND = "Player not found or has never played.";
    private static final String USER_DATA_NOT_AVAILABLE = "User data not available.";
    private static final String COMMAND_PLAYER_ONLY = "This command can only be run by a player.";

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

        // Register the event listener
        getServer().getPluginManager().registerEvents(this, this);
        // Log to console that the plugin has been enabled
        logToConsole("Gijinkakun Profile has been enabled!", ChatColor.GREEN);
    }

    @Override
    public void onDisable() {
        // Log to console that the plugin has been disabled
        logToConsole("Gijinkakun Profile has been disabled!", ChatColor.RED);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        savePlayerDataToYAML(player);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("pf") && args.length > 0) {
            if (sender instanceof Player) {
                resolvePlayerInfo((Player) sender, args[0]);
                return true;
            } else {
                sender.sendMessage(formatNotice(COMMAND_PLAYER_ONLY));
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
                    if (offlinePlayer.getName().toLowerCase().startsWith(partialName)) {
                        playerNames.add(offlinePlayer.getName());
                    }
                }
                return playerNames;
            }
        }
        return Collections.emptyList();
    }

    /**
     * Resolves player information based on the provided player name or UUID.
     *
     * @param requester       The player requesting the information.
     * @param playerNameOrUUID The player name or UUID to resolve.
     */
    private void resolvePlayerInfo(Player requester, String playerNameOrUUID) {
        UUID playerUUID = parseUUID(playerNameOrUUID);

        if (playerUUID != null) {
            CompletableFuture.runAsync(() -> {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
                if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
                    Bukkit.getScheduler().runTask(this,
                            () -> requester.sendMessage(formatNotice("Invalid player name or UUID.")));
                } else {
                    Bukkit.getScheduler().runTask(this, () -> {
                        if (offlinePlayer.isOnline()) {
                            getPlayerInfo(requester, offlinePlayer.getPlayer());
                        } else {
                            loadPlayerDataFromYAML(offlinePlayer.getUniqueId(), requester);
                        }
                    });
                }
            });
        } else {
            OfflinePlayer offlinePlayer = getOfflinePlayerByName(playerNameOrUUID);
            if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
                getPlayerInfo(requester, offlinePlayer);
            } else {
                requester.sendMessage(formatNotice("Invalid player name or UUID."));
            }
        }
    }

    /**
     * Parses a UUID from a string.
     *
     * @param input The string input to parse.
     * @return The parsed UUID, or null if the input is not a valid UUID.
     */
    private UUID parseUUID(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Retrieves an offline player by name.
     *
     * @param playerName The name of the player to retrieve.
     * @return The OfflinePlayer object, or null if the player is not found.
     */
    private OfflinePlayer getOfflinePlayerByName(String playerName) {
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(playerName)) {
                return offlinePlayer;
            }
        }
        return null;
    }

    /**
     * Retrieves and displays player information.
     *
     * @param requester     The player requesting the information.
     * @param offlinePlayer The offline player to retrieve information for.
     */
    private void getPlayerInfo(Player requester, OfflinePlayer offlinePlayer) {
        if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
            fetchUser(offlinePlayer.getUniqueId()).thenAccept(user -> {
                if (user != null) {
                    String groupName = capitalizeFirstLetter(getPrimaryGroupName(user));
                    int deathCount = offlinePlayer.getStatistic(Statistic.DEATHS);
                    int mobKillCount = getTotalMobKills(offlinePlayer);
                    int blocksMinedCount = getTotalBlocksMined(offlinePlayer);
                    int blocksWalkedCount = getTotalBlocksWalked(offlinePlayer);
                    int animalsBredCount = offlinePlayer.getStatistic(Statistic.ANIMALS_BRED);
                    int timePlayedMinutes = offlinePlayer.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20 / 60;
                    String firstJoinedFormatted = formatDate(new Date(offlinePlayer.getFirstPlayed()));
                    int playersKilledCount = offlinePlayer.getStatistic(Statistic.PLAYER_KILLS);

                    sendPlayerProfile(requester, offlinePlayer.getName(), groupName, firstJoinedFormatted,
                            timePlayedMinutes, deathCount, mobKillCount, blocksMinedCount, blocksWalkedCount,
                            animalsBredCount, playersKilledCount);
                } else {
                    requester.sendMessage(formatNotice(USER_DATA_NOT_AVAILABLE));
                }
            }).exceptionally(ex -> {
                requester.sendMessage(formatNotice(USER_DATA_NOT_AVAILABLE));
                ex.printStackTrace();
                return null;
            });
        } else {
            requester.sendMessage(formatNotice(PLAYER_NOT_FOUND));
        }
    }

    /**
     * Fetches the LuckPerms user data asynchronously.
     *
     * @param uuid The UUID of the player to fetch.
     * @return A CompletableFuture containing the LuckPerms User object.
     */
    private CompletableFuture<User> fetchUser(UUID uuid) {
        return luckPerms.getUserManager().loadUser(uuid);
    }

    /**
     * Loads player data from a YAML file and sends it to the requester.
     *
     * @param playerUUID The UUID of the player to load data for.
     * @param requester  The player requesting the information.
     */
    private void loadPlayerDataFromYAML(UUID playerUUID, Player requester) {
        File playerDataFile = new File(getDataFolder() + File.separator + "profiles", playerUUID + ".yml");
        logToConsole("Attempting to load data from: " + playerDataFile.getAbsolutePath(), ChatColor.YELLOW);

        if (!playerDataFile.exists()) {
            logToConsole("Player data file not found: " + playerDataFile.getAbsolutePath(), ChatColor.RED);
            requester.sendMessage(formatNotice(PLAYER_DATA_NOT_FOUND));
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

        logToConsole("Loaded data: Name: " + playerName + ", First Joined: " + firstJoined + ", Group: " + groupName, ChatColor.YELLOW);

        sendPlayerProfile(requester, playerName, groupName, firstJoined, timePlayed, deaths, mobKills, blocksMined,
                blocksWalked, animalsBred, playerKills);
    }

    /**
     * Saves player data to a YAML file.
     *
     * @param offlinePlayer The offline player to save data for.
     */
    private void savePlayerDataToYAML(OfflinePlayer offlinePlayer) {
        File playerDataFile = new File(getDataFolder() + File.separator + "profiles", offlinePlayer.getUniqueId() + ".yml");
        logToConsole("Saving data to: " + playerDataFile.getAbsolutePath(), ChatColor.YELLOW);

        FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerDataFile);

        playerData.set("name", offlinePlayer.getName());
        playerData.set("firstJoined", formatDate(new Date(offlinePlayer.getFirstPlayed())));
        playerData.set("timePlayed", offlinePlayer.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20 / 60);
        playerData.set("deaths", offlinePlayer.getStatistic(Statistic.DEATHS));
        playerData.set("mobKills", getTotalMobKills(offlinePlayer));
        playerData.set("blocksMined", getTotalBlocksMined(offlinePlayer));
        playerData.set("blocksWalked", getTotalBlocksWalked(offlinePlayer));
        playerData.set("animalsBred", offlinePlayer.getStatistic(Statistic.ANIMALS_BRED));
        playerData.set("playerKills", offlinePlayer.getStatistic(Statistic.PLAYER_KILLS));

        fetchUser(offlinePlayer.getUniqueId()).thenAccept(user -> {
            if (user != null) {
                String groupName = capitalizeFirstLetter(getPrimaryGroupName(user));
                playerData.set("groupName", groupName);
            } else {
                playerData.set("groupName", "Unknown");
            }

            try {
                playerData.save(playerDataFile);
                logToConsole("Data saved successfully for " + offlinePlayer.getName(), ChatColor.GREEN);
            } catch (IOException e) {
                logToConsole("Could not save data for " + offlinePlayer.getName(), ChatColor.RED);
                e.printStackTrace();
            }
        }).exceptionally(ex -> {
            logToConsole("Could not fetch user data for saving: " + ex.getMessage(), ChatColor.RED);
            ex.printStackTrace();
            return null;
        });
    }

    /**
     * Sends a formatted player profile to the requester.
     *
     * @param player       The player to send the profile to.
     * @param playerName   The name of the player.
     * @param groupName    The group name of the player.
     * @param firstJoined  The date the player first joined.
     * @param timePlayed   The total time played by the player.
     * @param deaths       The total deaths of the player.
     * @param mobKills     The total mob kills by the player.
     * @param blocksMined  The total blocks mined by the player.
     * @param blocksWalked The total blocks walked by the player.
     * @param animalsBred  The total animals bred by the player.
     * @param playerKills  The total player kills by the player.
     */
    private void sendPlayerProfile(Player player, String playerName, String groupName, String firstJoined,
        int timePlayed, int deaths, int mobKills, int blocksMined, int blocksWalked, int animalsBred, int playerKills) {
        String labelColor = getColor("colors.label");
        String valueColor = getColor("colors.value");
        String messageColor = getColor("colors.message");

        player.sendMessage(messageColor + "§lPlayer Profile for " + playerName);
        player.sendMessage(messageColor + "------------------------------");
        if (groupName != null)
        player.sendMessage(labelColor + "§lRank:§r " + valueColor + groupName);
        player.sendMessage(labelColor + "§lFirst Joined:§r " + valueColor + firstJoined);
        player.sendMessage(labelColor + "§lTime Played:§r " + valueColor + timePlayed + " minutes");
        player.sendMessage(labelColor + "§lDeaths:§r " + (deaths == 0 ? labelColor : valueColor) + deaths);
        player.sendMessage(labelColor + "§lMob Kills:§r " + (mobKills == 0 ? labelColor : valueColor) + mobKills);
        player.sendMessage(labelColor + "§lBlocks Mined:§r " + (blocksMined == 0 ? labelColor : valueColor) + blocksMined);
        player.sendMessage(labelColor + "§lBlocks Walked:§r " + (blocksWalked == 0 ? labelColor : valueColor) + blocksWalked);
        player.sendMessage(labelColor + "§lAnimals Bred:§r " + (animalsBred == 0 ? labelColor : valueColor) + animalsBred);
        player.sendMessage(labelColor + "§lPlayers Killed:§r " + (playerKills == 0 ? labelColor : valueColor) + playerKills);
    }


    /**
     * Calculates the total mob kills by the player.
     *
     * @param offlinePlayer The offline player to calculate for.
     * @return The total mob kills.
     */
    private int getTotalMobKills(OfflinePlayer offlinePlayer) {
        return Arrays.stream(EntityType.values())
                .filter(EntityType::isAlive)
                .mapToInt(entityType -> offlinePlayer.getStatistic(Statistic.KILL_ENTITY, entityType))
                .sum();
    }

    /**
     * Calculates the total blocks mined by the player.
     *
     * @param offlinePlayer The offline player to calculate for.
     * @return The total blocks mined.
     */
    private int getTotalBlocksMined(OfflinePlayer offlinePlayer) {
        return Arrays.stream(Material.values())
                .filter(Material::isBlock)
                .mapToInt(material -> offlinePlayer.getStatistic(Statistic.MINE_BLOCK, material))
                .sum();
    }

    /**
     * Calculates the total blocks walked by the player.
     *
     * @param offlinePlayer The offline player to calculate for.
     * @return The total blocks walked.
     */
    private int getTotalBlocksWalked(OfflinePlayer offlinePlayer) {
        return offlinePlayer.getStatistic(Statistic.WALK_ONE_CM) / 100;
    }

    /**
     * Formats a date to a readable string.
     *
     * @param date The date to format.
     * @return The formatted date string.
     */
    private String formatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
        return dateFormat.format(date);
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param input The input string.
     * @return The string with the first letter capitalized.
     */
    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    /**
     * Retrieves the primary group name of a LuckPerms user.
     *
     * @param user The LuckPerms user.
     * @return The primary group name.
     */
    private String getPrimaryGroupName(User user) {
        return user.getPrimaryGroup();
    }

    /**
     * Formats a notice message with a red color.
     *
     * @param message The message to format.
     * @return The formatted message.
     */
    private String formatNotice(String message) {
        String messageColor = getColor("colors.message");
        return messageColor + message;
    }

    /**
     * Logs a message to the console with a specific color.
     *
     * @param message The message to log.
     * @param color   The color to use.
     */
    private void logToConsole(String message, ChatColor color) {
        Bukkit.getConsoleSender().sendMessage(color + message);
    }

    /**
     * Retrieves a color from the configuration file.
     *
     * @param configPath The configuration path to the color.
     * @return The ChatColor corresponding to the color in the configuration file,
     *         or white ("#FFFFFF") if the path is not found.
     */
    private String getColor(String configPath) {
        return ChatColor.of(getConfig().getString(configPath, "#FFFFFF")) + "";
    }
}