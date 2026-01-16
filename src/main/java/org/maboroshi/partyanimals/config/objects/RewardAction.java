package org.maboroshi.partyanimals.config.objects;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import java.util.List;

@Configuration
public class RewardAction {

    @Comment("The chance for this reward to trigger.")
    public double chance = 100.0;

    @Comment("If true, the command runs for every player on the server.")
    public boolean global = false;

    @Comment("If true, no further rewards in this list will be processed if this one triggers.")
    public boolean preventFurtherRewards = false;

    @Comment("If true, commands in the list are shuffled before execution.")
    public boolean pickOneRandom = false;

    @Comment("Permission node required to be eligible for this reward.")
    public String permission = "";

    @Comment("List of console commands to execute. Use <player> for the player name.")
    public List<String> commands = List.of();

    public RewardAction() {}

    public RewardAction(double chance, List<String> commands) {
        this.chance = chance;
        this.commands = commands;
    }

    public RewardAction(
            double chance,
            boolean global,
            boolean preventFurtherRewards,
            boolean pickOneRandom,
            String permission,
            List<String> commands) {
        this.chance = chance;
        this.global = global;
        this.preventFurtherRewards = preventFurtherRewards;
        this.pickOneRandom = pickOneRandom;
        this.permission = permission;
        this.commands = commands;
    }
}
