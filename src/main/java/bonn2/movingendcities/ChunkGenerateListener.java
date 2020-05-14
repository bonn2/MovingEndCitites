package bonn2.movingendcities;

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class ChunkGenerateListener implements Listener {

    @EventHandler
    public void chunkGenerate(ChunkLoadEvent event) {

        // Only run logic if chunk is new
        if (!event.isNewChunk()) {
            return;
        }

        // Init Vars
        Main plugin = Main.plugin;
        Chunk chunk = event.getChunk();
        World world = event.getWorld();
        BoundingBox boundingBox = null;
        try {
            boundingBox = new BoundingBox(chunk);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        }

        // Only run in defined worlds
        if (!plugin.getConfig().getStringList("Worlds").contains(world.getName())) {
            return;
        }

        if (!boundingBox.isValid()) {
            return;
        }

        Region region = boundingBox.getWorldEditRegion();
        BlockVector3 minPoint = region.getMinimumPoint();
        BlockVector3 maxPoint = region.getMaximumPoint();
        int x1 = minPoint.getX();
        int y1 = minPoint.getY();
        int z1 = minPoint.getZ();
        int x2 = maxPoint.getX();
        int y2 = maxPoint.getY();
        int z2 = maxPoint.getZ();

        // List of blocks to remove
        List<Material> removeList = getRemoveList();

        // Load chunks and remove entities
        for (BlockVector2 blockVector2 : region.getChunks()) {
            Chunk removeChunk = world.getChunkAt(blockVector2.getX(), blockVector2.getZ());
            removeChunk.setForceLoaded(true);
            for (Entity entity : removeChunk.getEntities()) {
                if (!(entity instanceof Player)) {
                    entity.remove();
                }
            }
        }

        // Remove Blocks
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    Block block = chunk.getWorld().getBlockAt(x, y, z);
                    if (block instanceof Chest) {
                        Chest chest = (Chest) block;
                        chest.setLootTable(null);
                    }
                    if (removeList.contains(block.getType())) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }

        // TODO: Smooth out terrain after block deletion
        // TODO: Remove dummy entities from queue
            /*List<Material> airList = getAirList();
            for (int x = boundingBox.a; x <= boundingBox.d; x++) {
                for (int z = boundingBox.c; z <= boundingBox.f; z++) {
                    int highestY = world.getHighestBlockYAt(x, z);
                    if (highestY < boundingBox.b) {
                        continue;
                    }
                    for (int y = highestY; y > 0; y--) {
                        Block checkBlock = world.getBlockAt(x, y, z);
                        if (airList.contains(checkBlock.getType())) {
                            System.out.println(checkBlock.getType());
                            checkBlock.setType(Material.END_STONE);
                        } else {
                            System.out.println(checkBlock.getType());
                            break;
                        }
                    }
                }
            }*/

        // Double check all entities were removed
        for (BlockVector2 blockVector2 : region.getChunks()) {
            Chunk removeChunk = world.getChunkAt(blockVector2.getX(), blockVector2.getZ());
            for (Entity entity : removeChunk.getEntities()) {
                if (!(entity instanceof Player)) {
                    entity.remove();
                }
            }
            removeChunk.setForceLoaded(false);
        }
    }

    // Generates list of materials to remove
    private List<Material> getRemoveList() {
        List<Material> removeList = new ArrayList<>();
        removeList.add(Material.PURPUR_BLOCK);
        removeList.add(Material.PURPUR_STAIRS);
        removeList.add(Material.PURPUR_SLAB);
        removeList.add(Material.PURPUR_PILLAR);
        removeList.add(Material.MAGENTA_STAINED_GLASS);
        removeList.add(Material.END_STONE_BRICKS);
        removeList.add(Material.OBSIDIAN);
        removeList.add(Material.LADDER);
        removeList.add(Material.MAGENTA_BANNER);
        removeList.add(Material.DRAGON_WALL_HEAD);
        removeList.add(Material.END_ROD);
        removeList.add(Material.CHEST);
        removeList.add(Material.ENDER_CHEST);
        removeList.add(Material.BREWING_STAND);
        return removeList;
    }

    // Generate list of air types
    private List<Material> getAirList() {
        List<Material> airList = new ArrayList<>();
        airList.add(Material.AIR);
        airList.add(Material.CAVE_AIR);
        airList.add(Material.VOID_AIR);
        return airList;
    }
}
