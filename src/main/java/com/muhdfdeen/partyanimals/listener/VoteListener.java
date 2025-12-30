package com.muhdfdeen.partyanimals.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.config.ConfigManager;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

public class VoteListener implements Listener {
    private final PartyAnimals plugin;
    private final ConfigManager config;

    public VoteListener(PartyAnimals plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
    }

    @EventHandler
    public void onVoteEvent(VotifierEvent event) {
        if (!config.getMainConfig().modules.vote().enabled()) return;
        Vote vote = event.getVote();
        String serviceName = vote.getServiceName();
        String playerName = vote.getUsername();
        String address = vote.getAddress();
        String timeStamp = vote.getTimeStamp();
    }
}
