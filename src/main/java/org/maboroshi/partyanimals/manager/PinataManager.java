package org.maboroshi.partyanimals.manager;

import de.tr7zw.changeme.nbtapi.NBT;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.api.event.pinata.PinataSpawnEvent;
import org.maboroshi.partyanimals.behavior.PinataFleeGoal;
import org.maboroshi.partyanimals.behavior.PinataFloatGoal;
import org.maboroshi.partyanimals.behavior.PinataRoamGoal;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataVariant;
import org.maboroshi.partyanimals.handler.ActionHandler;
import org.maboroshi.partyanimals.handler.EffectHandler;
import org.maboroshi.partyanimals.util.Logger;
import org.maboroshi.partyanimals.util.MessageUtils;
import org.maboroshi.partyanimals.util.NamespacedKeys;

public class PinataManager {
    private final PartyAnimals plugin;
    private final Logger log;
    private final ConfigManager config;
    private final BossBarManager bossBarManager;
    private final EffectHandler effectHandler;
    private final ActionHandler actionHandler;
    private final MessageUtils messageUtils;

    private final Map<UUID, LivingEntity> activePinatas = new HashMap<>();
    private final Map<UUID, ScheduledTask> timeoutTasks = new HashMap<>();
    private final Map<ScheduledTask, UUID> activeCountdowns = new ConcurrentHashMap<>();

    public PinataManager(PartyAnimals plugin) {
        this.plugin = plugin;
        this.log = plugin.getPluginLogger();
        this.config = plugin.getConfiguration();
        this.bossBarManager = plugin.getBossBarManager();
        this.effectHandler = plugin.getEffectHandler();
        this.actionHandler = plugin.getActionHandler();
        this.messageUtils = plugin.getMessageUtils();
    }

    public void startCountdown(Location location, String templateId) {
        PinataConfiguration pinataConfig = config.getPinataConfig(templateId);
        if (pinataConfig == null) {
            log.warn("Tried to start countdown for invalid pinata template: " + templateId);
            return;
        }

        double countdownSeconds = pinataConfig.timer.countdown.duration;
        if (countdownSeconds <= 0) {
            spawnPinata(location, templateId);
            return;
        }

        effectHandler.playEffects(pinataConfig.timer.countdown.start, location, true);

        int totalSeconds = (int) countdownSeconds;
        long durationMillis = (long) (countdownSeconds * 1000);
        long endTime = System.currentTimeMillis() + durationMillis;

        UUID countdownId = bossBarManager.createCountdownBossBar(location, pinataConfig, totalSeconds);

        final int[] lastSeconds = {totalSeconds};
        final int[] taskDurationTicks = {0};

        ScheduledTask scheduledTask = Bukkit.getRegionScheduler()
                .runAtFixedRate(
                        plugin,
                        location,
                        (task) -> {
                            long now = System.currentTimeMillis();
                            long remainingMilis = endTime - now;
                            int displaySeconds = (int) Math.ceil(remainingMilis / 1000.0);

                            if (remainingMilis <= 0) {
                                bossBarManager.removeCountdownBossBar(countdownId);
                                effectHandler.playEffects(pinataConfig.timer.countdown.end, location, true);
                                spawnPinata(location, templateId);
                                activeCountdowns.remove(task);
                                task.cancel();
                                return;
                            }

                            bossBarManager.updateCountdownBar(
                                    countdownId, displaySeconds, totalSeconds, pinataConfig, ++taskDurationTicks[0]);

                            if (displaySeconds != lastSeconds[0]) {
                                effectHandler.playEffects(pinataConfig.timer.countdown.mid, location, true);
                                lastSeconds[0] = displaySeconds;
                            }
                        },
                        1L,
                        1L);

        activeCountdowns.put(scheduledTask, countdownId);
    }

