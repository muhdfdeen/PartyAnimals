package org.maboroshi.partyanimals.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;
import org.maboroshi.partyanimals.handler.MessageHandler;

public class BossBarManager {

    private final ConfigManager config;
    private final MessageHandler messageHandler;
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();

    public BossBarManager(PartyAnimals plugin) {
        this.config = plugin.getConfiguration();
        this.messageHandler = plugin.getMessageHandler();
    }

    public void createBossBar(
            LivingEntity pinata, int health, int maxHealth, int timeout, PinataConfiguration pinataConfig) {
        String rawMsg = config.getMessageConfig().pinata.visuals.bossBarActive;
        String timeStr = formatTime(timeout, pinataConfig);

        Component barName = messageHandler.parse(
                null,
                rawMsg,
                messageHandler.tagParsed("pinata", pinataConfig.appearance.name),
                messageHandler.tag("health", health),
                messageHandler.tag("max-health", maxHealth),
                messageHandler.tag("timer", timeStr));

        BossBar bossBar =
                BossBar.bossBar(barName, 1.0f, pinataConfig.health.bar.color, pinataConfig.health.bar.overlay);

        activeBossBars.put(pinata.getUniqueId(), bossBar);
        updateViewerList(pinata, bossBar, pinataConfig);
    }

    public void updateBossBar(
            LivingEntity pinata,
            int currentHealth,
            int maxHealth,
            NamespacedKey spawnTimeKey,
            PinataConfiguration pinataConfig) {
        BossBar bossBar = activeBossBars.get(pinata.getUniqueId());

        if (bossBar == null || !pinataConfig.health.bar.enabled) return;

        float progress = Math.max(0.0f, Math.min(1.0f, (float) currentHealth / maxHealth));
        bossBar.progress(progress);

        String timeStr = "∞";
        if (pinataConfig.timer.timeout.enabled) {
            long spawnTime = pinata.getPersistentDataContainer()
                    .getOrDefault(spawnTimeKey, PersistentDataType.LONG, System.currentTimeMillis());
            int totalTimeout = pinataConfig.timer.timeout.duration;
            int remaining = Math.max(0, totalTimeout - (int) ((System.currentTimeMillis() - spawnTime) / 1000));
            timeStr = formatTime(remaining, pinataConfig);
        }

        bossBar.name(messageHandler.parse(
                null,
                config.getMessageConfig().pinata.visuals.bossBarActive,
                messageHandler.tagParsed("pinata", pinataConfig.appearance.name),
                messageHandler.tag("health", currentHealth),
                messageHandler.tag("max-health", maxHealth),
                messageHandler.tag("timer", timeStr)));

        updateViewerList(pinata, bossBar, pinataConfig);
    }

    private void updateViewerList(LivingEntity pinata, BossBar bar, PinataConfiguration pinataConfig) {
        boolean global = pinataConfig.health.bar.global;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (global || p.getWorld().equals(pinata.getWorld())) {
                p.showBossBar(bar);
            } else {
                p.hideBossBar(bar);
            }
        }
    }

    private String formatTime(int seconds, PinataConfiguration pinataConfig) {
        if (!pinataConfig.timer.timeout.enabled || seconds <= 0) {
            return "∞";
        }
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    public boolean hasBossBar(UUID uuid) {
        return activeBossBars.containsKey(uuid);
    }

    public void removeBossBar(UUID uuid) {
        BossBar bar = activeBossBars.remove(uuid);
        if (bar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(bar);
            }
        }
    }

    public void removeAll() {
        activeBossBars.values().forEach(bar -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(bar);
            }
        });
        activeBossBars.clear();
    }

    public Map<UUID, BossBar> getBossBars() {
        return activeBossBars;
    }
}
