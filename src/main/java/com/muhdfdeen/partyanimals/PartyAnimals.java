package com.muhdfdeen.partyanimals;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import java.util.List;

import org.bstats.bukkit.Metrics;

import com.muhdfdeen.partyanimals.api.event.PartyAnimalsReloadEvent;
import com.muhdfdeen.partyanimals.command.PartyAnimalsCommand;
import com.muhdfdeen.partyanimals.config.ConfigManager;
import com.muhdfdeen.partyanimals.handler.RewardHandler;
import com.muhdfdeen.partyanimals.hook.PartyAnimalsExpansion;
import com.muhdfdeen.partyanimals.handler.HitCooldownHandler;
import com.muhdfdeen.partyanimals.handler.EffectHandler;
import com.muhdfdeen.partyanimals.handler.MessageHandler;
import com.muhdfdeen.partyanimals.listener.PinataListener;
import com.muhdfdeen.partyanimals.manager.BossBarManager;
import com.muhdfdeen.partyanimals.manager.DatabaseManager;
import com.muhdfdeen.partyanimals.manager.PinataManager;
import com.muhdfdeen.partyanimals.util.Logger;
import com.muhdfdeen.partyanimals.util.UpdateChecker;

public final class PartyAnimals extends JavaPlugin {
    private static PartyAnimals plugin;
    
    private ConfigManager configManager;
    private Logger log;
    private PinataManager pinataManager;
    private MessageHandler messageHandler;
    private BossBarManager bossBarManager;
    private HitCooldownHandler hitCooldownHandler;
    private EffectHandler effectHandler;
    private RewardHandler rewardHandler;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        plugin = this;
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

        this.messageHandler = new MessageHandler(configManager);
        this.bossBarManager = new BossBarManager(this);
        this.effectHandler = new EffectHandler(log);

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();

        setupModules();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            if (pinataManager != null) {
                new PartyAnimalsExpansion(this).register();
                log.info("Hooked into PlaceholderAPI.");
            }
        }

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            PartyAnimalsCommand partyanimalsCommand = new PartyAnimalsCommand(this);
            event.registrar().register(partyanimalsCommand.createCommand("partyanimals"), "Main command", List.of("pa"));
        });

        new UpdateChecker(this).checkForUpdates();
    }

    private void setupModules() {
        boolean pinataEnabled = configManager.getMainConfig().modules.pinata().enabled();
        if (pinataEnabled) {
            if (this.pinataManager == null) {
                this.pinataManager = new PinataManager(this);
                this.hitCooldownHandler = new HitCooldownHandler(this);
                this.rewardHandler = new RewardHandler(this);
                getServer().getPluginManager().registerEvents(new PinataListener(this), this);
                log.info("Pinata module enabled.");
            }
        } else {
            if (this.pinataManager != null) {
                this.pinataManager.cleanup();
                this.pinataManager = null;
                this.hitCooldownHandler = null;
                this.rewardHandler = null;
                log.info("Pinata module disabled.");
            }
        }
    }

    public boolean reload() {
        try {
            if (pinataManager != null) {
                pinataManager.cleanup(false);
            }

            configManager.loadConfig();
            configManager.loadMessages();

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

    public Logger getPluginLogger() {
        return log;
    }

    public ConfigManager getConfiguration() {
        return configManager;
    }

    public PinataManager getPinataManager() {
        return pinataManager;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
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

    public DatabaseManager gDatabaseManager() {
        return databaseManager;
    }
}
