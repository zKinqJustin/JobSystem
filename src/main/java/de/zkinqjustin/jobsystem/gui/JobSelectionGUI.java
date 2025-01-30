package de.zkinqjustin.jobsystem.gui;

import de.zkinqjustin.jobsystem.JobSystem;
import de.zkinqjustin.jobsystem.jobs.Job;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class JobSelectionGUI {

    private final JobSystem plugin;
    private final Player player;

    public JobSelectionGUI(JobSystem plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        String title = plugin.getConfigManager().getColoredString("gui.title");
        int size = plugin.getConfigManager().getInt("gui.size");
        Inventory inventory = Bukkit.createInventory(null, size, title);

        for (Job job : plugin.getJobManager().getJobs().values()) {
            ItemStack item = createJobItem(job);
            int slot = plugin.getConfigManager().getInt("gui.slots." + job.getName().toLowerCase());
            inventory.setItem(slot, item);
        }

        player.openInventory(inventory);
    }

    private ItemStack createJobItem(Job job) {
        String materialName = plugin.getConfigManager().getString("job_items." + job.getName().toLowerCase());
        Material material = Material.valueOf(materialName);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String displayName = plugin.getConfigManager().getColoredString("job_names." + job.getName().toLowerCase());
        meta.setDisplayName(displayName);

        List<String> lore = plugin.getConfigManager().getStringList("job_descriptions." + job.getName().toLowerCase());
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(coloredLore);

        item.setItemMeta(meta);
        return item;
    }
}

