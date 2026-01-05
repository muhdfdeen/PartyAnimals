package org.maboroshi.partyanimals.handler;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;

public class HitCooldownHandler {
    private final PartyAnimals plugin;
    private final ConfigManager config;
    private final MessageHandler messageHandler;

    public HitCooldownHandler(PartyAnimals plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.messageHandler = plugin.getMessageHandler();
    }

    public boolean isOnCooldown(Player player, LivingEntity pinata) {
        PinataConfiguration pinataConfig = plugin.getPinataManager().getPinataConfig(pinata);
        var cooldownConfig = pinataConfig.timer.hitCooldown;
        double cooldownSeconds = cooldownConfig.duration;
        if (cooldownSeconds <= 0) return false;

        boolean global = cooldownConfig.global;
        long now = System.currentTimeMillis();

        PersistentDataHolder target = global ? pinata : player;
        NamespacedKey key = plugin.getPinataManager().getCooldownKey();

        long nextHit = target.getPersistentDataContainer().getOrDefault(key, PersistentDataType.LONG, 0L);

        if (now < nextHit) {
            sendCooldownMessage(player, nextHit - now, pinataConfig);
            return true;
        }

        return false;
    }

    public void applyCooldown(Player player, LivingEntity pinata) {
        PinataConfiguration pinataConfig = plugin.getPinataManager().getPinataConfig(pinata);

        var cooldownConfig = pinataConfig.timer.hitCooldown;
        double cooldownSeconds = cooldownConfig.duration;
        if (cooldownSeconds <= 0) return;

        boolean global = cooldownConfig.global;
        long cooldownMillis = (long) (cooldownSeconds * 1000L);
        long now = System.currentTimeMillis();

        PersistentDataHolder target = global ? pinata : player;
        NamespacedKey key = plugin.getPinataManager().getCooldownKey();

        target.getPersistentDataContainer().set(key, PersistentDataType.LONG, now + cooldownMillis);
    }

    private void sendCooldownMessage(Player player, long remainingMillis, PinataConfiguration pinataConfig) {
        String msg = config.getMessageConfig().pinata.gameplay.hitCooldown;
        if (msg == null || msg.isEmpty()) return;

        double remainingSeconds = remainingMillis / 1000.0;

        var component = messageHandler.parse(player, msg, messageHandler.tag("countdown", String.format("%.1f", remainingSeconds)));

        String displayType = pinataConfig.timer.hitCooldown.notificationType;

        switch (displayType.toLowerCase()) {
            case "action_bar", "actionbar" -> player.sendActionBar(component);
            default -> player.sendMessage(component);
        }
    }
}
