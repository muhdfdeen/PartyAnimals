package org.maboroshi.partyanimals.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.util.MessageUtils;

public class ReloadCommand {
    private final PartyAnimals plugin;
    private final ConfigManager config;
    private final MessageUtils messageUtils;

    public ReloadCommand(PartyAnimals plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.messageUtils = plugin.getMessageUtils();
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("reload")
                .requires(sender -> sender.getSender().hasPermission("partyanimals.reload"))
                .executes(ctx -> {
                    var sender = ctx.getSource().getSender();
                    if (plugin.reload()) {
                        plugin.getPluginLogger().info("Configuration reloaded by " + sender.getName());
                        messageUtils.send(sender, config.getMessageConfig().general.reloadSuccess);
                    } else {
                        plugin.getPluginLogger().warn("Failed to reload configuration by " + sender.getName());
                        messageUtils.send(sender, config.getMessageConfig().general.reloadFail);
                    }
                    return Command.SINGLE_SUCCESS;
                });
    }
}
