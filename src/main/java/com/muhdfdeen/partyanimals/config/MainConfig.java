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

                public Reward(double chance, Boolean serverwide, Boolean skipRest, Boolean randomize,
                                String permission, List<String> commands) {
                        this.chance = chance;
                        this.serverwide = serverwide;
                        this.skipRest = skipRest;
                        this.randomize = randomize;
                        this.permission = permission;
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

        public record PinataAppearanceSettings(
                        @Comment( {
                                        "List of entity types that can be used as pinatas.",
                                        "Available types: https://jd.papermc.io/paper/1.21.1/org/bukkit/entity/EntityType.html"
                        }) List<String> types,
                        @Comment("Name of the pinata entity.") String name,
                        @Comment("Scale of the pinata entity.") double scale){
        }

        public record PinataDisplaySettings(
                        @Comment("Whether the countdown boss bar is enabled.") boolean showCountdownBar,
                        @Comment("Whether the active health boss bar is enabled.") boolean showHealthBar,
                        @Comment("The color of the countdown boss bar.") String countdownBarColor,
                        @Comment("The color of the health boss bar.") String healthBarColor,
                        @Comment("The overlay of the countdown boss bar.") String countdownBarOverlay,
                        @Comment("The overlay of the health boss bar.") String healthBarOverlay) {
        }

        public record PinataHealthSettings(
                        @Comment("Maximum health of the pinata.") int maxHealth,
                        @Comment( {
                                        "Whether the health is multiplied per player.",
                                        "If true, the value for maxHealthMultiplier is ignored." }) boolean multipliedPerPlayer,
                        @Comment("Multiplier for the maximum health of the pinata.") int maxHealthMultiplier){
        }

        public record PinataCooldownSettings(
                        @Comment("Duration of the hit cooldown in seconds.") double duration,
                        @Comment("If true, the cooldown is unique to each player. If false, it's server-wide.") boolean perPlayer) {
        }

        public record PinataAISettings(
                        @Comment( {
                                        "Whether the pinata AI is enabled.",
                                        "If true, the pinata moves around. Otherwise, it remains stationary." }) boolean enabled,
                        @Comment("Speed of the pinata's movement.") double movementSpeed){
        }

        public record PinataEffectSettings(
                        @Comment("Whether the pinata should have a glowing outline.") boolean glowing,
                        @Comment("Whether to play a sound when the pinata is hit.") String hitSound,
                        @Comment("Volume of the hit sound.") float hitSoundVolume,
                        @Comment("Pitch of the hit sound.") float hitSoundPitch,
                        @Comment("Particle type to spawn on hit.") String hitParticle,
                        @Comment("Number of particles to spawn on hit.") int hitParticleCount,
                        @Comment("Sound to play when the pinata spawns.") String spawnSound,
                        @Comment("Volume of the spawn sound.") float spawnSoundVolume,
                        @Comment("Pitch of the spawn sound.") float spawnSoundPitch,
                        @Comment("Particle type to spawn on hit.") String spawnParticle,
                        @Comment("Number of particles to spawn on hit.") int spawnParticleCount,
                        @Comment("Sound to play when the pinata dies.") String deathSound,
                        @Comment("Volume of the death sound.") float deathSoundVolume,
                        @Comment("Pitch of the death sound.") float deathSoundPitch,
                        @Comment("Particle type to spawn on death.") String deathParticle,
                        @Comment("Particle count to spawn on death.") int deathParticleCount) {
        }

        public record PinataCommands(
                        @Comment("Commands to execute for when the pinata spawns.") Map<String, Reward> spawn,
                        @Comment("Commands to execute for when the pinata is hit.") Map<String, Reward> hit,
                        @Comment("Commands to execute for when the pinata is last hit.") Map<String, Reward> lastHit,
                        @Comment("Commands to execute for when the pinata dies.") Map<String, Reward> death) {
        }

        public record PinataSettings(
                        PinataAppearanceSettings appearance,
                        PinataDisplaySettings display,
                        PinataHealthSettings health,
                        PinataCooldownSettings cooldown,
                        PinataAISettings ai,
                        PinataEffectSettings effects,
                        @Comment("Countdown time before the pinata spawns.") double countdown,
                        @Comment("Timeout duration in seconds for the pinata.") int timeout,
                        @Comment("Locations where pinatas can spawn.") Map<String, SerializableLocation> spawnLocations,
                        @Comment("Commands to execute on various pinata events.") PinataCommands commands) {
        }

        @Configuration
        public static class MainConfiguration {
                @Comment("Enable debug mode for more verbose logging.")
                public boolean debug = false;

                @Comment("Settings for the pinata module.")
                public PinataSettings pinata = new PinataSettings(
                                new PinataAppearanceSettings(List.of("LLAMA"), "<green>Pinata", 1.5),
                                new PinataDisplaySettings(true, true, "YELLOW", "GREEN", "PROGRESS", "PROGRESS"),
                                new PinataHealthSettings(100, true, 1),
                                new PinataCooldownSettings(1.0, true),
                                new PinataAISettings(true, 1.0),
                                new PinataEffectSettings(true,
                                                "entity.villager.hurt", 1.0f, 1.0f,
                                                "cloud", 10,
                                                "entity.player.levelup", 1.0f, 1.0f,
                                                "happy_villager", 10,
                                                "entity.generic.death", 1.0f, 1.0f,
                                                "explosion_huge", 20),
                                10.0,
                                300,
                                new HashMap<>(
                                                Map.of("spawn", new SerializableLocation(
                                                                new Location(Bukkit.getWorlds().get(0), 0, 100, 0)))),
                                new PinataCommands(
                                                new HashMap<>(
                                                                Map.of("1",
                                                                                new Reward(100.0, false, false, false,
                                                                                                null,
                                                                                                List.of("broadcast <green>A pinata has appeared!")))),
                                                new HashMap<>(Map.of("1",
                                                                new Reward(100.0, List.of(
                                                                                "msg {player} <yellow>You hit it!")))),
                                                new HashMap<>(Map.of("1",
                                                                new Reward(100.0, List.of(
                                                                                "msg {player} <gold>FINAL BLOW!")))),
                                                new HashMap<>(Map.of("1", new Reward(100.0,
                                                                List.of("give {player} diamond 1"))))));
        }
}
