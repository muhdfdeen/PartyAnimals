package org.maboroshi.partyanimals.task;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.MainConfig.VoteReminderSettings;

public class VoteReminder implements Runnable {
    private final PartyAnimals plugin;

    public VoteReminder(PartyAnimals plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        VoteReminderSettings settings = plugin.getConfiguration().getMainConfig().modules.vote.reminder;

        if (!settings.enabled) return;

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        Bukkit.getAsyncScheduler().runNow(plugin, (task) -> {
            long timeThreshold = System.currentTimeMillis() - (24 * 60 * 60 * 1000L);

            for (Player player : onlinePlayers) {
                int recentVotes = plugin.getDatabaseManager().getVotesSince(player.getUniqueId(), timeThreshold);

                if (recentVotes <= 0) {
                    player.getScheduler()
                            .run(
                                    plugin,
                                    (st) -> {
                                        if (player.isOnline()) {
                                            plugin.getEffectHandler()
                                                    .playEffects(settings.effects, player.getLocation(), false);
                                            plugin.getActionHandler().process(player, settings.actions.values());
                                        }
                                    },
                                    null);
                }
            }
        });
    }
}
