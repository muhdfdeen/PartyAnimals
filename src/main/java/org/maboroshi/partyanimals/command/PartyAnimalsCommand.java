package org.maboroshi.partyanimals.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.command.subcommands.HelpCommand;
import org.maboroshi.partyanimals.command.subcommands.PinataCommand;
import org.maboroshi.partyanimals.command.subcommands.ReloadCommand;
import org.maboroshi.partyanimals.command.subcommands.VoteCommand;
import org.maboroshi.partyanimals.util.MessageUtils;

public class PartyAnimalsCommand {
    private final PartyAnimals plugin;
    private final MessageUtils messageUtils;

    public PartyAnimalsCommand(PartyAnimals plugin) {
        this.plugin = plugin;
        this.messageUtils = plugin.getMessageUtils();
    }

    public LiteralCommandNode<CommandSourceStack> createCommand(final String commandName) {
        var root = Commands.literal(commandName).executes(ctx -> {
            var sender = ctx.getSource().getSender();
            messageUtils.send(
                    sender,
                    "<prefix> <gray>Plugin version: <green>"
                            + plugin.getPluginMeta().getVersion());
            messageUtils.send(
                    sender, "<green>🛈</green> <gray>Type <white>/pa help</white> for a list of commands.</gray>");
            return Command.SINGLE_SUCCESS;
        });

        root.then(new HelpCommand(plugin).build());
        root.then(new ReloadCommand(plugin).build());
        root.then(new PinataCommand(plugin).build());
        root.then(new VoteCommand(plugin).build());

        return root.build();
    }
}
