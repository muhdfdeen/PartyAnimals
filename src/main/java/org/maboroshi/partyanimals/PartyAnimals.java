package org.maboroshi.partyanimals;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.List;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.maboroshi.partyanimals.api.event.PartyAnimalsReloadEvent;
import org.maboroshi.partyanimals.command.PartyAnimalsCommand;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.handler.EffectHandler;
import org.maboroshi.partyanimals.handler.HitCooldownHandler;
import org.maboroshi.partyanimals.handler.ReflexHandler;
import org.maboroshi.partyanimals.handler.RewardHandler;
import org.maboroshi.partyanimals.hook.PartyAnimalsExpansion;
import org.maboroshi.partyanimals.listener.PinataListener;
import org.maboroshi.partyanimals.listener.VoteListener;
import org.maboroshi.partyanimals.manager.BossBarManager;
import org.maboroshi.partyanimals.manager.DatabaseManager;
import org.maboroshi.partyanimals.manager.PinataManager;
import org.maboroshi.partyanimals.util.Logger;
import org.maboroshi.partyanimals.util.MessageUtils;
import org.maboroshi.partyanimals.util.UpdateChecker;
import org.maboroshi.partyanimals.util.VoteReminder;

public final class PartyAnimals extends JavaPlugin {
    private static PartyAnimals plugin;

    private ConfigManager configManager;
    private Logger log;
    private MessageUtils messageUtils;
    private DatabaseManager databaseManager;
    private PinataManager pinataManager;
    private BossBarManager bossBarManager;
    private HitCooldownHandler hitCooldownHandler;
    private EffectHandler effectHandler;
    private RewardHandler rewardHandler;
    private ReflexHandler reflexHandler;
    private VoteListener voteListener;
    private ScheduledTask voteReminderTask;

    private static boolean isFolia;

    @Override
    public void onEnable() {
        plugin = this;
        isFolia = checkFolia();
        this.configManager = new ConfigManager(this, getDataFolder());
        this.log = new Logger(this);

        try {
            configManager.loadConfig();
            configManager.loadMessages();
        } catch (Exception e) {
            getLogger().severe("Failed to load configuration: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        @SuppressWarnings("unused")
        Metrics metrics = new Metrics(this, 28389);

        this.messageUtils = new MessageUtils(configManager);
        this.bossBarManager = new BossBarManager(this);
        this.effectHandler = new EffectHandler(log);
        this.rewardHandler = new RewardHandler(this);
        this.reflexHandler = new ReflexHandler(this);

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();

        setupModules();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PartyAnimalsExpansion(this).register();
            log.info("Hooked into PlaceholderAPI.");
        }

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            PartyAnimalsCommand partyanimalsCommand = new PartyAnimalsCommand(this);
            event.registrar()
                    .register(partyanimalsCommand.createCommand("partyanimals"), "Main command", List.of("pa"));
        });

        new UpdateChecker(this).checkForUpdates();
    }

    private static boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void setupModules() {
        boolean pinataEnabled = configManager.getMainConfig().modules.pinata.enabled;
        if (pinataEnabled) {
            if (this.pinataManager == null) {
                this.pinataManager = new PinataManager(this);
                this.hitCooldownHandler = new HitCooldownHandler(this);
                getServer().getPluginManager().registerEvents(new PinataListener(this), this);
                log.info("Pinata module enabled.");
            }
        } else {
            if (this.pinataManager != null) {
                this.pinataManager.cleanup();
                this.pinataManager = null;
                this.hitCooldownHandler = null;
                log.info("Pinata module disabled.");
            }
        }

        boolean voteEnabled = configManager.getMainConfig().modules.vote.enabled;
        boolean hasNuVotifier = getServer().getPluginManager().isPluginEnabled("Votifier");

        if (voteEnabled && hasNuVotifier) {
            if (this.voteListener == null) {
                this.voteListener = new VoteListener(this);
                getServer().getPluginManager().registerEvents(this.voteListener, this);
                log.info("Vote module enabled.");
            }
            var reminderSettings = configManager.getMainConfig().modules.vote.reminder;
            if (reminderSettings.enabled && voteReminderTask == null) {
                long intervalTicks = reminderSettings.interval * 20L;
                this.voteReminderTask = Bukkit.getGlobalRegionScheduler()
                        .runAtFixedRate(
                                this,
                                (task) -> {
                                    new VoteReminder(this).run();
                                },
                                intervalTicks,
                                intervalTicks);
            }
        } else {
            if (voteReminderTask != null) {
                voteReminderTask.cancel();
                voteReminderTask = null;
            }
            if (this.voteListener != null) {
                HandlerList.unregisterAll(this.voteListener);
                this.voteListener = null;
                log.info("Vote module disabled.");
            }

            if (voteEnabled && !hasNuVotifier) {
                log.warn("Vote module is enabled, but NuVotifier is not installed! Voting features will not work.");
            }
        }
    }

    public boolean reload() {
        try {
            if (pinataManager != null) {
                pinataManager.cleanup(false);
            }

            if (databaseManager != null) {
                databaseManager.disconnect();
            }

            configManager.loadConfig();
            configManager.loadMessages();

            if (databaseManager != null) {
                databaseManager.connect();
            }

            setupModules();

            if (pinataManager != null) {
                reloadPinatas();
            }

            getServer().getPluginManager().callEvent(new PartyAnimalsReloadEvent());

            return true;
        } catch (Exception e) {
            log.warn("Failed to reload configuration: " + e.getMessage());
            return false;
        }
    }

    private void reloadPinatas() {
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (pinataManager.isPinata(entity)) {
                    pinataManager.activatePinata(entity);
                }
            }
        }
        log.info("Reloaded pinata entities and tasks.");
    }

    @Override
    public void onDisable() {
        if (pinataManager != null) {
            pinataManager.cleanup();
        }
        if (bossBarManager != null) {
            bossBarManager.removeAll();
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }

    public static PartyAnimals getPlugin() {
        return plugin;
    }

    public static boolean isFolia() {
        return isFolia;
    }

    public Logger getPluginLogger() {
        return log;
    }

    public ConfigManager getConfiguration() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MessageUtils getMessageUtils() {
        return messageUtils;
    }

    public PinataManager getPinataManager() {
        return pinataManager;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public HitCooldownHandler getHitCooldownHandler() {
        return hitCooldownHandler;
    }

    public EffectHandler getEffectHandler() {
        return effectHandler;
    }

    public RewardHandler getRewardHandler() {
        return rewardHandler;
    }

    public ReflexHandler getReflexHandler() {
        return reflexHandler;
    }
}
