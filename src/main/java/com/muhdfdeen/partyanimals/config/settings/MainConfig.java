package com.muhdfdeen.partyanimals.config.settings;

import java.io.File;
import java.nio.file.Path;

import de.exlll.configlib.Comment;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;

public final class MainConfig {

    public static MainConfiguration load(File dataFolder) {
        YamlConfigurationProperties properties = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder().build();
        Path configFile = new File(dataFolder, "config.yml").toPath();
        return YamlConfigurations.update(configFile, MainConfiguration.class, properties);
    }

    @Configuration
    public static class MainConfiguration {

        @Comment("Enable debug mode to see detailed logs in the console.")
        public boolean debug = false;

        @Comment("Control which parts of the plugin should be active.")
        public ModuleSettings modules = new ModuleSettings(
            true,
            false
        );
    }

    public record ModuleSettings(
        @Comment("Toggle the pinata system.")
        boolean pinata,

        @Comment("Toggle the voting system.")
        boolean vote
    ) {}
}
