package com.muhdfdeen.partyanimals.util;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.muhdfdeen.partyanimals.config.MainConfig;

import me.clip.placeholderapi.PlaceholderAPI;

public class CommandUtils {

    public static void process(Player player, Map<String, MainConfig.Reward> rewards, Plugin plugin) {
        if (rewards == null || rewards.isEmpty())
            return;

        Map<String, MainConfig.Reward> sortedRewards = new TreeMap<>(rewards);

        for (MainConfig.Reward reward : sortedRewards.values()) {
            if (reward.permission != null && !reward.permission.isEmpty()) {
                if (player != null && !player.hasPermission(reward.permission))
                    continue;
            }

            double roll = ThreadLocalRandom.current().nextDouble(100.0);
            if (roll > reward.chance)
                continue;

            Player contextPlayer = (reward.serverwide != null && reward.serverwide) ? null : player;

            if (reward.randomize != null && reward.randomize && !reward.commands.isEmpty()) {
                String randomCmd = reward.commands.get(ThreadLocalRandom.current().nextInt(reward.commands.size()));
                dispatch(contextPlayer, randomCmd, plugin);
            } else {
                for (String cmd : reward.commands) {
                    dispatch(contextPlayer, cmd, plugin);
                }
            }

            if (reward.skipRest != null && reward.skipRest) {
                break;
            }
        }
    }

    private static void dispatch(Player player, String command, Plugin plugin) {
        if (command == null || command.isEmpty())
            return;

        String parsed = command;

        if (player != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            parsed = PlaceholderAPI.setPlaceholders(player, parsed);
        }

        if (player != null) {
            parsed = parsed
                    .replace("{player}", player.getName())
                    .replace("{uuid}", player.getUniqueId().toString());
        }

        if (parsed.startsWith("/"))
            parsed = parsed.substring(1);

        final String finalCommand = parsed;

        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
    }
}
