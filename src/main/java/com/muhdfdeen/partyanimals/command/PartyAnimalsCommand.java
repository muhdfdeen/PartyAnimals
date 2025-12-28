package com.muhdfdeen.partyanimals.command;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.config.ConfigManager;
import com.muhdfdeen.partyanimals.config.objects.SerializableLocation;
import com.muhdfdeen.partyanimals.handler.MessageHandler;

public class PartyAnimalsCommand {
    private final PartyAnimals plugin;
    private final ConfigManager config;
    private final MessageHandler messageHandler;

    public PartyAnimalsCommand(PartyAnimals plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.messageHandler = plugin.getMessageHandler();
    }

    private void sendUsage(CommandSender sender, String usage) {
        messageHandler.send(sender, config.getMessageConfig().commands.usageHelp(),
                messageHandler.tag("usage-help", usage));
    }

    public LiteralCommandNode<CommandSourceStack> createCommand(final String commandName) {
        return Commands.literal(commandName)
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    messageHandler.send(sender,
                            "<prefix> <gray>Plugin version: <green>" + plugin.getPluginMeta().getVersion());
                    messageHandler.send(sender,
                            "<green>🛈</green> <gray>Type <white>/partyanimals reload</white> to reload the configuration.</gray>");
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("reload")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.reload"))
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            if (plugin.reload()) {
                                plugin.getPluginLogger().info("Configuration reloaded by " + sender.getName());
                                messageHandler.send(sender, config.getMessageConfig().general.reloadSuccess());
                            } else {
                                plugin.getPluginLogger().warn("Failed to reload configuration by " + sender.getName());
                                messageHandler.send(sender, config.getMessageConfig().general.reloadFail());
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("start")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.start"))
                        .executes(ctx -> handleStart(ctx.getSource(), null, "default"))
                        .then(Commands.argument("pinata", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestPinataTemplates(ctx, builder).buildFuture())
                                .executes(ctx -> handleStart(ctx.getSource(), null,
                                        StringArgumentType.getString(ctx, "pinata")))
                                .then(Commands.argument("location", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestLocations(ctx, builder, "pinata")
                                                .buildFuture())
                                        .executes(ctx -> handleStart(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "location"),
                                                StringArgumentType.getString(ctx, "pinata"))))))
                .then(Commands.literal("spawn")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.spawn"))
                        .executes(ctx -> handleSpawn(ctx.getSource(), null, "default"))
                        .then(Commands.argument("pinata", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestPinataTemplates(ctx, builder).buildFuture())
                                .executes(ctx -> handleSpawn(ctx.getSource(), null,
                                        StringArgumentType.getString(ctx, "pinata")))
                                .then(Commands.argument("location", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestLocations(ctx, builder, "pinata")
                                                .buildFuture())
                                        .executes(ctx -> handleSpawn(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "location"),
                                                StringArgumentType.getString(ctx, "pinata"))))))
                .then(Commands.literal("killall")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.killall"))
                        .executes(ctx -> {
                            plugin.getPinataManager().cleanup();
                            messageHandler.send(ctx.getSource().getSender(),
                                    "<prefix> <green>Killed all active pinatas.");
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("addlocation")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.addlocation"))
                        .executes(ctx -> {
                            sendUsage(ctx.getSource().getSender(), "/pa addlocation <pinata> <name>");
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("pinata", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestPinataTemplates(ctx, builder).buildFuture())
                                .then(Commands.argument("location", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestLocations(ctx, builder, "pinata")
                                                .buildFuture())
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            if (!(source.getSender() instanceof Player player)) {
                                                messageHandler.send(source.getSender(),
                                                        config.getMessageConfig().commands.playerOnly());
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            String templateId = StringArgumentType.getString(ctx, "pinata");
                                            String locationName = StringArgumentType.getString(ctx, "location");
                                            SerializableLocation spawnLocation = new SerializableLocation(
                                                    player.getLocation());
                                            var pinataConfig = config.getPinataConfig(templateId);
                                            if (pinataConfig == null) {
                                                messageHandler.send(player,
                                                        "<red>Unknown pinata template: " + templateId);
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            config.getMainConfig().modules.pinata().spawnLocations().put(locationName, spawnLocation);
                                            config.saveConfig();
                                            messageHandler.send(player,
                                                    config.getMessageConfig().pinata.spawnPointAdded(),
                                                    messageHandler.tagParsed("pinata", templateId),
                                                    messageHandler.tag("location", locationName));
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .then(Commands.literal("removelocation")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.removelocation"))
                        .executes(ctx -> {
                            sendUsage(ctx.getSource().getSender(), "/pa removelocation <pinata> <name>");
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("pinata", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestPinataTemplates(ctx, builder).buildFuture())
                                .then(Commands.argument("location", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestLocations(ctx, builder, "pinata")
                                                .buildFuture())
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            String templateId = StringArgumentType.getString(ctx, "pinata");
                                            String locationName = StringArgumentType.getString(ctx, "location");
                                            var pinataConfig = config.getPinataConfig(templateId);
                                            if (pinataConfig == null) {
                                                messageHandler.send(source.getSender(),
                                                        "<red>Unknown pinata template: " + templateId);
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            SerializableLocation removed = config.getMainConfig().modules.pinata().spawnLocations().remove(locationName);
                                            if (removed != null) {
                                                config.saveConfig();
                                                messageHandler.send(source.getSender(),
                                                        config.getMessageConfig().pinata.spawnPointRemoved(),
                                                        messageHandler.tagParsed("pinata", templateId),
                                                        messageHandler.tag("location", locationName));
                                            } else {
                                                messageHandler.send(source.getSender(),
                                                        config.getMessageConfig().pinata.spawnPointUnknown(),
                                                        messageHandler.tagParsed("pinata", templateId),
                                                        messageHandler.tag("location", locationName));
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .build();
    }

    private int handleStart(CommandSourceStack source, String locationName, String templateId) {
        Location location = resolveLocation(source, locationName, templateId, "start");
        if (location == null)
            return Command.SINGLE_SUCCESS;
        plugin.getPinataManager().startCountdown(location, templateId);
        messageHandler.send(source.getSender(), config.getMessageConfig().pinata.countdownStarted());
        return Command.SINGLE_SUCCESS;
    }

    private int handleSpawn(CommandSourceStack source, String locationName, String templateId) {
        Location location = resolveLocation(source, locationName, templateId, "spawn");
        if (location == null)
            return Command.SINGLE_SUCCESS;
        plugin.getPinataManager().spawnPinata(location, templateId);
        messageHandler.send(source.getSender(), config.getMessageConfig().pinata.spawned());
        return Command.SINGLE_SUCCESS;
    }

    private Location resolveLocation(CommandSourceStack source, String locationName, String templateId,
            String commandContext) {
        if (locationName != null) {
            var pinataConfig = config.getPinataConfig(templateId);
            if (pinataConfig == null) {
                messageHandler.send(
                        source.getSender(),
                        config.getMessageConfig().pinata.unknownTemplate(),
                        messageHandler.tagParsed("pinata", templateId));
                return null;
            }
            SerializableLocation spawnLocation = config.getMainConfig().modules.pinata().spawnLocations().get(locationName);
            if (spawnLocation == null) {
                messageHandler.send(source.getSender(), config.getMessageConfig().pinata.spawnPointUnknown(),
                        messageHandler.tagParsed("pinata", templateId), messageHandler.tag("location", locationName));
                return null;
            }
            return spawnLocation.toBukkit();
        }
        if (source.getSender() instanceof Player player) {
            return player.getLocation();
        }
        sendUsage(source.getSender(), "/partyanimals " + commandContext + " [template] [location]");
        return null;
    }

    private SuggestionsBuilder suggestPinataTemplates(CommandContext<CommandSourceStack> ctx,
            SuggestionsBuilder builder) {
        config.getPinataConfigs().keySet().forEach(builder::suggest);
        return builder;
    }

    private SuggestionsBuilder suggestLocations(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder,
            String templateArg) {
        String templateId = StringArgumentType.getString(ctx, templateArg);
        var pinataConfig = config.getPinataConfig(templateId);
        if (pinataConfig != null && config.getMainConfig().modules.pinata().spawnLocations() != null) {
            config.getMainConfig().modules.pinata().spawnLocations().keySet().forEach(builder::suggest);
        }
        return builder;
    }
}
