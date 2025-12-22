package com.muhdfdeen.partyanimals.config;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Comment;
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
    public static class Reward {
        @Comment("The chance for this reward to be given.")
        public Double chance = 100.0;
        public Boolean serverwide = null;
        public Boolean skipRest = null;
        public Boolean randomize = null;
        public String permission = null;
        public List<String> commands = List.of();

        public Reward() {
        }

        public Reward(double chance, List<String> commands) {
            this.chance = chance;
            this.commands = commands;
        }
    }

    @Configuration
    public static class SerializableLocation {
        public String world = "world";
        public double x = 0;
        public double y = 100;
        public double z = 0;
        public float yaw = 0;
        public float pitch = 0;

        public SerializableLocation() {
        }

        public SerializableLocation(Location loc) {
            this.world = loc.getWorld().getName();
            this.x = loc.getX();
            this.y = loc.getY();
            this.z = loc.getZ();
            this.yaw = loc.getYaw();
            this.pitch = loc.getPitch();
        }

        public Location toBukkit() {
            return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
        }
    }

    public record PinataCommands(
            @Comment("Commands to execute for when the Pinata spawns.")
            Map<String, Reward> spawn,
            @Comment("Commands to execute for when the Pinata is hit.")
            Map<String, Reward> hit,
            @Comment("Commands to execute for when the Pinata is last hit.")
            Map<String, Reward> lastHit,
            @Comment("Commands to execute for when the Pinata dies.")
            Map<String, Reward> death) {
    }

    public record PinataHealthSettings(
            @Comment("Maximum health of the Pinata.") int maxHealth,
            @Comment({"Whether the health is multiplied per player.", "If true, the value for maxHealthMultiplier is ignored."}) boolean multipliedPerPlayer,
            @Comment("Multiplier for the maximum health of the Pinata.") int maxHealthMultiplier) {
    }

    public record PinataSettings(
            @Comment( {
                    "List of entity types that can be used as Pinatas.",
                    "Available types can be found here: https://jd.papermc.io/paper/1.21.11/org/bukkit/entity/EntityType.html" }) List<String> type,
            @Comment("Name of the Pinata entity.") String name,
            @Comment("Scale of the Pinata entity.") double scale,
            @Comment("Pinata health settings.") PinataHealthSettings health,
            @Comment("Hit cooldown in seconds for the Pinata.") double hitCooldown,
            @Comment("Whether the hit cooldown is applied per player or globally.") boolean cooldownPerPlayer,
            @Comment("Countdown time before the Pinata spawns.") double countdown,
            @Comment("Timeout duration in seconds for the Pinata.") int timeout,
            @Comment("Locations where Pinatas can spawn.") Map<String, SerializableLocation> spawnLocations,
            @Comment("Commands to execute on various Pinata events.") PinataCommands commands){
    }

    @Configuration
    public static class MainConfiguration {
        @Comment("Enable debug mode for more verbose logging.")
        public boolean debug = false;

        @Comment("Settings for the Pinata module.")
        public PinataSettings pinata = new PinataSettings(
                List.of("LLAMA", "PIG", "COW", "SHEEP"),
                "<green>Pinata",
                1.5,
                new PinataHealthSettings(100, true, 1),
                1.0,
                true,
                10.0,
                300,
                new HashMap<>(
                        Map.of("spawn",
                                new SerializableLocation(new Location(Bukkit.getWorlds().get(0), 0, 100, 0)))),
                new PinataCommands(
                        Map.of("1", new Reward(100.0, List.of("broadcast <green>A Pinata has appeared!"))),
                        Map.of("1", new Reward(100.0, List.of("msg %player% <yellow>You hit it!"))),
                        Map.of("1", new Reward(100.0, List.of("msg %player% <gold>FINAL BLOW!"))),
                        Map.of("1", new Reward(100.0, List.of("give %player% diamond 1")))));
    }
}
