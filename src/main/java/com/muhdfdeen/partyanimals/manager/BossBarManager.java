package com.muhdfdeen.partyanimals.manager;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.config.ConfigManager;
import com.muhdfdeen.partyanimals.handler.MessageHandler;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarManager {

    private final ConfigManager config;
    private final MessageHandler messageHandler;
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();

    public BossBarManager(PartyAnimals plugin) {
        this.config = plugin.getConfiguration();
        this.messageHandler = plugin.getMessageHandler();
    }

    public void createBossBar(LivingEntity pinata, int health, int maxHealth, int timeout) {
        String rawMsg = config.getMessageConfig().pinata.bossBarActive();
        String timeStr = formatTime(timeout);

        Component barName = messageHandler.parse(null, rawMsg,
                messageHandler.tagParsed("pinata", config.getPinataConfig().appearance.name()),
                messageHandler.tag("health", health),
                messageHandler.tag("max-health", maxHealth),
                messageHandler.tag("timer", timeStr)
        );

        BossBar bossBar = BossBar.bossBar(
                barName,
                1.0f,
                config.getPinataConfig().health.bar().color(),
                config.getPinataConfig().health.bar().overlay()
        );

        activeBossBars.put(pinata.getUniqueId(), bossBar);
        updateViewerList(pinata, bossBar);
    }

    public void updateBossBar(LivingEntity pinata, int currentHealth, int maxHealth, NamespacedKey spawnTimeKey) {
        BossBar bossBar = activeBossBars.get(pinata.getUniqueId());
        if (bossBar == null || !config.getPinataConfig().health.bar().enabled()) return;

        float progress = Math.max(0.0f, Math.min(1.0f, (float) currentHealth / maxHealth));
        bossBar.progress(progress);

        String timeStr = "∞";
        if (config.getPinataConfig().timer.timeout().enabled()) {
            long spawnTime = pinata.getPersistentDataContainer().getOrDefault(spawnTimeKey, PersistentDataType.LONG, System.currentTimeMillis());
            int totalTimeout = config.getPinataConfig().timer.timeout().duration();
            int remaining = Math.max(0, totalTimeout - (int) ((System.currentTimeMillis() - spawnTime) / 1000));
            timeStr = formatTime(remaining);
        }

        bossBar.name(messageHandler.parse(null, config.getMessageConfig().pinata.bossBarActive(),
                messageHandler.tagParsed("pinata", config.getPinataConfig().appearance.name()),
                messageHandler.tag("health", currentHealth),
                messageHandler.tag("max-health", maxHealth),
                messageHandler.tag("timer", timeStr)
        ));

        updateViewerList(pinata, bossBar);
    }

    private void updateViewerList(LivingEntity pinata, BossBar bar) {
        boolean global = config.getPinataConfig().health.bar().global();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (global || p.getWorld().equals(pinata.getWorld())) {
                p.showBossBar(bar);
            } else {
                p.hideBossBar(bar);
            }
        }
    }

    private String formatTime(int seconds) {
        if (!config.getPinataConfig().timer.timeout().enabled() || seconds <= 0) {
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
