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
        public String prefix = "<bold><gradient:#51CF66:#2f9e44>Party Animals</gradient></bold> <dark_gray>➟</dark_gray> ";

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

        @Comment("Messages specific to the Pinata module.")
        public PinataMessages pinata = new PinataMessages(
            "<prefix> <green>Countdown started!</green>",
            "<prefix> <yellow>A pinata has been summoned!</yellow>",
            "<prefix> <green>Pinata spawned at <white>{location}</white>!</green>",
            "<prefix> <yellow>You hit the pinata!</yellow>",
            "<prefix> <red>You must use a <bold>{item}</bold> to hit the pinata!</red>",
            "<prefix> <red>Too fast! Please wait a moment.</red>",
            "<prefix> <red>You are not allowed to hit this pinata.</red>",
            "<prefix> <gold><bold>{player}</bold> dealt the final blow!</gold>",
            "<prefix> <yellow>The pinata has been broken!</yellow>",
            "<prefix> <red>The pinata escaped! (Timeout)</red>",
            "<green>Pinata spawning in <white>{seconds}</white>s...</green>",
            "{pinata} <white>{health}❤</white> <gray>[{timeout}]</gray>",
            "<prefix> <green>Spawn location <white>{name}</white> added.</green>",
            "<prefix> <red>Spawn location <white>{name}</white> removed.</red>",
            "<prefix> <red>Spawn location <white>{name}</white> does not exist.</red>"
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
        String playersOnly,
        @Comment("Invalid arguments format.")
        String invalidUsage,
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
        String hitFeedback,
        @Comment("Error when hitting with wrong item.")
        String hitInvalidItem,
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
        String locationAdded,
        @Comment("Admin: Removed spawn location.")
        String locationRemoved,
        @Comment("Admin: Unknown spawn location.")
        String locationUnknown
    ) {}
}
