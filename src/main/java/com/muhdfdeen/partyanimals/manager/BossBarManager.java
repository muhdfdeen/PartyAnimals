package com.muhdfdeen.partyanimals.manager;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.config.ConfigManager;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
    private final MiniMessage mm;
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();

    public BossBarManager(PartyAnimals plugin) {
        this.config = plugin.getConfiguration();
        this.mm = MiniMessage.miniMessage();
    }

    public void createBossBar(LivingEntity pinata, int health, int maxHealth, int timeout) {
        String nameFormat = config.getPinataConfig().appearance.name();
        String rawMsg = config.getMessageConfig().pinata.bossBarActive();

        boolean timeoutEnabled = config.getPinataConfig().timer.timeout().enabled();
        String initialTimeoutString;

        if (timeoutEnabled) {
            int minutes = timeout / 60;
            int seconds = timeout % 60;
            initialTimeoutString = String.format("%02d:%02d", minutes, seconds);
        } else {
            initialTimeoutString = "∞";
        }

        BossBar bossBar = BossBar.bossBar(
                mm.deserialize(rawMsg
                        .replace("{pinata}", nameFormat)
                        .replace("{health}", String.valueOf(health))
                        .replace("{max_health}", String.valueOf(maxHealth))
                        .replace("{timeout}", String.valueOf(initialTimeoutString))),
                1.0f,
                BossBar.Color.valueOf(config.getPinataConfig().health.bar().color()),
                BossBar.Overlay.valueOf(config.getPinataConfig().health.bar().overlay()));

        activeBossBars.put(pinata.getUniqueId(), bossBar);

        if (config.getPinataConfig().health.bar().enabled()) {
            boolean serverwide = config.getPinataConfig().health.bar().serverwide(); 
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                boolean inSameWorld = p.getWorld().equals(pinata.getWorld());
                
                if (serverwide || inSameWorld) {
                    p.showBossBar(bossBar);
                }
            }
        }
    }
    
    public void updateBossBar(LivingEntity pinata, int currentHealth, int maxHealth, NamespacedKey spawnTimeKey) {
        BossBar bossBar = activeBossBars.get(pinata.getUniqueId());
        if (bossBar == null || !config.getPinataConfig().health.bar().enabled())
            return;

        float progress = Math.max(0.0f, (float) currentHealth / maxHealth);
        bossBar.progress(progress);

        boolean timeoutEnabled = config.getPinataConfig().timer.timeout().enabled();
        String timeoutString;

        if (timeoutEnabled) {
            long spawnTime = pinata.getPersistentDataContainer().getOrDefault(spawnTimeKey, PersistentDataType.LONG,
                    System.currentTimeMillis());
            int totalTimeoutSeconds = config.getPinataConfig().timer.timeout().duration();
            long elapsedMillis = System.currentTimeMillis() - spawnTime;
            int remainingSeconds = Math.max(0, totalTimeoutSeconds - (int) (elapsedMillis / 1000));

            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            timeoutString = String.format("%02d:%02d", minutes, seconds);
        } else {
            timeoutString = "∞";
        }

        String rawName = config.getMessageConfig().pinata.bossBarActive();
        bossBar.name(mm.deserialize(rawName
                .replace("{pinata}", config.getPinataConfig().appearance.name())
                .replace("{health}", String.valueOf(currentHealth))
                .replace("{max_health}", String.valueOf(maxHealth))
                .replace("{timeout}", timeoutString)));
        
        boolean serverwide = config.getPinataConfig().health.bar().serverwide();
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean inSameWorld = p.getWorld().equals(pinata.getWorld());
            
            if (serverwide || inSameWorld) {
                p.showBossBar(bossBar);
            } else {
                p.hideBossBar(bossBar);
            }
        }
    }

    public void removeBossBar(UUID pinataUUID) {
        BossBar bossBar = activeBossBars.remove(pinataUUID);
        if (bossBar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(bossBar);
            }
        }
    }

    public void removeAll() {
        activeBossBars.values().forEach(bar -> Bukkit.getOnlinePlayers().forEach(player -> player.hideBossBar(bar)));
        activeBossBars.clear();
    }

    public boolean hasBossBar(UUID uuid) {
        return activeBossBars.containsKey(uuid);
    }

    public Map<UUID, BossBar> getBossBars() {
        return activeBossBars;
    }
}
