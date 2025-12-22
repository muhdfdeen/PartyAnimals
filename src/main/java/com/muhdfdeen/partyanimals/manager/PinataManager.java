package com.muhdfdeen.partyanimals.manager;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.ai.PinataRoamAI;
import com.muhdfdeen.partyanimals.config.ConfigManager;
import com.muhdfdeen.partyanimals.config.PinataConfig.VisualAudioEffect;
import com.muhdfdeen.partyanimals.util.CommandUtils;
import com.muhdfdeen.partyanimals.util.Logger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class PinataManager {
    private final PartyAnimals plugin;
    private final Logger log;
    private final ConfigManager config;
    private final MiniMessage mm;
    private NamespacedKey is_pinata;
    private NamespacedKey health;
    private NamespacedKey max_health;
    private NamespacedKey hit_cooldown;
    private final Map<UUID, LivingEntity> activePinatas = new HashMap<>();
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();
    private final Map<UUID, BukkitTask> timeoutTasks = new HashMap<>();

    public PinataManager(PartyAnimals plugin) {
        this.plugin = plugin;
        this.log = plugin.getPluginLogger();
        this.config = plugin.getConfiguration();
        this.mm = MiniMessage.miniMessage();
        this.is_pinata = new NamespacedKey(plugin, "is_pinata");
        this.health = new NamespacedKey(plugin, "health");
        this.max_health = new NamespacedKey(plugin, "max_health");
        this.hit_cooldown = new NamespacedKey(plugin, "hit_cooldown");
    }

    public void startCountdown(Location location) {
        double countdownSeconds = config.getPinataConfig().pinata.countdown();
        if (countdownSeconds <= 0) {
            log.debug("Countdown is set to 0 or less; spawning pinata immediately.");
            spawnPinata(location);
            return;
        }

        String bossBarCountdown = config.getMessageConfig().messages.pinataMessages().bossBarCountdown();
        BossBar bossBar = BossBar.bossBar(mm.deserialize(bossBarCountdown.replace("{seconds}", String.valueOf((int) countdownSeconds))), 1.0f, BossBar.Color.valueOf(config.getPinataConfig().pinata.display().countdown().barColor()), BossBar.Overlay.valueOf(config.getPinataConfig().pinata.display().countdown().barOverlay()));

        boolean shouldShowBar = config.getPinataConfig().pinata.display().countdown().enabled();

        if (shouldShowBar)
            for (Player p : plugin.getServer().getOnlinePlayers())
                p.showBossBar(bossBar);

        long durationMillis = (long) (countdownSeconds * 1000);
        long endTime = System.currentTimeMillis() + durationMillis;

        new BukkitRunnable() {
            int lastSeconds = -1;

            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long remainingMilis = endTime - now;

                if (remainingMilis <= 0) {
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        p.hideBossBar(bossBar);
                    }
                    spawnPinata(location);
                    this.cancel();
                    return;
                }

                if (shouldShowBar) {
                    float progress = Math.max(0.0f, Math.min(1.0f, (float) remainingMilis / durationMillis));
                    int displaySeconds = (int) Math.ceil(remainingMilis / 1000.0);

                    bossBar.progress(progress);
                    if (displaySeconds != lastSeconds) {
                        log.debug("Countdown Tick: " + displaySeconds + "s remaining. Progress: " + progress);
                        bossBar.name(mm.deserialize(bossBarCountdown.replace("{seconds}", String.valueOf(displaySeconds))));
                        lastSeconds = displaySeconds;
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1L);
    }

    public void spawnPinata(Location location) {
        List<String> types = config.getPinataConfig().pinata.appearance().types();
        String randomType = types.get(ThreadLocalRandom.current().nextInt(types.size()));

        log.debug("Attempting to spawn pinata. Chosen type: " + randomType + " from list: " + types);

        EntityType pinataType = EntityType.valueOf(randomType.toUpperCase());

        double minScale = config.getPinataConfig().pinata.appearance().scale().min();
        double maxScale = config.getPinataConfig().pinata.appearance().scale().max();
        final double finalScale;
        if (minScale >= maxScale)
            finalScale = minScale;
        else
            finalScale = ThreadLocalRandom.current().nextDouble(minScale, maxScale);

        int timeout = config.getPinataConfig().pinata.timeout();
        int baseHealth = config.getPinataConfig().pinata.health().maxHealth();
        int calculatedHealth = baseHealth;

        if (config.getPinataConfig().pinata.health().multipliedPerPlayer()) {
            int playerCount = plugin.getServer().getOnlinePlayers().size();
            calculatedHealth = baseHealth * Math.max(1, playerCount);
        } else {
            int multiplier = config.getPinataConfig().pinata.health().maxHealthMultiplier();
            calculatedHealth = baseHealth * multiplier;
        }

        final int finalHealth = calculatedHealth;

        location.getWorld()
                .spawn(location,
                        pinataType.getEntityClass(),
                        pinata -> {
                            if (pinata instanceof LivingEntity livingEntity) {
                                livingEntity.getPersistentDataContainer().set(is_pinata, PersistentDataType.BOOLEAN, true);
                                livingEntity.getPersistentDataContainer().set(health, PersistentDataType.INTEGER, finalHealth);
                                livingEntity.getPersistentDataContainer().set(max_health, PersistentDataType.INTEGER, finalHealth);
                                livingEntity.getAttribute(Attribute.SCALE).setBaseValue(finalScale);
                                boolean aiEnabled = config.getPinataConfig().pinata.ai().enabled();
                                log.debug("Setting pinata AI to: " + aiEnabled + " for entity: " + livingEntity.getType() + " (UUID: " + livingEntity.getUniqueId() + ")");
                                if (aiEnabled) {
                                    livingEntity.setAI(true);
                                    if (livingEntity instanceof Creature creature) {
                                        var goalManager = Bukkit.getMobGoals();
                                        goalManager.removeAllGoals(creature);
                                        goalManager.addGoal(creature, 1, new PinataRoamAI(plugin, creature));
                                    }
                                    var knockbackAttribute = livingEntity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
                                    if (knockbackAttribute != null)
                                        knockbackAttribute.setBaseValue(config.getPinataConfig().pinata.ai().knockbackResistance());
                                } else {
                                    livingEntity.setAI(false);
                                }

                                livingEntity.setSilent(true);
                                livingEntity.setInvulnerable(false);
                                livingEntity.setRemoveWhenFarAway(false);

                                if (livingEntity instanceof Mob mob)
                                    mob.setTarget(null);

                                livingEntity.setGlowing(config.getPinataConfig().pinata.effects().glowing());
                                if (config.getPinataConfig().pinata.effects().glowing()) {
                                    String colorName = config.getPinataConfig().pinata.effects().glowColor();
                                    NamedTextColor glowColor = NamedTextColor.NAMES.value(colorName.toLowerCase());

                                    if (glowColor != null) {
                                        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
                                        String teamName = "PA_" + glowColor.toString().toUpperCase();
                                        Team team = mainBoard.getTeam(teamName);

                                        if (team == null)
                                            team = mainBoard.registerNewTeam(teamName);

                                        team.color(glowColor);
                                        team.addEntry(livingEntity.getUniqueId().toString());
                                    }
                                }

                                livingEntity.customName(mm.deserialize(config.getPinataConfig().pinata.appearance().name()));
                                livingEntity.setCustomNameVisible(true);

                                BossBar healthBar = BossBar.bossBar(mm.deserialize(config.getMessageConfig().messages.pinataMessages().bossBarActive().replace("{health}", String.valueOf(finalHealth)).replace("{max_health}", String.valueOf(finalHealth)).replace("{timeout}", String.valueOf(timeout))), 1.0f, BossBar.Color.valueOf(config.getPinataConfig().pinata.display().health().barColor()),BossBar.Overlay.valueOf(config.getPinataConfig().pinata.display().health().barOverlay()));

                                activePinatas.put(livingEntity.getUniqueId(), livingEntity);
                                activeBossBars.put(livingEntity.getUniqueId(), healthBar);

                                boolean shouldShowBar = config.getPinataConfig().pinata.display().health().enabled();
                                if (shouldShowBar)
                                    for (Player p : plugin.getServer().getOnlinePlayers())
                                        p.showBossBar(healthBar);

                                log.debug("Playing pinata spawn effect at location: " + location + " for entity: " + livingEntity.getType() + " (UUID: " + livingEntity.getUniqueId() + ")");
                                playEffect(config.getPinataConfig().pinata.effects().spawn(), location);

                                if (timeout > 0) {
                                    BukkitTask task = new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            if (pinata.isValid() && isPinata((LivingEntity) pinata)) {
                                                removeActiveBossBar((LivingEntity) pinata);
                                                pinata.remove();
                                                String timeoutMsg = config.getMessageConfig().messages.pinataMessages().pinataTimeout();
                                                if (timeoutMsg != null && !timeoutMsg.isEmpty()) {
                                                    plugin.getServer().broadcast(mm.deserialize(config.getMessageConfig().messages.prefix() + timeoutMsg));
                                                }
                                            }
                                            timeoutTasks.remove(pinata.getUniqueId());
                                        }
                                    }.runTaskLater(plugin, timeout * 20L);
                                    timeoutTasks.put(pinata.getUniqueId(), task);
                                }
                            }
                        });
        CommandUtils.process(null, config.getPinataConfig().pinata.commands().spawn(), plugin);
        String spawnMessage = config.getMessageConfig().messages.pinataMessages().pinataSpawned();
        if (spawnMessage != null && !spawnMessage.isEmpty())
            plugin.getServer()
                    .broadcast(
                            mm.deserialize(
                                    config.getMessageConfig().messages.prefix() + spawnMessage));
    }

    public void updateActiveBossBar(LivingEntity pinata, int currentHealth, int maxHealth, boolean showHealthBar) {
        BossBar bossBar = activeBossBars.get(pinata.getUniqueId());
        if (bossBar == null)
            return;
        if (!showHealthBar)
            return;
        float progress = Math.max(0.0f, (float) currentHealth / maxHealth);
        bossBar.progress(progress);
        bossBar.name(
                mm.deserialize(
                        config.getMessageConfig().messages
                                .pinataMessages()
                                .bossBarActive()
                                .replace("{health}", String.valueOf(currentHealth))));
    }

    public Map<UUID, BossBar> getActiveBossBars() {
        return activeBossBars;
    }

    public void removeActiveBossBar(LivingEntity pinata) {
        BossBar bossBar = activeBossBars.remove(pinata.getUniqueId());
        if (bossBar == null)
            return;
        for (Player p : plugin.getServer().getOnlinePlayers())
            p.hideBossBar(bossBar);
        BukkitTask task = timeoutTasks.remove(pinata.getUniqueId());
        if (task != null)
            task.cancel();
    }

    public void playEffect(VisualAudioEffect effect, Location location) {
        String soundType = effect.sound().type();
        float soundVolume = effect.sound().volume();
        float soundPitch = effect.sound().pitch();
        String particleType = effect.particle().type().toUpperCase();
        int particleCount = effect.particle().count();

        log.debug("Playing effect at location: " + location + " with sound: " + soundType + " (volume: " + soundVolume + ", pitch: " + soundPitch + ") and particle: " + particleType + " (count: " + particleCount + ")");
        if (soundType != null && !soundType.isEmpty()) {
            location.getWorld().playSound(location, soundType, soundVolume, soundPitch);
        }

        if (particleType != null && !particleType.isEmpty()) {
            try {
                Particle particle = Particle.valueOf(particleType);
                location.getWorld()
                        .spawnParticle(particle, location.clone().add(0, 1, 0), particleCount);
            } catch (IllegalArgumentException e) {
                log.error("Invalid particle type in effect: " + particleType);
            }
        }
    }

    public void cleanup() {
        log.debug("Running PinataManager cleanup. Tracking: " + activePinatas.size() + "" + " active pinatas and " + activeBossBars.size() + " active boss bars.");
        activeBossBars.values().forEach(bar -> Bukkit.getOnlinePlayers().forEach(player -> player.hideBossBar(bar)));
        activeBossBars.clear();

        activePinatas.values().forEach(entity -> {
            if (entity.isValid())
                entity.remove();
        });
        activePinatas.clear();

        timeoutTasks.values().forEach(BukkitTask::cancel);
        timeoutTasks.clear();
        log.debug("PinataManager cleanup complete. Tracking: " + activePinatas.size() + " active pinatas and " + activeBossBars.size() + " active boss bars.");
    }

    public boolean isPinata(LivingEntity pinata) {
        return pinata.getPersistentDataContainer().has(is_pinata, PersistentDataType.BOOLEAN);
    }

    public NamespacedKey getHealthKey() {
        return health;
    }

    public NamespacedKey getMaxHealthKey() {
        return max_health;
    }

    public NamespacedKey getCooldownKey() {
        return hit_cooldown;
    }
}
