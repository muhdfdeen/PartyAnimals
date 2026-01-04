package org.maboroshi.partyanimals.config.objects;

import java.util.List;

import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import de.exlll.configlib.Comment;

public record NameTagSettings(
    @Comment("Whether the name tag is enabled.")
    boolean enabled,

    @Comment("Type of display. Currently only TEXT is supported.")
    String type,

    @Comment("Lines of text. Supports MiniMessage and placeholders.")
    List<String> text,

    @Comment("Text alignment. Options: LEFT, RIGHT, CENTER.")
    TextDisplay.TextAlignment textAlignment,

    @Comment("Background settings.")
    BackgroundSettings background,

    @Comment("Shadow settings.")
    TextShadowSettings shadow,

    @Comment("How the text rotates relative to the player. Options: FIXED, VERTICAL, HORIZONTAL, CENTER.")
    Display.Billboard billboard,

    @Comment("Visible through walls.")
    boolean seeThrough,

    @Comment("How often to update placeholders (in ticks). Set to -1 to disable updates.")
    int updateTextInterval,

    @Comment("Position and Scale offsets relative to the Pinata.")
    TransformSettings transformation
) {

    public record BackgroundSettings(
        boolean enabled,
        int alpha,
        int red,
        int green,
        int blue
    ) {}

    public record TextShadowSettings(
        boolean enabled,
        float radius,
        float strength
    ) {}

    public record TransformSettings(
        TranslationSettings translation,
        ScaleSettings scale
    ) {}

    public record TranslationSettings(double x, double y, double z) {}
    public record ScaleSettings(double x, double y, double z) {}
}
