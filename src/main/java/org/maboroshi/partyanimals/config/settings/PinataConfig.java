package org.maboroshi.partyanimals.config.settings;

import de.exlll.configlib.Comment;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay.TextAlignment;
import org.maboroshi.partyanimals.config.objects.CommandAction;
import org.maboroshi.partyanimals.config.objects.NameTagSettings;
import org.maboroshi.partyanimals.config.objects.NameTagSettings.*;
import org.maboroshi.partyanimals.config.objects.effects.*;

public final class PinataConfig {

    public static PinataConfiguration load(File pinataFile) {
        YamlConfigurationProperties properties = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder()
                .setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
                .build();
        return YamlConfigurations.update(pinataFile.toPath(), PinataConfiguration.class, properties);
    }

    @Configuration
    public static class PinataConfiguration {
        public Appearance appearance = new Appearance();
        public HealthSettings health = new HealthSettings();
        public InteractionSettings interaction = new InteractionSettings();
        public TimerSettings timer = new TimerSettings();
        public BehaviorSettings behavior = new BehaviorSettings();
        public EventRegistry events = new EventRegistry();
    }

    @Configuration
    public static class ScaleSettings {
        @Comment("Minimum size multiplier.")
        public double min = 0.75;

        @Comment("Maximum size multiplier.")
        public double max = 1.25;

        public ScaleSettings() {}

        public ScaleSettings(double min, double max) {
            this.min = min;
            this.max = max;
        }
    }

    @Configuration
    public static class Appearance {
        @Comment({
            "Entity types to use for the pinata.",
            "If multiple types are provided, one is chosen randomly.",
            "See: https://jd.papermc.io/paper/1.21.11/org/bukkit/entity/EntityType.html"
        })
        public List<String> entityTypes = List.of("LLAMA", "MULE");

        @Comment("Custom name of the pinata entity.")
        public String name = "<gradient:#FF5555:#FF55FF>🪅 <bold>Party Pinata</bold></gradient>";

        @Comment("Custom name displayed above the pinata.")
        public NameTagSettings nameTag = new NameTagSettings(
                true,
                "TEXT",
                List.of("<pinata>", "<health> <gray>/</gray> <max-health> <red>❤</red>", "<timer>"),
                TextAlignment.CENTER,
                new BackgroundSettings(false, 64, 0, 0, 0),
                new TextShadowSettings(true, 0, 0),
                Display.Billboard.VERTICAL,
                true,
                20,
                new TransformSettings(new TranslationSettings(0, 0.5, 0), new NameTagSettings.ScaleSettings(1, 1, 1)));

        @Comment("Size randomization settings.")
        public ScaleSettings scale = new ScaleSettings(0.75, 1.25);

        @Comment("Flash red when taking damage.")
        public boolean damageFlash = false;

        @Comment("Show glowing outline.")
        public boolean glowing = true;

        @Comment("Color of the glowing outline.")
        public String glowColor = "LIGHT_PURPLE";
    }

    @Configuration
    public static class BossBarSettings {
        @Comment("Show a boss bar for this phase.")
        public boolean enabled = true;

        @Comment("If true, all players see the bar. If false, only those near the pinata.")
        public boolean global = true;

        @Comment({"Bar color.", "See: https://jd.advntr.dev/api/4.25.0/net/kyori/adventure/bossbar/BossBar.Color.html"})
        public BossBar.Color color = BossBar.Color.PURPLE;

        @Comment({
            "Bar overlay.",
            "See: https://jd.advntr.dev/api/4.25.0/net/kyori/adventure/bossbar/BossBar.Overlay.html"
        })
        public BossBar.Overlay overlay = BossBar.Overlay.PROGRESS;

        @Comment("Text displayed on the boss bar.")
        public String text = "";

        public BossBarSettings() {}

        public BossBarSettings(
                boolean enabled, boolean global, BossBar.Color color, BossBar.Overlay overlay, String text) {
            this.enabled = enabled;
            this.global = global;
            this.color = color;
            this.overlay = overlay;
            this.text = text;
        }
    }

    @Configuration
    public static class HealthSettings {
        @Comment("Base health points.")
        public int baseHealth = 5;

        @Comment("If true, health is multiplied according to player count.")
        public boolean perPlayer = true;

        @Comment("Maximum health points.")
        public int maxHealth = 250;

