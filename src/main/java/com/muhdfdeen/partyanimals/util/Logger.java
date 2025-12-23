package com.muhdfdeen.partyanimals.util;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.config.ConfigManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

public class Logger {
    private final ConfigManager config;

    public Logger(PartyAnimals plugin) {
        this.config = plugin.getConfiguration();
    }

    private void log(String colorTag, String message) {
        String prefix;
        if (config != null && config.getMessageConfig() != null)
            prefix = config.getMessageConfig().prefix;
        else
            prefix = "<color:#51CF66><bold>Party Animals</bold> ➟ </color>";
        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize(prefix + colorTag + message));
    }

    public void debug(String message) {
        if (config != null && config.getMainConfig().debug)
            log("<gray>[DEBUG] </gray>", message);
    }

    public void info(String message) {
        log("", message);
    }

    public void warn(String message) {
        log("<yellow>", message);
    }

    public void error(String message) {
        log("<red>", message);
    }
}