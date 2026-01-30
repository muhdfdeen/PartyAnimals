package org.maboroshi.partyanimals.hook;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.util.MessageUtils;

public class VotifierHook {
    public static void sendVote(
            CommandSender sender, String targetName, String serviceName, MessageUtils messageUtils) {
        ConfigManager config = PartyAnimals.getPlugin().getConfiguration();
        Vote vote = new Vote(serviceName, targetName, "127.0.0.1", String.valueOf(System.currentTimeMillis()));
        VotifierEvent event = new VotifierEvent(vote);

        Bukkit.getPluginManager().callEvent(event);

        messageUtils.send(
                sender,
                config.getMessageConfig().commands.voteTriggered,
                messageUtils.tag("<player>", targetName),
                messageUtils.tag("<service>", serviceName));
    }
}