    public void spawnPinata(Location location, String templateId) {
        PinataConfiguration pinataConfig = config.getPinataConfig(templateId);
        if (pinataConfig == null) {
            log.error("Cannot spawn pinata! Template '" + templateId + "' not found.");
            return;
        }

        Map.Entry<String, PinataVariant> variantEntry = selectVariant(pinataConfig.appearance.variants);
        String variantId = variantEntry.getKey();
        PinataVariant variant = variantEntry.getValue();

        String chosenType;
        if (variant.types == null || variant.types.isEmpty()) {
            chosenType = "LLAMA";
        } else {
            chosenType = variant.types.get(ThreadLocalRandom.current().nextInt(variant.types.size()));
        }
        EntityType type;
        try {
            type = EntityType.valueOf(chosenType);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid entity type '" + chosenType + "' in template " + templateId + ". Defaulting to LLAMA.");
            type = EntityType.LLAMA;
        }

        double minScale = variant.scale.min;
        double maxScale = variant.scale.max;
        final double finalScale =
                (minScale >= maxScale) ? minScale : ThreadLocalRandom.current().nextDouble(minScale, maxScale);

        int baseHealth = pinataConfig.health.baseHealth;
        int calculatedHealth = pinataConfig.health.perPlayer
                ? baseHealth * Math.max(1, plugin.getServer().getOnlinePlayers().size())
                : baseHealth;
        final int finalHealth = Math.min(calculatedHealth, pinataConfig.health.maxHealth);

        Location spawnLocation = location.clone();
        spawnLocation.setPitch(0);

        final PinataVariant selectedVariant = variant;
        final String selectedVariantId = variantId;

        location.getWorld().spawn(spawnLocation, type.getEntityClass(), entity -> {
            if (entity instanceof LivingEntity livingEntity) {
                initializePinataEntity(
                        livingEntity,
                        pinataConfig,
                        selectedVariant,
                        selectedVariantId,
                        templateId,
                        finalHealth,
                        finalScale);

                var event = new PinataSpawnEvent(livingEntity, spawnLocation);
                plugin.getServer().getPluginManager().callEvent(event);

                if (!event.isCancelled()) {
                    if (pinataConfig.appearance.nameTag.enabled) spawnNameTag(livingEntity);

                    activatePinata(livingEntity);

                    effectHandler.playEffects(pinataConfig.events.spawn.effects, location, false);
                    actionHandler.process(null, pinataConfig.events.spawn.actions.values(), cmd -> {
                        if (livingEntity instanceof LivingEntity livingPinata) {
                            return messageUtils.parsePinataPlaceholders(livingPinata, cmd);
                        } else {
                            return cmd;
                        }
                    });
                } else {
                    log.debug("Pinata spawn event was cancelled by an API event; removing entity.");
                    livingEntity.remove();
                    return;
                }

                log.debug("Playing pinata spawn effect at location: "
                        + location
                        + " for entity: "
                        + livingEntity.getType()
                        + " (Template: "
                        + templateId
                        + ")");
            }
        });

        String spawnMessage = config.getMessageConfig().pinata.events.spawnedNaturally;
        messageUtils.send(
                plugin.getServer(),
                spawnMessage,
                messageUtils.tagParsed("pinata", selectedVariant.name),
                messageUtils.tagParsed(
                        "location", location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ()));
    }

