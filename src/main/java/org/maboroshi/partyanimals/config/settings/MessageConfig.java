package org.maboroshi.partyanimals.config.settings;

import de.exlll.configlib.Comment;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import java.io.File;
import java.nio.file.Path;

public final class MessageConfig {

    public static MessageConfiguration load(File dataFolder) {
        YamlConfigurationProperties properties =
                ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder().build();
        Path messagesFile = new File(dataFolder, "messages.yml").toPath();
        return YamlConfigurations.update(messagesFile, MessageConfiguration.class, properties);
    }

    @Configuration
    public static class MessageConfiguration {

        @Comment("The global prefix used in messages. Use <prefix> in other messages to include it.")
        public String prefix =
                "<gradient:#51CF66:#2f9e44>🪅 <bold>Party Animals</bold></gradient> <dark_gray>➟</dark_gray>";

        @Comment("General plugin notifications.")
        public GeneralMessages general = new GeneralMessages();

        @Comment("Generic command responses and errors.")
        public CommandMessages commands = new CommandMessages();

        @Comment("Messages for the help command.")
        public HelpMessages help = new HelpMessages();

        @Comment("Messages specific to the pinata module.")
        public PinataMessages pinata = new PinataMessages();
    }

    @Configuration
    public static class GeneralMessages {
        public String reloadSuccess = "<prefix> <green>Configuration reloaded successfully.</green>";
        public String reloadFail = "<prefix> <red>Reload failed! Check console for errors.</red>";
        public String updateAvailable =
                "<prefix> <gray>New version available: <green><latest-version></green> (Current: <red><current-version></red>)</gray>";
    }

    @Configuration
    public static class CommandMessages {
        public String noPermission = "<prefix> <red>You do not have permission for this.</red>";
        public String playerOnly = "<prefix> <red>This command is for players only.</red>";
        public String usageHelp = "<prefix> <red>Usage: <gray><usage-help></gray></red>";
        public String playerNotFound = "<prefix> <red>Player <dark_red><player-name></dark_red> not found.</red>";
    }

    @Configuration
    public static class HelpMessages {
        public String header = "<gradient:#51CF66:#2f9e44>🪅 <bold>Party Animals Help</bold></gradient>";
        public String entry =
                "<gray>-</gray> <click:suggest_command:\"/<command>\"><white>/<command></white> <dark_gray>»</dark_gray> <gray><description></gray></click>";
    }

    @Configuration
    public static class PinataMessages {
        @Comment("Broadcasts and public announcements.")
        public PinataEvents events = new PinataEvents();

        @Comment("Feedback messages sent to individual players.")
        public PinataGameplay gameplay = new PinataGameplay();

        @Comment("Boss bar text configurations.")
        public PinataVisuals visuals = new PinataVisuals();

        @Comment("Admin command responses.")
        public PinataAdmin admin = new PinataAdmin();
    }

    @Configuration
    public static class PinataEvents {
        public String countdownStarted =
                "<prefix> <gray>Countdown for <white><pinata></white> at <white><location></white> has <green>begun</green>!</gray>";
        public String spawned =
                "<prefix> <gray>The pinata <white><pinata></white> has been <yellow>spawned</yellow> at <white><location></white>!</gray>";
        public String spawnedNaturally = "<prefix> <gray>A pinata has spawned at <white><location></white>!</gray>";
        public String defeated = "<prefix> <gray>The pinata has been <green>defeated</green>!</gray>";
        public String timeout = "<prefix> <gray>The pinata has <red>escaped</red>!</gray>";
    }

    @Configuration
    public static class PinataGameplay {
        public String hitSuccess = "<prefix> <gray>You landed a hit on the pinata!</gray>";
        public String hitWrongItem = "<prefix> <gray>You must use <red><item></red> to hit this pinata!</gray>";
        public String hitCooldown = "<prefix> <red><bold>Too fast!</bold></red> <gray>Please wait a moment.</gray>";
        public String hitNoPermission = "<prefix> <gray>You are <red>not allowed</red> to hit this pinata.</gray>";
        public String lastHit = "<prefix> <gray><white><player-name></white> dealt the final blow!</gray>";
    }

    @Configuration
    public static class PinataVisuals {
        public String bossBarCountdown = "A pinata party will begin in <white><countdown></white>. Get ready!";
        public String bossBarActive =
                "<pinata> <health> <gray>/</gray> <max-health> <red>❤</red> <gray>[<timer>]</gray>";
    }

    @Configuration
    public static class PinataAdmin {
        public String unknownTemplate = "<prefix> <red>Unknown pinata template: <white><pinata></white></red>";
        public String spawnPointAdded =
                "<prefix> <gray><white><location></white> has been <green>added</green> as a spawn point.</gray>";
        public String spawnPointRemoved =
                "<prefix> <gray><white><location></white> has been <red>removed</red> as a spawn point.</gray>";
        public String spawnPointUnknown =
                "<prefix> <gray>The spawn point <white><location></white> does not exist.</gray>";
    }
}
