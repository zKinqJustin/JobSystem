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

    public JobManager(JobSystem plugin) {
        this.plugin = plugin;
        this.jobs = new HashMap<>();
        this.activeJobs = new HashMap<>();
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
            DatabaseManager.JobData jobData = plugin.getDatabaseManager().getPlayerJob(player.getUniqueId(), jobName);
            int level = 1;
            int experience = 0;
            if (jobData != null) {
                level = jobData.level;
                experience = jobData.experience;
            }
            plugin.getDatabaseManager().savePlayerJob(player, jobName, level, experience);
            activeJobs.put(player, jobName);
            applyJobBuffs(player, jobName, level);
            player.sendMessage(plugin.getConfigManager().getColoredMessage("job_set").replace("%job%", jobName));
            updateScoreboard(player);
        }
    }

    public void addExperience(Player player, String jobName, int amount) {
        String activeJob = activeJobs.get(player);
        if (activeJob != null && activeJob.equals(jobName)) {
            DatabaseManager.JobData jobData = plugin.getDatabaseManager().getPlayerJob(player.getUniqueId(), jobName);
            if (jobData != null) {
                int newExperience = jobData.experience + amount;
                int newLevel = jobData.level;
                boolean leveledUp = false;
                while (newExperience >= getExperienceForNextLevel(newLevel)) {
                    newExperience -= getExperienceForNextLevel(newLevel);
                    newLevel++;
                    leveledUp = true;
                }
                if (leveledUp) {
                    player.sendMessage(plugin.getConfigManager().getColoredMessage("level_up")
                            .replace("%job%", jobName)
                            .replace("%level%", String.valueOf(newLevel)));
                    applyJobBuffs(player, jobName, newLevel);
                }
                plugin.getDatabaseManager().savePlayerJob(player, jobName, newLevel, newExperience);
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
        DatabaseManager.JobData jobData = plugin.getDatabaseManager().getPlayerJob(player.getUniqueId(), activeJob);
        String jobName = (activeJob != null) ? activeJob : "None";
        int level = (jobData != null) ? jobData.level : 0;

        Score jobScore = objective.getScore(plugin.getConfigManager().getColoredString("scoreboard.job").replace("%job%", jobName));
        jobScore.setScore(3);

        Score levelScore = objective.getScore(plugin.getConfigManager().getColoredString("scoreboard.level").replace("%level%", String.valueOf(level)));
        levelScore.setScore(2);

        Score moneyScore = objective.getScore(plugin.getConfigManager().getColoredString("scoreboard.money").replace("%money%", String.format("%.2f", plugin.getEconomy().getBalance(player))));
        moneyScore.setScore(1);

        player.setScoreboard(board);
    }

    public String getActiveJob(Player player) {
        return activeJobs.get(player);
    }

    public void loadPlayerJob(Player player) {
        Map<String, DatabaseManager.JobData> jobs = plugin.getDatabaseManager().getPlayerJobs(player.getUniqueId());
        if (!jobs.isEmpty()) {
            String lastJob = jobs.keySet().iterator().next(); // Get the first job (assuming it's the last active one)
            setPlayerJob(player, lastJob);
        }
    }
}

