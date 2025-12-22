package com.muhdfdeen.partyanimals.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.config.ConfigManager;
import com.muhdfdeen.partyanimals.config.MainConfig.VisualAudioEffect;
import com.muhdfdeen.partyanimals.util.CommandUtils;
import com.muhdfdeen.partyanimals.util.Logger;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class PinataManager {
    private final PartyAnimals plugin;
    private final Logger log;
    private final ConfigManager config;
    private final MiniMessage mm;
    private NamespacedKey is_pinata;
    private NamespacedKey health;
    private NamespacedKey max_health;
    private NamespacedKey hit_cooldown;
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
        double countdownSeconds = config.getMainConfig().pinata.countdown();
        if (countdownSeconds <= 0) {
            log.debug("Countdown is set to 0 or less; spawning pinata immediately.");
            spawnPinata(location);
            return;
        }

        String bossBarCountdown = config.getMessageConfig().messages.pinataMessages().bossBarCountdown();

        BossBar bossBar = BossBar.bossBar(
                mm.deserialize(bossBarCountdown
                        .replace("{seconds}", String.valueOf((int) countdownSeconds))),
                1.0f,
                BossBar.Color.GREEN,
                BossBar.Overlay.PROGRESS);

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
                    for (Player p : plugin.getServer().getOnlinePlayers())
                        p.hideBossBar(bossBar);
                    spawnPinata(location);
                    this.cancel();
                    return;
                }

                float progress = Math.max(0.0f, Math.min(1.0f, (float) remainingMilis / durationMillis));
                int displaySeconds = (int) Math.ceil(remainingMilis / 1000.0);

                bossBar.progress(progress);
                if (displaySeconds != lastSeconds) {
                    bossBar.name(mm.deserialize(bossBarCountdown
                            .replace("{seconds}", String.valueOf(displaySeconds))));
                    lastSeconds = displaySeconds;
                }
            }
        }.runTaskTimer(plugin, 0, 1L);
    }

    public void spawnPinata(Location location) {
        List<String> types = config.getMainConfig().pinata.appearance().types();
        String randomType = types.get(ThreadLocalRandom.current().nextInt(types.size()));
        EntityType pinataType = EntityType.valueOf(randomType.toUpperCase());
        double scale = config.getMainConfig().pinata.appearance().scale();
        int timeout = config.getMainConfig().pinata.timeout();

        int baseHealth = config.getMainConfig().pinata.health().maxHealth();
        int calculatedHealth = baseHealth;

        if (config.getMainConfig().pinata.health().multipliedPerPlayer()) {
            int playerCount = plugin.getServer().getOnlinePlayers().size();
            calculatedHealth = baseHealth * Math.max(1, playerCount);
        } else {
            int multiplier = config.getMainConfig().pinata.health().maxHealthMultiplier();
            calculatedHealth = baseHealth * multiplier;
        }

        final int finalHealth = calculatedHealth;

        location.getWorld().spawn(location, pinataType.getEntityClass(), pinata -> {
            if (pinata instanceof LivingEntity livingEntity) {
                livingEntity.getPersistentDataContainer().set(is_pinata, PersistentDataType.BOOLEAN, true);
                livingEntity.getPersistentDataContainer().set(health, PersistentDataType.INTEGER, finalHealth);
                livingEntity.getPersistentDataContainer().set(max_health, PersistentDataType.INTEGER, finalHealth);
                livingEntity.getAttribute(Attribute.SCALE).setBaseValue(scale);
                if (config.getMainConfig().pinata.ai().enabled()) {
                    livingEntity.setAI(true);
                    var movementSpeed = livingEntity.getAttribute(Attribute.MOVEMENT_SPEED);
                    if (movementSpeed != null) {
                        double baseSpeed = movementSpeed.getBaseValue();
                        double movementSpeedMultiplier = config.getMainConfig().pinata.ai().movementSpeedMultiplier();
                        movementSpeed.setBaseValue(baseSpeed * movementSpeedMultiplier);
                    }
                } else {
                    livingEntity.setAI(false);
                }
                livingEntity.setSilent(true);
                livingEntity.setInvulnerable(false);
                livingEntity.setRemoveWhenFarAway(false);
                if (livingEntity instanceof Mob mob)
                    mob.setTarget(null);

                livingEntity.setGlowing(config.getMainConfig().pinata.effects().glowing());
                if (config.getMainConfig().pinata.effects().glowing()) {
                    String colorName = config.getMainConfig().pinata.effects().glowColor();
                    NamedTextColor glowColor = NamedTextColor.NAMES.value(colorName.toLowerCase());
                    if (glowColor != null) {
                        Scoreboard mainBoard = plugin.getServer().getScoreboardManager().getMainScoreboard();
                        String teamName = "PA_" + glowColor.toString().toUpperCase();
                        Team team = mainBoard.getTeam(teamName);
                        if (team == null) {
                            team = mainBoard.registerNewTeam(teamName);
                        }
                        team.color(glowColor);
                        team.addEntry(livingEntity.getUniqueId().toString());
                    } else {
                        log.error("Invalid glow color in config: " + colorName);
                    }
                }

                livingEntity.customName(mm.deserialize(config.getMainConfig().pinata.appearance().name()));
                livingEntity.setCustomNameVisible(true);
                BossBar healthBar = BossBar.bossBar(
                        mm.deserialize(config.getMessageConfig().messages.pinataMessages().bossBarActive()
                                .replace("{health}", String.valueOf(finalHealth))
                                .replace("{max_health}", String.valueOf(finalHealth))),
                        1.0f,
                        BossBar.Color.valueOf(config.getMainConfig().pinata.display().healthBarColor()),
                        BossBar.Overlay.valueOf(config.getMainConfig().pinata.display().healthBarOverlay()));
                activeBossBars.put(livingEntity.getUniqueId(), healthBar);
                for (Player p : plugin.getServer().getOnlinePlayers())
                    p.showBossBar(healthBar);

                playEffect(config.getMainConfig().pinata.effects().spawn(), location);

                if (timeout > 0) {
                    org.bukkit.scheduler.BukkitTask task = new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (pinata.isValid() && isPinata((LivingEntity) pinata)) {
                                removeActiveBossBar((LivingEntity) pinata);
                                pinata.remove();
                                String timeoutMsg = config.getMessageConfig().messages.pinataMessages().pinataTimeout();
                                if (timeoutMsg != null && !timeoutMsg.isEmpty()) {
                                    plugin.getServer().broadcast(
                                            mm.deserialize(config.getMessageConfig().messages.prefix() + timeoutMsg));
                                }
                            }
                            timeoutTasks.remove(pinata.getUniqueId());
                        }
                    }.runTaskLater(plugin, timeout * 20L);
                    timeoutTasks.put(pinata.getUniqueId(), task);
                }
            }
        });
        CommandUtils.process(null, config.getMainConfig().pinata.commands().spawn(), plugin);
        String spawnMessage = config.getMessageConfig().messages.pinataMessages().pinataSpawned();
        if (spawnMessage != null && !spawnMessage.isEmpty())
            plugin.getServer().broadcast(mm.deserialize(config.getMessageConfig().messages.prefix() + spawnMessage));
    }

    public void updateActiveBossBar(LivingEntity pinata, int currentHealth, int maxHealth) {
        BossBar bossBar = activeBossBars.get(pinata.getUniqueId());
        if (bossBar == null)
            return;
        float progress = Math.max(0.0f, (float) currentHealth / maxHealth);
        bossBar.progress(progress);
        bossBar.name(mm.deserialize(config.getMessageConfig().messages.pinataMessages().bossBarActive()
                .replace("{health}", String.valueOf(currentHealth))));
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

        if (soundType != null && !soundType.isEmpty()) {
            location.getWorld().playSound(location, soundType, soundVolume, soundPitch);
        }

        if (particleType != null && !particleType.isEmpty()) {
            try {
                Particle particle = Particle.valueOf(particleType);
                location.getWorld().spawnParticle(particle, location.clone().add(0, 1, 0), particleCount);
            } catch (IllegalArgumentException e) {
                log.error("Invalid particle type in effect: " + particleType);
            }
        }
    }

    public void cleanup() {
        for (BossBar bossBar : activeBossBars.values())
            for (Player p : plugin.getServer().getOnlinePlayers())
                p.hideBossBar(bossBar);
        activeBossBars.clear();
        for (BukkitTask task : timeoutTasks.values())
            task.cancel();
        timeoutTasks.clear();
        for (World world : plugin.getServer().getWorlds()) {
            for (LivingEntity pinata : world.getLivingEntities()) {
                if (isPinata(pinata)) {
                    pinata.remove();
                }
            }
        }
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
