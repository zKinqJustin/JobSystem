package de.zkinqjustin.jobsystem.commands;

import de.zkinqjustin.jobsystem.JobSystem;
import de.zkinqjustin.jobsystem.gui.JobSelectionGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JobsCommand implements CommandExecutor {

    private final JobSystem plugin;

    public JobsCommand(JobSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_only"));
            return true;
        }

        Player player = (Player) sender;
        new JobSelectionGUI(plugin, player).open();
        return true;
    }
}

