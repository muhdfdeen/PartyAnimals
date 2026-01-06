package org.maboroshi.partyanimals.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.util.MessageUtils;

public class HelpCommand {
    private final PartyAnimals plugin;
    private final MessageUtils messageUtils;

    public HelpCommand(PartyAnimals plugin) {
        this.plugin = plugin;
        this.messageUtils = plugin.getMessageUtils();
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("help").executes(this::executeHelp);
    }

    private int executeHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        messageUtils.send(sender, plugin.getConfiguration().getMessageConfig().help.header);

        sendHelpLine(sender, "pa reload", "Reload configuration", "partyanimals.reload");

        sendHelpLine(sender, "pa pinata start <pinata> <loc>", "Start pinata countdown", "partyanimals.pinata.start");
        sendHelpLine(sender, "pa pinata spawn <pinata> <loc>", "Spawn pinata immediately", "partyanimals.pinata.spawn");
        sendHelpLine(sender, "pa pinata killall", "Remove all active pinatas", "partyanimals.pinata.killall");

        sendHelpLine(
                sender,
                "pa pinata spawnpoint add <name>",
                "Save current location",
                "partyanimals.pinata.spawnpoint.add");
        sendHelpLine(
                sender,
                "pa pinata spawnpoint remove <name>",
                "Remove saved location",
                "partyanimals.pinata.spawnpoint.remove");

        sendHelpLine(sender, "pa vote check <player>", "Check player votes", "partyanimals.vote.check");
        sendHelpLine(sender, "pa vote <add|remove|set>", "Modify vote data", "partyanimals.vote.add/remove/set");
        sendHelpLine(sender, "pa vote send", "Send a legitimate vote", "partyanimals.vote.send");
        sendHelpLine(sender, "pa vote test", "Send a fake vote", "partyanimals.vote.test");
        sendHelpLine(
                sender, "pa vote migrate <plugin>", "Migrate data from another plugin", "partyanimals.vote.migrate");

        return Command.SINGLE_SUCCESS;
    }

    private void sendHelpLine(CommandSender sender, String command, String description, String permission) {
        if (permission != null && !sender.hasPermission(permission)) {
            return;
        }

        String format = plugin.getConfiguration().getMessageConfig().help.entry;

        messageUtils.send(
                sender, format, messageUtils.tag("command", command), messageUtils.tag("description", description));
    }
}
