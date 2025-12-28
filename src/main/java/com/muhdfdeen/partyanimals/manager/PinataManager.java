package com.muhdfdeen.partyanimals.manager;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.api.event.pinata.PinataSpawnEvent;
import com.muhdfdeen.partyanimals.behavior.PinataFloatGoal;
import com.muhdfdeen.partyanimals.behavior.PinataRoamGoal;
import com.muhdfdeen.partyanimals.config.ConfigManager;
import com.muhdfdeen.partyanimals.handler.EffectHandler;
import com.muhdfdeen.partyanimals.handler.MessageHandler;
import com.muhdfdeen.partyanimals.util.Logger;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;

public class PinataManager {
    private final PartyAnimals plugin;
    private final Logger log;
    private final ConfigManager config;
    private final BossBarManager bossBarManager;
    private final EffectHandler effectHandler;
    private final MessageHandler messageHandler;
    
    private final NamespacedKey is_pinata;
    private final NamespacedKey health;
    private final NamespacedKey max_health;
    private final NamespacedKey hit_cooldown;
    private final NamespacedKey spawn_time;

    private final Map<UUID, LivingEntity> activePinatas = new HashMap<>();
    private final Map<UUID, ScheduledTask> timeoutTasks = new HashMap<>();

    public PinataManager(PartyAnimals plugin) {
        this.plugin = plugin;
        this.log = plugin.getPluginLogger();
        this.config = plugin.getConfiguration();
        this.bossBarManager = plugin.getBossBarManager();
        this.effectHandler = plugin.getEffectHandler();
        this.messageHandler = plugin.getMessageHandler();
        this.is_pinata = new NamespacedKey(plugin, "is_pinata");
        this.health = new NamespacedKey(plugin, "health");
        this.max_health = new NamespacedKey(plugin, "max_health");
        this.hit_cooldown = new NamespacedKey(plugin, "hit_cooldown");
        this.spawn_time = new NamespacedKey(plugin, "spawn_time");
    }

