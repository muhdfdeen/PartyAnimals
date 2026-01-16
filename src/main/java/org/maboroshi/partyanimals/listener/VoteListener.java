package org.maboroshi.partyanimals.listener;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.config.objects.RewardAction;
import org.maboroshi.partyanimals.config.settings.MainConfig.VoteEvent;
import org.maboroshi.partyanimals.handler.EffectHandler;
import org.maboroshi.partyanimals.handler.RewardHandler;
import org.maboroshi.partyanimals.manager.DatabaseManager;
import org.maboroshi.partyanimals.util.Logger;

public class VoteListener implements Listener {
    private final PartyAnimals plugin;
    private final ConfigManager config;
    private final Logger log;
    private final EffectHandler effectHandler;
    private final RewardHandler rewardHandler;
    private final DatabaseManager databaseManager;

    public VoteListener(PartyAnimals plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.log = plugin.getPluginLogger();
        this.effectHandler = plugin.getEffectHandler();
        this.rewardHandler = plugin.getRewardHandler();
        this.databaseManager = plugin.getDatabaseManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!config.getMainConfig().modules.vote.offline.enabled
                || !config.getMainConfig().modules.vote.offline.queueRewards) {
            return;
        }

        Player player = event.getPlayer();

        Bukkit.getAsyncScheduler().runNow(plugin, (task) -> {
            UUID uuid = databaseManager.getPlayerUUID(player.getName());

            List<String> commands = databaseManager.retrieveRewards(uuid);

            if (!commands.isEmpty()) {
                player.getScheduler()
                        .run(
                                plugin,
                                (scheduledTask) -> {
                                    log.info("Delivering "
                                            + commands.size()
                                            + " offline rewards to "
                                            + player.getName());
                                    for (String cmd : commands) {
                                        String finalCmd = cmd.replace("<player>", player.getName())
                                                .replace(
                                                        "<uuid>",
                                                        player.getUniqueId().toString());
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                                    }
                                },
                                null);
            }
        });
    }

    @EventHandler
    public void onVoteEvent(VotifierEvent event) {
        if (!config.getMainConfig().modules.vote.enabled) return;

        Vote vote = event.getVote();
        String serviceName = vote.getServiceName();
        String playerName = vote.getUsername();
        String address = vote.getAddress();
        String timeStamp = vote.getTimeStamp();

        Bukkit.getAsyncScheduler().runNow(plugin, (task) -> {
            log.debug("Received vote from "
                    + playerName
                    + " via "
                    + serviceName
                    + " at "
                    + timeStamp
                    + " (IP: "
                    + address
                    + ")");
            UUID uuid = databaseManager.getPlayerUUID(playerName);

            if (!serviceName.equals("TestVote (Dry Run)")) {
                databaseManager.addVote(uuid, playerName, serviceName, 1);
                var goalConfig = config.getMainConfig().modules.vote.communityGoal;

                if (goalConfig.enabled && goalConfig.votesRequired > 0) {
                    int currentTotal = databaseManager.incrementCommunityGoalProgress();
                    int required = goalConfig.votesRequired;

                    if (currentTotal % required == 0) {
                        log.info("Community Goal reached (Total Votes: " + currentTotal + ")! Firing rewards...");

                        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
                            rewardHandler.process(null, goalConfig.rewards.values());
                        });
                    }
                }
            }

            final UUID finalUUID = uuid;

            Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
                Player player = Bukkit.getPlayer(playerName);

                if (player != null) {
                    player.getScheduler()
                            .run(
                                    plugin,
                                    (st) -> {
                                        VoteEvent voteEvent = config.getMainConfig().modules.vote.events.playerVote;
                                        if (!voteEvent.enabled) return;

                                        effectHandler.playEffects(voteEvent.effects, player.getLocation(), false);
                                        rewardHandler.process(player, voteEvent.rewards.values());
                                    },
                                    null);
                } else {
                    var offlineSettings = config.getMainConfig().modules.vote.offline;
                    if (offlineSettings.enabled) {
                        VoteEvent voteEvent = config.getMainConfig().modules.vote.events.playerVote;

                        if (offlineSettings.queueRewards) {
                            Bukkit.getAsyncScheduler().runNow(plugin, (at) -> {
                                for (var action : voteEvent.rewards.values()) {
                                    if (shouldRun(action)) {
                                        processActionForQueue(finalUUID, playerName, action);
                                        if (action.preventFurtherRewards) break;
                                    }
                                }
                            });
                        } else {
                            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(finalUUID);
                            rewardHandler.process(offlinePlayer, voteEvent.rewards.values());
                            log.debug("Processed immediate offline rewards for " + playerName);
                        }
                    }
                }
            });
        });
    }

    private boolean shouldRun(RewardAction action) {
        if (action.chance >= 100.0) return true;
        return ThreadLocalRandom.current().nextDouble(100.0) <= action.chance;
    }

    private void processActionForQueue(UUID uuid, String playerName, RewardAction action) {
        if (action.commands.isEmpty()) return;

        if (action.pickOneRandom) {
            int index = ThreadLocalRandom.current().nextInt(action.commands.size());
            String cmd = action.commands.get(index);
            databaseManager.queueRewards(uuid, cmd);
        } else {
            for (String cmd : action.commands) {
                databaseManager.queueRewards(uuid, cmd);
            }
        }
        log.debug("Queued offline rewards for " + playerName);
    }
}
