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
                "uuid VARCHAR(36) PRIMARY KEY," +
                "current_job VARCHAR(50)," +
                "woodcutter_level INT DEFAULT 1," +
                "woodcutter_xp INT DEFAULT 0," +
                "fisher_level INT DEFAULT 1," +
                "fisher_xp INT DEFAULT 0," +
                "miner_level INT DEFAULT 1," +
                "miner_xp INT DEFAULT 0," +
                "butcher_level INT DEFAULT 1," +
                "butcher_xp INT DEFAULT 0," +
                "farmer_level INT DEFAULT 1," +
                "farmer_xp INT DEFAULT 0" +
                ")";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public void savePlayerJob(Player player, String currentJob, Map<String, JobData> jobData) {
        String sql = "INSERT INTO player_jobs (uuid, current_job, " +
                "woodcutter_level, woodcutter_xp, fisher_level, fisher_xp, " +
                "miner_level, miner_xp, butcher_level, butcher_xp, " +
                "farmer_level, farmer_xp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE current_job = ?, " +
                "woodcutter_level = ?, woodcutter_xp = ?, " +
                "fisher_level = ?, fisher_xp = ?, " +
                "miner_level = ?, miner_xp = ?, " +
                "butcher_level = ?, butcher_xp = ?, " +
                "farmer_level = ?, farmer_xp = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            pstmt.setString(2, currentJob);
            setJobData(pstmt, 3, jobData.get("Woodcutter"));
            setJobData(pstmt, 5, jobData.get("Fisher"));
            setJobData(pstmt, 7, jobData.get("Miner"));
            setJobData(pstmt, 9, jobData.get("Butcher"));
            setJobData(pstmt, 11, jobData.get("Farmer"));
            pstmt.setString(13, currentJob);
            setJobData(pstmt, 14, jobData.get("Woodcutter"));
            setJobData(pstmt, 16, jobData.get("Fisher"));
            setJobData(pstmt, 18, jobData.get("Miner"));
            setJobData(pstmt, 20, jobData.get("Butcher"));
            setJobData(pstmt, 22, jobData.get("Farmer"));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setJobData(PreparedStatement pstmt, int startIndex, JobData jobData) throws SQLException {
        if (jobData != null) {
            pstmt.setInt(startIndex, jobData.level);
            pstmt.setInt(startIndex + 1, jobData.xp);
        } else {
            pstmt.setInt(startIndex, 1);
            pstmt.setInt(startIndex + 1, 0);
        }
    }

    public PlayerJobData getPlayerJobData(UUID uuid) {
        String sql = "SELECT current_job, " +
                "woodcutter_level, woodcutter_xp, fisher_level, fisher_xp, " +
                "miner_level, miner_xp, butcher_level, butcher_xp, " +
                "farmer_level, farmer_xp FROM player_jobs WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String currentJob = rs.getString("current_job");
                Map<String, JobData> jobData = new HashMap<>();
                jobData.put("Woodcutter", new JobData(rs.getInt("woodcutter_level"), rs.getInt("woodcutter_xp")));
                jobData.put("Fisher", new JobData(rs.getInt("fisher_level"), rs.getInt("fisher_xp")));
                jobData.put("Miner", new JobData(rs.getInt("miner_level"), rs.getInt("miner_xp")));
                jobData.put("Butcher", new JobData(rs.getInt("butcher_level"), rs.getInt("butcher_xp")));
                jobData.put("Farmer", new JobData(rs.getInt("farmer_level"), rs.getInt("farmer_xp")));
                return new PlayerJobData(currentJob, jobData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class PlayerJobData {
        public final String currentJob;
        public final Map<String, JobData> jobData;

        public PlayerJobData(String currentJob, Map<String, JobData> jobData) {
            this.currentJob = currentJob;
            this.jobData = jobData;
        }
    }

    public static class JobData {
        public int level;
        public int xp;

        public JobData(int level, int xp) {
            this.level = level;
            this.xp = xp;
        }
    }
}