    public void startCountdown(Location location) {
        double countdownSeconds = config.getPinataConfig().timer.countdown().duration();
        if (countdownSeconds <= 0) {
            log.debug("Countdown is set to 0 or less; spawning pinata immediately.");
            spawnPinata(location);
            return;
        }

        effectHandler.playEffects(config.getPinataConfig().timer.countdown().start(), location, true);

        String bossBarCountdown = config.getMessageConfig().pinata.bossBarCountdown();
        var barSettings = config.getPinataConfig().timer.countdown().bar();

        BossBar bossBar = BossBar.bossBar(
            messageHandler.parse(null, bossBarCountdown, messageHandler.tag("countdown", (int) countdownSeconds)), 
            1.0f, 
            barSettings.color(), 
            barSettings.overlay()
        );
        
        boolean shouldShowBar = barSettings.enabled();
        boolean global = barSettings.global(); 
        if (shouldShowBar) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (global || p.getWorld().equals(location.getWorld())) {
                    p.showBossBar(bossBar);
                }
            }
        }

        long durationMillis = (long) (countdownSeconds * 1000);
        long endTime = System.currentTimeMillis() + durationMillis;

        new BukkitRunnable() {
            int lastSeconds = (int) countdownSeconds;

            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long remainingMilis = endTime - now;

                if (remainingMilis <= 0) {
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        p.hideBossBar(bossBar);
                    }
                    effectHandler.playEffects(config.getPinataConfig().timer.countdown().end(), location, true);
                    spawnPinata(location);
                    this.cancel();
                    return;
                }

                if (shouldShowBar) {
                    float progress = Math.max(0.0f, Math.min(1.0f, (float) remainingMilis / durationMillis));
                    int displaySeconds = (int) Math.ceil(remainingMilis / 1000.0);

                    bossBar.progress(progress);
                    
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        boolean inSameWorld = p.getWorld().equals(location.getWorld());
                        if (global || inSameWorld) {
                            p.showBossBar(bossBar);
                        } else {
                            p.hideBossBar(bossBar);
                        }
                    }

                    if (displaySeconds != lastSeconds) {
                        log.debug("Countdown Tick: " + displaySeconds + "s remaining. Progress: " + progress);
                        effectHandler.playEffects(config.getPinataConfig().timer.countdown().mid(), location, true);
                        bossBar.name(messageHandler.parse(null, bossBarCountdown, messageHandler.tag("countdown", displaySeconds)));
                        lastSeconds = displaySeconds;
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1L);
    }

    public void spawnPinata(Location location) {
        Location spawnLocation = location.clone();
        spawnLocation.setPitch(0);

        List<String> types = config.getPinataConfig().appearance.entityTypes();
        String randomType = types.get(ThreadLocalRandom.current().nextInt(types.size()));
        EntityType pinataType = EntityType.valueOf(randomType.toUpperCase());

        double minScale = config.getPinataConfig().appearance.scale().min();
        double maxScale = config.getPinataConfig().appearance.scale().max();
        final double finalScale = (minScale >= maxScale) ? minScale : ThreadLocalRandom.current().nextDouble(minScale, maxScale);

        int baseHealth = config.getPinataConfig().health.maxHealth();
        int calculatedHealth = config.getPinataConfig().health.perPlayer() 
            ? baseHealth * Math.max(1, plugin.getServer().getOnlinePlayers().size()) 
            : baseHealth * config.getPinataConfig().health.multiplier();
        final int finalHealth = calculatedHealth;

        location.getWorld().spawn(spawnLocation, pinataType.getEntityClass(), pinata -> {
            if (pinata instanceof LivingEntity livingEntity) {
                livingEntity.getPersistentDataContainer().set(is_pinata, PersistentDataType.BOOLEAN, true);
                livingEntity.getPersistentDataContainer().set(health, PersistentDataType.INTEGER, finalHealth);
                livingEntity.getPersistentDataContainer().set(max_health, PersistentDataType.INTEGER, finalHealth);
                livingEntity.getPersistentDataContainer().set(spawn_time, PersistentDataType.LONG, System.currentTimeMillis());
                livingEntity.getAttribute(Attribute.SCALE).setBaseValue(finalScale);
                
                var event = new PinataSpawnEvent(livingEntity, spawnLocation);
                plugin.getServer().getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    log.debug("Pinata spawn event was cancelled by an API event; removing entity.");
                    livingEntity.remove();
                    return;
                }

                livingEntity.setSilent(true);
                livingEntity.setInvulnerable(false);
                livingEntity.setRemoveWhenFarAway(false);
                livingEntity.setMaximumAir(100000);
                livingEntity.setRemainingAir(100000);

                if (livingEntity instanceof Mob mob) mob.setTarget(null);

                livingEntity.setGlowing(config.getPinataConfig().appearance.glowing());
                if (config.getPinataConfig().appearance.glowing()) {
                    String colorName = config.getPinataConfig().appearance.glowColor();
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

                if (config.getPinataConfig().appearance.nameTag().enabled()) {
                    spawnNameTag(livingEntity);
                }

                activatePinata(livingEntity);

                log.debug("Playing pinata spawn effect at location: " + location + " for entity: " + livingEntity.getType() + " (UUID: " + livingEntity.getUniqueId() + ")");
                effectHandler.playEffects(config.getPinataConfig().events.spawn().effects(), location, false);
            }
        });
        if (plugin.getRewardHandler() != null) {
            plugin.getRewardHandler().process(null, config.getPinataConfig().events.spawn().rewards().values());
        }
        
        String spawnMessage = config.getMessageConfig().pinata.spawnedNaturally();
        messageHandler.send(plugin.getServer(), spawnMessage, messageHandler.tagParsed("location", location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ()));
    }

    public void activatePinata(LivingEntity pinata) {
        if (!pinata.isValid()) return;

        activePinatas.put(pinata.getUniqueId(), pinata);
        applyPinataGoal(pinata);
        startUpdateTask(pinata);
        startTimeoutTask(pinata);

        if (config.getPinataConfig().appearance.nameTag().enabled()) {
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

    private void startUpdateTask(LivingEntity livingEntity) {
        int currentHealth = livingEntity.getPersistentDataContainer().getOrDefault(health, PersistentDataType.INTEGER, config.getPinataConfig().health.maxHealth());
        int maxHealthVal = livingEntity.getPersistentDataContainer().getOrDefault(max_health, PersistentDataType.INTEGER, currentHealth);
        int timeout = config.getPinataConfig().timer.timeout().duration();

        if (!bossBarManager.hasBossBar(livingEntity.getUniqueId())) {
            bossBarManager.createBossBar(livingEntity, currentHealth, maxHealthVal, timeout);
        }

        livingEntity.getScheduler().runAtFixedRate(plugin, (task) -> {

            if (!livingEntity.isValid()) {
                if (livingEntity.isDead()) {
                    removeActiveBossBar(livingEntity);
                }
                task.cancel();
                return;
            }

            if (!bossBarManager.hasBossBar(livingEntity.getUniqueId())) {
                task.cancel();
                return;
            }

            int currHealth = livingEntity.getPersistentDataContainer().getOrDefault(health, PersistentDataType.INTEGER, 0);
            int maxHealth = livingEntity.getPersistentDataContainer().getOrDefault(max_health, PersistentDataType.INTEGER, 0);

            bossBarManager.updateBossBar(livingEntity, currHealth, maxHealth, spawn_time);
        }, () -> {}, 20L, 20L);
    }

    private void startTimeoutTask(LivingEntity pinata) {
        var existing = timeoutTasks.remove(pinata.getUniqueId());
        if (existing != null) existing.cancel();

        if (!config.getPinataConfig().timer.timeout().enabled()) return;

        long spawnTime = pinata.getPersistentDataContainer().getOrDefault(spawn_time, PersistentDataType.LONG, 0L);
        int timeoutSeconds = config.getPinataConfig().timer.timeout().duration();
        if (spawnTime <= 0) spawnTime = System.currentTimeMillis();

        long elapsedMillis = System.currentTimeMillis() - spawnTime;
        long remainingMillis = (timeoutSeconds * 1000L) - elapsedMillis;
        long remainingTicks = remainingMillis / 50;

        if (remainingTicks <= 0) {
            log.debug("Restoring pinata but timeout passed. Removing.");
            safelyRemovePinata(pinata);
            return;
        }

        var task = pinata.getScheduler().runDelayed(plugin, (t) -> {
            if (pinata.isValid() && isPinata(pinata)) {
                safelyRemovePinata(pinata);
                String timeoutMsg = config.getMessageConfig().pinata.timeout();
                messageHandler.send(plugin.getServer(), timeoutMsg);
            }
            timeoutTasks.remove(pinata.getUniqueId());
        }, () -> {}, remainingTicks);

        timeoutTasks.put(pinata.getUniqueId(), task);
    }

    private void startNameTagTask(LivingEntity livingEntity, TextDisplay nameTag) {
        int interval = config.getPinataConfig().appearance.nameTag().updateTextInterval();
        if (interval <= 0) return;

        long intervalTicks = (long) interval;
        livingEntity.getScheduler().runAtFixedRate(plugin, (task) -> {
            if (!nameTag.isValid() || !livingEntity.isValid()) {
                task.cancel();
                if (nameTag.isValid()) nameTag.remove();
                return;
            }

            int currentHealth = livingEntity.getPersistentDataContainer().getOrDefault(health, PersistentDataType.INTEGER, 0);
            int maxHealthVal = livingEntity.getPersistentDataContainer().getOrDefault(max_health, PersistentDataType.INTEGER, 1);

            String timeStr = "∞";
            if (config.getPinataConfig().timer.timeout().enabled() && config.getPinataConfig().timer.timeout().duration() > 0) {
                long spawnTime = livingEntity.getPersistentDataContainer().getOrDefault(spawn_time, PersistentDataType.LONG, System.currentTimeMillis());
                int totalTimeout = config.getPinataConfig().timer.timeout().duration();
                int remaining = Math.max(0, totalTimeout - (int) ((System.currentTimeMillis() - spawnTime) / 1000));
                timeStr = String.format("%02d:%02d", remaining / 60, remaining % 60);
            }

            List<String> lines = config.getPinataConfig().appearance.nameTag().text();
            List<Component> components = new ArrayList<>();
            if (lines != null) {
                for (String line : lines) {
                    components.add(messageHandler.parse(null, line,
                        messageHandler.tag("pinata", config.getPinataConfig().appearance.name()),
                        messageHandler.tag("health", currentHealth),
                        messageHandler.tag("max-health", maxHealthVal),
                        messageHandler.tag("timer", timeStr)
                    ));
                }
            }
            nameTag.text(Component.join(JoinConfiguration.newlines(), components));
        }, () -> {}, intervalTicks, intervalTicks);
    }

    public void applyPinataGoal(LivingEntity pinata) {
        if (!config.getPinataConfig().behavior.enabled()) {
            pinata.setAI(false);
            return;
        }
        pinata.setAI(true);
        if (pinata instanceof Creature creature) {
            Bukkit.getMobGoals().removeAllGoals(creature);
            Bukkit.getMobGoals().addGoal(creature, 0, new PinataFloatGoal(plugin, creature));
            Bukkit.getMobGoals().addGoal(creature, 2, new PinataRoamGoal(plugin, creature));
        }
        var knockbackAttribute = pinata.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockbackAttribute != null) {
            knockbackAttribute.setBaseValue(config.getPinataConfig().behavior.knockbackResistance());
        }
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
        bossBarManager.removeBossBar(pinata.getUniqueId());
        activePinatas.remove(pinata.getUniqueId());
        
        ScheduledTask task = timeoutTasks.remove(pinata.getUniqueId());
        if (task != null)
            task.cancel();
    }

    private void spawnNameTag(LivingEntity pinata) {
        Location location = pinata.getLocation();
        TextDisplay nameTag = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);

        nameTag.setPersistent(false);

        int currentHealth = pinata.getPersistentDataContainer().getOrDefault(health, PersistentDataType.INTEGER, config.getPinataConfig().health.maxHealth());
        int maxHealthVal = pinata.getPersistentDataContainer().getOrDefault(max_health, PersistentDataType.INTEGER, currentHealth);

        int totalSeconds = config.getPinataConfig().timer.timeout().duration();
        String initialTimeStr = "∞";

        if (config.getPinataConfig().timer.timeout().enabled() && totalSeconds > 0) {
            initialTimeStr = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
        }

        List<String> lines = config.getPinataConfig().appearance.nameTag().text();
        List<Component> components = new ArrayList<>();
        
        if (lines != null) {
            for (String line : lines) {
                components.add(messageHandler.parse(null, line,
                    messageHandler.tag("pinata", config.getPinataConfig().appearance.name()),
                    messageHandler.tag("health", currentHealth),
                    messageHandler.tag("max-health", maxHealthVal),
                    messageHandler.tag("timer", initialTimeStr)
                ));
            }
        }
        
        nameTag.text(Component.join(JoinConfiguration.newlines(), components));

        nameTag.setAlignment(config.getPinataConfig().appearance.nameTag().textAlignment());

        nameTag.setDefaultBackground(false);

        if (config.getPinataConfig().appearance.nameTag().background().enabled()) {
            nameTag.setBackgroundColor(Color.fromARGB(
                config.getPinataConfig().appearance.nameTag().background().alpha(),
                config.getPinataConfig().appearance.nameTag().background().red(),
                config.getPinataConfig().appearance.nameTag().background().green(),
                config.getPinataConfig().appearance.nameTag().background().blue()
            ));
        } else {
            nameTag.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        }

        nameTag.setShadowed(config.getPinataConfig().appearance.nameTag().shadow().enabled());
        nameTag.setShadowRadius(config.getPinataConfig().appearance.nameTag().shadow().radius());
        nameTag.setShadowStrength(config.getPinataConfig().appearance.nameTag().shadow().strength());
        nameTag.setBillboard(config.getPinataConfig().appearance.nameTag().billboard());
        nameTag.setSeeThrough(config.getPinataConfig().appearance.nameTag().seeThrough());

        Transformation nameTransform = nameTag.getTransformation();

        float scaleX = (float) config.getPinataConfig().appearance.nameTag().transformation().scale().x();
        float scaleY = (float) config.getPinataConfig().appearance.nameTag().transformation().scale().y();
        float scaleZ = (float) config.getPinataConfig().appearance.nameTag().transformation().scale().z();

        float transX = (float) config.getPinataConfig().appearance.nameTag().transformation().translation().x();
        float transY = (float) config.getPinataConfig().appearance.nameTag().transformation().translation().y();
        float transZ = (float) config.getPinataConfig().appearance.nameTag().transformation().translation().z();

        nameTransform.getTranslation().set(transX, transY, transZ);
        nameTransform.getScale().set(scaleX, scaleY, scaleZ);

        nameTag.setTransformation(nameTransform);

        pinata.addPassenger(nameTag);
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
    }

    public boolean isPinata(LivingEntity pinata) {
        return pinata.getPersistentDataContainer().has(is_pinata, PersistentDataType.BOOLEAN);
    }

    public int getActivePinataCount() {
        return activePinatas.size();
    }

    public boolean isPinataAlive() {
        return !activePinatas.isEmpty();
    }

    private LivingEntity getFirstActivePinata() {
        if (activePinatas.isEmpty()) return null;
        return activePinatas.values().iterator().next();
    }

    public int getCurrentHealth() {
        LivingEntity pinata = getFirstActivePinata();
        if (pinata == null || !pinata.isValid()) return 0;
        return pinata.getPersistentDataContainer().getOrDefault(health, PersistentDataType.INTEGER, 0);
    }

    public int getMaxHealth() {
        LivingEntity pinata = getFirstActivePinata();
        if (pinata == null || !pinata.isValid()) return 0;
        return pinata.getPersistentDataContainer().getOrDefault(max_health, PersistentDataType.INTEGER, 0);
    }

    public Location getPinataLocation() {
        LivingEntity pinata = getFirstActivePinata();
        if (pinata == null || !pinata.isValid()) return null;
        return pinata.getLocation();
    }

    public NamespacedKey getHealthKey() { return health; }
    public NamespacedKey getMaxHealthKey() { return max_health; }
    public NamespacedKey getCooldownKey() { return hit_cooldown; }
    public NamespacedKey getSpawnTimeKey() { return spawn_time; }
}
