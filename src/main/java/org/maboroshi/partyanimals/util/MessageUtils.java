package org.maboroshi.partyanimals.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.maboroshi.partyanimals.config.ConfigManager;

public class MessageUtils {
    private final ConfigManager config;
    private final MiniMessage mm;
    private final boolean hasPAPI;

    public MessageUtils(ConfigManager config) {
        this.config = config;
        this.mm = MiniMessage.miniMessage();
        this.hasPAPI = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    private record PinataData(String name, String template, String variant, int health, int maxHealth, String type) {
        static PinataData from(LivingEntity entity) {
            return new PinataData(
                    entity.getPersistentDataContainer()
                            .getOrDefault(NamespacedKeys.PINATA_NAME, PersistentDataType.STRING, "Unknown"),
                    entity.getPersistentDataContainer()
                            .getOrDefault(NamespacedKeys.PINATA_TEMPLATE, PersistentDataType.STRING, "Unknown"),
                    entity.getPersistentDataContainer()
                            .getOrDefault(NamespacedKeys.PINATA_VARIANT, PersistentDataType.STRING, "Unknown"),
                    entity.getPersistentDataContainer()
                            .getOrDefault(NamespacedKeys.PINATA_HEALTH, PersistentDataType.INTEGER, 0),
                    entity.getPersistentDataContainer()
                            .getOrDefault(NamespacedKeys.PINATA_MAX_HEALTH, PersistentDataType.INTEGER, 0),
                    entity.getType().toString());
        }
    }

    public Component parse(Audience receiver, String message, TagResolver... tags) {
        if (message == null || message.isEmpty()) return Component.empty();

        String parsedMessage = message;
        if (hasPAPI && receiver instanceof Player player) {
            parsedMessage = PlaceholderAPI.setPlaceholders(player, parsedMessage);
        }

        TagResolver prefixTag = Placeholder.parsed(
                "prefix", config.getMessageConfig().prefix != null ? config.getMessageConfig().prefix : "");

        TagResolver finalResolver;
        if (receiver instanceof Player player) {
            finalResolver = TagResolver.resolver(
                    TagResolver.resolver(tags), prefixTag, Placeholder.unparsed("player", player.getName()));
        } else {
            finalResolver = TagResolver.resolver(TagResolver.resolver(tags), prefixTag);
        }

        return mm.deserialize(parsedMessage, finalResolver);
    }

    public void send(Audience receiver, String message, TagResolver... tags) {
        if (message == null || message.isEmpty()) return;
        receiver.sendMessage(parse(receiver, message, tags));
    }

    public TagResolver tag(String key, Object value) {
        return Placeholder.unparsed(key, String.valueOf(value));
    }

    public TagResolver tagParsed(String key, String value) {
        return Placeholder.parsed(key, value);
    }

    public TagResolver getPinataTags(LivingEntity pinata) {
        PinataData data = PinataData.from(pinata);
        return TagResolver.resolver(
                tagParsed("pinata_name", data.name),
                tag("pinata_health", data.health),
                tag("pinata_max_health", data.maxHealth),
                tag("pinata_template", data.template),
                tag("pinata_variant", data.variant),
                tag("pinata_type", data.type));
    }

    public String parsePinataPlaceholders(LivingEntity pinata, String string) {
        if (string == null) return null;
        PinataData data = PinataData.from(pinata);

        return string.replace("<pinata_name>", data.name)
                .replace("<pinata_template>", data.template)
                .replace("<pinata_variant>", data.variant)
                .replace("<pinata_health>", String.valueOf(data.health))
                .replace("<pinata_max_health>", String.valueOf(data.maxHealth))
                .replace("<pinata_type>", data.type);
    }
}
