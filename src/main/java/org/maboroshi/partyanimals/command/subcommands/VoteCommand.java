package org.maboroshi.partyanimals.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.handler.MessageHandler;
import org.maboroshi.partyanimals.util.VoteTester;

public class VoteCommand {
    private final PartyAnimals plugin;
    private final MessageHandler messageHandler;

    public VoteCommand(PartyAnimals plugin) {
        this.plugin = plugin;
        this.messageHandler = plugin.getMessageHandler();
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        var voteNode = Commands.literal("vote")
                .requires(s -> s.getSender().hasPermission("partyanimals.vote"))
                .then(Commands.literal("check")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(this::suggestPlayers)
                                .executes(this::handleCheck)))
                .then(Commands.literal("add")
                        .requires(s -> s.getSender().hasPermission("partyanimals.votes.admin"))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(this::suggestPlayers)
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> handleModify(ctx, "add")))))
                .then(Commands.literal("remove")
                        .requires(s -> s.getSender().hasPermission("partyanimals.votes.admin"))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(this::suggestPlayers)
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> handleModify(ctx, "remove")))))
                .then(Commands.literal("set")
                        .requires(s -> s.getSender().hasPermission("partyanimals.votes.admin"))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(this::suggestPlayers)
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> handleModify(ctx, "set")))));

        if (Bukkit.getPluginManager().isPluginEnabled("Votifier")) {

            voteNode.then(Commands.literal("test")
                    .requires(s -> s.getSender().hasPermission("partyanimals.votes.admin"))
                    .then(Commands.argument("player", StringArgumentType.word())
                            .suggests(this::suggestPlayers)
                            .executes(ctx -> handleTestSafe(ctx, "TestVote (Dry Run)"))));

            voteNode.then(Commands.literal("send")
                    .requires(s -> s.getSender().hasPermission("partyanimals.votes.admin"))
                    .then(Commands.argument("player", StringArgumentType.word())
                            .suggests(this::suggestPlayers)
                            .executes(ctx -> handleTestSafe(ctx, "TestVote"))));
        }

        return voteNode;
    }

    private CompletableFuture<Suggestions> suggestPlayers(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        Bukkit.getOnlinePlayers().forEach(player -> builder.suggest(player.getName()));
        return builder.buildFuture();
    }

    private int handleCheck(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String targetName = StringArgumentType.getString(ctx, "player");

        Bukkit.getAsyncScheduler().runNow(plugin, (task) -> {
            UUID uuid = plugin.getDatabaseManager().getPlayerUUID(targetName);
            int votes = plugin.getDatabaseManager().getVotes(uuid);
            messageHandler.send(
                    sender, "<prefix> <white>" + targetName + "</white> has <aqua>" + votes + "</aqua> votes.");
        });

        return Command.SINGLE_SUCCESS;
    }

    private int handleModify(CommandContext<CommandSourceStack> ctx, String action) {
        CommandSender sender = ctx.getSource().getSender();
        String targetName = StringArgumentType.getString(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID uuid = plugin.getDatabaseManager().getPlayerUUID(targetName);
            int currentVotes = plugin.getDatabaseManager().getVotes(uuid);
            int finalChange = 0;

            if (action.equals("add")) {
                finalChange = amount;
            } else if (action.equals("remove")) {
                finalChange = -amount;
            } else if (action.equals("set")) {
                finalChange = amount - currentVotes;
            }

            if (finalChange == 0) {
                messageHandler.send(
                        sender,
                        "<prefix> <yellow>No changes made. " + targetName + " already has " + currentVotes + " votes.");
                return;
            }

            plugin.getDatabaseManager().addVote(uuid, targetName, "Admin", finalChange);
            int newTotal = currentVotes + finalChange;
            messageHandler.send(sender, "<prefix> <green>Updated votes for <white>" + targetName + "</white>.");
            messageHandler.send(
                    sender,
                    "<prefix> <gray>Old: <yellow>" + currentVotes + "</yellow> -> New: <aqua>" + newTotal + "</aqua>");
        });

        return Command.SINGLE_SUCCESS;
    }

    private int handleTestSafe(CommandContext<CommandSourceStack> ctx, String serviceName) {
        String targetName = StringArgumentType.getString(ctx, "player");
        VoteTester.triggerTestVote(ctx.getSource().getSender(), targetName, serviceName, messageHandler);
        return Command.SINGLE_SUCCESS;
    }
}
