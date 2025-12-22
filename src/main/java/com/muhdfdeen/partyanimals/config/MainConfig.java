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
                @Comment("Whether the command is triggered server-wide or per-player.")
                public Boolean serverwide = null;
                @Comment("Whether to skip the rest of the commands if this one is given.")
                public Boolean skipRest = null;
                @Comment("Whether to randomize the command execution.")
                public Boolean randomize = null;
                @Comment("Permission required to receive this reward.")
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
                                        "If multiple types are provided, one will be chosen at random.",
                                        "",
                                        "Available types: https://jd.papermc.io/paper/1.21.1/org/bukkit/entity/EntityType.html"
                        }) List<String> types,
                        @Comment("Name of the pinata entity.") String name,
                        @Comment("Scale of the pinata entity.") double scale){
        }

        public record PinataDisplaySettings(
                        @Comment("Whether the countdown boss bar is enabled.") boolean showCountdownBar,
                        @Comment("The color of the countdown boss bar.") String countdownBarColor,
                        @Comment("The overlay of the countdown boss bar.") String countdownBarOverlay,
                        @Comment("Whether the active health boss bar is enabled.") boolean showHealthBar,
                        @Comment("The color of the health boss bar.") String healthBarColor,
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
                        @Comment("The knockback resistance of the pinata.") double knockbackResistance,
                        @Comment("Multiplier for the pinata's movement speed.") double movementSpeedMultiplier){
        }

        public record PinataEffectSettings(
                        @Comment("Whether the pinata should have a glowing outline.") boolean glowing,
                        @Comment("The color of the glowing outline.") String glowColor,
                        @Comment("Whether the pinata should flash red when hit.") boolean damageFlash,
                        @Comment("Effects triggered when the pinata is hit.") VisualAudioEffect hit,
                        @Comment("Effects triggered when the pinata spawns.") VisualAudioEffect spawn,
                        @Comment("Effects triggered when the pinata dies.") VisualAudioEffect death) {
        }

        public record SoundEffect(
                        String type,
                        float volume,
                        float pitch) {
        }

        public record ParticleEffect(
                        String type,
                        int count) {
        }

        public record VisualAudioEffect(
                        SoundEffect sound,
                        ParticleEffect particle) {
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

                @Comment("Settings related to pinatas.")
                public PinataSettings pinata = new PinataSettings(
                                new PinataAppearanceSettings(List.of("LLAMA"), "<green>Pinata", 1.5),
                                new PinataDisplaySettings(true, "GREEN", "PROGRESS", true, "RED", "PROGRESS"),
                                new PinataHealthSettings(10, true, 1),
                                new PinataCooldownSettings(0.75, true),
                                new PinataAISettings(true, 1.0, 1.5),
                                new PinataEffectSettings(
                                                true,
                                                "WHITE",
                                                false,
                                                new VisualAudioEffect(
                                                                new SoundEffect("entity.player.hurt", 1.0f, 1.0f),
                                                                new ParticleEffect("HIT_PARTICLE", 10)),
                                                new VisualAudioEffect(
                                                                new SoundEffect("entity.villager.yes", 1.0f, 1.0f),
                                                                new ParticleEffect("SPAWN_PARTICLE", 20)),
                                                new VisualAudioEffect(
                                                                new SoundEffect("entity.generic.death", 1.0f, 1.0f),
                                                                new ParticleEffect("DEATH_PARTICLE", 30))),
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
