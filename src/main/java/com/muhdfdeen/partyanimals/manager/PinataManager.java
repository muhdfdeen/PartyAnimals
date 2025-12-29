package com.muhdfdeen.partyanimals.manager;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.api.event.pinata.PinataSpawnEvent;
import com.muhdfdeen.partyanimals.behavior.PinataFloatGoal;
import com.muhdfdeen.partyanimals.behavior.PinataRoamGoal;
import com.muhdfdeen.partyanimals.config.ConfigManager;
import com.muhdfdeen.partyanimals.config.settings.PinataConfig.PinataConfiguration;
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
    private final NamespacedKey pinata_template; 

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
        this.pinata_template = new NamespacedKey(plugin, "pinata_template");
    }

    public PinataConfiguration getPinataConfig(LivingEntity entity) {
        if (entity == null) return config.getPinataConfig("default");
        String id = entity.getPersistentDataContainer().get(pinata_template, PersistentDataType.STRING);
        PinataConfiguration pc = config.getPinataConfig(id != null ? id : "default");
        return pc != null ? pc : config.getPinataConfig("default");
    }

    public void startCountdown(Location location, String templateId) {
        PinataConfiguration pinataConfig = config.getPinataConfig(templateId);
        if (pinataConfig == null) {
            log.warn("Tried to start countdown for invalid pinata template: " + templateId);
            return;
        }

        double countdownSeconds = pinataConfig.timer.countdown().duration();
        if (countdownSeconds <= 0) {
            log.debug("Countdown is set to 0 or less; spawning pinata immediately.");
            spawnPinata(location, templateId);
            return;
        }

        effectHandler.playEffects(pinataConfig.timer.countdown().start(), location, true);

        String bossBarCountdown = config.getMessageConfig().pinata.bossBarCountdown();
        var barSettings = pinataConfig.timer.countdown().bar();

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

        final int[] lastSeconds = {(int) countdownSeconds};

        Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, (task) -> {
            long now = System.currentTimeMillis();
            long remainingMilis = endTime - now;

            if (remainingMilis <= 0) {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.hideBossBar(bossBar);
                }
                effectHandler.playEffects(pinataConfig.timer.countdown().end(), location, true);
                spawnPinata(location, templateId);
                task.cancel();
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

                if (displaySeconds != lastSeconds[0]) {
                    effectHandler.playEffects(pinataConfig.timer.countdown().mid(), location, true);
                    bossBar.name(messageHandler.parse(null, bossBarCountdown, messageHandler.tag("countdown", displaySeconds)));
                    lastSeconds[0] = displaySeconds;
                }
            }
        }, 1L, 1L);
    }

    public void spawnPinata(Location location, String templateId) {
        PinataConfiguration pinataConfig = config.getPinataConfig(templateId);
        if (pinataConfig == null) {
            log.error("Cannot spawn pinata! Template '" + templateId + "' not found.");
            return;
        }

        Location spawnLocation = location.clone();
        spawnLocation.setPitch(0);

        List<String> types = pinataConfig.appearance.entityTypes();
        String randomType = types.get(ThreadLocalRandom.current().nextInt(types.size()));
        EntityType pinataType = EntityType.valueOf(randomType.toUpperCase());

        double minScale = pinataConfig.appearance.scale().min();
        double maxScale = pinataConfig.appearance.scale().max();
        final double finalScale = (minScale >= maxScale) ? minScale : ThreadLocalRandom.current().nextDouble(minScale, maxScale);

        int baseHealth = pinataConfig.health.maxHealth();
        int calculatedHealth = pinataConfig.health.perPlayer() 
            ? baseHealth * Math.max(1, plugin.getServer().getOnlinePlayers().size()) 
            : baseHealth;
        final int finalHealth = calculatedHealth;

        location.getWorld().spawn(spawnLocation, pinataType.getEntityClass(), pinata -> {
            if (pinata instanceof LivingEntity livingEntity) {
                initializePinataEntity(livingEntity, pinataConfig, templateId, finalHealth, finalScale);
                
                var event = new PinataSpawnEvent(livingEntity, spawnLocation);
                plugin.getServer().getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    log.debug("Pinata spawn event was cancelled by an API event; removing entity.");
                    livingEntity.remove();
                    return;
                }

                if (pinataConfig.appearance.nameTag().enabled()) {
                    spawnNameTag(livingEntity);
                }

                activatePinata(livingEntity);

                log.debug("Playing pinata spawn effect at location: " + location + " for entity: " + livingEntity.getType() + " (Template: " + templateId + ")");
                effectHandler.playEffects(pinataConfig.events.spawn().effects(), location, false);
            }
        });

        if (plugin.getRewardHandler() != null) {
            plugin.getRewardHandler().process(null, pinataConfig.events.spawn().rewards().values());
        }
        
        String spawnMessage = config.getMessageConfig().pinata.spawnedNaturally();
        messageHandler.send(plugin.getServer(), spawnMessage, messageHandler.tagParsed("location", location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ()));
    }

    private void initializePinataEntity(LivingEntity livingEntity, PinataConfiguration pinataConfig, String templateId, int health, double scale) {
        livingEntity.getPersistentDataContainer().set(pinata_template, PersistentDataType.STRING, templateId);
        livingEntity.getPersistentDataContainer().set(is_pinata, PersistentDataType.BOOLEAN, true);
        livingEntity.getPersistentDataContainer().set(this.health, PersistentDataType.INTEGER, health);
        livingEntity.getPersistentDataContainer().set(max_health, PersistentDataType.INTEGER, health);
        livingEntity.getPersistentDataContainer().set(spawn_time, PersistentDataType.LONG, System.currentTimeMillis());
        livingEntity.getAttribute(Attribute.SCALE).setBaseValue(scale);

        livingEntity.setSilent(true);
        livingEntity.setInvulnerable(false);
        livingEntity.setRemoveWhenFarAway(false);
        livingEntity.setMaximumAir(100000);
        livingEntity.setRemainingAir(100000);

        if (livingEntity instanceof Mob mob) mob.setTarget(null);

        livingEntity.setGlowing(pinataConfig.appearance.glowing());
        if (pinataConfig.appearance.glowing()) {
            String colorName = pinataConfig.appearance.glowColor();
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
    }

    public void activatePinata(LivingEntity pinata) {
        if (pinata == null || pinata.isDead()) return;

        PinataConfiguration pinataConfig = getPinataConfig(pinata);

        activePinatas.put(pinata.getUniqueId(), pinata);
        applyPinataGoal(pinata);
        startUpdateTask(pinata);
        startTimeoutTask(pinata);

        if (pinataConfig.appearance.nameTag().enabled()) {
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
        PinataConfiguration pinataConfig = getPinataConfig(livingEntity);
        
        int currentHealth = livingEntity.getPersistentDataContainer().getOrDefault(health, PersistentDataType.INTEGER, pinataConfig.health.maxHealth());
        int maxHealthVal = livingEntity.getPersistentDataContainer().getOrDefault(max_health, PersistentDataType.INTEGER, currentHealth);
        int timeout = pinataConfig.timer.timeout().duration();

        if (!bossBarManager.hasBossBar(livingEntity.getUniqueId())) {
            bossBarManager.createBossBar(livingEntity, currentHealth, maxHealthVal, timeout, pinataConfig);
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

            bossBarManager.updateBossBar(livingEntity, currHealth, maxHealth, spawn_time, pinataConfig);
        }, () -> {}, 20L, 20L);
    }

    private void startTimeoutTask(LivingEntity pinata) {
        var existing = timeoutTasks.remove(pinata.getUniqueId());
        if (existing != null) existing.cancel();

        PinataConfiguration pinataConfig = getPinataConfig(pinata);

        if (!pinataConfig.timer.timeout().enabled()) return;

        long spawnTime = pinata.getPersistentDataContainer().getOrDefault(spawn_time, PersistentDataType.LONG, 0L);
        int timeoutSeconds = pinataConfig.timer.timeout().duration();
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
        PinataConfiguration pinataConfig = getPinataConfig(livingEntity);
        int interval = pinataConfig.appearance.nameTag().updateTextInterval();
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
            if (pinataConfig.timer.timeout().enabled() && pinataConfig.timer.timeout().duration() > 0) {
                long spawnTime = livingEntity.getPersistentDataContainer().getOrDefault(spawn_time, PersistentDataType.LONG, System.currentTimeMillis());
                int totalTimeout = pinataConfig.timer.timeout().duration();
                int remaining = Math.max(0, totalTimeout - (int) ((System.currentTimeMillis() - spawnTime) / 1000));
                timeStr = String.format("%02d:%02d", remaining / 60, remaining % 60);
            }

            List<String> lines = pinataConfig.appearance.nameTag().text();
            List<Component> components = new ArrayList<>();
            if (lines != null) {
                for (String line : lines) {
                    components.add(messageHandler.parse(null, line,
                        messageHandler.tagParsed("pinata", pinataConfig.appearance.name()),
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
        PinataConfiguration pinataConfig = getPinataConfig(pinata);

        if (!pinataConfig.behavior.enabled()) {
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
            knockbackAttribute.setBaseValue(pinataConfig.behavior.knockbackResistance());
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
        PinataConfiguration pinataConfig = getPinataConfig(pinata);
        Location location = pinata.getLocation();
        TextDisplay nameTag = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);

        nameTag.setPersistent(false);

        int currentHealth = pinata.getPersistentDataContainer().getOrDefault(health, PersistentDataType.INTEGER, pinataConfig.health.maxHealth());
        int maxHealthVal = pinata.getPersistentDataContainer().getOrDefault(max_health, PersistentDataType.INTEGER, currentHealth);

        int totalSeconds = pinataConfig.timer.timeout().duration();
        String initialTimeStr = "∞";

        if (pinataConfig.timer.timeout().enabled() && totalSeconds > 0) {
            initialTimeStr = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
        }

        List<String> lines = pinataConfig.appearance.nameTag().text();
        List<Component> components = new ArrayList<>();
        
        if (lines != null) {
            for (String line : lines) {
                components.add(messageHandler.parse(null, line,
                    messageHandler.tagParsed("pinata", pinataConfig.appearance.name()),
                    messageHandler.tag("health", currentHealth),
                    messageHandler.tag("max-health", maxHealthVal),
                    messageHandler.tag("timer", initialTimeStr)
                ));
            }
        }
        
        nameTag.text(Component.join(JoinConfiguration.newlines(), components));

        nameTag.setAlignment(pinataConfig.appearance.nameTag().textAlignment());

        nameTag.setDefaultBackground(false);

        if (pinataConfig.appearance.nameTag().background().enabled()) {
            nameTag.setBackgroundColor(Color.fromARGB(
                pinataConfig.appearance.nameTag().background().alpha(),
                pinataConfig.appearance.nameTag().background().red(),
                pinataConfig.appearance.nameTag().background().green(),
                pinataConfig.appearance.nameTag().background().blue()
            ));
        } else {
            nameTag.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        }

        nameTag.setShadowed(pinataConfig.appearance.nameTag().shadow().enabled());
        nameTag.setShadowRadius(pinataConfig.appearance.nameTag().shadow().radius());
        nameTag.setShadowStrength(pinataConfig.appearance.nameTag().shadow().strength());
        nameTag.setBillboard(pinataConfig.appearance.nameTag().billboard());
        nameTag.setSeeThrough(pinataConfig.appearance.nameTag().seeThrough());
        Transformation nameTransform = nameTag.getTransformation();

        float scaleX = (float) pinataConfig.appearance.nameTag().transformation().scale().x();
        float scaleY = (float) pinataConfig.appearance.nameTag().transformation().scale().y();
        float scaleZ = (float) pinataConfig.appearance.nameTag().transformation().scale().z();

        float transX = (float) pinataConfig.appearance.nameTag().transformation().translation().x();
        float transY = (float) pinataConfig.appearance.nameTag().transformation().translation().y();
        float transZ = (float) pinataConfig.appearance.nameTag().transformation().translation().z();

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
    public NamespacedKey getTemplateKey() { return pinata_template; }
}
