package de.zkinqjustin.jobsystem.listeners;

import de.zkinqjustin.jobsystem.JobSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final JobSystem plugin;

    public PlayerJoinListener(JobSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getJobManager().loadPlayerJob(event.getPlayer());
    }
}

