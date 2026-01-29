package org.maboroshi.partyanimals.listener;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.api.event.pinata.PinataDeathEvent;
import org.maboroshi.partyanimals.api.event.pinata.PinataHitEvent;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.config.settings.PinataConfig.ItemWhitelist;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;
import org.maboroshi.partyanimals.handler.ActionHandler;
import org.maboroshi.partyanimals.handler.EffectHandler;
import org.maboroshi.partyanimals.handler.HitCooldownHandler;
import org.maboroshi.partyanimals.handler.ReflexHandler;
import org.maboroshi.partyanimals.manager.BossBarManager;
import org.maboroshi.partyanimals.manager.PinataManager;
import org.maboroshi.partyanimals.util.Logger;
import org.maboroshi.partyanimals.util.MessageUtils;

public class PinataListener implements Listener {
    private final PartyAnimals plugin;
    private final Logger log;
    private final ConfigManager config;
    private final MessageUtils messageUtils;
    private final PinataManager pinataManager;
    private final BossBarManager bossBarManager;
    private final HitCooldownHandler hitCooldownHandler;
    private final EffectHandler effectHandler;
    private final ActionHandler actionHandler;
    private final ReflexHandler reflexHandler;

    public PinataListener(PartyAnimals plugin) {
        this.plugin = plugin;
        this.log = plugin.getPluginLogger();
        this.config = plugin.getConfiguration();
        this.messageUtils = plugin.getMessageUtils();
        this.pinataManager = plugin.getPinataManager();
        this.bossBarManager = plugin.getBossBarManager();
        this.hitCooldownHandler = plugin.getHitCooldownHandler();
        this.effectHandler = plugin.getEffectHandler();
        this.actionHandler = plugin.getActionHandler();
        this.reflexHandler = plugin.getReflexHandler();
    }

