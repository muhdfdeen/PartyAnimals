package org.maboroshi.partyanimals.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.handler.MessageHandler;

public class UpdateChecker implements Listener {
    private final PartyAnimals plugin;
    private final ConfigManager config;
    private final MessageHandler messageHandler;
    private boolean updateAvailable = false;
    private String latestVersion = "";

    public UpdateChecker(PartyAnimals plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.messageHandler = plugin.getMessageHandler();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (updateAvailable && player.hasPermission("partyanimals.admin")) {
            plugin.getPluginLogger().debug("Notifying " + player.getName() + " about available update.");
            messageHandler.send(player, config.getMessageConfig().general.updateAvailable, messageHandler.tag("current-version", plugin.getPluginMeta().getVersion()), messageHandler.tag("latest-version", latestVersion));
        }
    }

    public void checkForUpdates() {
        CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.github.com/repos/MaboroshiKobo/PartyAnimals/releases/latest"))
                        .build();
                HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new RuntimeException("GitHub returned code " + response.statusCode());
                }
                return response.body();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(jsonResponse -> {
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
            if (json.has("tag_name")) {
                String tagName = json.get("tag_name").getAsString().replace("v", "");
                if (isNewer(plugin.getPluginMeta().getVersion(), tagName)) {
                    this.updateAvailable = true;
                    this.latestVersion = tagName;
                }
            }
        }).exceptionally(exception -> {
            plugin.getPluginLogger().warn("Update check failed: " + exception.getMessage());
            return null;
        });
    }

    private boolean isNewer(String current, String remote) {
        current = current.replace("v", "").split("-")[0];
        remote = remote.replace("v", "").split("-")[0];
        String[] currentParts = current.split("\\.");
        String[] remoteParts = remote.split("\\.");

        int length = Math.max(currentParts.length, remoteParts.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int v2 = i < remoteParts.length ? Integer.parseInt(remoteParts[i]) : 0;
            if (v2 > v1) {
                return true;
            }
            if (v2 < v1) {
                return false;
            }
        }
        return false;
    }
}
