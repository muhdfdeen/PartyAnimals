package com.muhdfdeen.partyanimals.command;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.config.ConfigManager;
import com.muhdfdeen.partyanimals.config.SerializableLocation;
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

    public LiteralCommandNode<CommandSourceStack> createCommand(final String commandName) {
        return Commands.literal(commandName)
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    messageHandler.send(sender, "<prefix> <gray>Plugin version: <green>" + plugin.getPluginMeta().getVersion());
                    messageHandler.send(sender, "<green>🛈</green> <gray>Type <white>/partyanimals reload</white> to reload the configuration.</gray>");
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
                        .executes(ctx -> handleStart(ctx.getSource(), null))
                        .then(Commands.argument("location", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    config.getPinataConfig().spawnLocations.keySet().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> handleStart(ctx.getSource(), StringArgumentType.getString(ctx, "location")))))
                .then(Commands.literal("summon")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.summon"))
                        .executes(ctx -> handleSummon(ctx.getSource(), null))
                        .then(Commands.argument("location", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    config.getPinataConfig().spawnLocations.keySet().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> handleSummon(ctx.getSource(), StringArgumentType.getString(ctx, "location")))))
                .then(Commands.literal("killall")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.killall"))
                        .executes(ctx -> {
                            plugin.getPinataManager().cleanup();
                            messageHandler.send(ctx.getSource().getSender(), "<prefix> <green>Killed all active pinatas.");
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("addlocation")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.addlocation"))
                        .then(Commands.argument("location", StringArgumentType.word())
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    if (!(source.getSender() instanceof Player player)) {
                                        messageHandler.send(source.getSender(), config.getMessageConfig().commands.playersOnly());
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    String locationName = StringArgumentType.getString(ctx, "location");
                                    Location currentLocation = player.getLocation();
                                    SerializableLocation spawnLocation = new SerializableLocation(currentLocation);
                                    config.getPinataConfig().spawnLocations.put(locationName, spawnLocation);
                                    config.saveConfig();
                                    messageHandler.send(player, config.getMessageConfig().pinata.locationAdded(), messageHandler.tag("name", locationName));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("removelocation")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.removelocation"))
                        .then(Commands.argument("location", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    config.getPinataConfig().spawnLocations.keySet().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    String locationName = StringArgumentType.getString(ctx, "location");
                                    SerializableLocation removed = config.getPinataConfig().spawnLocations.remove(locationName);
                                    if (removed != null) {
                                        config.saveConfig();
                                        messageHandler.send(source.getSender(), config.getMessageConfig().pinata.locationRemoved(), messageHandler.tag("name", locationName));
                                    } else {
                                        messageHandler.send(source.getSender(), config.getMessageConfig().pinata.locationUnknown(), messageHandler.tag("name", locationName));
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                        ))
                .build();
    }

    private int handleStart(CommandSourceStack source, String locationName) {
        Location location = resolveLocation(source, locationName);
        if (location == null)
            return Command.SINGLE_SUCCESS;

        plugin.getPinataManager().startCountdown(location);
        messageHandler.send(source.getSender(), config.getMessageConfig().pinata.countdownStarted());
        return Command.SINGLE_SUCCESS;
    }

    private int handleSummon(CommandSourceStack source, String locationName) {
        Location location = resolveLocation(source, locationName);
        if (location == null)
            return Command.SINGLE_SUCCESS;
        plugin.getPinataManager().spawnPinata(location);
        messageHandler.send(source.getSender(), config.getMessageConfig().pinata.summoned());
        return Command.SINGLE_SUCCESS;
    }

    private Location resolveLocation(CommandSourceStack source, String locationName) {
        if (locationName != null) {
            SerializableLocation spawnLocation = config.getPinataConfig().spawnLocations.get(locationName);
            if (spawnLocation == null) {
                messageHandler.send(source.getSender(), config.getMessageConfig().pinata.locationUnknown(),
                    messageHandler.tag("name", locationName));
                return null;
            }
            return spawnLocation.toBukkit();
        }

        if (source.getSender() instanceof Player player) {
            return player.getLocation();
        }

        messageHandler.send(source.getSender(), "<prefix> <red>Console must specify a location name.");
        return null;
    }
}
