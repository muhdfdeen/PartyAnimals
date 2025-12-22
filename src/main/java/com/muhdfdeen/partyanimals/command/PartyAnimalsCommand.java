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
import com.muhdfdeen.partyanimals.config.MainConfig;

public class PartyAnimalsCommand {
    private final PartyAnimals plugin;
    private final ConfigManager config;

    public PartyAnimalsCommand(PartyAnimals plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
    }

    public LiteralCommandNode<CommandSourceStack> createCommand(final String commandName) {
        return Commands.literal(commandName)
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    sender.sendRichMessage(
                            config.getMessageConfig().messages.prefix() + "Plugin version: "
                                    + plugin.getPluginMeta().getVersion());
                    sender.sendRichMessage(
                            "<green>🛈</green> <gray>Type <white>/partyanimals reload</white> to reload the configuration.</gray>");
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("reload")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.reload"))
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            if (plugin.reload()) {
                                plugin.getPluginLogger().info("Configuration reloaded by " + sender.getName());
                                sender.sendRichMessage(config.getMessageConfig().messages.prefix()
                                        + config.getMessageConfig().messages.reloadSuccess());
                            } else {
                                plugin.getPluginLogger().warn("Failed to reload configuration by " + sender.getName());
                                sender.sendRichMessage(config.getMessageConfig().messages.prefix()
                                        + config.getMessageConfig().messages.reloadFail());
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("start")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.start"))
                        .executes(ctx -> handleStart(ctx.getSource(), null))
                        .then(Commands.argument("location", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    config.getMainConfig().pinata.spawnLocations().keySet().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> handleStart(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "location")))))
                .then(Commands.literal("summon")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.summon"))
                        .executes(ctx -> handleSummon(ctx.getSource(), null))
                        .then(Commands.argument("location", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    config.getMainConfig().pinata.spawnLocations().keySet().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> handleSummon(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "location")))))
                .then(Commands.literal("addspawnlocation")
                        .requires(sender -> sender.getSender().hasPermission("partyanimals.addspawnlocation"))
                        .then(Commands.argument("Location Name", StringArgumentType.word())
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    if (!(source.getSender() instanceof Player player)) {
                                        source.getSender().sendRichMessage("<red>Only players can add locations.");
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    String locationName = StringArgumentType.getString(ctx, "Location Name");
                                    Location currentLocation = player.getLocation();

                                    MainConfig.SerializableLocation spawnLocation = new MainConfig.SerializableLocation(
                                            currentLocation);

                                    config.getMainConfig().pinata.spawnLocations().put(locationName, spawnLocation);

                                    config.save();

                                    player.sendRichMessage(config.getMessageConfig().messages.prefix() +
                                            config.getMessageConfig().messages.pinataMessages()
                                                    .addedSpawnLocation().replace("{location_name}", locationName));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .build();
    }

    private int handleStart(CommandSourceStack source, String locationName) {
        Location location = resolveLocation(source, locationName);
        if (location == null)
            return Command.SINGLE_SUCCESS;

        plugin.getPinataManager().startCountdown(location);
        source.getSender().sendRichMessage(
                config.getMessageConfig().messages.prefix() + config.getMessageConfig().messages.pinataMessages()
                        .startCountdown());
        return Command.SINGLE_SUCCESS;
    }

    private int handleSummon(CommandSourceStack source, String locationName) {
        Location location = resolveLocation(source, locationName);
        if (location == null)
            return Command.SINGLE_SUCCESS;

        plugin.getPinataManager().spawnPinata(location);
        source.getSender().sendRichMessage(config.getMessageConfig().messages.prefix()
                + config.getMessageConfig().messages.pinataMessages().pinataSummoned());
        return Command.SINGLE_SUCCESS;
    }

    private Location resolveLocation(CommandSourceStack source, String locationName) {
        if (locationName != null) {
            MainConfig.SerializableLocation spawnLocation = config.getMainConfig().pinata.spawnLocations()
                    .get(locationName);
            if (spawnLocation == null) {
                source.getSender().sendRichMessage("<red>Unknown location: " + locationName);
                return null;
            }
            return spawnLocation.toBukkit();
        }

        if (source.getSender() instanceof Player player) {
            return player.getLocation();
        }

        source.getSender().sendRichMessage("<red>Console must specify a location name.");
        return null;
    }
}