package org.maboroshi.partyanimals.hook;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.manager.PinataManager;
import org.maboroshi.partyanimals.manager.DatabaseManager.TopVoter;

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
        
        if (pinataManager != null && params.startsWith("pinata_")) {
            if (params.equals("pinata_count")) {
                return String.valueOf(pinataManager.getActivePinataCount());
            }
            if (params.equals("pinata_any_alive")) {
                return String.valueOf(pinataManager.isPinataAlive());
            }

            if (params.startsWith("pinata_nearest_")) {
                if (player == null) return "";
                
                LivingEntity pinata = pinataManager.getNearestPinata(player.getLocation());
                String subParam = params.substring("pinata_nearest_".length());

                if (pinata == null) {
                    return switch (subParam) {
                        case "health", "max_health" -> "0";
                        case "alive" -> "false";
                        case "location" -> "N/A";
                        default -> null;
                    };
                }

                return switch (subParam) {
                    case "alive" -> "true";
                    case "health" -> String.valueOf(pinataManager.getPinataHealth(pinata));
                    case "max_health" -> String.valueOf(pinataManager.getPinataMaxHealth(pinata));
                    case "location" -> {
                        Location loc = pinata.getLocation();
                        yield loc.getWorld().getName() + ", " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
                    }
                    default -> null;
                };
            }
        }

        if (plugin.getLeaderboardManager() != null && params.startsWith("top_")) {
            return handleLeaderboardPlaceholders(params);
        }

        if (player != null && params.equals("votes")) {
            return String.valueOf(plugin.getDatabaseManager().getVotes(player.getUniqueId()));
        }

        return null;
    }

    private String handleLeaderboardPlaceholders(String params) {
        String[] parts = params.split("_");
        
        if (parts.length < 4) return null;

        String field = parts[parts.length - 1];
        
        String rankStr = parts[parts.length - 2];
        int rank;
        try {
            rank = Integer.parseInt(rankStr);
        } catch (NumberFormatException e) {
            return null;
        }

        StringBuilder typeBuilder = new StringBuilder();
        for (int i = 1; i < parts.length - 2; i++) {
            if (i > 1) typeBuilder.append("_");
            typeBuilder.append(parts[i]);
        }
        String type = typeBuilder.toString();

        TopVoter top = plugin.getLeaderboardManager().getTopVoter(type, rank);

        if (field.equalsIgnoreCase("name")) return top.name();
        if (field.equalsIgnoreCase("votes")) return String.valueOf(top.votes());

        return null;
    }
}
