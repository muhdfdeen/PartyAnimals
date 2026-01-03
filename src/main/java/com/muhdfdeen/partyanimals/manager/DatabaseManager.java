package com.muhdfdeen.partyanimals.manager;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.util.Logger;

public class DatabaseManager {
    private final PartyAnimals plugin;
    private final Logger log;
    private final File databaseFile;
    private Connection connection;

    public DatabaseManager(PartyAnimals plugin) {
        this.plugin = plugin;
        this.log = plugin.getPluginLogger();
        this.databaseFile = new File(plugin.getDataFolder(), "database.db");
    }

    public void connect() {
        try {
            Class.forName("org.sqlite.JDBC");

            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();

            connection = DriverManager.getConnection(url);
            log.info("Successfully connected to the database.");
        } catch (ClassNotFoundException e) {
            log.error("Missing SQLite driver!");
            e.printStackTrace();
        } catch (SQLException e) {
            log.error("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                log.info("Database connection closed.");
            }
        } catch (SQLException e) {
            log.error("Failed to close connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
