package com.muhdfdeen.partyanimals.command;

import org.bukkit.command.CommandSender;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.config.Config.MainConfiguration; 

public class PartyAnimalsCommand {
    private final PartyAnimals plugin;

    public PartyAnimalsCommand(PartyAnimals plugin) {
        this.plugin = plugin;
    }

    public LiteralCommandNode<CommandSourceStack> createCommand(final String commandName) {
        MainConfiguration config = plugin.getConfiguration();
        return Commands.literal(commandName)
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                sender.sendRichMessage(config.messages.prefix() + "Plugin version: " + plugin.getPluginMeta().getVersion());
                sender.sendRichMessage("<green>🛈</green> <gray>Type <white>/partyanimals reload</white> to reload the configuration.</gray>");
                return Command.SINGLE_SUCCESS;
            })
            .then(Commands.literal("reload")
                .requires(sender -> sender.getSender().hasPermission("partyanimals.reload"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (plugin.reload()) {
                        plugin.getPluginLogger().info("Configuration reloaded by " + sender.getName());
                        sender.sendRichMessage(config.messages.prefix() + config.messages.reloadSuccess());
                    } else {
                        plugin.getPluginLogger().warn("Failed to reload configuration by " + sender.getName());
                        sender.sendRichMessage(config.messages.prefix() + config.messages.reloadFail());
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
            .build();
    }
}
