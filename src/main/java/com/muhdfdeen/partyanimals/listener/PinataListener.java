package com.muhdfdeen.partyanimals.listener;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataHolder;
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

        String hitPermission = config.getPinataConfig().pinata.hitPermission();
        if (hitPermission != null && !hitPermission.isEmpty()) {
            log.debug("Checking hit permission: " + hitPermission + " for player: " + player.getName());
            if (!player.hasPermission(hitPermission)) {
                log.debug("Player " + player.getName() + " lacks permission to hit pinata.");
                String noPermissionMsg = config.getMessageConfig().messages.pinataMessages().noHitPermission();
                if (noPermissionMsg != null && !noPermissionMsg.isEmpty()) {
                    player.sendRichMessage(config.getMessageConfig().messages.prefix() + noPermissionMsg);
                }
                event.setCancelled(true);
                return;
            }
        }

        double cooldownSeconds = config.getPinataConfig().pinata.cooldown().duration();
        boolean perPlayer = config.getPinataConfig().pinata.cooldown().perPlayer();
        long cooldownMillis = (long) (cooldownSeconds * 1000L);
        long now = System.currentTimeMillis();

        if (cooldownMillis > 0) {
            PersistentDataHolder target = perPlayer ? player : pinata;
            NamespacedKey key = pinataManager.getCooldownKey();

            long nextHit = target.getPersistentDataContainer().getOrDefault(key, PersistentDataType.LONG, 0L);

            if (now < nextHit) {
                log.debug((perPlayer ? "Player " : "Pinata ") + "is on hit cooldown. Remaining time: " + (nextHit - now) + " ms");
                double remaining = (nextHit - now) / 1000.0;
                String msg = config.getMessageConfig().messages.pinataMessages().pinataHitCooldown();
                if (msg != null && !msg.isEmpty()) {
                    var component = mm.deserialize(config.getMessageConfig().messages.prefix() + msg.replace("{seconds}", String.format("%.1f", remaining)));
                    String displayType = config.getPinataConfig().pinata.cooldown().displayType();
                    switch (displayType.toLowerCase()) {
                        case "action_bar", "actionbar" -> player.sendActionBar(component);
                        case "chat" -> player.sendMessage(component);
                        default -> player.sendMessage(component);
                    }
                }
                event.setCancelled(true);
                return;
            }
            log.debug("Cooldown passed. Setting new cooldown for " + (perPlayer ? "player " + player.getName() : "pinata"));
            target.getPersistentDataContainer().set(key, PersistentDataType.LONG, now + cooldownMillis);
        }

        event.setCancelled(true);
        int currentHits = pinata.getPersistentDataContainer().getOrDefault(pinataManager.getHealthKey(), PersistentDataType.INTEGER, 1);
        currentHits--;

        log.debug("Pinata " + pinata + " (UUID: " + pinata.getUniqueId() + ") hit by player " + player.getName() + ". Remaining hits: " + currentHits);
        if (currentHits <= 0) {
            handlePinataDeath(pinata, player);
        } else {
            pinata.getPersistentDataContainer().set(pinataManager.getHealthKey(), PersistentDataType.INTEGER, currentHits);
            pinataManager.playEffect(config.getPinataConfig().pinata.effects().hit(), pinata.getLocation());
            if (config.getPinataConfig().pinata.effects().damageFlash()) {
                pinata.playHurtAnimation(0);
            }
            log.debug("Processing hit commands for player: " + player.getName());
            CommandUtils.process(player, config.getPinataConfig().pinata.commands().hit(), plugin);
            String hitMessage = config.getMessageConfig().messages.pinataMessages().pinataHit();
            if (hitMessage != null && !hitMessage.isEmpty())
                player.sendRichMessage(config.getMessageConfig().messages.prefix() + hitMessage);
            NamespacedKey maxHealthKey = pinataManager.getMaxHealthKey();
            int actualMaxHealth = pinata.getPersistentDataContainer().getOrDefault(maxHealthKey, PersistentDataType.INTEGER, currentHits);
            pinataManager.updateActiveBossBar(pinata, currentHits, actualMaxHealth, config.getPinataConfig().pinata.display().health().enabled());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        pinataManager.getActiveBossBars().values().forEach(bossBar -> player.showBossBar(bossBar));
    }

    private void handlePinataDeath(LivingEntity pinata, Player player) {
        log.debug("Handling pinata death for pinata: " + pinata + " (UUID: " + pinata.getUniqueId() + ") by player: " + player.getName());
        pinataManager.playEffect(config.getPinataConfig().pinata.effects().death(), pinata.getLocation());

        log.debug("Processing last hit commands for player: " + player.getName() + " on pinata: " + pinata + " (UUID: " + pinata.getUniqueId() + ")");
        CommandUtils.process(player, config.getPinataConfig().pinata.commands().lastHit(), plugin);
        String lastHitMessage = config.getMessageConfig().messages.pinataMessages().pinataLastHit();
        if (lastHitMessage != null && !lastHitMessage.isEmpty()) {
            player.sendMessage(mm.deserialize(config.getMessageConfig().messages.prefix()
                    + lastHitMessage.replace("{player}", player.getName())));
        }

        log.debug("Processing death commands for player: " + player.getName() + " on pinata: " + pinata + " (UUID: " + pinata.getUniqueId() + ")");
        CommandUtils.process(player, config.getPinataConfig().pinata.commands().death(), plugin);
        String downedMessage = config.getMessageConfig().messages.pinataMessages().pinataDowned();
        if (downedMessage != null && !downedMessage.isEmpty()) {
            player.sendMessage(mm.deserialize(config.getMessageConfig().messages.prefix() + downedMessage));
        }

        pinataManager.removeActiveBossBar(pinata);
        pinata.remove();
    }
}
