package com.muhdfdeen.partyanimals.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Particle;
import org.bukkit.Sound;
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
        if (event.getRightClicked() instanceof LivingEntity entity) {
            if (pinataManager.isPinata(entity)) {
                log.debug("Player attempted to interact with a pinata: " + entity);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPinataHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity))
            return;
        if (!pinataManager.isPinata(entity))
            return;

        if (!(event.getDamager() instanceof Player player)) {
            log.debug("Non-player entity attempted to damage pinata: " + entity);
            event.setCancelled(true);
            return;
        }

        double cooldownSeconds = config.getMainConfig().pinata.hitCooldown();
        boolean perPlayer = config.getMainConfig().pinata.cooldownPerPlayer();
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
                long nextHit = entity.getPersistentDataContainer().getOrDefault(
                        pinataManager.getCooldownKey(), PersistentDataType.LONG, 0L);
                if (now < nextHit) {
                    log.debug("Pinata " + entity + " is on hit cooldown.");
                    event.setCancelled(true);
                    return;
                }
                entity.getPersistentDataContainer().set(
                        pinataManager.getCooldownKey(), PersistentDataType.LONG, now + cooldownMillis);
            }
        }

        event.setDamage(0);
        entity.setNoDamageTicks(0);

        int currentHits = entity.getPersistentDataContainer().getOrDefault(pinataManager.getHealthKey(),
                PersistentDataType.INTEGER, 5);
        currentHits--;
        log.debug("Pinata Health: " + currentHits);

        if (currentHits <= 0) {
            handlePinataDeath(entity, player);
        } else {
            entity.getPersistentDataContainer().set(pinataManager.getHealthKey(), PersistentDataType.INTEGER,
                    currentHits);
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.0f);
            entity.playHurtAnimation(0);
            CommandUtils.process(player, config.getMainConfig().pinata.commands().hit(), plugin);
            String hitMessage = config.getMessageConfig().messages.pinataMessages().pinataHit();
            if (hitMessage != null && !hitMessage.isEmpty()) {
                player.sendMessage(mm.deserialize(config.getMessageConfig().messages.prefix() + hitMessage));
            }
            pinataManager.updateActiveBossBar(entity, currentHits, config.getMainConfig().pinata.health().maxHealth());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        log.debug("Removing hit cooldown for player: " + event.getPlayer().getName());
        hitCooldowns.remove(event.getPlayer().getUniqueId());
    }

    private void handlePinataDeath(LivingEntity entity, Player player) {
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation().add(0, 1, 0), 5);
        CommandUtils.process(player, config.getMainConfig().pinata.commands().lastHit(), plugin);
        String lastHitMessage = config.getMessageConfig().messages.pinataMessages().pinataLastHit();
        if (lastHitMessage != null && !lastHitMessage.isEmpty()) {
            player.sendMessage(mm.deserialize(config.getMessageConfig().messages.prefix() + lastHitMessage.replace("{player}", player.getName())));
        }
        CommandUtils.process(player, config.getMainConfig().pinata.commands().death(), plugin);
        String downedMessage = config.getMessageConfig().messages.pinataMessages().pinataDowned();
        if (downedMessage != null && !downedMessage.isEmpty()) {
            player.sendMessage(mm.deserialize(config.getMessageConfig().messages.prefix() + downedMessage));
        }
        pinataManager.removeActiveBossBar(entity);
        entity.remove();
    }
}