    private void initializePinataEntity(
            LivingEntity livingEntity,
            PinataConfiguration pinataConfig,
            PinataVariant variant,
            String variantId,
            String templateId,
            int health,
            double scale) {
        livingEntity
                .getPersistentDataContainer()
                .set(NamespacedKeys.PINATA_TEMPLATE, PersistentDataType.STRING, templateId);
        livingEntity.getPersistentDataContainer().set(NamespacedKeys.IS_PINATA, PersistentDataType.BOOLEAN, true);
        livingEntity
                .getPersistentDataContainer()
                .set(NamespacedKeys.PINATA_NAME, PersistentDataType.STRING, variant.name);
        livingEntity
                .getPersistentDataContainer()
                .set(NamespacedKeys.PINATA_VARIANT, PersistentDataType.STRING, variantId);
        livingEntity.getPersistentDataContainer().set(NamespacedKeys.PINATA_HEALTH, PersistentDataType.INTEGER, health);
        livingEntity
                .getPersistentDataContainer()
                .set(NamespacedKeys.PINATA_MAX_HEALTH, PersistentDataType.INTEGER, health);
        livingEntity
                .getPersistentDataContainer()
                .set(NamespacedKeys.PINATA_SPAWN_TIME, PersistentDataType.LONG, System.currentTimeMillis());
        livingEntity.getAttribute(Attribute.SCALE).setBaseValue(scale);
        livingEntity.setMaximumNoDamageTicks(0);
        livingEntity.setSilent(true);
        livingEntity.setInvulnerable(false);
        livingEntity.setRemoveWhenFarAway(false);
        livingEntity.setMaximumAir(100000);
        livingEntity.setRemainingAir(100000);

        if (livingEntity instanceof Mob mob) mob.setTarget(null);

        livingEntity.setGlowing(pinataConfig.appearance.glowing);
        if (pinataConfig.appearance.glowing) {
            String colorName = pinataConfig.appearance.glowColor;
            NamedTextColor glowColor = NamedTextColor.NAMES.value(colorName.toLowerCase());
            if (glowColor != null) {
                Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
                String teamName = "PA_" + glowColor.toString().toUpperCase();
                Team team = mainBoard.getTeam(teamName);
                if (team == null) team = mainBoard.registerNewTeam(teamName);
                team.color(glowColor);
                team.addEntry(livingEntity.getUniqueId().toString());
            }
        }

        if (variant.nbt != null && !variant.nbt.isEmpty() && !variant.nbt.equals("{}")) {
            try {
                NBT.modify(livingEntity, nbt -> {
                    var customNbt = NBT.parseNBT(variant.nbt);
                    nbt.mergeCompound(customNbt);
                });
            } catch (Exception e) {
                log.warn("Failed to apply NBT to pinata variant: " + variant + ". Error: " + e.getMessage());
            }
        }
    }

    public void activatePinata(LivingEntity pinata) {
        if (pinata == null || pinata.isDead()) return;

        PinataConfiguration pinataConfig = getPinataConfig(pinata);

        activePinatas.put(pinata.getUniqueId(), pinata);
        applyPinataGoal(pinata);
        startUpdateTask(pinata);
        startTimeoutTask(pinata);

        if (pinataConfig.appearance.nameTag.enabled) {
            boolean tagFound = false;
            if (pinata.getPassengers() != null) {
                for (org.bukkit.entity.Entity passenger : pinata.getPassengers()) {
                    if (passenger instanceof TextDisplay textDisplay) {
                        startNameTagTask(pinata, textDisplay);
                        tagFound = true;
                        break;
                    }
                }
            }

            if (!tagFound) {
                spawnNameTag(pinata);
            }
        }
    }

    public void cleanup() {
        cleanup(true);
    }

    public void cleanup(boolean killEntities) {
        log.debug("Running PinataManager cleanup (Kill entities: " + killEntities + ")...");
        bossBarManager.removeAll();

        for (LivingEntity entity : List.copyOf(activePinatas.values())) {
            if (killEntities) {
                safelyRemovePinata(entity);
            }
        }

        activePinatas.clear();
        timeoutTasks.values().forEach(ScheduledTask::cancel);
        timeoutTasks.clear();

        activeCountdowns.keySet().forEach(ScheduledTask::cancel);
        activeCountdowns.clear();
    }

    public void safelyRemovePinata(LivingEntity pinata) {
        if (pinata.getPassengers() != null) {
            for (org.bukkit.entity.Entity passenger : new ArrayList<>(pinata.getPassengers())) {
                passenger.remove();
            }
        }

        removeActiveBossBar(pinata);

        if (pinata.isValid()) {
            pinata.remove();
        }
    }

    public void removeActiveBossBar(LivingEntity pinata) {
        bossBarManager.removePinataBossBar(pinata.getUniqueId());
        activePinatas.remove(pinata.getUniqueId());

        ScheduledTask task = timeoutTasks.remove(pinata.getUniqueId());
        if (task != null) task.cancel();
    }

