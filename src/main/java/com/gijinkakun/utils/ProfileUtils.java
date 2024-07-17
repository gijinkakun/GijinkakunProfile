package com.gijinkakun.utils;

import net.luckperms.api.model.user.User;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import net.md_5.bungee.api.ChatColor;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class ProfileUtils {

    public static int getTotalBlocksWalked(OfflinePlayer offlinePlayer) {
        return offlinePlayer.getStatistic(Statistic.WALK_ONE_CM) / 100;
    }

    public static int getTotalBlocksMined(OfflinePlayer offlinePlayer) {
        return Arrays.stream(Material.values())
                .filter(Material::isBlock)
                .mapToInt(material -> offlinePlayer.getStatistic(Statistic.MINE_BLOCK, material))
                .sum();
    }

    public static int getTotalMobKills(OfflinePlayer offlinePlayer) {
        return Arrays.stream(EntityType.values())
                .filter(EntityType::isAlive)
                .mapToInt(entityType -> offlinePlayer.getStatistic(Statistic.KILL_ENTITY, entityType))
                .sum();
    }

    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public static String getPrimaryGroupName(User user) {
        return user.getPrimaryGroup();
    }

    public static String formatNotice(String message) {
        return ChatColor.RED + message;
    }

    public static String formatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
        return dateFormat.format(date);
    }
}
