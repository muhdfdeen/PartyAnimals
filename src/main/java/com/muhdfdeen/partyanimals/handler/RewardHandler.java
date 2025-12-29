package com.muhdfdeen.partyanimals.handler;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.config.objects.RewardAction;

import me.clip.placeholderapi.PlaceholderAPI;

public class RewardHandler {

    private final PartyAnimals plugin;
    private final boolean hasPAPI;

    public RewardHandler(PartyAnimals plugin) {
        this.plugin = plugin;
        this.hasPAPI = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public void process(Player player, Collection<RewardAction> commands) {
        if (commands == null || commands.isEmpty()) return;
        for (RewardAction action : commands) {
            if (!action.global && action.permission != null && !action.permission.isEmpty()) {
                if (player != null && !player.hasPermission(action.permission)) {
                    continue;
                }
            }
            if (action.chance < 100.0) { 
                double roll = ThreadLocalRandom.current().nextDouble(100.0);
                if (roll > action.chance) continue;
            }
            if (action.global) {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    executeAction(onlinePlayer, action);
                }
            } else {
                executeAction(player, action);
            }
            if (action.preventFurtherRewards) {
                break;
            }
        }
    }

    private void executeAction(Player target, RewardAction action) {
        if (action.commands.isEmpty()) return;
        if (action.pickOneRandom) {
            int index = ThreadLocalRandom.current().nextInt(action.commands.size());
            String randomCmd = action.commands.get(index);
            dispatch(target, randomCmd);
        } else {
            for (String cmd : action.commands) {
                dispatch(target, cmd);
            }
        }
    }

    private void dispatch(Player player, String command) {
        if (command == null || command.isEmpty()) return;
        String parsed = command;
        if (player != null) {
            parsed = parsed.replace("{player}", player.getName()).replace("{uuid}", player.getUniqueId().toString());
        }
        if (player != null && hasPAPI) {
            parsed = PlaceholderAPI.setPlaceholders(player, parsed);
        }
        if (parsed.startsWith("/")) {
            parsed = parsed.substring(1);
        }
        final String finalCommand = parsed;
        Bukkit.getScheduler().runTask(plugin, () -> 
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand)
        );
    }
}
