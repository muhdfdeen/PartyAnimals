package com.muhdfdeen.partyanimals.util;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.config.Config.MainConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

public class Logger {
    private final PartyAnimals plugin;

    public Logger(PartyAnimals plugin) {
        this.plugin = plugin;
    }

    private void log(String colorTag, String message) {
        MainConfiguration config = plugin.getConfiguration();
        Bukkit.getConsoleSender().sendMessage(
            MiniMessage.miniMessage().deserialize(config.messages.prefix() + colorTag + message)
        );
    }

    public void debug(String message) {
        MainConfiguration config = plugin.getConfiguration();
        if (config != null && config.debug) {
            log("<gray>[DEBUG] </gray>", message);
        }
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