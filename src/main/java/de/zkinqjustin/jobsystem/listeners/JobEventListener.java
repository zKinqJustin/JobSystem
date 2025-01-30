package de.zkinqjustin.jobsystem.listeners;

import de.zkinqjustin.jobsystem.JobSystem;
import de.zkinqjustin.jobsystem.database.DatabaseManager;
import de.zkinqjustin.jobsystem.jobs.Job;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.ChatColor;

public class JobEventListener implements Listener {

    private final JobSystem plugin;

    public JobEventListener(JobSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getString("gui.title")))) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null) {
                Player player = (Player) event.getWhoClicked();
                String jobName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                plugin.getJobManager().setPlayerJob(player, jobName);
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String activeJob = plugin.getJobManager().getActiveJob(player);
        if (activeJob != null) {
            Material material = event.getBlock().getType();
            if (activeJob.equals("Woodcutter") && material.name().endsWith("_LOG")) {
                addJobExperience(player, "Woodcutter", plugin.getConfigManager().getInt("xp.woodcutter.log"));
            } else if (activeJob.equals("Miner") && material.name().endsWith("_ORE")) {
                addJobExperience(player, "Miner", plugin.getConfigManager().getInt("xp.miner.ore"));
            }
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Player player = event.getPlayer();
            String activeJob = plugin.getJobManager().getActiveJob(player);
            if (activeJob != null && activeJob.equals("Fisher")) {
                addJobExperience(player, "Fisher", plugin.getConfigManager().getInt("xp.fisher.fish"));
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player != null) {
            String activeJob = plugin.getJobManager().getActiveJob(player);
            if (activeJob != null && activeJob.equals("Butcher")) {
                addJobExperience(player, "Butcher", plugin.getConfigManager().getInt("xp.butcher.kill"));
            }
        }
    }

    @EventHandler
    public void onPlayerHarvest(PlayerHarvestBlockEvent event) {
        Player player = event.getPlayer();
        String activeJob = plugin.getJobManager().getActiveJob(player);
        if (activeJob != null && activeJob.equals("Farmer")) {
            addJobExperience(player, "Farmer", plugin.getConfigManager().getInt("xp.farmer.harvest"));
        }
    }

    private void addJobExperience(Player player, String jobName, int amount) {
        plugin.getJobManager().addExperience(player, jobName, amount);
        double money = plugin.getConfigManager().getDouble("money." + jobName.toLowerCase());
        plugin.getEconomy().depositPlayer(player, money);
        player.sendMessage(plugin.getConfigManager().getColoredMessage("money_earned")
                .replace("%amount%", String.format("%.2f", money)));
    }
}

