package com.muhdfdeen.partyanimals.config.objects;

import java.util.List;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import de.exlll.configlib.Comment;

public record NameTagSettings(
    @Comment("Type of display. Currently only TEXT is supported.")
    String type,

    @Comment("Lines of text. Supports MiniMessage and placeholders.")
    List<String> text,

    @Comment("Text alignment. Options: LEFT, RIGHT, CENTER.")
    TextDisplay.TextAlignment textAlignment,

    @Comment("Background color (#AARRGGBB) or 'transparent'.")
    String backgroundColor,

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

    public record TransformSettings(
        TranslationSettings translation,
        ScaleSettings scale,
        float yaw,
        float pitch
    ) {}

    public record TextShadowSettings(
        boolean enabled,
        float radius,
        float strength
    ) {}

    public record TranslationSettings(double x, double y, double z) {}
    public record ScaleSettings(double x, double y, double z) {}
}
