package com.muhdfdeen.partyanimals.config.settings;

import java.io.File;
import java.nio.file.Path;

import de.exlll.configlib.Comment;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;

public final class MessageConfig {

    public static MessageConfiguration load(File dataFolder) {
        YamlConfigurationProperties properties = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder().build();
        Path messagesFile = new File(dataFolder, "messages.yml").toPath();
        return YamlConfigurations.update(messagesFile, MessageConfiguration.class, properties);
    }

    @Configuration
    public static class MessageConfiguration {

        @Comment("The global prefix used in messages. Use <prefix> in other messages to include it.")
        public String prefix = "<bold><gradient:#51CF66:#2f9e44>Party Animals</gradient></bold> <dark_gray>➟</dark_gray>";

        @Comment("General plugin notifications.")
        public GeneralMessages general = new GeneralMessages(
            "<prefix> <green>Configuration reloaded successfully.</green>",
            "<prefix> <red>Reload failed! Check console for errors.</red>",
            "<prefix> <gray>New version available: <green>{latest}</green> (Current: <red>{current}</red>)</gray>"
        );

        @Comment("Generic command responses and errors.")
        public CommandMessages commands = new CommandMessages(
            "<prefix> <red>You do not have permission for this.</red>",
            "<prefix> <red>This command is for players only.</red>",
            "<prefix> <red>Usage: <gray>{usage}</gray></red>",
            "<prefix> <red>Player <dark_red>{player}</dark_red> not found.</red>"
        );

        @Comment("Messages specific to the pinata module.")
        public PinataMessages pinata = new PinataMessages(
            "<prefix> <gray>Countdown for the pinata has <green>begun</green>!</gray>",
            "<prefix> <gray>A pinata has been <yellow>summoned</yellow> instantly!</gray>",
            "<prefix> <gray>A pinata has spawned at <white>{location}</white>!</gray>",
            "<prefix> <gray>You landed a hit on the pinata!</gray>",
            "<prefix> <gray>You must use <red>{item}</red> to hit the pinata!</gray>",
            "<prefix> <red><bold>Too fast!</bold></red> <gray>Please wait a moment.</gray>",
            "<prefix> <gray>You are <red>not allowed</red> to hit this pinata.</gray>",
            "<prefix> {player} <gray>dealt the final blow!</gray>",
            "<prefix> <gray>The pinata has been <green>defeated</green>!</gray>",
            "<prefix> <gray>The pinata has <red>escaped</red>!</gray>",
            "A pinata party will begin in <white>{seconds}</white>. Get ready!",
            "{pinata} {health} <red>❤</red> <gray>[{timeout}]</gray>",
            "<prefix> <gray>A new spawn location named <white>{name}</white> has been <green>added</green>.</gray>",
            "<prefix> <gray>A new spawn location named <white>{name}</white> has been <red>removed</red>.</gray>",
            "<prefix> <gray>The spawn location named <white>{name}</white> does not exist.</gray>"
        );
    }

    public record GeneralMessages(
        @Comment("Message on successful reload.")
        String reloadSuccess,
        @Comment("Message on failed reload.")
        String reloadFail,
        @Comment("Update notification.")
        String updateAvailable
    ) {}

    public record CommandMessages(
        @Comment("No permission message.")
        String noPermission,
        @Comment("Console sender error.")
        String playerOnly,
        @Comment("Invalid arguments format.")
        String usageHelp,
        @Comment("Target player not found.")
        String playerNotFound
    ) {}

    public record PinataMessages(
        @Comment("Broadcast when countdown begins.")
        String countdownStarted,
        @Comment("Broadcast when manually summoned.")
        String summoned,
        @Comment("Broadcast when spawned naturally.")
        String spawned,
        @Comment("Feedback when a player hits the entity.")
        String hitSuccess,
        @Comment("Error when hitting with wrong item.")
        String hitWrongItem,
        @Comment("Error when hitting during cooldown.")
        String hitCooldown,
        @Comment("Error when hitting without permission.")
        String hitNoPermission,
        @Comment("Broadcast for the last person to hit.")
        String lastHit,
        @Comment("Broadcast when pinata is defeated.")
        String defeated,
        @Comment("Broadcast when pinata despawns due to time.")
        String timeout,
        @Comment("Boss bar countdown text.")
        String bossBarCountdown,
        @Comment("Boss bar active text.")
        String bossBarActive,
        @Comment("Admin: Added spawn location.")
        String spawnPointAdded,
        @Comment("Admin: Removed spawn location.")
        String spawnPointRemoved,
        @Comment("Admin: Unknown spawn location.")
        String spawnPointUnknown
    ) {}
}
