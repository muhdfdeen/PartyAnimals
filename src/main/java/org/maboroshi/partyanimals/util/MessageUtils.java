package org.maboroshi.partyanimals.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

    public Component parse(Audience receiver, String message, TagResolver... tags) {
        if (message == null || message.isEmpty()) return Component.empty();

        String parsedMessage = message;
        if (hasPAPI && receiver instanceof Player player) {
            parsedMessage = PlaceholderAPI.setPlaceholders(player, parsedMessage);
        }

        String prefix = config.getMessageConfig().prefix;
        TagResolver prefixTag = Placeholder.parsed("prefix", prefix != null ? prefix : "");

        TagResolver defaultTags = TagResolver.empty();
        if (receiver instanceof Player player) {
            defaultTags = Placeholder.unparsed("player", player.getName());
        }

        TagResolver finalResolver = TagResolver.resolver(TagResolver.resolver(tags), defaultTags, prefixTag);

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
}
