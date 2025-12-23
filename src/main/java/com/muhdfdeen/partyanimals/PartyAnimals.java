package com.muhdfdeen.partyanimals;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import java.util.List;

import org.bstats.bukkit.Metrics;

import com.muhdfdeen.partyanimals.command.PartyAnimalsCommand;
import com.muhdfdeen.partyanimals.config.ConfigManager;
import com.muhdfdeen.partyanimals.handler.CommandHandler;
import com.muhdfdeen.partyanimals.handler.CooldownHandler;
import com.muhdfdeen.partyanimals.handler.EffectHandler;
import com.muhdfdeen.partyanimals.handler.MessageHandler;
import com.muhdfdeen.partyanimals.listener.PinataListener;
import com.muhdfdeen.partyanimals.manager.BossBarManager;
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
    private CooldownHandler cooldownHandler;
    private EffectHandler effectHandler;
    private CommandHandler commandHandler;

@Override
    public void onEnable() {
        plugin = this;
        this.configManager = new ConfigManager(getDataFolder());
        this.log = new Logger(this);

        try {
            configManager.loadConfig();
            configManager.loadMessages();
        } catch (Exception e) {
            getLogger().severe("Failed to load initial configuration: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.messageHandler = new MessageHandler(configManager);

        @SuppressWarnings("unused")
        Metrics metrics = new Metrics(this, 28389);

        this.bossBarManager = new BossBarManager(this);
        this.effectHandler = new EffectHandler(log);
        this.cooldownHandler = new CooldownHandler(this);
        this.commandHandler = new CommandHandler(this);
        this.pinataManager = new PinataManager(this);

        getServer().getPluginManager().registerEvents(new PinataListener(this), this);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            PartyAnimalsCommand partyanimalsCommand = new PartyAnimalsCommand(this);
            event.registrar().register(partyanimalsCommand.createCommand("partyanimals"), "Main PartyAnimals command", List.of("pa"));
        });

        UpdateChecker updateChecker = new UpdateChecker(this);
        updateChecker.checkForUpdates();
        getServer().getPluginManager().registerEvents(updateChecker, this);

        log.info("Plugin enabled successfully");
    }

    @Override
    public void onDisable() {
        if (pinataManager != null) {
            pinataManager.cleanup();
        }
        if (bossBarManager != null) {
            bossBarManager.removeAll(); 
        }
    }

    public boolean reload() {
        try {
            if (pinataManager != null) {
                pinataManager.cleanup();
            }
            
            configManager.loadConfig();
            configManager.loadMessages();
            
            if (pinataManager != null) {
                reloadPinatas();
            }

            return true;
        } catch (Exception e) {
            if (log != null) {
                log.error("Failed to load configuration: " + e.getMessage());
            } else {
                getLogger().severe("Failed to load configuration: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }

    private void reloadPinatas() {
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (pinataManager.isPinata(entity)) {
                    pinataManager.restorePinata(entity);
                }
            }
        }
        log.info("Reloaded pinata entities and tasks.");
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

    public CooldownHandler getCooldownHandler() {
        return cooldownHandler;
    }

    public EffectHandler getEffectHandler() {
        return effectHandler;
    }
    
    public CommandHandler getCommandHandler() {
        return commandHandler;
    }
}
