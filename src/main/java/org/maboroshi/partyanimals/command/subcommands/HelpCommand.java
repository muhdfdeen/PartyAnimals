package org.maboroshi.partyanimals.command.subcommands;

import org.bukkit.command.CommandSender;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.handler.MessageHandler;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public class HelpCommand {
    private final PartyAnimals plugin;
    private final MessageHandler messageHandler;

    public HelpCommand(PartyAnimals plugin) {
        this.plugin = plugin;
        this.messageHandler = plugin.getMessageHandler();
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("help")
                .executes(this::executeHelp);
    }

    private int executeHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        messageHandler.send(sender, plugin.getConfiguration().getMessageConfig().help.header);
        
        sendHelpLine(sender, "pa reload", "Reload configuration", "partyanimals.reload");
        
        sendHelpLine(sender, "pa pinata start <pinata> <loc>", "Start pinata countdown", "partyanimals.start");
        sendHelpLine(sender, "pa pinata spawn <pinata> <loc>", "Spawn pinata immediately", "partyanimals.spawn");
        sendHelpLine(sender, "pa pinata killall", "Remove all active pinatas", "partyanimals.killall");
        
        sendHelpLine(sender, "pa pinata spawnpoint add <name>", "Save current location", "partyanimals.addlocation");
        sendHelpLine(sender, "pa pinata spawnpoint remove <name>", "Remove saved location", "partyanimals.removelocation");
        
        sendHelpLine(sender, "pa vote check <player>", "Check player votes", "partyanimals.votes");
        sendHelpLine(sender, "pa vote <add|remove|set>", "Modify vote data", "partyanimals.votes.admin");

        return Command.SINGLE_SUCCESS;
    }

    private void sendHelpLine(CommandSender sender, String command, String description, String permission) {
        if (permission != null && !sender.hasPermission(permission)) {
            return;
        }
        
        String format = plugin.getConfiguration().getMessageConfig().help.entry;
        
        messageHandler.send(sender, format, 
            messageHandler.tag("command", command),
            messageHandler.tag("description", description)
        );
    }
}
