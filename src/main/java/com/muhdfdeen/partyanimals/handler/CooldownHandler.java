package com.muhdfdeen.partyanimals.handler;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.config.ConfigManager;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

public class CooldownHandler {
    private final PartyAnimals plugin;
    private final ConfigManager config;
    private final MessageHandler messageHandler;

    public CooldownHandler(PartyAnimals plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.messageHandler = plugin.getMessageHandler();
    }

    public boolean isOnCooldown(Player player, LivingEntity pinata) {
        var cooldownConfig = config.getPinataConfig().timer.hitCooldown();
        double cooldownSeconds = cooldownConfig.duration();
        if (cooldownSeconds <= 0) return false;

        boolean serverwide = cooldownConfig.serverwide();
        long now = System.currentTimeMillis();

        PersistentDataHolder target = serverwide ? pinata : player;
        NamespacedKey key = plugin.getPinataManager().getCooldownKey();

        long nextHit = target.getPersistentDataContainer().getOrDefault(key, PersistentDataType.LONG, 0L);

        if (now < nextHit) {
            sendCooldownMessage(player, nextHit - now, serverwide);
            return true;
        }

        return false;
    }

    public void applyCooldown(Player player, LivingEntity pinata) {
        var cooldownConfig = config.getPinataConfig().timer.hitCooldown();
        double cooldownSeconds = cooldownConfig.duration();
        if (cooldownSeconds <= 0) return;

        boolean serverwide = cooldownConfig.serverwide();
        long cooldownMillis = (long) (cooldownSeconds * 1000L);
        long now = System.currentTimeMillis();

        PersistentDataHolder target = serverwide ? pinata : player;
        NamespacedKey key = plugin.getPinataManager().getCooldownKey();

        target.getPersistentDataContainer().set(key, PersistentDataType.LONG, now + cooldownMillis);
    }

    private void sendCooldownMessage(Player player, long remainingMillis, boolean serverwide) {
        String msg = config.getMessageConfig().pinata.hitCooldown();
        if (msg == null || msg.isEmpty()) return;

        double remainingSeconds = remainingMillis / 1000.0;

        var component = messageHandler.parse(player, msg, messageHandler.tag("seconds", String.format("%.1f", remainingSeconds))
        );

        String displayType = config.getPinataConfig().timer.hitCooldown().type();

        switch (displayType.toLowerCase()) {
            case "action_bar", "actionbar" -> player.sendActionBar(component);
            default -> player.sendMessage(component);
        }
    }
}
