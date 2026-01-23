package org.maboroshi.partyanimals.config.objects;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import java.util.List;

@Configuration
public class CommandAction {

    @Comment("The chance for this action to execute (0.0 to 100.0).")
    public double chance = 100.0;

    @Comment("If true, the command runs for every player on the server.")
    public boolean global = false;

    @Comment("If true, no further actions in this list will be processed if this one triggers.")
    public boolean stopProcessing = false;

    @Comment("If true, commands in the list are shuffled before execution.")
    public boolean pickOneRandom = false;

    @Comment("Permission node required to trigger this action.")
    public String permission = "";

    @Comment("List of console commands to execute. Use <player> for the player name.")
    public List<String> commands = List.of();

    public CommandAction() {}

    public CommandAction(double chance, List<String> commands) {
        this.chance = chance;
        this.commands = commands;
    }

    public CommandAction(
            double chance,
            boolean global,
            boolean stopProcessing,
            boolean pickOneRandom,
            String permission,
            List<String> commands) {
        this.chance = chance;
        this.global = global;
        this.stopProcessing = stopProcessing;
        this.pickOneRandom = pickOneRandom;
        this.permission = permission;
        this.commands = commands;
    }
}
