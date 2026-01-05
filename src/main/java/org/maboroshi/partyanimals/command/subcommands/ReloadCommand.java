package org.maboroshi.partyanimals.command.subcommands;

import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.handler.MessageHandler;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public class ReloadCommand {
    private final PartyAnimals plugin;
    private final ConfigManager config;
    private final MessageHandler messageHandler;

    public ReloadCommand(PartyAnimals plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.messageHandler = plugin.getMessageHandler();
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("reload")
                .requires(sender -> sender.getSender().hasPermission("partyanimals.reload"))
                .executes(ctx -> {
                    var sender = ctx.getSource().getSender();
                    if (plugin.reload()) {
                        plugin.getPluginLogger().info("Configuration reloaded by " + sender.getName());
                        messageHandler.send(sender, config.getMessageConfig().general.reloadSuccess);
                    } else {
                        plugin.getPluginLogger().warn("Failed to reload configuration by " + sender.getName());
                        messageHandler.send(sender, config.getMessageConfig().general.reloadFail);
                    }
                    return Command.SINGLE_SUCCESS;
                });
    }
}
