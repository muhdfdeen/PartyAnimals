package com.muhdfdeen.partyanimals.config;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import de.exlll.configlib.Comment;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;

public final class PinataConfig {

        public static PinataConfiguration load(File dataFolder) {
                YamlConfigurationProperties properties = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder().build();
                Path pinataFile = new File(dataFolder, "modules/pinata.yml").toPath();
                return YamlConfigurations.update(pinataFile, PinataConfiguration.class, properties);
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

                public Reward() {}

                public Reward(double chance, List<String> commands) {
                        this.chance = chance;
                        this.commands = commands;
                }

                public Reward(
                                double chance,
                                Boolean serverwide,
                                Boolean skipRest,
                                Boolean randomize,
                                String permission,
                                List<String> commands) {
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

        public record Appearance(
                        @Comment( {
                                        "List of entity types that can be used as pinatas.",
                                        "If multiple types are provided, one will be chosen at random.",
                                        " ",
                                        "Available types:"
                                                        + " https://jd.papermc.io/paper/1.21.1/org/bukkit/entity/EntityType.html"
                        }) List<String> types,
                        @Comment("Name of the pinata entity.") String name,
                        @Comment({
                                        "Scale settings of the pinata entity.",
                                        "Scale is fixed when both min and max are the same value."
                        }) ScaleSettings scale){
        }

        public record ScaleSettings(
                        @Comment("Minimum scale of the pinata entity.") double min,
                        @Comment("Maximum scale of the pinata entity.") double max) {
        }

        public record Display(
                        DisplayCountdown countdown,
                        DisplayHealth health) {
        }

        public record DisplayCountdown(
                        @Comment("Whether the countdown boss bar is enabled.") boolean enabled,
                        @Comment("The color of the countdown boss bar.") String barColor,
                        @Comment("The overlay of the countdown boss bar.") String barOverlay) {
        }

        public record DisplayHealth(
                        @Comment("Whether the active health boss bar is enabled.") boolean enabled,
                        @Comment("The color of the health boss bar.") String barColor,
                        @Comment("The overlay of the health boss bar.") String barOverlay) {
        }

        public record Health(
                        @Comment("Maximum health of the pinata.") int maxHealth,
                        @Comment( {
                                        "Whether the health is multiplied per player.",
                                        "If true, the value for maxHealthMultiplier is ignored."
                        }) boolean multipliedPerPlayer,
                        @Comment("Multiplier for the maximum health of the pinata.") int maxHealthMultiplier){
        }

        public record Cooldown(
                        @Comment("Duration of the hit cooldown in seconds.") double duration,
                        @Comment("If true, the cooldown is unique to each player. If false, it's server-wide.") boolean perPlayer,
                        @Comment( {
                                        "The type of display to use for the cooldown.",
                                        "Available values: ACTION_BAR, CHAT" }) String displayType){
        }

        public record AI(
                        @Comment( {
                                        "Whether the pinata AI is enabled.",
                                        "If true, the pinata moves around. Otherwise, it remains stationary."
                        }) boolean enabled,
                        @Comment("The knockback resistance of the pinata.") double knockbackResistance,
                        @Comment("Pathfinding settings for the pinata.") PathfindingSettings pathfinding){
        }

        public record PathfindingSettings(
                        @Comment("Range within which the pinata can pathfind.") PathfindingRange range,
                        @Comment("Movement speed multiplier for the pinata.") double movementSpeedMultiplier) {
        }

        public record PathfindingRange(double x, double y, double z) {
        }

        public record Effect(
                        @Comment("Whether the pinata should have a glowing outline.") boolean glowing,
                        @Comment("The color of the glowing outline.") String glowColor,
                        @Comment("Whether the pinata should flash red when hit.") boolean damageFlash,
                        @Comment("Effects triggered at the start of the countdown before the pinata spawns.") SoundEffect countdownStart,
                        @Comment("Effects triggered during the countdown before the pinata spawns.") SoundEffect countdownMid,
                        @Comment("Effects triggered at the end of the countdown before the pinata spawns.") SoundEffect countdownEnd,
                        @Comment("Effects triggered when the pinata is hit.") VisualAudioEffect hit,
                        @Comment("Effects triggered when the pinata spawns.") VisualAudioEffect spawn,
                        @Comment("Effects triggered when the pinata dies.") VisualAudioEffect death) {
        }

        public record SoundEffect(String type, float volume, float pitch) {
        }

        public record ParticleEffect(String type, int count) {
        }

        public record VisualAudioEffect(SoundEffect sound, ParticleEffect particle) {
        }

        public record PinataCommands(
                        @Comment("Commands to execute for when the pinata spawns.") Map<String, Reward> spawn,
                        @Comment("Commands to execute for when the pinata is hit.") Map<String, Reward> hit,
                        @Comment("Commands to execute for when the pinata is last hit.") Map<String, Reward> lastHit,
                        @Comment("Commands to execute for when the pinata dies.") Map<String, Reward> death) {
        }

        public record PinataSettings(
                        Appearance appearance,
                        Display display,
                        Health health,
                        Cooldown cooldown,
                        AI ai,
                        Effect effects,
                        @Comment("Countdown time before the pinata spawns.") double countdown,
                        @Comment("Timeout duration in seconds for the pinata.") int timeout,
                        @Comment("Permission required to hit the pinata.") String hitPermission,
                        @Comment("Locations where pinatas can spawn.") Map<String, SerializableLocation> spawnLocations,
                        @Comment("Commands to execute on various pinata events.") PinataCommands commands) {
        }

        @Configuration
        public static class PinataConfiguration {
                @Comment("Settings related to pinatas.")
                public PinataSettings pinata = new PinataSettings(
                                new Appearance(
                                                List.of("LLAMA"), "<green>Pinata", new ScaleSettings(0.5, 1.5)),
                                new Display(
                                        new DisplayCountdown(true, "GREEN", "PROGRESS"),
                                        new DisplayHealth(true, "RED", "PROGRESS")
                                ),
                                new Health(10, true, 1),
                                new Cooldown(0.75, true, "ACTION_BAR"),
                                new AI(
                                                true,
                                                1.0,
                                                new PathfindingSettings(
                                                                new PathfindingRange(10.0, 5.0, 10.0), 1.75)),
                                new Effect(
                                                true,
                                                "GREEN",
                                                false,
                                                new SoundEffect("block.note_block.bit", 1.0f, 1.0f),
                                                new SoundEffect("block.note_block.pling", 1.0f, 1.0f),
                                                new SoundEffect("entity.experience_orb.pickup", 1.0f, 1.0f),
                                                new VisualAudioEffect(
                                                                new SoundEffect("entity.player.hurt", 1.0f, 1.0f),
                                                                new ParticleEffect("CRIT", 10)),
                                                new VisualAudioEffect(
                                                                new SoundEffect("entity.villager.yes", 1.0f, 1.0f),
                                                                new ParticleEffect("ITEM_COBWEB", 20)),
                                                new VisualAudioEffect(
                                                                new SoundEffect("entity.generic.death", 1.0f, 1.0f),
                                                                new ParticleEffect("LARGE_SMOKE", 30))),
                                10.0,
                                300,
                                "",
                                new HashMap<>(
                                                Map.of(
                                                                "spawn",
                                                                new SerializableLocation(
                                                                                new Location(
                                                                                                Bukkit.getWorlds()
                                                                                                                .get(0),
                                                                                                0, 100, 0)))),
                                new PinataCommands(
                                                new HashMap<>(
                                                                Map.of(
                                                                                "1",
                                                                                new Reward(
                                                                                                100.0,
                                                                                                false,
                                                                                                false,
                                                                                                false,
                                                                                                null,
                                                                                                List.of(
                                                                                                                "broadcast <green>A pinata has"
                                                                                                                                + " appeared!")))),
                                                new HashMap<>(
                                                                Map.of(
                                                                                "1",
                                                                                new Reward(
                                                                                                100.0,
                                                                                                List.of(
                                                                                                                "msg {player} <yellow>You hit"
                                                                                                                                + " it!")))),
                                                new HashMap<>(
                                                                Map.of(
                                                                                "1",
                                                                                new Reward(
                                                                                                100.0,
                                                                                                List.of(
                                                                                                                "msg {player} <gold>FINAL"
                                                                                                                                + " BLOW!")))),
                                                new HashMap<>(
                                                                Map.of(
                                                                                "1",
                                                                                new Reward(
                                                                                                100.0,
                                                                                                List.of("give {player} diamond 1"))))));
        }
}
