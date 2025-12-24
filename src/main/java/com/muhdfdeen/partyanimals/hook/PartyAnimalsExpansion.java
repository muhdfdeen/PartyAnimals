package com.muhdfdeen.partyanimals.hook;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.manager.PinataManager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PartyAnimalsExpansion extends PlaceholderExpansion {
    private final PartyAnimals plugin;

    public PartyAnimalsExpansion(PartyAnimals plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "partyanimals";
    }

    @Override
    public String getAuthor() {
        return plugin.getPluginMeta().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        PinataManager pinataManager = plugin.getPinataManager();
        if (pinataManager == null) return null;
        if (params.startsWith("pinata_")) {
            return handlePinataPlaceholders(pinataManager, params.substring(7));
        }
        return null;
    }

    private String handlePinataPlaceholders(PinataManager pinataManager, String params) {
        switch (params) {
            case "alive":
                return String.valueOf(pinataManager.isPinataAlive());
            case "health":
                return String.valueOf(pinataManager.getCurrentHealth());
            case "max_health":
                return String.valueOf(pinataManager.getMaxHealth());
            case "count":
                return String.valueOf(pinataManager.getActivePinataCount());
            case "location":
                var loc = pinataManager.getPinataLocation();
                return loc != null ? loc.getWorld().getName() + ", " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() : "N/A";
            default:
                return null;
        }
    }
}
