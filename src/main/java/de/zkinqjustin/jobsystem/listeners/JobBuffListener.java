package de.zkinqjustin.jobsystem.listeners;

import de.zkinqjustin.jobsystem.JobSystem;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JobBuffListener implements Listener {

    private final JobSystem plugin;
    private final Random random = new Random();

    public JobBuffListener(JobSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String activeJob = plugin.getJobManager().getActiveJob(player);
        if (activeJob == null) return;

        int jobLevel = plugin.getJobManager().getJobLevel(player, activeJob);

        switch (activeJob.toLowerCase()) {
            case "woodcutter":
                if (jobLevel >= plugin.getConfig().getInt("buff_levels.woodcutter", 5)) {
                    int blocksBreak = handleWoodcutterBuff(event);
                    plugin.getJobManager().addExperience(player, activeJob, plugin.getConfigManager().getInt("xp.woodcutter.log") * blocksBreak);
                    plugin.getEconomy().depositPlayer(player, plugin.getConfigManager().getDouble("money.woodcutter") * blocksBreak);
                }
                break;
            case "miner":
                if (jobLevel >= plugin.getConfig().getInt("buff_levels.miner", 5)) {
                    int blocksBreak = handleMinerBuff(event);
                    plugin.getJobManager().addExperience(player, activeJob, plugin.getConfigManager().getInt("xp.miner.ore") * blocksBreak);
                    plugin.getEconomy().depositPlayer(player, plugin.getConfigManager().getDouble("money.miner") * blocksBreak);
                }
                break;
            case "farmer":
                if (jobLevel >= plugin.getConfig().getInt("buff_levels.farmer", 5)) {
                    handleFarmerBuff(event);
                }
                break;
        }
    }

    private int handleWoodcutterBuff(BlockBreakEvent event) {
        if (event.getBlock().getType().name().endsWith("_LOG")) {
            return breakConnectedBlocks(event.getBlock(), event.getBlock().getType());
        }
        return 0;
    }

    private int handleMinerBuff(BlockBreakEvent event) {
        if (event.getBlock().getType().name().endsWith("_ORE")) {
            return breakConnectedBlocks(event.getBlock(), event.getBlock().getType());
        }
        return 0;
    }

    private int breakConnectedBlocks(Block startBlock, Material material) {
        List<Block> blocksToBreak = new ArrayList<>();
        findConnectedBlocks(startBlock, material, blocksToBreak, 64); // Limit to 64 blocks to prevent lag

        for (Block block : blocksToBreak) {
            block.breakNaturally();
        }

        return blocksToBreak.size();
    }

    private void findConnectedBlocks(Block block, Material material, List<Block> blocksToBreak, int limit) {
        if (blocksToBreak.size() >= limit) return;
        if (block.getType() != material) return;
        if (blocksToBreak.contains(block)) return;

        blocksToBreak.add(block);

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    Block relativeBlock = block.getRelative(x, y, z);
                    findConnectedBlocks(relativeBlock, material, blocksToBreak, limit);
                }
            }
        }
    }

    private void handleFarmerBuff(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (block.getBlockData() instanceof Ageable) {
            Ageable ageable = (Ageable) block.getBlockData();
            if (ageable.getAge() == ageable.getMaximumAge()) {
                Material cropType = block.getType();
                Material seedType = getSeedType(cropType);
                if (seedType != null && player.getInventory().contains(seedType)) {
                    player.getInventory().removeItem(new ItemStack(seedType, 1));
                    block.setType(cropType);
                    Ageable newCrop = (Ageable) block.getBlockData();
                    newCrop.setAge(0);
                    block.setBlockData(newCrop);
                }
            }
        }
    }

    private Material getSeedType(Material cropType) {
        switch (cropType) {
            case WHEAT: return Material.WHEAT_SEEDS;
            case CARROTS: return Material.CARROT;
            case POTATOES: return Material.POTATO;
            case BEETROOTS: return Material.BEETROOT_SEEDS;
            default: return null;
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Player player = event.getPlayer();
            String activeJob = plugin.getJobManager().getActiveJob(player);
            if (activeJob != null && activeJob.equalsIgnoreCase("fisher")) {
                int jobLevel = plugin.getJobManager().getJobLevel(player, activeJob);
                if (jobLevel >= plugin.getConfig().getInt("buff_levels.fisher", 5)) {
                    if (random.nextDouble() < 0.1) { // 10% chance for treasure
                        Entity caught = event.getCaught();
                        if (caught instanceof Item) {
                            Item item = (Item) caught;
                            item.setItemStack(getRandomTreasure());
                        }
                    }
                }
            }
        }
    }

    private ItemStack getRandomTreasure() {
        Material[] treasures = {
                Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT, Material.IRON_INGOT,
                Material.PRISMARINE_CRYSTALS, Material.PRISMARINE_SHARD, Material.NAUTILUS_SHELL
        };
        return new ItemStack(treasures[random.nextInt(treasures.length)]);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            String activeJob = plugin.getJobManager().getActiveJob(killer);
            if (activeJob != null && activeJob.equalsIgnoreCase("butcher")) {
                int jobLevel = plugin.getJobManager().getJobLevel(killer, activeJob);
                if (jobLevel >= plugin.getConfig().getInt("buff_levels.butcher", 5)) {
                    ItemStack weapon = killer.getInventory().getItemInMainHand();
                    if (weapon.getType().name().endsWith("_AXE")) {
                        List<ItemStack> drops = event.getDrops();
                        int extraDrops = 0;
                        for (ItemStack drop : new ArrayList<>(drops)) {
                            if (random.nextDouble() < 0.5) { // 50% chance to double each drop
                                drops.add(drop.clone());
                                extraDrops++;
                            }
                        }
                        plugin.getJobManager().addExperience(killer, activeJob, plugin.getConfigManager().getInt("xp.butcher.kill") * (1 + extraDrops));
                        plugin.getEconomy().depositPlayer(killer, plugin.getConfigManager().getDouble("money.butcher") * (1 + extraDrops));
                    }
                }
            }
        }
    }
}

