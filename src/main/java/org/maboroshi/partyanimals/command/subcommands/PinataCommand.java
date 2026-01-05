package org.maboroshi.partyanimals.command.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.config.objects.SerializableLocation;
import org.maboroshi.partyanimals.handler.MessageHandler;

public class PinataCommand {
    private final PartyAnimals plugin;
    private final ConfigManager config;
    private final MessageHandler messageHandler;

    public PinataCommand(PartyAnimals plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.messageHandler = plugin.getMessageHandler();
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("pinata")
                .requires(s -> s.getSender().hasPermission("partyanimals.pinata"))
                .then(buildStart())
                .then(buildSpawn())
                .then(buildKillAll())
                .then(buildSpawnPoint());
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildStart() {
        return Commands.literal("start")
                .requires(s -> s.getSender().hasPermission("partyanimals.start"))
                .executes(ctx -> handleStart(ctx.getSource(), null, "default"))
                .then(Commands.argument("pinata", StringArgumentType.word())
                        .suggests(this::suggestPinataTemplates)
                        .executes(
                                ctx -> handleStart(ctx.getSource(), null, StringArgumentType.getString(ctx, "pinata")))
                        .then(Commands.argument("location", StringArgumentType.word())
                                .suggests(this::suggestCentralLocations)
                                .executes(ctx -> handleStart(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "location"),
                                        StringArgumentType.getString(ctx, "pinata")))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildSpawn() {
        return Commands.literal("spawn")
                .requires(s -> s.getSender().hasPermission("partyanimals.spawn"))
                .executes(ctx -> handleSpawn(ctx.getSource(), null, "default"))
                .then(Commands.argument("pinata", StringArgumentType.word())
                        .suggests(this::suggestPinataTemplates)
                        .executes(
                                ctx -> handleSpawn(ctx.getSource(), null, StringArgumentType.getString(ctx, "pinata")))
                        .then(Commands.argument("location", StringArgumentType.word())
                                .suggests(this::suggestCentralLocations)
                                .executes(ctx -> handleSpawn(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "location"),
                                        StringArgumentType.getString(ctx, "pinata")))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildKillAll() {
        return Commands.literal("killall")
                .requires(s -> s.getSender().hasPermission("partyanimals.killall"))
                .executes(ctx -> {
                    plugin.getPinataManager().cleanup();
                    messageHandler.send(ctx.getSource().getSender(), "<prefix> <green>Killed all active pinatas.");
                    return Command.SINGLE_SUCCESS;
                });
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildSpawnPoint() {
        return Commands.literal("spawnpoint")
                .then(Commands.literal("add")
                        .requires(s -> s.getSender().hasPermission("partyanimals.addlocation"))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(this::handleAddSpawnPoint)))
                .then(Commands.literal("remove")
                        .requires(s -> s.getSender().hasPermission("partyanimals.removelocation"))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(this::suggestCentralLocations)
                                .executes(this::handleRemoveSpawnPoint)));
    }

    private int handleStart(CommandSourceStack source, String locationName, String templateId) {
        Location location = resolveLocation(source, locationName, templateId, "start");
        if (location == null) return Command.SINGLE_SUCCESS;
        plugin.getPinataManager().startCountdown(location, templateId);
        messageHandler.send(source.getSender(), config.getMessageConfig().pinata.events.countdownStarted);
        return Command.SINGLE_SUCCESS;
    }

    private int handleSpawn(CommandSourceStack source, String locationName, String templateId) {
        Location location = resolveLocation(source, locationName, templateId, "spawn");
        if (location == null) return Command.SINGLE_SUCCESS;
        plugin.getPinataManager().spawnPinata(location, templateId);
        messageHandler.send(source.getSender(), config.getMessageConfig().pinata.events.spawned);
        return Command.SINGLE_SUCCESS;
    }

    private int handleAddSpawnPoint(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getSender() instanceof Player player)) {
            messageHandler.send(source.getSender(), config.getMessageConfig().commands.playerOnly);
            return Command.SINGLE_SUCCESS;
        }

        String locationName = StringArgumentType.getString(ctx, "name");
        SerializableLocation spawnLocation = new SerializableLocation(player.getLocation());

        config.getMainConfig().modules.pinata.spawnPoints.put(locationName, spawnLocation);
        config.saveConfig();

        messageHandler.send(
                player,
                config.getMessageConfig().pinata.admin.spawnPointAdded,
                messageHandler.tag("location", locationName));
        return Command.SINGLE_SUCCESS;
    }

    private int handleRemoveSpawnPoint(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String locationName = StringArgumentType.getString(ctx, "name");

        SerializableLocation removed =
                config.getMainConfig().modules.pinata.spawnPoints.remove(locationName);

        if (removed != null) {
            config.saveConfig();
            messageHandler.send(
                    source.getSender(),
                    config.getMessageConfig().pinata.admin.spawnPointRemoved,
                    messageHandler.tag("location", locationName));
        } else {
            messageHandler.send(
                    source.getSender(),
                    config.getMessageConfig().pinata.admin.spawnPointUnknown,
                    messageHandler.tag("location", locationName));
        }
        return Command.SINGLE_SUCCESS;
    }

    private Location resolveLocation(
            CommandSourceStack source, String locationName, String templateId, String commandContext) {
        if (locationName != null) {
            var pinataConfig = config.getPinataConfig(templateId);
            if (pinataConfig == null) {
                messageHandler.send(
                        source.getSender(),
                        config.getMessageConfig().pinata.admin.unknownTemplate,
                        messageHandler.tagParsed("pinata", templateId));
                return null;
            }
            SerializableLocation spawnLocation =
                    config.getMainConfig().modules.pinata.spawnPoints.get(locationName);
            if (spawnLocation == null) {
                messageHandler.send(
                        source.getSender(),
                        config.getMessageConfig().pinata.admin.spawnPointUnknown,
                        messageHandler.tagParsed("pinata", templateId),
                        messageHandler.tag("location", locationName));
                return null;
            }
            return spawnLocation.toBukkit();
        }
        if (source.getSender() instanceof Player player) {
            return player.getLocation();
        }
        sendUsage(source.getSender(), "/partyanimals pinata " + commandContext + " [template] [location]");
        return null;
    }

    private void sendUsage(CommandSender sender, String usage) {
        messageHandler.send(
                sender, config.getMessageConfig().commands.usageHelp, messageHandler.tag("usage-help", usage));
    }

    private CompletableFuture<Suggestions> suggestPinataTemplates(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        config.getPinataConfigs().keySet().forEach(builder::suggest);
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestCentralLocations(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        if (config.getMainConfig().modules.pinata.spawnPoints != null) {
            config.getMainConfig().modules.pinata.spawnPoints.keySet().forEach(builder::suggest);
        }
        return builder.buildFuture();
    }
}
