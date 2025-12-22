package com.muhdfdeen.partyanimals.config;

import java.io.File;
import java.nio.file.Path;

import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;

public final class MessageConfig {
    public static MessageConfiguration load(File dataFolder) {
        YamlConfigurationProperties properties = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder().build();
        Path configFile = new File(dataFolder, "messages.yml").toPath();
        return YamlConfigurations.update(configFile, MessageConfiguration.class, properties);
    }

    public record MessageSettings(
        @Comment("Prefix for all messages sent by the plugin.")
        String prefix,
        @Comment("Message displayed when the plugin is reloaded.")
        String reloadSuccess,
        @Comment("Message displayed when the plugin fails to reload.")
        String reloadFail,
        @Comment("Message displayed when a new version of the plugin is available.")
        String updateAvailable,
        @Comment("Messages related to pinata module.")
        PinataMessages pinataMessages
    ) {}

    public record PinataMessages(
        @Comment("Message displayed when a pinata countdown starts.")
        String startCountdown,
        @Comment("Message displayed when a pinata is summoned.")
        String pinataSummoned,
        @Comment("Message displayed when a pinata spawns.")
        String pinataSpawned,
        @Comment("Message displayed when a pinata is hit.")
        String pinataHit,
        @Comment("Message displayed when a pinata is last hit.")
        String pinataLastHit,
        @Comment("Message displayed when a pinata is downed.")
        String pinataDowned,
        @Comment("Message displayed when a pinata is removed due to timeout.")
        String pinataTimeout,
        @Comment("Boss bar countdown message format.")
        String bossBarCountdown,
        @Comment("Boss bar message format while the pinata is active.")
        String bossBarActive,
        @Comment("Message displayed when a new spawn location is added.")
        String addedSpawnLocation
    ) {}

    @Configuration
    public static class MessageConfiguration {
        @Comment("Settings related to messages sent by the plugin.")
        public MessageSettings messages = new MessageSettings(
            "<color:#51CF66><bold>Party Animals</bold> ➟ </color>",
            "Plugin configuration has been reloaded successfully.",
            "<red>Failed to reload plugin configuration! Check console for errors.</red>",
            "A new version is available! <gray>(Current: <red>{current_version}</red> | Latest: <green>{latest_version}</green>)</gray>",
            new PinataMessages(
                "<green>Countdown has been started for a pinata.</green>",
                "<yellow>Pinata has been summoned!</yellow>",
                "<green>A pinata has spawned!</green>",
                "<yellow>You hit the pinata!</yellow>",
                "<gold>{player} last hit the pinata!</gold>",
                "<yellow>A pinata has been downed!</yellow>",
                "<red>The pinata has escaped due to timeout.</red>",
                "Pinata spawning in: {seconds}s",
                "<green>Pinata Health:</green> {health} <red>❤</red>",
                "<green>Spawn location <white>{location_name}</white> has been added!</green>"
            ));
    }
}