package org.maboroshi.partyanimals.util;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class VoteTester {

    public static void triggerTestVote(
            CommandSender sender, String targetName, String serviceName, MessageUtils messageUtils) {
        Vote vote = new Vote(serviceName, targetName, "127.0.0.1", String.valueOf(System.currentTimeMillis()));
        VotifierEvent event = new VotifierEvent(vote);
        Bukkit.getPluginManager().callEvent(event);
        messageUtils.send(
                sender,
                "<prefix> <green>Triggered vote event for <white>"
                        + targetName
                        + "</white> via <white>"
                        + serviceName
                        + "</white>.");
    }
}
