package org.maboroshi.partyanimals.config.objects;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

@Configuration
public class NameTagSettings {

    @Comment("Whether the name tag is enabled.")
    public boolean enabled = true;

    @Comment("Type of display. Currently only TEXT is supported.")
    public String type = "TEXT";

    @Comment("Lines of text. Supports MiniMessage and placeholders.")
    public List<String> text =
            new ArrayList<>(List.of("<pinata>", "<health> <gray>/</gray> <max-health> <red>❤</red>", "<timer>"));

    @Comment("Text alignment. Options: LEFT, RIGHT, CENTER.")
    public TextDisplay.TextAlignment textAlignment = TextDisplay.TextAlignment.CENTER;

    @Comment("Background settings.")
    public BackgroundSettings background = new BackgroundSettings();

    @Comment("Shadow settings.")
    public TextShadowSettings shadow = new TextShadowSettings();

    @Comment("How the text rotates relative to the player. Options: FIXED, VERTICAL, HORIZONTAL, CENTER.")
    public Display.Billboard billboard = Display.Billboard.VERTICAL;

    @Comment("Visible through walls.")
    public boolean seeThrough = true;

    @Comment("How often to update placeholders (in ticks). Set to -1 to disable updates.")
    public int updateTextInterval = 20;

    @Comment("Position and Scale offsets relative to the Pinata.")
    public TransformSettings transformation = new TransformSettings();

    public NameTagSettings() {}

    public NameTagSettings(
            boolean enabled,
            String type,
            List<String> text,
            TextDisplay.TextAlignment textAlignment,
            BackgroundSettings background,
            TextShadowSettings shadow,
            Display.Billboard billboard,
            boolean seeThrough,
            int updateTextInterval,
            TransformSettings transformation) {
        this.enabled = enabled;
        this.type = type;
        this.text = text;
        this.textAlignment = textAlignment;
        this.background = background;
        this.shadow = shadow;
        this.billboard = billboard;
        this.seeThrough = seeThrough;
        this.updateTextInterval = updateTextInterval;
        this.transformation = transformation;
    }

    @Configuration
    public static class BackgroundSettings {
        public boolean enabled = false;
        public int alpha = 64;
        public int red = 0;
        public int green = 0;
        public int blue = 0;

        public BackgroundSettings() {}

        public BackgroundSettings(boolean enabled, int alpha, int red, int green, int blue) {
            this.enabled = enabled;
            this.alpha = alpha;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
    }

    @Configuration
    public static class TextShadowSettings {
        public boolean enabled = true;
        public float radius = 0;
        public float strength = 0;

        public TextShadowSettings() {}

        public TextShadowSettings(boolean enabled, float radius, float strength) {
            this.enabled = enabled;
            this.radius = radius;
            this.strength = strength;
        }
    }

    @Configuration
    public static class TransformSettings {
        public TranslationSettings translation = new TranslationSettings();
        public ScaleSettings scale = new ScaleSettings();

        public TransformSettings() {}

        public TransformSettings(TranslationSettings translation, ScaleSettings scale) {
            this.translation = translation;
            this.scale = scale;
        }
    }

    @Configuration
    public static class TranslationSettings {
        public double x = 0.0;
        public double y = 0.5;
        public double z = 0.0;

        public TranslationSettings() {}

        public TranslationSettings(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    @Configuration
    public static class ScaleSettings {
        public double x = 1.0;
        public double y = 1.0;
        public double z = 1.0;

        public ScaleSettings() {}

        public ScaleSettings(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
