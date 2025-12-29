package com.muhdfdeen.partyanimals.command;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.concurrent.CompletableFuture;

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
                                .suggests(this::suggestPinataTemplates)
                                .executes(ctx -> handleStart(ctx.getSource(), null,
                                        StringArgumentType.getString(ctx, "pinata")))
                                .then(Commands.argument("location", StringArgumentType.word())
                                        .suggests(this::suggestCentralLocations)
                                        .executes(ctx -> handleStart(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "location"),
                                                StringArgumentType.getString(ctx, "pinata"))))))
                .then(Commands.literal("spawn")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.spawn"))
                        .executes(ctx -> handleSpawn(ctx.getSource(), null, "default"))
                        .then(Commands.argument("pinata", StringArgumentType.word())
                                .suggests(this::suggestPinataTemplates)
                                .executes(ctx -> handleSpawn(ctx.getSource(), null,
                                        StringArgumentType.getString(ctx, "pinata")))
                                .then(Commands.argument("location", StringArgumentType.word())
                                        .suggests(this::suggestCentralLocations)
                                        .executes(ctx -> handleSpawn(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "location"),
                                                StringArgumentType.getString(ctx, "pinata"))))))
                .then(Commands.literal("killall")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.killall"))
                        .executes(ctx -> {
                            plugin.getPinataManager().cleanup();
                            messageHandler.send(ctx.getSource().getSender(), "<prefix> <green>Killed all active pinatas.");
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("addlocation")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.addlocation"))
                        .executes(ctx -> {
                            sendUsage(ctx.getSource().getSender(), "/pa addlocation <name>");
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("location", StringArgumentType.word())
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    if (!(source.getSender() instanceof Player player)) {
                                        messageHandler.send(source.getSender(),
                                                config.getMessageConfig().commands.playerOnly());
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    
                                    String locationName = StringArgumentType.getString(ctx, "location");
                                    SerializableLocation spawnLocation = new SerializableLocation(player.getLocation());
                                    
                                    config.getMainConfig().modules.pinata().spawnPoints().put(locationName, spawnLocation);
                                    config.saveConfig();
                                    
                                    messageHandler.send(player,
                                            config.getMessageConfig().pinata.spawnPointAdded(),
                                            messageHandler.tag("location", locationName));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("removelocation")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.removelocation"))
                        .executes(ctx -> {
                            sendUsage(ctx.getSource().getSender(), "/pa removelocation <name>");
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("location", StringArgumentType.word())
                                .suggests(this::suggestCentralLocations)
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    String locationName = StringArgumentType.getString(ctx, "location");
                                    
                                    SerializableLocation removed = config.getMainConfig().modules.pinata().spawnPoints().remove(locationName);
                                    
                                    if (removed != null) {
                                        config.saveConfig();
                                        messageHandler.send(source.getSender(),
                                                config.getMessageConfig().pinata.spawnPointRemoved(),
                                                messageHandler.tag("location", locationName));
                                    } else {
                                        messageHandler.send(source.getSender(),
                                                config.getMessageConfig().pinata.spawnPointUnknown(),
                                                messageHandler.tag("location", locationName));
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))
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

    private Location resolveLocation(CommandSourceStack source, String locationName, String templateId, String commandContext) {
        if (locationName != null) {
            var pinataConfig = config.getPinataConfig(templateId);
            if (pinataConfig == null) {
                messageHandler.send(
                        source.getSender(),
                        config.getMessageConfig().pinata.unknownTemplate(),
                        messageHandler.tagParsed("pinata", templateId));
                return null;
            }

            SerializableLocation spawnLocation = config.getMainConfig().modules.pinata().spawnPoints().get(locationName);
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

    private CompletableFuture<Suggestions> suggestPinataTemplates(CommandContext<CommandSourceStack> ctx,
            SuggestionsBuilder builder) {
        config.getPinataConfigs().keySet().forEach(builder::suggest);
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestCentralLocations(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        if (config.getMainConfig().modules.pinata().spawnPoints() != null) {
            config.getMainConfig().modules.pinata().spawnPoints().keySet().forEach(builder::suggest);
        }
        return builder.buildFuture();
    }
}
