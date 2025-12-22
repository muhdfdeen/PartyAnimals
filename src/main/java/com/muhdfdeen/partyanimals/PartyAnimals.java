package com.muhdfdeen.partyanimals;

import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import java.util.List;

import org.bstats.bukkit.Metrics;

import com.muhdfdeen.partyanimals.command.PartyAnimalsCommand;
import com.muhdfdeen.partyanimals.config.ConfigManager;
import com.muhdfdeen.partyanimals.listener.PinataListener;
import com.muhdfdeen.partyanimals.manager.PinataManager;
import com.muhdfdeen.partyanimals.util.Logger;
import com.muhdfdeen.partyanimals.util.UpdateChecker;

public final class PartyAnimals extends JavaPlugin {
    private static PartyAnimals plugin;
    private ConfigManager config;
    private Logger log;
    private PinataManager pinataManager;

    @Override
    public void onEnable() {
        plugin = this;
        this.config = new ConfigManager(getDataFolder());
        this.log = new Logger(this);
        if (!reload()) {
            getLogger().severe("Disabling plugin due to critical configuration error.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        @SuppressWarnings("unused")
        Metrics metrics = new Metrics(this, 28389);
        UpdateChecker updateChecker = new UpdateChecker(this);
        updateChecker.checkForUpdates();
        this.pinataManager = new PinataManager(this);
        getServer().getPluginManager().registerEvents(updateChecker, this);
        getServer().getPluginManager().registerEvents(new PinataListener(this), this);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            PartyAnimalsCommand partyanimalsCommand = new PartyAnimalsCommand(this);
            event.registrar().register(partyanimalsCommand.createCommand("partyanimals"), "Main PartyAnimals command",
                    List.of("pa"));
        });
        log.info("Plugin enabled successfully");
    }

    @Override
    public void onDisable() {
        if (pinataManager != null) {
            pinataManager.cleanup();
        }
    }

    public boolean reload() {
        try {
            if (pinataManager != null) {
                pinataManager.cleanup();
            }
            config.load();
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

    public static PartyAnimals getPlugin() {
        return plugin;
    }

    public Logger getPluginLogger() {
        return log;
    }

    public ConfigManager getConfiguration() {
        return config;
    }

    public PinataManager getPinataManager() {
        return pinataManager;
    }
}
