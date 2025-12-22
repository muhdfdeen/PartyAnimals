package com.muhdfdeen.partyanimals.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.config.ConfigManager;
import com.muhdfdeen.partyanimals.manager.PinataManager;
import com.muhdfdeen.partyanimals.util.CommandUtils;
import com.muhdfdeen.partyanimals.util.Logger;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class PinataListener implements Listener {
    private final PartyAnimals plugin;
    private final Logger log;
    private final ConfigManager config;
    private final PinataManager pinataManager;
    private final MiniMessage mm;
    private final Map<UUID, Long> hitCooldowns = new HashMap<>();

    public PinataListener(PartyAnimals plugin) {
        this.plugin = plugin;
        this.log = plugin.getPluginLogger();
        this.config = plugin.getConfiguration();
        this.pinataManager = plugin.getPinataManager();
        this.mm = MiniMessage.miniMessage();
    }

    @EventHandler
    public void onPinataInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof LivingEntity pinata) {
            if (pinataManager.isPinata(pinata)) {
                log.debug("Player attempted to interact with a pinata: " + pinata);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPinataHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity pinata))
            return;
        if (!pinataManager.isPinata(pinata))
            return;

        if (!(event.getDamager() instanceof Player player)) {
            log.debug("Non-player entity attempted to damage pinata: " + pinata);
            event.setCancelled(true);
            return;
        }

        double cooldownSeconds = config.getMainConfig().pinata.cooldown().duration();
        boolean perPlayer = config.getMainConfig().pinata.cooldown().perPlayer();
        long cooldownMillis = (long) (cooldownSeconds * 1000L);
        long now = System.currentTimeMillis();

        if (cooldownMillis > 0) {
            if (perPlayer) {
                long nextHit = hitCooldowns.getOrDefault(player.getUniqueId(), 0L);
                if (now < nextHit) {
                    log.debug("Player " + player.getName() + " is on hit cooldown for pinata.");
                    event.setCancelled(true);
                    return;
                }
                hitCooldowns.put(player.getUniqueId(), now + cooldownMillis);
            } else {
                long nextHit = pinata.getPersistentDataContainer().getOrDefault(
                        pinataManager.getCooldownKey(), PersistentDataType.LONG, 0L);
                if (now < nextHit) {
                    log.debug("Pinata " + pinata + " is on hit cooldown.");
                    event.setCancelled(true);
                    return;
                }
                pinata.getPersistentDataContainer().set(
                        pinataManager.getCooldownKey(), PersistentDataType.LONG, now + cooldownMillis);
            }
        }

        event.setDamage(0);
        pinata.setNoDamageTicks(0);

        int currentHits = pinata.getPersistentDataContainer().getOrDefault(pinataManager.getHealthKey(),
                PersistentDataType.INTEGER, 5);
        currentHits--;
        log.debug("Pinata Health: " + currentHits);

        if (currentHits <= 0) {
            handlePinataDeath(pinata, player);
        } else {
            pinata.getPersistentDataContainer().set(pinataManager.getHealthKey(), PersistentDataType.INTEGER,
                    currentHits);
            pinataManager.playEffect(config.getMainConfig().pinata.effects().hit(), pinata.getLocation());
            pinata.playHurtAnimation(0);
            CommandUtils.process(player, config.getMainConfig().pinata.commands().hit(), plugin);
            String hitMessage = config.getMessageConfig().messages.pinataMessages().pinataHit();
            if (hitMessage != null && !hitMessage.isEmpty()) {
                player.sendMessage(mm.deserialize(config.getMessageConfig().messages.prefix() + hitMessage));
            }
            NamespacedKey maxHealthKey = pinataManager.getMaxHealthKey();
            int actualMaxHealth = pinata.getPersistentDataContainer().getOrDefault(maxHealthKey,
                    PersistentDataType.INTEGER, currentHits);
            pinataManager.updateActiveBossBar(pinata, currentHits, actualMaxHealth);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        log.debug("Removing hit cooldown for player: " + event.getPlayer().getName());
        hitCooldowns.remove(event.getPlayer().getUniqueId());
    }

    private void handlePinataDeath(LivingEntity pinata, Player player) {
        pinataManager.playEffect(config.getMainConfig().pinata.effects().death(), pinata.getLocation());

        CommandUtils.process(player, config.getMainConfig().pinata.commands().lastHit(), plugin);
        String lastHitMessage = config.getMessageConfig().messages.pinataMessages().pinataLastHit();
        if (lastHitMessage != null && !lastHitMessage.isEmpty()) {
            player.sendMessage(mm.deserialize(config.getMessageConfig().messages.prefix()
                    + lastHitMessage.replace("{player}", player.getName())));
        }

        CommandUtils.process(player, config.getMainConfig().pinata.commands().death(), plugin);
        String downedMessage = config.getMessageConfig().messages.pinataMessages().pinataDowned();
        if (downedMessage != null && !downedMessage.isEmpty()) {
            player.sendMessage(mm.deserialize(config.getMessageConfig().messages.prefix() + downedMessage));
        }

        pinataManager.removeActiveBossBar(pinata);
        pinata.remove();
    }
}
