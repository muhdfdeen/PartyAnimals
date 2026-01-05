package org.maboroshi.partyanimals.manager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.MainConfig.DatabaseSettings;
import org.maboroshi.partyanimals.config.settings.MainConfig.PoolSettings;
import org.maboroshi.partyanimals.util.Logger;

public class DatabaseManager {
    private final PartyAnimals plugin;
    private final Logger log;
    private HikariDataSource dataSource;

    private String votesTable;
    private String rewardsTable;
    private String serverDataTable;

    public DatabaseManager(PartyAnimals plugin) {
        this.plugin = plugin;
        this.log = plugin.getPluginLogger();
    }

    public void connect() {
        DatabaseSettings settings = plugin.getConfiguration().getMainConfig().database;
        PoolSettings pool = settings.pool;

        this.votesTable = settings.tablePrefix + "votes";
        this.rewardsTable = settings.tablePrefix + "offline_rewards";
        this.serverDataTable = settings.tablePrefix + "server_data";

        HikariConfig config = new HikariConfig();
        config.setPoolName("PartyAnimals-Pool");
        config.setConnectionTimeout(pool.connectionTimeout);
        config.setMaximumPoolSize(pool.maximumPoolSize);
        config.setLeakDetectionThreshold(pool.leakDetectionThreshold);

        String type = settings.type.toLowerCase();
        if (type.equals("mariadb")) {
            config.setDriverClassName("org.mariadb.jdbc.Driver");
            config.setJdbcUrl("jdbc:mariadb://" + settings.host + ":" + settings.port + "/" + settings.database);
            config.setUsername(settings.username);
            config.setPassword(settings.password);
            log.info("Connecting to MariaDB database...");

        } else if (type.equals("mysql")) {
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl("jdbc:mysql://"
                    + settings.host
                    + ":"
                    + settings.port
                    + "/"
                    + settings.database
                    + "?useSSL=false&autoReconnect=true");
            config.setUsername(settings.username);
            config.setPassword(settings.password);
            log.info("Connecting to MySQL database...");

        } else {
            config.setDriverClassName("org.sqlite.JDBC");
            config.setJdbcUrl("jdbc:sqlite:" + new File(plugin.getDataFolder(), "database.db").getAbsolutePath());
            log.info("Connecting to SQLite database...");
        }

        try {
            this.dataSource = new HikariDataSource(config);
            initializeTables();
            log.info("Successfully connected to the database.");
        } catch (Exception e) {
            log.error("Failed to connect to database! Please check your config.yml.");
            log.error("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void initializeTables() {
        String autoInc = isSQLite() ? "AUTOINCREMENT" : "AUTO_INCREMENT";

        String createVotesTable = "CREATE TABLE IF NOT EXISTS "
                + votesTable
                + " ("
                + "id INTEGER PRIMARY KEY "
                + autoInc
                + ", "
                + "uuid VARCHAR(36) NOT NULL, "
                + "username VARCHAR(16) NOT NULL, "
                + "amount INTEGER NOT NULL DEFAULT 1, "
                + "service VARCHAR(64) NOT NULL, "
                + "timestamp LONG NOT NULL"
                + ");";

        String createRewardsTable = "CREATE TABLE IF NOT EXISTS "
                + rewardsTable
                + " ("
                + "id INTEGER PRIMARY KEY "
                + autoInc
                + ", "
                + "uuid VARCHAR(36) NOT NULL, "
                + "command TEXT NOT NULL"
                + ");";

        String createServerDataTable = "CREATE TABLE IF NOT EXISTS "
                + serverDataTable
                + " ("
                + "key VARCHAR(64) PRIMARY KEY, "
                + "value TEXT"
                + ");";

        String indexName = votesTable + "_uuid_idx";
        String createIndex = "CREATE INDEX IF NOT EXISTS " + indexName + " ON " + votesTable + "(uuid);";

        try (Connection connection = getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(createVotesTable);
            statement.execute(createRewardsTable);
            statement.execute(createServerDataTable);
            statement.execute(createIndex);
            log.info(
                    "Database tables initialized (" + votesTable + ", " + rewardsTable + ", " + serverDataTable + ").");
        } catch (SQLException e) {
            log.error("Failed to create database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addVote(UUID uuid, String username, String service, int amount) {
        String sql =
                "INSERT INTO " + votesTable + " (uuid, username, service, amount, timestamp) VALUES (?, ?, ?, ?, ?);";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, username);
            statement.setString(3, service);
            statement.setInt(4, amount);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
            log.debug("Saved vote for " + username);
        } catch (SQLException e) {
            log.error("Failed to save vote: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getVotes(UUID uuid) {
        String sql = "SELECT SUM(amount) FROM " + votesTable + " WHERE uuid = ?;";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Failed to get votes: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public UUID getPlayerUUID(String playerName) {
        boolean forceOffline = plugin.getConfiguration().getMainConfig().modules.vote.forceOfflineUUIDs;

        if (forceOffline) {
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
        } else {
            try {
                return Bukkit.createProfile(playerName).getId();
            } catch (Exception ignored) {
            }

            return Bukkit.getOfflinePlayer(playerName).getUniqueId();
        }
    }

    public void queueRewards(UUID uuid, String command) {
        String sql = "INSERT INTO " + rewardsTable + " (uuid, command) VALUES (?, ?);";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, command);
            statement.executeUpdate();
            log.info("Queued reward for " + uuid);
        } catch (SQLException e) {
            log.error("Failed to queue reward: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<String> retrieveRewards(UUID uuid) {
        List<String> commands = new ArrayList<>();
        String selectSql = "SELECT command FROM " + rewardsTable + " WHERE uuid = ?;";
        String deleteSql = "DELETE FROM " + rewardsTable + " WHERE uuid = ?;";

        try {
            try (Connection connection = getConnection();
                    PreparedStatement selectStatement = connection.prepareStatement(selectSql)) {
                selectStatement.setString(1, uuid.toString());
                ResultSet resultSet = selectStatement.executeQuery();
                while (resultSet.next()) {
                    commands.add(resultSet.getString("command"));
                }
            }

            if (!commands.isEmpty()) {
                try (Connection connection = getConnection();
                        PreparedStatement deleteStatement = connection.prepareStatement(deleteSql)) {
                    deleteStatement.setString(1, uuid.toString());
                    deleteStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            log.error("Failed to retrieve rewards: " + e.getMessage());
            e.printStackTrace();
        }
        return commands;
    }

    public int getCommunityGoalProgress() {
        String sql = "SELECT value FROM " + serverDataTable + " WHERE key = 'community_vote_count';";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return Integer.parseInt(rs.getString("value"));
            }
        } catch (Exception e) {
        }
        return 0;
    }

    public int incrementCommunityGoalProgress() {
        String updateSql = "UPDATE " + serverDataTable + " SET value = value + 1 WHERE key = 'community_vote_count';";
        String selectSql = "SELECT value FROM " + serverDataTable + " WHERE key = 'community_vote_count';";

        try (Connection connection = getConnection()) {
            try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                int rowsAffected = updateStmt.executeUpdate();

                if (rowsAffected == 0) {
                    setCommunityGoalProgress(1);
                    return 1;
                }
            }

            try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    return Integer.parseInt(rs.getString("value"));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to atomic increment community goal: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    private void setCommunityGoalProgress(int value) {
        String sql = "REPLACE INTO " + serverDataTable + " (key, value) VALUES ('community_vote_count', ?);";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, String.valueOf(value));
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update community goal: " + e.getMessage());
        }
    }

    public record TopVoter(String name, int votes) {}

    public List<TopVoter> getTopVoters(long afterTimestamp, int limit) {
        List<TopVoter> results = new ArrayList<>();
        String sql = "SELECT MAX(username) as username, SUM(amount) as total FROM "
                + votesTable
                + " WHERE timestamp >= ? GROUP BY uuid ORDER BY total DESC LIMIT ?;";

        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, afterTimestamp);
            statement.setInt(2, limit);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                results.add(new TopVoter(rs.getString("username"), rs.getInt("total")));
            }
        } catch (SQLException e) {
            log.error("Failed to calculate top voters: " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("Database connection pool closed.");
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private boolean isSQLite() {
        if (dataSource == null) return false;
        String driver = dataSource.getDriverClassName();
        return driver != null && driver.contains("sqlite");
    }
}
