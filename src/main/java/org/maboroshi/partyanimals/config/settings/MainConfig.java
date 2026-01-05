package org.maboroshi.partyanimals.config.settings;

import de.exlll.configlib.Comment;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.maboroshi.partyanimals.config.objects.EffectTypes.EffectGroup;
import org.maboroshi.partyanimals.config.objects.EffectTypes.SoundEffect;
import org.maboroshi.partyanimals.config.objects.RewardAction;
import org.maboroshi.partyanimals.config.objects.SerializableLocation;

public final class MainConfig {

    public static MainConfiguration load(File dataFolder) {
        YamlConfigurationProperties properties =
                ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder().build();
        Path configFile = new File(dataFolder, "config.yml").toPath();
        return YamlConfigurations.update(configFile, MainConfiguration.class, properties);
    }

    @Configuration
    public static class MainConfiguration {

        @Comment("Enable debug mode to see detailed logs in the console.")
        public boolean debug = false;

        @Comment("Database settings.")
        public DatabaseSettings database = new DatabaseSettings();

        @Comment("Control which parts of the plugin should be active.")
        public ModuleSettings modules = new ModuleSettings();
    }

    @Configuration
    public static class DatabaseSettings {
        @Comment("Type of database: 'sqlite' or 'mysql' or 'mariadb'.")
        public String type = "sqlite";

        @Comment("Hostname (for MySQL/MariaDB).")
        public String host = "localhost";

        @Comment("Port (default 3306 for MySQL/MariaDB).")
        public int port = 3306;

        @Comment("Database name.")
        public String database = "partyanimals";

        @Comment("Username.")
        public String username = "root";

        @Comment("Password.")
        public String password = "password";

        @Comment("Prefix for tables (e.g., 'pa_votes').")
        public String tablePrefix = "pa_";

        @Comment("Advanced connection pool settings.")
        public PoolSettings pool = new PoolSettings();
    }

    @Configuration
    public static class PoolSettings {
        @Comment("Maximum number of concurrent connections (Default: 10).")
        public int maximumPoolSize = 10;

        @Comment("How long to wait for a connection in milliseconds (Default: 30000).")
        public int connectionTimeout = 30000;

        @Comment("Warn if a connection is held longer than this (Default: 10000).")
        public int leakDetectionThreshold = 10000;
    }

    @Configuration
    public static class ModuleSettings {
        @Comment("Toggle the pinata module.")
        public PinataSettings pinata = new PinataSettings();

        @Comment("Toggle the voting module.")
        public VoteSettings vote = new VoteSettings();
    }

    @Configuration
    public static class PinataSettings {
        @Comment("Enable or disable the pinata module.")
        public boolean enabled = true;

        @Comment("Defined spawn points.")
        public Map<String, SerializableLocation> spawnPoints =
                new HashMap<>(Map.of("default", new SerializableLocation()));
    }

    @Configuration
    public static class VoteSettings {
        @Comment("Enable or disable the voting module.")
        public boolean enabled = true;

        @Comment({
            "If true, the plugin will always generate UUIDs based on the player name.",
            "Useful for offline-mode servers or testing environments.",
            "WARNING: Changing this after database creation will reset player data links!"
        })
        public boolean forceOfflineUUIDs = false;

        @Comment("Settings for handling offline votes.")
        public OfflineVoteSettings offline = new OfflineVoteSettings();

        @Comment("Periodic reminders for players to vote.")
        public VoteReminderSettings reminder = new VoteReminderSettings();

        @Comment("Community Goal settings.")
        public CommunityGoalSettings communityGoal = new CommunityGoalSettings();

        @Comment("Event configurations for votes.")
        public EventRegistry events = new EventRegistry();
    }

    @Configuration
    public static class OfflineVoteSettings {
        @Comment("Process votes even if the player is offline?")
        public boolean enabled = true;

        @Comment(
                "If true, rewards are queued and given when the player joins. If false, rewards are given immediately.")
        public boolean queueRewards = true;
    }

    @Configuration
    public static class VoteReminderSettings {
        @Comment("Enable or disable the vote reminders.")
        public boolean enabled = true;

        @Comment("Interval in seconds (default 3 hours).")
        public int interval = 10800;

        @Comment("Visual/Audio effects.")
        public EffectGroup effects = new EffectGroup(List.of(new SoundEffect("entity.player.levelup", 1f, 1f)), null);
    }

    @Configuration
    public static class CommunityGoalSettings {
        @Comment("Enable the community goal system?")
        public boolean enabled = true;

        @Comment("How many votes are needed to trigger the rewards?")
        public int votesRequired = 50;

        @Comment("Rewards to execute when the goal is reached.")
        public Map<String, RewardAction> rewards = new HashMap<>();

        public CommunityGoalSettings() {
            this.rewards.put(
                    "community_reward",
                    new RewardAction(100.0, List.of("say <green>Community goal reached!", "pa spawn default spawn")));
        }
    }

    @Configuration
    public static class VoteEvent {
        @Comment("Enable this event.")
        public boolean enabled = true;

        @Comment("Visual/Audio effects.")
        public EffectGroup effects = new EffectGroup(List.of(new SoundEffect("entity.player.levelup", 1f, 1f)), null);

        @Comment("Rewards to give.")
        public Map<String, RewardAction> rewards = new HashMap<>(
                Map.of("announce", new RewardAction(100.0, List.of("say <green>Thank you {player} for voting!"))));
    }

    @Configuration
    public static class EventRegistry {
        @Comment("Triggered when a single vote is received.")
        public VoteEvent vote = new VoteEvent();
    }
}
