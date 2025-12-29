package com.muhdfdeen.partyanimals.config.settings;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.muhdfdeen.partyanimals.config.objects.EffectTypes.*;
import com.muhdfdeen.partyanimals.config.objects.NameTagSettings;
import com.muhdfdeen.partyanimals.config.objects.NameTagSettings.*;
import com.muhdfdeen.partyanimals.config.objects.RewardAction;

import de.exlll.configlib.Comment;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;

import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay.TextAlignment;

import net.kyori.adventure.bossbar.BossBar;

public final class PinataConfig {

    public static PinataConfiguration load(File pinataFile) {
        YamlConfigurationProperties properties = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder().build();
        return YamlConfigurations.update(pinataFile.toPath(), PinataConfiguration.class, properties);
    }

    @Configuration
    public static class PinataConfiguration {
        public Appearance appearance = new Appearance(
            List.of("LLAMA", "MULE"),
            "<gradient:#FF5555:#FF55FF>🪅 <bold>Party Pinata</bold></gradient>",
            new NameTagSettings(
                true,
                "TEXT",
                List.of("<pinata>", "<health> <gray>/</gray> <max-health> <red>❤</red>", "<timer>"),
                TextAlignment.CENTER,
                new BackgroundSettings(
                    false,
                    64,
                    0,
                    0,
                    0
                ),
                new TextShadowSettings(
                    true,
                    0,
                    0
                ),
                Display.Billboard.VERTICAL,
                true,
                20,
                new TransformSettings(
                    new TranslationSettings(
                        0,
                        0.5,
                        0
                    ),
                    new NameTagSettings.ScaleSettings(
                        1,
                        1,
                        1
                    )
                )
            ), 
            new ScaleSettings(0.75, 1.25), 
            false, 
            true,
            "LIGHT_PURPLE"
        );

        public HealthSettings health = new HealthSettings(
            10,
            true,
            new BossBarSettings(true, true, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10)
        );
        
        public InteractionSettings interaction = new InteractionSettings(
            "", 
            new ItemWhitelist(false, Set.of("STICK", "BLAZE_ROD"))
        );
        
        public TimerSettings timer = new TimerSettings(
            new PhaseSettings(10,
                new BossBarSettings(true, true, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS),
                new EffectGroup(List.of(new SoundEffect("block.note_block.bit", 1f, 1f)), List.of(new ParticleEffect("FIREWORK", 5, new ParticleOffset(0.0, 0.0, 0.0), 0.0))),
                new EffectGroup(List.of(new SoundEffect("block.note_block.bit", 1f, 0.8f)), List.of(new ParticleEffect("NOTE", 5, new ParticleOffset(0.0, 0.0, 0.0), 0.0))),
                new EffectGroup(List.of(new SoundEffect("entity.firework_rocket.launch", 1f, 0.8f)), List.of(new ParticleEffect("SONIC_BOOM", 5, new ParticleOffset(0.0, 0.0, 0.0), 0.0)))
            ),
            new TimeoutSettings(true, 300),
            new HitCooldown(true, 0.75, false, "ACTION_BAR")
        );
        
        public BehaviorSettings behavior = new BehaviorSettings(
            true, 
            1.0,
            new MovementSettings(new PathfindingRange(15.0, 5.0, 15.0), 1.75)
        );
        
        public EventRegistry events = new EventRegistry(
            new GameEvent(true,
                new EffectGroup(List.of(new SoundEffect("entity.firework_rocket.twinkle", 1f, 1f)), List.of(new ParticleEffect("TOTEM_OF_UNDYING", 10, new ParticleOffset(0.5, 1.0, 0.5), 0.1))),
                new HashMap<>(Map.of("announce", new RewardAction(100.0, List.of("broadcast <green>A pinata has arrived!"))))
            ),
            new GameEvent(true,
                new EffectGroup(List.of(new SoundEffect("entity.player.attack.crit", 1f, 1f)), List.of(new ParticleEffect("CRIT", 5, new ParticleOffset(0.3, 1.0, 0.3), 0.0))),
                new HashMap<>()
            ),
            new GameEvent(true,
                new EffectGroup(List.of(new SoundEffect("ui.toast.challenge_complete", 1f, 1f)), List.of(new ParticleEffect("HEART", 20, new ParticleOffset(0.0, 0.0, 0.0), 0.0))),
                new HashMap<>(Map.of("vip_reward", new RewardAction(50.0, true, false, false, "partyanimals.vip", List.of("give {player} diamond 1"))))
            ),
            new GameEvent(true,
                new EffectGroup(List.of(new SoundEffect("entity.generic.explode", 1f, 1f)), List.of(new ParticleEffect("EXPLOSION", 5, new ParticleOffset(0.0, 0.0, 0.0), 0.0))),
                new HashMap<>(Map.of("everyone_emerald", new RewardAction(100.0, true, false, false, "", List.of("give @a emerald 5"))))
            )
        ); 
    }

