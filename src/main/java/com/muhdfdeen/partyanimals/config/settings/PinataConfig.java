package com.muhdfdeen.partyanimals.config.settings;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.muhdfdeen.partyanimals.config.SerializableLocation;

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
    public static class RewardAction {
        @Comment("The chance (0-100) for this reward to trigger.")
        public double chance = 100.0;

        @Comment("If true, the command runs for every player on the server.")
        public boolean global = false;

        @Comment("If true, no further rewards in this list will be processed if this one triggers.")
        public boolean preventFurtherRewards = false;

        @Comment("If true, commands in the list are shuffled before execution.")
        public boolean randomize = false;

        @Comment("Permission node required to be eligible for this reward.")
        public String permission = "";

        @Comment("List of console commands to execute. Use {player} for the player name.")
        public List<String> commands = List.of();

        public RewardAction() {}

        public RewardAction(double chance, List<String> commands) {
            this.chance = chance;
            this.commands = commands;
        }
        
        public RewardAction(double chance, boolean global, String permission, List<String> commands) {
            this.chance = chance;
            this.global = global;
            this.permission = permission;
            this.commands = commands;
        }
    }

    public record ScaleSettings(
        @Comment("Minimum size multiplier.") double min,
        @Comment("Maximum size multiplier.") double max
    ) {}

    public record BossBarSettings(
        @Comment("Show a boss bar for this phase.") boolean enabled,
        @Comment("If true, all players see the bar. If false, only those near the pinata.") boolean global,
        @Comment("Bar color (PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE).") String color,
        @Comment("Bar style (SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20).") String style
    ) {}

    public record Appearance(
        @Comment({"Entity types to use (randomly chosen).", "See: https://jd.papermc.io/paper/1.21.1/org/bukkit/entity/EntityType.html"}) 
        List<String> entityTypes,
        
        @Comment("Display name (supports MiniMessage).") 
        String name,
        
        @Comment("Size randomization settings.") 
        ScaleSettings scale,
        
        @Comment("Flash red when taking damage.") 
        boolean damageFlash,
        
        @Comment("Show glowing spectral outline.") 
        boolean glowing,
        
        @Comment("Color of the glowing outline.") 
        String glowColor
    ) {}

    public record HealthSettings(
        @Comment("Base health points.") int maxHealth,
        @Comment("If true, health scales based on player count (maxHealth * players).") boolean perPlayer,
        @Comment("Multiplier used if perPlayer is true.") int multiplier,
        @Comment("Health bar visual settings.") BossBarSettings bar
    ) {}

    public record ItemWhitelist(
        @Comment("Only allow specific items to deal damage.") boolean enabled,
        @Comment("List of allowed material names (e.g. WOODEN_SWORD).") List<String> materialNames
    ) {}

    public record InteractionSettings(
        @Comment("Permission required to hit the pinata.") String permission,
        @Comment("Item restriction settings.") ItemWhitelist whitelist
    ) {}

    public record TimeoutSettings(
        @Comment("Enable despawning if not killed in time.") boolean enabled,
        @Comment("Seconds before despawning.") int duration
    ) {}

    public record HitCooldown(
        @Comment("Enable attack speed limits.") boolean enabled,
        @Comment("Seconds between hits.") double duration,
        @Comment("If true, the cooldown is global (all players share the timer).") boolean global,
        @Comment("Feedback type: ACTION_BAR or CHAT.") String type
    ) {}

    public record TimerSettings(
        @Comment("Countdown before the pinata spawns.") PhaseSettings countdown,
        @Comment("Maximum time to kill the pinata.") TimeoutSettings timeout,
        @Comment("Anti-spam click settings.") HitCooldown hitCooldown
    ) {}

    public record PathfindingRange(double x, double y, double z) {}

    public record MovementSettings(
        @Comment("Wandering radius.") PathfindingRange range,
        @Comment("Movement speed multiplier.") double speed
    ) {}

    public record BehaviorSettings( // Renamed from GoalSettings
        @Comment("If false, the pinata acts like a statue.") boolean enabled,
        @Comment("Resistance to being pushed (0.0 to 1.0).") double knockbackResistance,
        @Comment("Movement logic settings.") MovementSettings movement
    ) {}

    public record SoundEffect(String type, float volume, float pitch) {}
    public record ParticleEffect(String type, int count) {}
    public record EffectGroup(SoundEffect sound, ParticleEffect particle) {}

    public record PhaseSettings(
        @Comment("Duration in seconds.") int duration,
        @Comment("Boss bar for this phase.") BossBarSettings bar,
        @Comment("Effects at start of phase.") EffectGroup start,
        @Comment("Effects during phase.") EffectGroup mid,
        @Comment("Effects at end of phase.") EffectGroup end
    ) {}

    public record GameEvent(
        @Comment("Enable this event.") boolean enabled,
        @Comment("Visual/Audio effects.") EffectGroup effects,
        @Comment("Rewards to give. Key is the internal ID of the reward.") Map<String, RewardAction> rewards
    ) {}

    public record EventRegistry(
        @Comment("Triggered when pinata spawns.") GameEvent spawn,
        @Comment("Triggered when pinata is damaged.") GameEvent hit,
        @Comment("Triggered on the final killing blow.") GameEvent lastHit,
        @Comment("Triggered when pinata dies (Global rewards).") GameEvent death
    ) {}

    @Configuration
    public static class PinataConfiguration {
        
        public Appearance appearance = new Appearance(
            List.of("LLAMA", "MULE"), 
            "<gradient:#FF5555:#FF55FF><bold>Party Pinata</bold></gradient>", 
            new ScaleSettings(0.8, 1.2), 
            true, 
            true, 
            "LIGHT_PURPLE"
        );
        
        public HealthSettings health = new HealthSettings(
            50, 
            true, 
            10, 
            new BossBarSettings(true, true, "MAGENTA", "SEGMENTED_10")
        );
        
        public InteractionSettings interaction = new InteractionSettings(
            "", 
            new ItemWhitelist(false, List.of("STICK", "BLAZE_ROD"))
        );
        
        public TimerSettings timer = new TimerSettings(
            new PhaseSettings(10,
                new BossBarSettings(true, true, "YELLOW", "SOLID"),
                new EffectGroup(new SoundEffect("block.note_block.bit", 1f, 1f), new ParticleEffect("FIREWORK", 10)),
                new EffectGroup(new SoundEffect("block.note_block.bit", 1f, 1.2f), new ParticleEffect("NOTE", 5)),
                new EffectGroup(new SoundEffect("entity.firework_rocket.launch", 1f, 1f), new ParticleEffect("FLASH", 1))
            ),
            new TimeoutSettings(true, 300),
            new HitCooldown(true, 0.5, false, "ACTION_BAR")
        );
        
        public BehaviorSettings behavior = new BehaviorSettings( // Renamed from goal
            true, 
            0.5, 
            new MovementSettings(new PathfindingRange(15.0, 5.0, 15.0), 1.3)
        );
        
        @Comment("Defined spawn points.")
        public Map<String, SerializableLocation> spawnLocations = new HashMap<>(Map.of(
            "default", new SerializableLocation()
        ));
        
        public EventRegistry events = new EventRegistry(
            new GameEvent(true, 
                new EffectGroup(new SoundEffect("entity.firework_rocket.twinkle", 1f, 1f), new ParticleEffect("TOTEM_OF_UNDYING", 50)),
                new HashMap<>(Map.of("announce", new RewardAction(100.0, true, "", List.of("broadcast <green>A Pinata has arrived!"))))
            ),
            new GameEvent(true, 
                new EffectGroup(new SoundEffect("entity.player.attack.crit", 1f, 1f), new ParticleEffect("CRIT", 5)),
                new HashMap<>()
            ),
            new GameEvent(true, 
                new EffectGroup(new SoundEffect("ui.toast.challenge_complete", 1f, 1f), new ParticleEffect("HEART", 20)),
                new HashMap<>(Map.of("vip_reward", new RewardAction(50.0, false, "partyanimals.vip", List.of("give {player} diamond 1"))))
            ),
            new GameEvent(true, 
                new EffectGroup(new SoundEffect("entity.generic.explode", 1f, 1f), new ParticleEffect("EXPLOSION", 5)),
                new HashMap<>(Map.of("everyone_cash", new RewardAction(100.0, true, "", List.of("eco give * 100"))))
            )
        );
    }
}