    public void applyPinataGoal(LivingEntity pinata) {
        PinataConfiguration pinataConfig = getPinataConfig(pinata);

        if (!pinataConfig.behavior.enabled) {
            pinata.setAI(false);
            return;
        }
        pinata.setAI(true);
        if (pinata instanceof Mob mob) {
            Bukkit.getMobGoals().removeAllGoals(mob);
            Bukkit.getMobGoals().addGoal(mob, 0, new PinataFloatGoal(plugin, mob));

            String rawType = pinataConfig.behavior.movement.type;
            String activeMovementType = (rawType != null) ? rawType.toUpperCase() : "BOTH";

            if (!java.util.List.of("FLEE", "ROAM", "NONE", "BOTH").contains(activeMovementType)) {
                plugin.getPluginLogger()
                        .warn("Unknown movement type '" + activeMovementType + "' for pinata "
                                + mob.getUniqueId()
                                + ". Defaulting to BOTH.");
                activeMovementType = "BOTH";
            }

            switch (activeMovementType) {
                case "FLEE" -> Bukkit.getMobGoals().addGoal(mob, 2, new PinataFleeGoal(plugin, mob));
                case "ROAM" -> Bukkit.getMobGoals().addGoal(mob, 2, new PinataRoamGoal(plugin, mob));
                case "NONE" -> {}
                case "BOTH" -> {
                    Bukkit.getMobGoals().addGoal(mob, 2, new PinataFleeGoal(plugin, mob));
                    Bukkit.getMobGoals().addGoal(mob, 3, new PinataRoamGoal(plugin, mob));
                }
            }
        }
        var knockbackAttribute = pinata.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockbackAttribute != null) {
            knockbackAttribute.setBaseValue(pinataConfig.behavior.knockbackResistance);
        }
    }

    private void startUpdateTask(LivingEntity livingEntity) {
        PinataConfiguration pinataConfig = getPinataConfig(livingEntity);

        int currentHealth = livingEntity
                .getPersistentDataContainer()
                .getOrDefault(NamespacedKeys.PINATA_HEALTH, PersistentDataType.INTEGER, pinataConfig.health.baseHealth);
        int maxHealthVal = livingEntity
                .getPersistentDataContainer()
                .getOrDefault(NamespacedKeys.PINATA_MAX_HEALTH, PersistentDataType.INTEGER, currentHealth);
        int timeout = pinataConfig.timer.timeout.duration;

        if (!bossBarManager.hasPinataBossBar(livingEntity.getUniqueId())) {
            bossBarManager.createPinataBossBar(livingEntity, currentHealth, maxHealthVal, timeout, pinataConfig);
        }

        livingEntity
                .getScheduler()
                .runAtFixedRate(
                        plugin,
                        (task) -> {
                            if (!livingEntity.isValid()) {
                                if (livingEntity.isDead()) {
                                    removeActiveBossBar(livingEntity);
                                }
                                task.cancel();
                                return;
                            }

                            if (!bossBarManager.hasPinataBossBar(livingEntity.getUniqueId())) {
                                task.cancel();
                                return;
                            }

                            int currHealth = livingEntity
                                    .getPersistentDataContainer()
                                    .getOrDefault(NamespacedKeys.PINATA_HEALTH, PersistentDataType.INTEGER, 0);
                            int maxHealth = livingEntity
                                    .getPersistentDataContainer()
                                    .getOrDefault(NamespacedKeys.PINATA_MAX_HEALTH, PersistentDataType.INTEGER, 0);

                            bossBarManager.updatePinataBossBar(
                                    livingEntity,
                                    currHealth,
                                    maxHealth,
                                    NamespacedKeys.PINATA_SPAWN_TIME,
                                    pinataConfig);
                        },
                        () -> {},
                        20L,
                        20L);
    }

    private void startTimeoutTask(LivingEntity pinata) {
        var existing = timeoutTasks.remove(pinata.getUniqueId());
        if (existing != null) existing.cancel();

        PinataConfiguration pinataConfig = getPinataConfig(pinata);

        if (!pinataConfig.timer.timeout.enabled) return;

        long spawnTime = pinata.getPersistentDataContainer()
                .getOrDefault(NamespacedKeys.PINATA_SPAWN_TIME, PersistentDataType.LONG, 0L);
        int timeoutSeconds = pinataConfig.timer.timeout.duration;
        if (spawnTime <= 0) spawnTime = System.currentTimeMillis();

        long elapsedMillis = System.currentTimeMillis() - spawnTime;
        long remainingMillis = (timeoutSeconds * 1000L) - elapsedMillis;
        long remainingTicks = remainingMillis / 50;

        if (remainingTicks <= 0) {
            log.debug("Restoring pinata but timeout passed. Removing.");
            safelyRemovePinata(pinata);
            return;
        }

        var task = pinata.getScheduler()
                .runDelayed(
                        plugin,
                        (t) -> {
                            if (pinata.isValid() && isPinata(pinata)) {
                                safelyRemovePinata(pinata);
                                String timeoutMsg = config.getMessageConfig().pinata.events.timeout;
                                messageUtils.send(plugin.getServer(), timeoutMsg);
                            }
                            timeoutTasks.remove(pinata.getUniqueId());
                        },
                        () -> {},
                        remainingTicks);

        timeoutTasks.put(pinata.getUniqueId(), task);
    }

    private void spawnNameTag(LivingEntity pinata) {
        PinataConfiguration pinataConfig = getPinataConfig(pinata);
        Location location = pinata.getLocation();
        TextDisplay nameTag = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);

        nameTag.setPersistent(false);

        int totalSeconds = pinataConfig.timer.timeout.duration;
        String initialTimeStr = "∞";

        if (pinataConfig.timer.timeout.enabled && totalSeconds > 0) {
            initialTimeStr = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
        }

        List<String> lines = pinataConfig.appearance.nameTag.text;
        List<Component> components = new ArrayList<>();

        if (lines != null) {
            for (String line : lines) {
                components.add(messageUtils.parse(
                        null, line, messageUtils.getPinataTags(pinata), messageUtils.tag("timer", initialTimeStr)));
            }
        }

        nameTag.text(Component.join(JoinConfiguration.newlines(), components));

        nameTag.setAlignment(pinataConfig.appearance.nameTag.textAlignment);

        nameTag.setDefaultBackground(false);

        if (pinataConfig.appearance.nameTag.background.enabled) {
            nameTag.setBackgroundColor(Color.fromARGB(
                    pinataConfig.appearance.nameTag.background.alpha,
                    pinataConfig.appearance.nameTag.background.red,
                    pinataConfig.appearance.nameTag.background.green,
                    pinataConfig.appearance.nameTag.background.blue));
        } else {
            nameTag.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        }

        nameTag.setShadowed(pinataConfig.appearance.nameTag.shadow.enabled);
        nameTag.setShadowRadius(pinataConfig.appearance.nameTag.shadow.radius);
        nameTag.setShadowStrength(pinataConfig.appearance.nameTag.shadow.strength);
        nameTag.setBillboard(pinataConfig.appearance.nameTag.billboard);
        nameTag.setSeeThrough(pinataConfig.appearance.nameTag.seeThrough);
        Transformation nameTransform = nameTag.getTransformation();

        float scaleX = (float) pinataConfig.appearance.nameTag.transformation.scale.x;
        float scaleY = (float) pinataConfig.appearance.nameTag.transformation.scale.y;
        float scaleZ = (float) pinataConfig.appearance.nameTag.transformation.scale.z;

        float transX = (float) pinataConfig.appearance.nameTag.transformation.translation.x;
        float transY = (float) pinataConfig.appearance.nameTag.transformation.translation.y;
        float transZ = (float) pinataConfig.appearance.nameTag.transformation.translation.z;

        nameTransform.getTranslation().set(transX, transY, transZ);
        nameTransform.getScale().set(scaleX, scaleY, scaleZ);

        nameTag.setTransformation(nameTransform);

        pinata.addPassenger(nameTag);
    }

    private void startNameTagTask(LivingEntity livingEntity, TextDisplay nameTag) {
        PinataConfiguration pinataConfig = getPinataConfig(livingEntity);
        int interval = pinataConfig.appearance.nameTag.updateTextInterval;
        if (interval <= 0) return;

        long intervalTicks = (long) interval;
        livingEntity
                .getScheduler()
                .runAtFixedRate(
                        plugin,
                        (task) -> {
                            if (!nameTag.isValid() || !livingEntity.isValid()) {
                                task.cancel();
                                if (nameTag.isValid()) nameTag.remove();
                                return;
                            }

                            String timeStr = "∞";
                            if (pinataConfig.timer.timeout.enabled && pinataConfig.timer.timeout.duration > 0) {
                                long spawnTime = livingEntity
                                        .getPersistentDataContainer()
                                        .getOrDefault(
                                                NamespacedKeys.PINATA_SPAWN_TIME,
                                                PersistentDataType.LONG,
                                                System.currentTimeMillis());
                                int totalTimeout = pinataConfig.timer.timeout.duration;
                                int remaining = Math.max(
                                        0, totalTimeout - (int) ((System.currentTimeMillis() - spawnTime) / 1000));
                                timeStr = String.format("%02d:%02d", remaining / 60, remaining % 60);
                            }

                            List<String> lines = pinataConfig.appearance.nameTag.text;
                            List<Component> components = new ArrayList<>();
                            if (lines != null) {
                                for (String line : lines) {
                                    components.add(messageUtils.parse(
                                            null,
                                            line,
                                            messageUtils.getPinataTags(livingEntity),
                                            messageUtils.tag("timer", timeStr)));
                                }
                            }
                            nameTag.text(Component.join(JoinConfiguration.newlines(), components));
                        },
                        () -> {},
                        intervalTicks,
                        intervalTicks);
    }

    public PinataConfiguration getPinataConfig(LivingEntity entity) {
        if (entity == null) return config.getPinataConfig("default");
        String id = entity.getPersistentDataContainer().get(NamespacedKeys.PINATA_TEMPLATE, PersistentDataType.STRING);
        PinataConfiguration pc = config.getPinataConfig(id != null ? id : "default");
        return pc != null ? pc : config.getPinataConfig("default");
    }

    private Map.Entry<String, PinataVariant> selectVariant(Map<String, PinataVariant> variants) {
        if (variants == null || variants.isEmpty()) return null;

        double totalWeight =
                variants.values().stream().mapToDouble(v -> v.weight).sum();
        double random = ThreadLocalRandom.current().nextDouble() * totalWeight;

        for (Map.Entry<String, PinataVariant> entry : variants.entrySet()) {
            random -= entry.getValue().weight;
            if (random <= 0) {
                return entry;
            }
        }

        return variants.entrySet().iterator().next();
    }

    public boolean isPinata(LivingEntity pinata) {
        return pinata.getPersistentDataContainer().has(NamespacedKeys.IS_PINATA, PersistentDataType.BOOLEAN);
    }

    public boolean isPinataAlive() {
        return !activePinatas.isEmpty();
    }

    public int getActivePinataCount() {
        return activePinatas.size();
    }

    public LivingEntity getNearestPinata(Location location) {
        if (activePinatas.isEmpty() || location == null) return null;

        LivingEntity nearest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (LivingEntity pinata : activePinatas.values()) {
            if (!pinata.isValid()) continue;
            if (pinata.getWorld() != location.getWorld()) continue;

            double distSq = pinata.getLocation().distanceSquared(location);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                nearest = pinata;
            }
        }
        return nearest;
    }

    public int getPinataHealth(LivingEntity pinata) {
        if (pinata == null || !pinata.isValid()) return 0;
        return pinata.getPersistentDataContainer()
                .getOrDefault(NamespacedKeys.PINATA_HEALTH, PersistentDataType.INTEGER, 0);
    }

    public int getPinataMaxHealth(LivingEntity pinata) {
        if (pinata == null || !pinata.isValid()) return 0;
        return pinata.getPersistentDataContainer()
                .getOrDefault(NamespacedKeys.PINATA_MAX_HEALTH, PersistentDataType.INTEGER, 0);
    }
}
