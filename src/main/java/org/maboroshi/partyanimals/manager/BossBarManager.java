package org.maboroshi.partyanimals.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;
import org.maboroshi.partyanimals.util.MessageUtils;

public class BossBarManager {
    private final MessageUtils messageUtils;

    private final Map<UUID, BossBar> pinataBossBars = new ConcurrentHashMap<>();

    private final Map<UUID, BossBar> countdownBossBars = new ConcurrentHashMap<>();
    private final Map<UUID, Location> countdownLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> countdownGlobalSettings = new ConcurrentHashMap<>();

    public BossBarManager(PartyAnimals plugin) {
        this.messageUtils = plugin.getMessageUtils();
    }

    public UUID createCountdownBossBar(Location location, PinataConfiguration pinataConfig, int totalSeconds) {
        UUID uuid = UUID.randomUUID();
        var barSettings = pinataConfig.timer.countdown.bar;
        boolean isRainbow = barSettings.color.equalsIgnoreCase("RAINBOW");

        BossBar bossBar = BossBar.bossBar(
                messageUtils.parse(null, barSettings.text, messageUtils.tag("countdown", totalSeconds)),
                1.0f,
                isRainbow ? BossBar.Color.PINK : BossBar.Color.valueOf(barSettings.color),
                barSettings.overlay);

        if (barSettings.enabled) {
            countdownBossBars.put(uuid, bossBar);
            countdownLocations.put(uuid, location);
            countdownGlobalSettings.put(uuid, barSettings.global);
            updateViewerList(bossBar, barSettings.global, location.getWorld().getName());
        }

        return uuid;
    }

    public void updateCountdownBar(
            UUID uuid, int remainingSeconds, int totalSeconds, PinataConfiguration pinataConfig, int tickCounter) {
        BossBar bossBar = countdownBossBars.get(uuid);
        if (bossBar == null) return;
        var countdownBarSettings = pinataConfig.timer.countdown.bar;

        float progress = Math.max(0.0f, Math.min(1.0f, (float) remainingSeconds / totalSeconds));
        bossBar.progress(progress);

        bossBar.name(
                messageUtils.parse(null, countdownBarSettings.text, messageUtils.tag("countdown", remainingSeconds)));

        if (countdownBarSettings.color.equalsIgnoreCase("rainbow") && tickCounter % 5 == 0) {
            int next = (bossBar.color().ordinal() + 1) % BossBar.Color.values().length;
            bossBar.color(BossBar.Color.values()[next]);
        }

        Location location = countdownLocations.get(uuid);
        if (location != null) {
            updateViewerList(
                    bossBar,
                    countdownGlobalSettings.get(uuid),
                    location.getWorld().getName());
        }
    }

    public void removeCountdownBossBar(UUID uuid) {
        BossBar bar = countdownBossBars.remove(uuid);
        countdownLocations.remove(uuid);
        countdownGlobalSettings.remove(uuid);

        if (bar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(bar);
            }
        }
    }

    public void createPinataBossBar(
            LivingEntity pinata, int health, int maxHealth, int timeout, PinataConfiguration pinataConfig) {
        String rawMsg = pinataConfig.health.bar.text;
        String timeStr = formatTime(timeout, pinataConfig);

        Component barName = messageUtils.parse(
                null,
                rawMsg,
                messageUtils.tagParsed("pinata", pinataConfig.appearance.name),
                messageUtils.tag("health", health),
                messageUtils.tag("max-health", maxHealth),
                messageUtils.tag("timer", timeStr));

        BossBar bossBar = BossBar.bossBar(
                barName, 1.0f, BossBar.Color.valueOf(pinataConfig.health.bar.color), pinataConfig.health.bar.overlay);

        pinataBossBars.put(pinata.getUniqueId(), bossBar);
        updateViewerList(
                bossBar, pinataConfig.health.bar.global, pinata.getWorld().getName());
    }

    public void updatePinataBossBar(
            LivingEntity pinata,
            int currentHealth,
            int maxHealth,
            NamespacedKey spawnTimeKey,
            PinataConfiguration pinataConfig) {

        BossBar bossBar = pinataBossBars.get(pinata.getUniqueId());

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

        bossBar.name(messageUtils.parse(
                null,
                pinataConfig.health.bar.text,
                messageUtils.tagParsed("pinata", pinataConfig.appearance.name),
                messageUtils.tag("health", currentHealth),
                messageUtils.tag("max-health", maxHealth),
                messageUtils.tag("timer", timeStr)));

        updateViewerList(
                bossBar, pinataConfig.health.bar.global, pinata.getWorld().getName());
    }

    public boolean hasPinataBossBar(UUID uuid) {
        return pinataBossBars.containsKey(uuid);
    }

    public void removePinataBossBar(UUID uuid) {
        BossBar bar = pinataBossBars.remove(uuid);
        if (bar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(bar);
            }
        }
    }

    public Map<UUID, BossBar> getPinataBossBars() {
        return pinataBossBars;
    }

    public void removeAll() {
        pinataBossBars.values().forEach(bar -> Bukkit.getOnlinePlayers().forEach(p -> p.hideBossBar(bar)));
        pinataBossBars.clear();

        countdownBossBars.values().forEach(bar -> Bukkit.getOnlinePlayers().forEach(p -> p.hideBossBar(bar)));
        countdownBossBars.clear();
        countdownLocations.clear();
        countdownGlobalSettings.clear();
    }

    public void handleJoin(Player player) {
        pinataBossBars.forEach((uuid, bar) -> {
            LivingEntity livingEntity = (LivingEntity) Bukkit.getEntity(uuid);
            if (livingEntity != null) {
                if (player.getWorld().equals(livingEntity.getWorld())) {
                    player.showBossBar(bar);
                }
            }
        });

        countdownBossBars.forEach((uuid, bar) -> {
            Location location = countdownLocations.get(uuid);
            boolean global = countdownGlobalSettings.getOrDefault(uuid, true);

            if (global || (location != null && location.getWorld().equals(player.getWorld()))) {
                player.showBossBar(bar);
            }
        });
    }

    private void updateViewerList(BossBar bar, boolean global, String worldName) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean inSameWorld = p.getWorld().getName().equals(worldName);
            if (global || inSameWorld) {
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
}
