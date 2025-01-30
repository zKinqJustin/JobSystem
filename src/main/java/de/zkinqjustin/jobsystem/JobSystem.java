package de.zkinqjustin.jobsystem;

import de.zkinqjustin.jobsystem.commands.JobsCommand;
import de.zkinqjustin.jobsystem.config.ConfigManager;
import de.zkinqjustin.jobsystem.database.DatabaseManager;
import de.zkinqjustin.jobsystem.jobs.JobManager;
import de.zkinqjustin.jobsystem.listeners.JobEventListener;
import de.zkinqjustin.jobsystem.listeners.JobBuffListener;
import de.zkinqjustin.jobsystem.listeners.PlayerJoinListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class JobSystem extends JavaPlugin {

    private static JobSystem instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private JobManager jobManager;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize config
        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        // Initialize database
        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        // Initialize job manager
        jobManager = new JobManager(this);

        // Setup Vault economy
        if (!setupEconomy()) {
            getLogger().severe("Vault not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register commands
        getCommand("jobs").setExecutor(new JobsCommand(this));

        // Register event listeners
        getServer().getPluginManager().registerEvents(new JobEventListener(this), this);
        getServer().getPluginManager().registerEvents(new JobBuffListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        getLogger().info("JobSystem has been enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("JobSystem has been disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public static JobSystem getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public JobManager getJobManager() {
        return jobManager;
    }

    public Economy getEconomy() {
        return economy;
    }
}

