package de.zkinqjustin.jobsystem.database;

import de.zkinqjustin.jobsystem.JobSystem;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {

    private final JobSystem plugin;
    private Connection connection;

    public DatabaseManager(JobSystem plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        String host = plugin.getConfig().getString("database.host");
        int port = plugin.getConfig().getInt("database.port");
        String database = plugin.getConfig().getString("database.name");
        String username = plugin.getConfig().getString("database.username");
        String password = plugin.getConfig().getString("database.password");

        try {
            Class.forName("org.mariadb.jdbc.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:mariadb://" + host + ":" + port + "/" + database, username, password);
            createTables();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS player_jobs (" +
                "uuid VARCHAR(36)," +
                "job_name VARCHAR(50)," +
                "level INT," +
                "experience INT," +
                "PRIMARY KEY (uuid, job_name)" +
                ")";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public void savePlayerJob(Player player, String jobName, int level, int experience) {
        String sql = "INSERT INTO player_jobs (uuid, job_name, level, experience) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE level = ?, experience = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            pstmt.setString(2, jobName);
            pstmt.setInt(3, level);
            pstmt.setInt(4, experience);
            pstmt.setInt(5, level);
            pstmt.setInt(6, experience);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, JobData> getPlayerJobs(UUID uuid) {
        Map<String, JobData> jobs = new HashMap<>();
        String sql = "SELECT job_name, level, experience FROM player_jobs WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String jobName = rs.getString("job_name");
                int level = rs.getInt("level");
                int experience = rs.getInt("experience");
                jobs.put(jobName, new JobData(jobName, level, experience));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return jobs;
    }

    public JobData getPlayerJob(UUID uuid, String jobName) {
        String sql = "SELECT level, experience FROM player_jobs WHERE uuid = ? AND job_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, jobName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new JobData(jobName, rs.getInt("level"), rs.getInt("experience"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class JobData {
        public final String job;
        public final int level;
        public final int experience;

        public JobData(String job, int level, int experience) {
            this.job = job;
            this.level = level;
            this.experience = experience;
        }
    }
}

