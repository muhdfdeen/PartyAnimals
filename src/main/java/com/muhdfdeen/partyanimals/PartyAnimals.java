package com.muhdfdeen.partyanimals;

import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import org.bstats.bukkit.Metrics;

import com.muhdfdeen.partyanimals.command.PartyAnimalsCommand;
import com.muhdfdeen.partyanimals.config.Config;
import com.muhdfdeen.partyanimals.config.Config.MainConfiguration;
import com.muhdfdeen.partyanimals.util.Logger;
import com.muhdfdeen.partyanimals.util.UpdateChecker;

public final class PartyAnimals extends JavaPlugin {
    private static PartyAnimals plugin;
    private MainConfiguration config;
    private Logger log;

    @Override
    public void onEnable() {
        plugin = this;
        this.log = new Logger(this);
        UpdateChecker updateChecker = new UpdateChecker(this);
        updateChecker.checkForUpdates();
        if (!reload()) {
            log.error("Disabling plugin due to critical configuration error.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        @SuppressWarnings("unused")
        Metrics metrics = new Metrics(this, 28389);
        getServer().getPluginManager().registerEvents(updateChecker, this);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            PartyAnimalsCommand partyanimalsCommand = new PartyAnimalsCommand(this);
            event.registrar().register(partyanimalsCommand.createCommand("partyanimals"), "Main PartyAnimals command");
        });
        log.info("Plugin enabled successfully");
    }

    public boolean reload() {
        try {
            this.config = Config.load(getDataFolder());
            return true;
        } catch (Exception e) {
            log.error("Failed to load configuration: " + e.getMessage());
            return false;
        }
    }

    public static PartyAnimals getPlugin() {
        return plugin;
    }

    public Logger getPluginLogger() {
        return log;
    }

    public MainConfiguration getConfiguration() {
        return config;
    }
}
