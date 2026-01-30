package org.maboroshi.partyanimals.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class NamespacedKeys {
    public static NamespacedKey IS_PINATA;
    public static NamespacedKey PINATA_TEMPLATE;
    public static NamespacedKey PINATA_VARIANT;
    public static NamespacedKey PINATA_NAME;
    public static NamespacedKey PINATA_HEALTH;
    public static NamespacedKey PINATA_MAX_HEALTH;
    public static NamespacedKey PINATA_SPAWN_TIME;
    public static NamespacedKey PINATA_HIT_COOLDOWN;

    private NamespacedKeys() {}

    public static void load(Plugin plugin) {
        IS_PINATA = new NamespacedKey(plugin, "is_pinata");
        PINATA_TEMPLATE = new NamespacedKey(plugin, "pinata_template");
        PINATA_VARIANT = new NamespacedKey(plugin, "pinata_variant");
        PINATA_NAME = new NamespacedKey(plugin, "pinata_name");
        PINATA_HEALTH = new NamespacedKey(plugin, "pinata_health");
        PINATA_MAX_HEALTH = new NamespacedKey(plugin, "pinata_max_health");
        PINATA_SPAWN_TIME = new NamespacedKey(plugin, "pinata_spawn_time");
        PINATA_HIT_COOLDOWN = new NamespacedKey(plugin, "pinata_hit_cooldown");
    }
}