    public record ScaleSettings(
        @Comment("Minimum size multiplier.") double min,
        @Comment("Maximum size multiplier.") double max
    ) {}

    public record Appearance(
        @Comment({"Entity types to use for the pinata.", "If multiple types are provided, one is chosen randomly.", "See: https://jd.papermc.io/paper/1.21.11/org/bukkit/entity/EntityType.html"}) 
        List<String> entityTypes,

        @Comment("Custom name of the pinata entity.")
        String name,
        
        @Comment("Custom name displayed above the pinata.")
        NameTagSettings nameTag,
        
        @Comment("Size randomization settings.")
        ScaleSettings scale,
        
        @Comment("Flash red when taking damage.")
        boolean damageFlash,
        
        @Comment("Show glowing outline.")
        boolean glowing,
        
        @Comment("Color of the glowing outline.")
        String glowColor
    ) {}

    public record BossBarSettings(
        @Comment("Show a boss bar for this phase.") boolean enabled,
        @Comment("If true, all players see the bar. If false, only those near the pinata.") boolean global,
        @Comment({"Bar color.", "See: https://jd.advntr.dev/api/4.25.0/net/kyori/adventure/bossbar/BossBar.Color.html"}) BossBar.Color color,
        @Comment({"Bar overlay.", "See: https://jd.advntr.dev/api/4.25.0/net/kyori/adventure/bossbar/BossBar.Overlay.html"}) BossBar.Overlay overlay
    ) {}

    public record HealthSettings(
        @Comment("Base health points.") int maxHealth,
        @Comment("If true, health is multiplied according to player count.") boolean perPlayer,
        @Comment("Health bar visual settings.") BossBarSettings bar
    ) {}

    public record ItemWhitelist(
        @Comment("Only allow specific items to deal damage.") boolean enabled,
        @Comment({"List of allowed material names.", "See: https://jd.papermc.io/paper/1.21.11/org/bukkit/Material.html"}) Set<String> materialNames
    ) {}

    public record InteractionSettings(
        @Comment("Permission required to hit the pinata.") String permission,
        @Comment("Item restriction settings.") ItemWhitelist allowedItems
    ) {}

    public record TimeoutSettings(
        @Comment("Enable despawning if not killed in time.") boolean enabled,
        @Comment("Seconds before despawning.") int duration
    ) {}

    public record HitCooldown(
        @Comment("Enable attack speed limits.") boolean enabled,
        @Comment("Seconds between hits.") double duration,
        @Comment("If true, the cooldown is global (all players share the timer).") boolean global,
        @Comment({"Feedback type.", "Options: ACTION_BAR, CHAT"}) String notificationType
    ) {}

    public record TimerSettings(
        @Comment("Countdown before the pinata spawns.") PhaseSettings countdown,
        @Comment("Maximum time to kill the pinata.") TimeoutSettings timeout,
        @Comment("Anti-spam click settings.") HitCooldown hitCooldown
    ) {}

    public record PathfindingRange(double x, double y, double z) {}

    public record MovementSettings(
        @Comment("Wandering radius.") PathfindingRange wanderRadius,
        @Comment("Movement speed multiplier.") double speed
    ) {}

    public record BehaviorSettings(
        @Comment("If false, the pinata acts like a statue.") boolean enabled,
        @Comment({"Resistance to being pushed.", "Range: 0.0 to 1.0"}) double knockbackResistance,
        @Comment("Movement logic settings.") MovementSettings movement
    ) {}

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
        @Comment("Triggered when pinata dies.") GameEvent death
    ) {}
}