        @Comment("Health bar visual settings.")
        public BossBarSettings bar = new BossBarSettings(
                true,
                true,
                BossBar.Color.GREEN,
                BossBar.Overlay.NOTCHED_10,
                "<pinata> <health> <gray>/</gray> <max-health> <red>❤</red> <gray>[<timer>]</gray>");
    }

    @Configuration
    public static class ItemWhitelist {
        @Comment("Only allow specific items to deal damage.")
        public boolean enabled = false;

        @Comment({"List of allowed material names.", "See: https://jd.papermc.io/paper/1.21.11/org/bukkit/Material.html"
        })
        public Set<String> materialNames = Set.of("STICK", "BLAZE_ROD");

        public ItemWhitelist() {}

        public ItemWhitelist(boolean enabled, Set<String> materialNames) {
            this.enabled = enabled;
            this.materialNames = materialNames;
        }
    }

    @Configuration
    public static class HitCooldown {
        @Comment("Enable attack speed limits.")
        public boolean enabled = true;

        @Comment("Seconds between hits.")
        public double duration = 0.75;

        @Comment("If true, the cooldown is global (all players share the timer).")
        public boolean global = false;

        @Comment({"Feedback type.", "Options: ACTION_BAR, CHAT"})
        public String notificationType = "ACTION_BAR";

        public HitCooldown() {}

        public HitCooldown(boolean enabled, double duration, boolean global, String notificationType) {
            this.enabled = enabled;
            this.duration = duration;
            this.global = global;
            this.notificationType = notificationType;
        }
    }

    @Configuration
    public static class InteractionSettings {
        @Comment("Permission required to hit the pinata.")
        public String permission = "";

        @Comment("Item restriction settings.")
        public ItemWhitelist allowedItems = new ItemWhitelist(false, Set.of("STICK", "BLAZE_ROD"));

        @Comment("Anti-spam click settings.")
        public HitCooldown hitCooldown = new HitCooldown(true, 0.75, false, "ACTION_BAR");
    }

    @Configuration
    public static class TimeoutSettings {
        @Comment("Enable despawning if not killed in time.")
        public boolean enabled = true;

        @Comment("Seconds before despawning.")
        public int duration = 300;

        public TimeoutSettings() {}

        public TimeoutSettings(boolean enabled, int duration) {
            this.enabled = enabled;
            this.duration = duration;
        }
    }

    @Configuration
    public static class TimerSettings {
        @Comment("Countdown before the pinata spawns.")
        public PhaseSettings countdown = new PhaseSettings(
                10,
                new BossBarSettings(
                        true,
                        true,
                        BossBar.Color.YELLOW,
                        BossBar.Overlay.PROGRESS,
                        "A pinata party will begin in <white><countdown></white>. Get ready!"),
                new EffectGroup(
                        Map.of("tick", new SoundEffect("block.note_block.bit", 1f, 1f)),
                        Map.of(
                                "spawn-flare",
                                new ParticleEffect("FIREWORK", 5, new ParticleOffset(0.0, 0.0, 0.0), 0.0))),
                new EffectGroup(
                        Map.of("pulse", new SoundEffect("block.note_block.bit", 1f, 0.8f)),
                        Map.of("note-trail", new ParticleEffect("NOTE", 5, new ParticleOffset(0.0, 0.0, 0.0), 0.0))),
                new EffectGroup(
                        Map.of("launch", new SoundEffect("entity.firework_rocket.launch", 1f, 0.8f)),
                        Map.of("boom", new ParticleEffect("SONIC_BOOM", 5, new ParticleOffset(0.0, 0.0, 0.0), 0.0))));

        @Comment("Maximum time to kill the pinata.")
        public TimeoutSettings timeout = new TimeoutSettings(true, 300);
    }

    @Configuration
    public static class PathfindingRange {
        public double x = 15.0;
        public double y = 5.0;
        public double z = 15.0;

        public PathfindingRange() {}

