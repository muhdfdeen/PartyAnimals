package org.maboroshi.partyanimals.config.settings;

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
        public String prefix = "<gradient:#51CF66:#2f9e44>🪅 <bold>Party Animals</bold></gradient> <dark_gray>➟</dark_gray>";

        @Comment("General plugin notifications.")
        public GeneralMessages general = new GeneralMessages(
            "<prefix> <green>Configuration reloaded successfully.</green>",
            "<prefix> <red>Reload failed! Check console for errors.</red>",
            "<prefix> <gray>New version available: <green><latest-version></green> (Current: <red><current-version></red>)</gray>"
        );

        @Comment("Generic command responses and errors.")
        public CommandMessages commands = new CommandMessages(
            "<prefix> <red>You do not have permission for this.</red>",
            "<prefix> <red>This command is for players only.</red>",
            "<prefix> <red>Usage: <gray><usage-help></gray></red>",
            "<prefix> <red>Player <dark_red><player-name></dark_red> not found.</red>"
        );

        @Comment("Messages for the help command.")
        public HelpMessages help = new HelpMessages(
            "<gradient:#51CF66:#2f9e44>🪅 <bold>Party Animals Help</bold></gradient>",
            "<gray>-</gray> <click:suggest_command:/<command>><white>/<command></white> <dark_gray>»</dark_gray> <gray><description></gray></click>"
        );

        @Comment("Messages specific to the pinata module.")
        public PinataMessages pinata = new PinataMessages(
            new PinataEvents(
                "<prefix> <gray>Countdown for <white><pinata></white> at <white><location></white> has <green>begun</green>!</gray>",
                "<prefix> <gray>The pinata <white><pinata></white> has been <yellow>spawned</yellow> at <white><location></white>!</gray>",
                "<prefix> <gray>A pinata has spawned at <white><location></white>!</gray>",
                "<prefix> <gray>The pinata has been <green>defeated</green>!</gray>",
                "<prefix> <gray>The pinata has <red>escaped</red>!</gray>"
            ),
            new PinataGameplay(
                "<prefix> <gray>You landed a hit on the pinata!</gray>",
                "<prefix> <gray>You must use <red><item></red> to hit this pinata!</gray>",
                "<prefix> <red><bold>Too fast!</bold></red> <gray>Please wait a moment.</gray>",
                "<prefix> <gray>You are <red>not allowed</red> to hit this pinata.</gray>",
                "<prefix> <gray><white><player-name></white> dealt the final blow!</gray>"
            ),
            new PinataVisuals(
                "A pinata party will begin in <white><countdown></white>. Get ready!",
                "<pinata> <health> <gray>/</gray> <max-health> <red>❤</red> <gray>[<timer>]</gray>"
            ),
            new PinataAdmin(
                "<prefix> <red>Unknown pinata template: <white><pinata></white></red>",
                "<prefix> <gray><white><location></white> has been <green>added</green> as a spawn point.</gray>",
                "<prefix> <gray><white><location></white> has been <red>removed</red> as a spawn point.</gray>",
                "<prefix> <gray>The spawn point <white><location></white> does not exist.</gray>"
            )
        );
    }

    public record GeneralMessages(String reloadSuccess, String reloadFail, String updateAvailable) {}
    public record CommandMessages(String noPermission, String playerOnly, String usageHelp, String playerNotFound) {}
    public record HelpMessages(String header, String entry) {}

    public record PinataMessages(
        @Comment("Broadcasts and public announcements.") PinataEvents events,
        @Comment("Feedback messages sent to individual players.") PinataGameplay gameplay,
        @Comment("Boss bar text configurations.") PinataVisuals visuals,
        @Comment("Admin command responses.") PinataAdmin admin
    ) {}

    public record PinataEvents(
        String countdownStarted,
        String spawned,
        String spawnedNaturally,
        String defeated,
        String timeout
    ) {}

    public record PinataGameplay(
        String hitSuccess,
        String hitWrongItem,
        String hitCooldown,
        String hitNoPermission,
        String lastHit
    ) {}

    public record PinataVisuals(
        String bossBarCountdown,
        String bossBarActive
    ) {}

    public record PinataAdmin(
        String unknownTemplate,
        String spawnPointAdded,
        String spawnPointRemoved,
        String spawnPointUnknown
    ) {}
}
