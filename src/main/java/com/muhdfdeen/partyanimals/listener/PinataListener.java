package com.muhdfdeen.partyanimals.listener;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.config.ConfigManager;
import com.muhdfdeen.partyanimals.handler.RewardHandler;
import com.muhdfdeen.partyanimals.handler.HitCooldownHandler;
import com.muhdfdeen.partyanimals.handler.EffectHandler;
import com.muhdfdeen.partyanimals.handler.MessageHandler;
import com.muhdfdeen.partyanimals.manager.BossBarManager;
import com.muhdfdeen.partyanimals.manager.PinataManager;
import com.muhdfdeen.partyanimals.util.Logger;

public class PinataListener implements Listener {
    private final PartyAnimals plugin;
    private final Logger log;
    private final ConfigManager config;
    private final PinataManager pinataManager;
    private final BossBarManager bossBarManager;
    private final HitCooldownHandler hitCooldownHandler;
    private final EffectHandler effectHandler;
    private final RewardHandler rewardHandler;
    private final MessageHandler messageHandler;

    public PinataListener(PartyAnimals plugin) {
        this.plugin = plugin;
        this.log = plugin.getPluginLogger();
        this.config = plugin.getConfiguration();
        this.pinataManager = plugin.getPinataManager();
        this.bossBarManager = plugin.getBossBarManager();
        this.hitCooldownHandler = plugin.getHitCooldownHandler();
        this.effectHandler = plugin.getEffectHandler();
        this.rewardHandler = plugin.getRewardHandler();
        this.messageHandler = plugin.getMessageHandler();
    }

    @EventHandler
    public void onPinataLoad(EntityAddToWorldEvent event) {
        if (!(event.getEntity() instanceof LivingEntity pinata)) return;
        if (!pinataManager.isPinata(pinata)) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (pinata.isValid()) {
                log.debug("Restoring pinata state: " + pinata.getUniqueId());
                pinataManager.restorePinata(pinata);
            }
        });
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
        if (plugin.getPinataManager() == null) return;

        if (!(event.getEntity() instanceof LivingEntity pinata) || !pinataManager.isPinata(pinata))
            return;

        if (!(event.getDamager() instanceof Player player)) {
            log.debug("Non-player entity attempted to damage pinata: " + pinata);
            event.setCancelled(true);
            return;
        }

        if (!checkPermission(player)) {
            log.debug("Player " + player.getName() + " does not have permission to hit pinatas.");
            event.setCancelled(true);
            return;
        }

        var whitelist = config.getPinataConfig().interaction.whitelist();
        if (whitelist.enabled() && whitelist.materialNames() != null && !whitelist.materialNames().isEmpty()) {
            String heldItem = player.getInventory().getItemInMainHand().getType().name();
            
            if (!whitelist.materialNames().contains(heldItem)) {
                String whitelistMessage = config.getMessageConfig().pinata.hitWrongItem();
                if (whitelistMessage != null && !whitelistMessage.isEmpty()) {
                    messageHandler.send(player, whitelistMessage, messageHandler.tag("item", heldItem));
                }
                event.setCancelled(true);
                return;
            }
        }

        if (hitCooldownHandler.isOnCooldown(player, pinata)) {
            log.debug("Player " + player.getName() + " attempted to hit pinata " + pinata + " but is on cooldown.");
            event.setCancelled(true);
            return;
        }

        hitCooldownHandler.applyCooldown(player, pinata);
        event.setCancelled(true);

        int currentHits = pinata.getPersistentDataContainer().getOrDefault(pinataManager.getHealthKey(), PersistentDataType.INTEGER, 1);
        currentHits--;

        log.debug("Pinata " + pinata + " (UUID: " + pinata.getUniqueId() + ") hit by player " + player.getName() + ". Remaining hits: " + currentHits);
        
        if (currentHits <= 0) {
            handlePinataDeath(pinata, player);
        } else {
            pinata.getPersistentDataContainer().set(pinataManager.getHealthKey(), PersistentDataType.INTEGER, currentHits);
            effectHandler.playEffects(config.getPinataConfig().events.hit().effects(), pinata.getLocation(), false);
            
            if (config.getPinataConfig().appearance.damageFlash()) {
                pinata.playHurtAnimation(0);
            }
            
            log.debug("Processing hit commands for player: " + player.getName());
            rewardHandler.process(player, config.getPinataConfig().events.hit().rewards());
            
            String hitMessage = config.getMessageConfig().pinata.hitSuccess();
            if (hitMessage != null && !hitMessage.isEmpty())
                messageHandler.send(player, hitMessage);
            
            NamespacedKey maxHealthKey = pinataManager.getMaxHealthKey();
            int actualMaxHealth = pinata.getPersistentDataContainer().getOrDefault(maxHealthKey, PersistentDataType.INTEGER, currentHits);
            
            bossBarManager.updateBossBar(pinata, currentHits, actualMaxHealth, pinataManager.getSpawnTimeKey());
        }
    }

    @EventHandler
    public void onPinataDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity pinata) || !pinataManager.isPinata(pinata))
            return;
        
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK && event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) {
            log.debug("Pinata " + pinata + " (UUID: " + pinata.getUniqueId() + ") attempted to take non-player damage: " + event.getCause());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!config.getPinataConfig().health.bar().enabled()) return;
        Player player = event.getPlayer();
        bossBarManager.getBossBars().values().forEach(player::showBossBar);
    }

    private boolean checkPermission(Player player) {
        String permission = config.getPinataConfig().interaction.permission();
        if (permission == null || permission.isEmpty())
            return true;
        if (!player.hasPermission(permission)) {
            String noPermission = config.getMessageConfig().pinata.hitNoPermission();
            if (noPermission != null && !noPermission.isEmpty()) {
                messageHandler.send(player, noPermission);
            }
            return false;
        }
        return true;
    }

    private void handlePinataDeath(LivingEntity pinata, Player player) {
        log.debug("Handling pinata death for pinata: " + pinata + " (UUID: " + pinata.getUniqueId() + ") by player: " + player.getName());
        
        effectHandler.playEffects(config.getPinataConfig().events.death().effects(), pinata.getLocation(), false);

        log.debug("Processing last hit commands...");
        rewardHandler.process(player, config.getPinataConfig().events.lastHit().rewards());
        
        String lastHitMessage = config.getMessageConfig().pinata.lastHit();
        messageHandler.send(player, lastHitMessage, messageHandler.tag("player", player.getName())); 

        log.debug("Processing death commands...");
        rewardHandler.process(player, config.getPinataConfig().events.death().rewards());
        
        String downedMessage = config.getMessageConfig().pinata.defeated();
        messageHandler.send(plugin.getServer(), downedMessage, messageHandler.tag("player", player.getName()));

        pinataManager.removeActiveBossBar(pinata);
        pinata.remove();
    }
}