    @EventHandler
    public void onPinataLoad(EntityAddToWorldEvent event) {
        if (!(event.getEntity() instanceof LivingEntity pinata)) return;
        if (!pinataManager.isPinata(pinata)) return;

        pinata.getScheduler()
                .run(
                        plugin,
                        (task) -> {
                            if (pinata.isValid()) {
                                log.debug("Restoring pinata state: " + pinata.getUniqueId());
                                pinataManager.activatePinata(pinata);
                            }
                        },
                        null);
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onPinataHit(EntityDamageByEntityEvent event) {
        if (plugin.getPinataManager() == null) return;

        if (!(event.getEntity() instanceof LivingEntity pinata) || !pinataManager.isPinata(pinata)) return;

        if (!(event.getDamager() instanceof Player player)) {
            log.debug("Non-player entity attempted to damage pinata: " + pinata);
            event.setCancelled(true);
            return;
        }

        PinataConfiguration pinataConfig = pinataManager.getPinataConfig(pinata);

        if (!checkPermission(player, pinataConfig)) {
            log.debug("Player " + player.getName() + " does not have permission to hit pinatas.");
            event.setCancelled(true);
            return;
        }

        if (!isItemAllowed(player, pinataConfig.interaction.allowedItems)) {
            log.debug("Player " + player.getName() + " attempted to hit pinata " + pinata + " with disallowed item.");
            event.setCancelled(true);
            return;
        }

        if (hitCooldownHandler.isOnCooldown(player, pinata)) {
            log.debug("Player " + player.getName() + " attempted to hit pinata " + pinata + " but is on cooldown.");
            event.setCancelled(true);
            return;
        }

        hitCooldownHandler.applyCooldown(player, pinata);

        reflexHandler.onDamage(pinata, player, pinataConfig);

        var hitEvent = new PinataHitEvent(pinata, player);
        plugin.getServer().getPluginManager().callEvent(hitEvent);

        if (hitEvent.isCancelled()) {
            return;
        }

        event.setCancelled(true);

        int currentHits = pinata.getPersistentDataContainer()
                .getOrDefault(pinataManager.getHealthKey(), PersistentDataType.INTEGER, 1);
        currentHits--;

        log.debug("Pinata "
                + pinata
                + " (UUID: "
                + pinata.getUniqueId()
                + ") hit by player "
                + player.getName()
                + ". Remaining hits: "
                + currentHits);

        if (currentHits <= 0) {
            handlePinataDeath(pinata, player, pinataConfig);
        } else {
            pinata.getPersistentDataContainer()
                    .set(pinataManager.getHealthKey(), PersistentDataType.INTEGER, currentHits);
            effectHandler.playEffects(pinataConfig.events.hit.effects, pinata.getLocation(), false);

            if (pinataConfig.appearance.damageFlash) {
                pinata.playHurtAnimation(0);
            }

            log.debug("Processing hit commands for player: " + player.getName());
            actionHandler.process(player, pinataConfig.events.hit.rewards.values(), cmd -> plugin.getMessageUtils()
                    .parsePinataPlaceholders(pinata, cmd));

            String hitMessage = config.getMessageConfig().pinata.gameplay.hitSuccess;
            if (hitMessage != null && !hitMessage.isEmpty()) messageUtils.send(player, hitMessage);

            NamespacedKey maxHealthKey = pinataManager.getMaxHealthKey();
            int actualMaxHealth = pinata.getPersistentDataContainer()
                    .getOrDefault(maxHealthKey, PersistentDataType.INTEGER, currentHits);

            bossBarManager.updatePinataBossBar(
                    pinata, currentHits, actualMaxHealth, pinataManager.getSpawnTimeKey(), pinataConfig);
        }
    }

    @EventHandler
    public void onPinataDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity pinata) || !pinataManager.isPinata(pinata)) return;

        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) {
            log.debug("Pinata "
                    + pinata
                    + " (UUID: "
                    + pinata.getUniqueId()
                    + ") attempted to take non-player damage: "
                    + event.getCause());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        bossBarManager.handleJoin(event.getPlayer());
    }

    private boolean checkPermission(Player player, PinataConfiguration pinataConfig) {
        String permission = pinataConfig.interaction.permission;
        if (permission == null || permission.isEmpty()) return true;
        if (!player.hasPermission(permission)) {
            String noPermission = config.getMessageConfig().pinata.gameplay.hitNoPermission;
            if (noPermission != null && !noPermission.isEmpty()) {
                messageUtils.send(player, noPermission);
            }
            return false;
        }
        return true;
    }

    private boolean isItemAllowed(Player player, ItemWhitelist allowedItems) {
        if (!allowedItems.enabled || allowedItems.materialNames == null || allowedItems.materialNames.isEmpty()) {
            return true;
        }

        Material heldMaterial = player.getInventory().getItemInMainHand().getType();
        boolean isAllowed = false;

        for (String configName : allowedItems.materialNames) {
            Material targetMaterial = Material.matchMaterial(configName);
            if (targetMaterial != null && targetMaterial == heldMaterial) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
            String allowedItemsMessage = config.getMessageConfig().pinata.gameplay.hitWrongItem;
            if (allowedItemsMessage != null && !allowedItemsMessage.isEmpty()) {
                messageUtils.send(player, allowedItemsMessage, messageUtils.tag("item", heldMaterial.name()));
            }
            return false;
        }

        return true;
    }

    private void handlePinataDeath(LivingEntity pinata, Player player, PinataConfiguration pinataConfig) {
        log.debug("Handling pinata death for pinata: "
                + pinata
                + " (UUID: "
                + pinata.getUniqueId()
                + ") by player: "
                + player.getName());

        var event = new PinataDeathEvent(pinata, player);
        plugin.getServer().getPluginManager().callEvent(event);

        effectHandler.playEffects(pinataConfig.events.death.effects, pinata.getLocation(), false);

        log.debug("Processing last hit commands...");
        actionHandler.process(player, pinataConfig.events.lastHit.rewards.values(), cmd -> plugin.getMessageUtils()
                .parsePinataPlaceholders(pinata, cmd));

        String lastHitMessage = config.getMessageConfig().pinata.gameplay.lastHit;
        messageUtils.send(player, lastHitMessage, messageUtils.tag("player", player.getName()));

        log.debug("Processing death commands...");
        actionHandler.process(player, pinataConfig.events.death.rewards.values(), cmd -> plugin.getMessageUtils()
                .parsePinataPlaceholders(pinata, cmd));

        String downedMessage = config.getMessageConfig().pinata.events.defeated;
        messageUtils.send(plugin.getServer(), downedMessage, messageUtils.tag("player", player.getName()));

        pinataManager.safelyRemovePinata(pinata);
    }
}