        public PathfindingRange(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    @Configuration
    public static class MovementSettings {
        @Comment({"Active movement type.", "Options: ROAM, FLEE, BOTH, NONE"})
        public String type = "FLEE";

        @Comment("Radius for random movement.")
        public PathfindingRange radius = new PathfindingRange(15.0, 5.0, 15.0);

        @Comment("Movement speed multiplier.")
        public double speed = 1.75;

        public MovementSettings() {}

        public MovementSettings(String type, PathfindingRange radius, double speed) {
            this.type = type;
            this.radius = radius;
            this.speed = speed;
        }
    }

    @Configuration
    public static class ShockwaveReflex {
        public boolean enabled = true;

        @Comment("Chance to trigger on hit.")
        public double chance = 20.0;

        @Comment("Strength multiplier.")
        public double strength = 1.5;

        @Comment("Vertical boost applied to affected players.")
        public double verticalBoost = 0.5;

        @Comment("Radius in blocks to find players to affect.")
        public double radius = 5.0;

        @Comment("Visual/Audio effects.")
        public EffectGroup effects = new EffectGroup();

        @Comment("Commands to execute when triggered.")
        public Map<String, CommandAction> commands = new HashMap<>();
    }

    @Configuration
    public static class MorphReflex {
        public boolean enabled = true;

        @Comment("Chance to trigger on hit.")
        public double chance = 20.0;

        @Comment("Duration in ticks.")
        public int duration = 60;

        @Comment({"Toggle whether to morph into a different age or scale randomly.", "Options: AGE, SCALE"})
        public String type = "AGE";

        @Comment({
            "Scale settings.",
            "Set 'type' to SCALE to use these. If both scale values are the same, a fixed size is used."
        })
        public ScaleSettings scale = new ScaleSettings(0.5, 1.5);

        @Comment("Visual/Audio effects.")
        public EffectGroup effects = new EffectGroup();

        @Comment("Commands to execute when triggered.")
        public Map<String, CommandAction> commands = new HashMap<>();
    }

    @Configuration
    public static class BlinkReflex {
        public boolean enabled = true;

        @Comment("Chance to trigger on hit.")
        public double chance = 10.0;

        @Comment("Teleportation distance in blocks.")
        public double distance = 10.0;

        @Comment("If true, y-coordinates are ignored from the teleportation calculation.")
        public boolean ignoreYLevel = true;

        @Comment("Visual/Audio effects.")
        public EffectGroup effects = new EffectGroup();

        @Comment("Commands to execute when triggered.")
        public Map<String, CommandAction> commands = new HashMap<>();
    }

    @Configuration
    public static class LeapReflex {
        public boolean enabled = true;

        @Comment("Chance to trigger on hit.")
        public double chance = 20.0;

        @Comment("Strength multiplier.")
        public double strength = 1.0;

        @Comment("Visual/Audio effects.")
        public EffectGroup effects = new EffectGroup();

        @Comment("Commands to execute when triggered.")
        public Map<String, CommandAction> commands = new HashMap<>();
    }

    @Configuration
    public static class SugarRushReflex {
        public boolean enabled = true;

        @Comment("Chance to trigger when hit.")
        public double chance = 20.0;

        @Comment("Duration of speed burst (ticks).")
        public int duration = 40;

        @Comment("Speed level (0 = Speed I, 1 = Speed II, 2 = Speed III).")
        public int amplifier = 2;

        @Comment("Visual/Audio effects.")
        public EffectGroup effects = new EffectGroup();

        @Comment("Commands to execute when triggered.")
        public Map<String, CommandAction> commands = new HashMap<>();
    }

    @Configuration
    public static class DazzleReflex {
        public boolean enabled = true;

        @Comment("Chance to trigger when hit.")
        public double chance = 20.0;

        @Comment("Duration of blindness (ticks).")
        public int duration = 30;

        @Comment({"Visual/Audio effects.", "These apply to the players' view."})
        public EffectGroup effects = new EffectGroup(
                Map.of(),
                Map.of("blind-flash", new ParticleEffect("GLOW", 10, new ParticleOffset(0.2, 0.2, 0.2), 0.1)));

        @Comment("Commands to execute when triggered.")
        public Map<String, CommandAction> commands = new HashMap<>();
    }

    @Configuration
    public static class ReflexSettings {
        @Comment("Shockwave settings.")
        public ShockwaveReflex shockwave = new ShockwaveReflex();

        @Comment("Morph settings.")
        public MorphReflex morph = new MorphReflex();

        @Comment("Blink settings.")
        public BlinkReflex blink = new BlinkReflex();

        @Comment("Leap settings.")
        public LeapReflex leap = new LeapReflex();

        @Comment("Sugar Rush settings.")
        public SugarRushReflex sugarRush = new SugarRushReflex();

        @Comment("Dazzle settings.")
        public DazzleReflex dazzle = new DazzleReflex();
    }

    @Configuration
    public static class BehaviorSettings {
        @Comment("If false, the pinata acts like a statue.")
        public boolean enabled = true;

        @Comment({"Resistance to being pushed.", "Range: 0.0 to 1.0"})
        public double knockbackResistance = 1.0;

        @Comment("Movement logic settings.")
        public MovementSettings movement = new MovementSettings("FLEE", new PathfindingRange(15.0, 5.0, 15.0), 1.75);

        @Comment("Defensive reactions to being attacked or stuck.")
        public ReflexSettings reflexes = new ReflexSettings();
    }

    @Configuration
    public static class PhaseSettings {
        @Comment("Duration in seconds.")
        public int duration = 10;

        @Comment("Boss bar for this phase.")
        public BossBarSettings bar = new BossBarSettings();

        @Comment("Effects at start of phase.")
        public EffectGroup start = new EffectGroup();

        @Comment("Effects during phase.")
        public EffectGroup mid = new EffectGroup();

        @Comment("Effects at end of phase.")
        public EffectGroup end = new EffectGroup();

        public PhaseSettings() {}

        public PhaseSettings(int duration, BossBarSettings bar, EffectGroup start, EffectGroup mid, EffectGroup end) {
            this.duration = duration;
            this.bar = bar;
            this.start = start;
            this.mid = mid;
            this.end = end;
        }
    }

    @Configuration
    public static class GameEvent {
        @Comment("Enable this event.")
        public boolean enabled = true;

        @Comment("Visual/Audio effects.")
        public EffectGroup effects = new EffectGroup();

        @Comment("Rewards to give. Key is the internal ID of the reward.")
        public Map<String, CommandAction> rewards = new HashMap<>();

        public GameEvent() {}

        public GameEvent(boolean enabled, EffectGroup effects, Map<String, CommandAction> rewards) {
            this.enabled = enabled;
            this.effects = effects;
            this.rewards = rewards;
        }
    }

    @Configuration
    public static class EventRegistry {
        @Comment("Triggered when pinata spawns.")
        public GameEvent spawn = new GameEvent(
                true,
                new EffectGroup(
                        Map.of("twinkle", new SoundEffect("entity.firework_rocket.twinkle", 1f, 1f)),
                        Map.of(
                                "totem",
                                new ParticleEffect("TOTEM_OF_UNDYING", 10, new ParticleOffset(0.5, 1.0, 0.5), 0.1))),
                new HashMap<>(
                        Map.of("announce", new CommandAction(100.0, List.of("say <green>A pinata has arrived!")))));

        @Comment("Triggered when pinata is damaged.")
        public GameEvent hit = new GameEvent(
                true,
                new EffectGroup(
                        Map.of("crit-sound", new SoundEffect("entity.player.attack.crit", 1f, 1f)),
                        Map.of("crit-particle", new ParticleEffect("CRIT", 5, new ParticleOffset(0.3, 1.0, 0.3), 0.0))),
                new HashMap<>(Map.of(
                        "vip_reward",
                        new CommandAction(
                                50.0, false, false, false, "partyanimals.vip", List.of("give <player> diamond 1")))));

        @Comment("Triggered on the final killing blow.")
        public GameEvent lastHit = new GameEvent(
                false,
                new EffectGroup(
                        Map.of("challenge-complete", new SoundEffect("ui.toast.challenge_complete", 1f, 1f)),
                        Map.of("hearts", new ParticleEffect("HEART", 20, new ParticleOffset(0.0, 0.0, 0.0), 0.0))),
                new HashMap<>());

        @Comment("Triggered when pinata dies.")
        public GameEvent death = new GameEvent(
                true,
                new EffectGroup(
                        Map.of("explosion-sound", new SoundEffect("entity.generic.explode", 1f, 1f)),
                        Map.of(
                                "explosion-particle",
                                new ParticleEffect("EXPLOSION", 5, new ParticleOffset(0.0, 0.0, 0.0), 0.0))),
                new HashMap<>(Map.of(
                        "everyone_emerald",
                        new CommandAction(100.0, false, false, false, "", List.of("give @a emerald 5")))));
    }
}
