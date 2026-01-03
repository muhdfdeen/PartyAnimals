package org.maboroshi.partyanimals.config.settings;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.maboroshi.partyanimals.config.objects.RewardAction;
import org.maboroshi.partyanimals.config.objects.SerializableLocation;
import org.maboroshi.partyanimals.config.objects.EffectTypes.EffectGroup;
import org.maboroshi.partyanimals.config.objects.EffectTypes.SoundEffect;

import de.exlll.configlib.Comment;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;

public final class MainConfig {

    public static MainConfiguration load(File dataFolder) {
        YamlConfigurationProperties properties = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder().build();
        Path configFile = new File(dataFolder, "config.yml").toPath();
        return YamlConfigurations.update(configFile, MainConfiguration.class, properties);
    }

    @Configuration
    public static class MainConfiguration {

        @Comment("Enable debug mode to see detailed logs in the console.")
        public boolean debug = false;

        @Comment("Control which parts of the plugin should be active.")
        public ModuleSettings modules = new ModuleSettings(
            new PinataSettings(true, new HashMap<>(Map.of("default", new SerializableLocation()))),
            new VoteSettings(
                true,
                new OfflineVoteSettings(true, true),
                new VoteReminderSettings(true, 10800, new EffectGroup(List.of(new SoundEffect("entity.player.levelup", 1f, 1f)), null)),
                new EventRegistry(
                    new VoteEvent(
                        true,
                        new EffectGroup(List.of(new SoundEffect("entity.player.levelup", 1f, 1f)), null),
                        new HashMap<>(Map.of())
                    )
                )
            )
        );
    }

    public record ModuleSettings(
            @Comment("Toggle the pinata module.") PinataSettings pinata,
            @Comment("Toggle the voting module.") VoteSettings vote
    ) {}

    public record PinataSettings(
            @Comment("Enable or disable the pinata module.") boolean enabled,
            @Comment("Defined spawn points.") Map<String, SerializableLocation> spawnPoints
    ) {}

    public record VoteSettings(
            @Comment("Enable or disable the voting module.") 
            boolean enabled,

            @Comment("Settings for handling offline votes.") 
            OfflineVoteSettings offline,

            @Comment("Periodic reminders for players to vote.") 
            VoteReminderSettings reminder,

            @Comment("Event configurations for votes.") 
            EventRegistry events
    ) {}

    public record OfflineVoteSettings(
        @Comment("Process votes even if the player is offline?") boolean enabled,
        @Comment("If true, rewards are queued and given when the player joins. If false, rewards are given immediately.") boolean queueRewards
    ) {}

    public record VoteReminderSettings(
        @Comment("Enable or disable the vote reminders.") boolean enabled,
        @Comment("Interval in seconds (default 3 hours).") int interval,
        @Comment("Visual/Audio effects.") EffectGroup effects
    ) {}

    public record VoteEvent(
        @Comment("Enable this event.") boolean enabled,
        @Comment("Visual/Audio effects.") EffectGroup effects,
        @Comment("Rewards to give.") Map<String, RewardAction> rewards
    ) {}

    public record EventRegistry(
        @Comment("Triggered when a single vote is received.") VoteEvent vote
    ) {}
}
