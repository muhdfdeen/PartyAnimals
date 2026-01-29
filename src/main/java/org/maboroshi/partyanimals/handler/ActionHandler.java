package org.maboroshi.partyanimals.handler;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.objects.CommandAction;

public class ActionHandler {
    private final boolean hasPAPI;

    public ActionHandler(PartyAnimals plugin) {
        this.hasPAPI = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public void process(OfflinePlayer player, Collection<CommandAction> commands) {
        process(player, commands, str -> str);
    }

    public void process(
            OfflinePlayer player, Collection<CommandAction> commands, Function<String, String> commandParser) {
        if (commands == null || commands.isEmpty()) return;

        for (CommandAction action : commands) {
            if (!action.global && action.permission != null && !action.permission.isEmpty()) {
                Player onlinePlayer = player.getPlayer();
                if (onlinePlayer != null && !onlinePlayer.hasPermission(action.permission)) {
                    continue;
                }
            }

            if (action.chance < 100.0) {
                double roll = ThreadLocalRandom.current().nextDouble(100.0);
                if (roll > action.chance) continue;
            }

            if (action.global) {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    executeAction(onlinePlayer, action, commandParser);
                }
            } else {
                executeAction(player, action, commandParser);
            }

            if (action.stopProcessing) {
                break;
            }
        }
    }

    private void executeAction(OfflinePlayer target, CommandAction action, Function<String, String> commandParser) {
        if (action.commands.isEmpty()) return;
        if (action.pickOneRandom) {
            int index = ThreadLocalRandom.current().nextInt(action.commands.size());
            String randomCmd = action.commands.get(index);
            dispatch(target, commandParser.apply(randomCmd));
        } else {
            for (String cmd : action.commands) {
                dispatch(target, commandParser.apply(cmd));
            }
        }
    }

    private void dispatch(OfflinePlayer player, String command) {
        if (command == null || command.isEmpty()) return;

        String parsed = command;
        if (player != null) {
            String name = player.getName();
            parsed = parsed.replace("<player>", name != null ? name : "Unknown")
                    .replace("<uuid>", player.getUniqueId().toString());
        }

        if (player != null && hasPAPI) {
            parsed = PlaceholderAPI.setPlaceholders(player, parsed);
        }

        if (parsed.startsWith("/")) {
            parsed = parsed.substring(1);
        }

        final String finalCommand = parsed;
        Bukkit.getGlobalRegionScheduler()
                .execute(
                        PartyAnimals.getPlugin(),
                        () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
    }
}
