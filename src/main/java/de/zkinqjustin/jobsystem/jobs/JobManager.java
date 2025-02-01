package de.zkinqjustin.jobsystem.jobs;

import de.zkinqjustin.jobsystem.JobSystem;
import de.zkinqjustin.jobsystem.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;

public class JobManager {

    private final JobSystem plugin;
    private final Map<String, Job> jobs;
    private final Map<Player, String> activeJobs;
    private final Map<Player, Map<String, DatabaseManager.JobData>> playerJobData;

    public JobManager(JobSystem plugin) {
        this.plugin = plugin;
        this.jobs = new HashMap<>();
        this.activeJobs = new HashMap<>();
        this.playerJobData = new HashMap<>();
        loadJobs();
    }

    private void loadJobs() {
        jobs.put("Woodcutter", new Job("Woodcutter", "Chop trees to earn money and experience"));
        jobs.put("Fisher", new Job("Fisher", "Catch fish to earn money and experience"));
        jobs.put("Miner", new Job("Miner", "Mine ores to earn money and experience"));
        jobs.put("Butcher", new Job("Butcher", "Slaughter animals to earn money and experience"));
        jobs.put("Farmer", new Job("Farmer", "Harvest crops to earn money and experience"));
    }

    public void setPlayerJob(Player player, String jobName) {
        Job job = jobs.get(jobName);
        if (job != null) {
            activeJobs.put(player, jobName);
            if (!playerJobData.containsKey(player)) {
                playerJobData.put(player, new HashMap<>());
            }
            Map<String, DatabaseManager.JobData> jobData = playerJobData.get(player);
            DatabaseManager.JobData data = jobData.getOrDefault(jobName, new DatabaseManager.JobData(1, 0));
            applyJobBuffs(player, jobName, data.level);
            savePlayerData(player);
            player.sendMessage(plugin.getConfigManager().getColoredMessage("job_set").replace("%job%", jobName));
            updateScoreboard(player);
        }
    }

    public void addExperience(Player player, String jobName, int amount) {
        String activeJob = activeJobs.get(player);
        if (activeJob != null && activeJob.equals(jobName)) {
            Map<String, DatabaseManager.JobData> jobData = playerJobData.get(player);
            if (jobData != null) {
                DatabaseManager.JobData data = jobData.getOrDefault(jobName, new DatabaseManager.JobData(1, 0));
                data.xp += amount;
                boolean leveledUp = false;
                while (data.xp >= getExperienceForNextLevel(data.level)) {
                    data.xp -= getExperienceForNextLevel(data.level);
                    data.level++;
                    leveledUp = true;
                }
                if (leveledUp) {
                    player.sendMessage(plugin.getConfigManager().getColoredMessage("level_up")
                            .replace("%job%", jobName)
                            .replace("%level%", String.valueOf(data.level)));
                    applyJobBuffs(player, jobName, data.level);
                }
                jobData.put(jobName, data);
                savePlayerData(player);
                updateScoreboard(player);
            }
        }
    }

    private int getExperienceForNextLevel(int currentLevel) {
        // You can implement a custom leveling curve here
        return currentLevel * 100;
    }

    private void applyJobBuffs(Player player, String jobName, int level) {
        // Apply job-specific buffs based on level
        // This method remains unchanged
    }

    public Job getJob(String jobName) {
        return jobs.get(jobName);
    }

    public Map<String, Job> getJobs() {
        return jobs;
    }

    public void updateScoreboard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("jobinfo", "dummy", plugin.getConfigManager().getColoredString("scoreboard.title"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        String activeJob = activeJobs.get(player);
        String jobName = (activeJob != null) ? activeJob : "None";
        DatabaseManager.JobData jobData = getJobData(player, activeJob);
        int level = jobData != null ? jobData.level : 1;
        int xp = jobData != null ? jobData.xp : 0;
        int xpForNextLevel = getExperienceForNextLevel(level);

        Score jobScore = objective.getScore(plugin.getConfigManager().getColoredString("scoreboard.job").replace("%job%", jobName));
        jobScore.setScore(4);

        Score levelScore = objective.getScore(plugin.getConfigManager().getColoredString("scoreboard.level").replace("%level%", String.valueOf(level)));
        levelScore.setScore(3);

        Score xpScore = objective.getScore(plugin.getConfigManager().getColoredString("scoreboard.xp").replace("%xp%", xp + "/" + xpForNextLevel));
        xpScore.setScore(2);

        Score moneyScore = objective.getScore(plugin.getConfigManager().getColoredString("scoreboard.money").replace("%money%", String.format("%.2f", plugin.getEconomy().getBalance(player))));
        moneyScore.setScore(1);

        player.setScoreboard(board);
    }

    public String getActiveJob(Player player) {
        return activeJobs.get(player);
    }

    public void loadPlayerJob(Player player) {
        DatabaseManager.PlayerJobData jobData = plugin.getDatabaseManager().getPlayerJobData(player.getUniqueId());
        if (jobData != null) {
            activeJobs.put(player, jobData.currentJob);
            playerJobData.put(player, jobData.jobData);
            updateScoreboard(player);
        }
    }

    public DatabaseManager.JobData getJobData(Player player, String jobName) {
        Map<String, DatabaseManager.JobData> jobData = playerJobData.get(player);
        return jobData != null ? jobData.getOrDefault(jobName, new DatabaseManager.JobData(1, 0)) : new DatabaseManager.JobData(1, 0);
    }

    private void savePlayerData(Player player) {
        String currentJob = activeJobs.get(player);
        Map<String, DatabaseManager.JobData> jobData = playerJobData.get(player);
        if (currentJob != null && jobData != null) {
            plugin.getDatabaseManager().savePlayerJob(player, currentJob, jobData);
        }
    }

    public int getJobLevel(Player player, String jobName) {
        Map<String, DatabaseManager.JobData> jobData = playerJobData.get(player);
        if (jobData != null && jobData.containsKey(jobName)) {
            return jobData.get(jobName).level;
        }
        return 1; // Default level if no data is found
    }
}

