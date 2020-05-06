package bonn2.movingendcities;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;

public class EndCityManager {

    private static Location getEndCityLocation(World world) {
        Main plugin = Main.plugin;
        WorldBorder worldBorder = world.getWorldBorder();
        Location center = worldBorder.getCenter();
        int size = (int) worldBorder.getSize();
        Random rand = new Random();
        int x = rand.nextInt(size);
        int z = rand.nextInt(size);
        if (x < 1000) {
            x += 1000;
        }
        if (z < 1000){
            z += 1000;
        }
        if (x > (size / 2)) {
            x /= -2;
        }
        if (z > (size / 2)) {
            z /= -2;
        }
        x -= center.getBlockX();
        z -= center.getBlockZ();
        int y = 75;
        Location destination = new Location(world, x, y, z);
        if (destination.getBlock().getBiome() != Biome.END_HIGHLANDS) {
            return null;
        }
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Player player : players) {
            if (plugin.getConfig().getStringList("Worlds").contains(player.getWorld().getName())) {
                if (destination.distance(player.getLocation()) <= 500) {
                    return null;
                }
            }
        }
        while (destination.getBlock().getType() != Material.END_STONE) {
            if (destination.getY() < 56) {
                return null;
            }
            destination.setY(destination.getBlockY() - 1);
        }
        File endcityYml = new File(plugin.getDataFolder() + File.separator + "cities.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(endcityYml);
        for (String key : yml.getKeys(false)) {
            Location location = yml.getLocation(key + ".MinLocation");
            if (location.getWorld() == destination.getWorld()) {
                if (location.distance(destination) <= 200) {
                    return null;
                }
            }
        }
        return destination;
    }

    private static Clipboard getEndCityClipboard() {
        Main plugin = Main.plugin;
        File schemDir = new File(plugin.getDataFolder() + File.separator + "schematics");
        File[] schematics;
        File chosenSchematic;
        Random rand = new Random();
        if (schemDir.isDirectory()) {
            schematics = schemDir.listFiles();
            if (schematics == null) {
                return null;
            }
            chosenSchematic = schematics[rand.nextInt(schematics.length)];
            Clipboard clipboard = null;
            try (ClipboardReader reader = ClipboardFormats.findByFile(chosenSchematic).getReader(new FileInputStream(chosenSchematic))) {
                clipboard = reader.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return clipboard;
        }
        return null;
    }

    private static void savePrePasteLocation(Location destination, Clipboard clipboard, String name) {
        Main plugin = Main.plugin;
        if (plugin.getConfig().getBoolean("Debug")) {
            plugin.getLogger().info("Saving undo for city " + name);
        }
        BlockVector3 dest3 = BlockVector3.at(destination.getX(), destination.getY(), destination.getZ());
        BlockVector3 minVector = clipboard.getMinimumPoint().subtract(clipboard.getOrigin());
        BlockVector3 maxVector = clipboard.getMaximumPoint().subtract(clipboard.getOrigin());
        BlockVector3 minPoint = minVector.add(dest3);
        BlockVector3 maxPoint = maxVector.add(dest3).add(0, 1, 0);
        Location minLocation = new Location(destination.getWorld(), minPoint.getBlockX(), minPoint.getBlockY(), minPoint.getBlockZ());
        Location maxLocation = new Location(destination.getWorld(), maxPoint.getBlockX(), maxPoint.getBlockY(), maxPoint.getBlockZ());
        System.out.println(minPoint.toString());
        System.out.println(maxPoint.toString());
        Region region = new CuboidRegion(new BukkitWorld(destination.getWorld()), minPoint, maxPoint);

        Clipboard newClipboard = new BlockArrayClipboard(region);

        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(new BukkitWorld(destination.getWorld()), -1)) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                    editSession, region, newClipboard, region.getMinimumPoint()
            );
            // configure here
            Operations.complete(forwardExtentCopy);
        } catch (WorldEditException e) {
            e.printStackTrace();
        }

        File outputFolder = new File(plugin.getDataFolder().getAbsolutePath() + File.separator + "undos");
        if (!outputFolder.exists()) {
            outputFolder.mkdir();
        }

        File outputFile = new File(plugin.getDataFolder().getAbsolutePath() + File.separator + "undos" + File.separator + name + ".schem");

        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(outputFile))) {
            writer.write(newClipboard);
        } catch (IOException e) {
            e.printStackTrace();
        }

        File endcityYml = new File(plugin.getDataFolder() + File.separator + "cities.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(endcityYml);
        yml.set(name + ".MinLocation", minLocation);
        yml.set(name + ".MaxLocation", maxLocation);
        try {
            yml.save(endcityYml);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (plugin.getConfig().getBoolean("Debug")) {
            plugin.getLogger().info("Saved undo for city " + name);
        }
    }

    public static void summonEndCity(World world, String name) {
        Main plugin = Main.plugin;
        if (plugin.getConfig().getBoolean("Debug")) {
            plugin.getLogger().info("Attempting to summon city " + name);
        }
        Location destination = getEndCityLocation(world);
        if (destination == null) {
            if (plugin.getConfig().getBoolean("Debug")) {
                plugin.getLogger().info("Failed to get location for city " + name + " retrying in 15 seconds");
            }
            new BukkitRunnable() {

                @Override
                public void run() {
                    summonEndCity(world, name);
                }

            }.runTaskLater(plugin, 300);
            return;
        }
        if (plugin.getConfig().getBoolean("Debug")) {
            plugin.getLogger().info("Got location, pasting...");
        }
        Clipboard clipboard = getEndCityClipboard();
        if (clipboard == null) {
            if (plugin.getConfig().getBoolean("Debug")) {
                plugin.getLogger().warning("Failed to get clipboard!");
            }
            return;
        }

        savePrePasteLocation(destination, clipboard, name);

        new BukkitRunnable() {

            @Override
            public void run() {
                try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(new BukkitWorld(destination.getWorld()), -1)) {
                    Operation operation = new ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(BlockVector3.at(destination.getX(), destination.getY() + 1, destination.getZ()))
                            .ignoreAirBlocks(true)
                            .build();
                    Operations.complete(operation);
                } catch (WorldEditException e) {
                    e.printStackTrace();
                    return;
                }
                Date createdDate = new Date();
                File endcityYml = new File(plugin.getDataFolder() + File.separator + "cities.yml");
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(endcityYml);
                yml.set(name + ".CreatedDate", createdDate);
                Chunk minChunk = yml.getLocation(name + ".MinLocation").getChunk();
                Chunk maxChunk = yml.getLocation(name + ".MaxLocation").getChunk();
                try {
                    yml.save(endcityYml);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Random rand = new Random();
                for(int cx = minChunk.getX(); cx <= maxChunk.getX(); cx++) {
                    for (int cz = minChunk.getZ(); cz <= maxChunk.getZ(); cz++) {
                        Chunk currentChunk = minChunk.getWorld().getChunkAt(cx, cz);
                        for (BlockState tileEntity : currentChunk.getTileEntities()) {
                            if (tileEntity instanceof Chest) {
                                Chest chest = (Chest) tileEntity;
                                chest.setSeed(rand.nextLong());
                                tileEntity.update(true);
                            }
                        }
                    }
                }
                Main.pasting = false;
                Location location = yml.getLocation(name + ".MaxLocation");
                plugin.getLogger().info("Pasted city " + name + " at (" + location.getX() + " " + location.getY() + " " + location.getZ() + ")");
            }

        }.runTaskLater(plugin, 100);
    }



    public static void deleteCity(String name) {
        Main plugin = Main.plugin;
        File endcityYml = new File(plugin.getDataFolder() + File.separator + "cities.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(endcityYml);
        yml.set(name, null);
        try {
            yml.save(endcityYml);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean removeCity(String name) {
        Main plugin = Main.plugin;
        if (plugin.getConfig().getBoolean("Debug")) {
            plugin.getLogger().info("Removing city " + name);
        }
        File endcityYml = new File(plugin.getDataFolder() + File.separator + "cities.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(endcityYml);
        if (!yml.getKeys(false).contains(name)) {
            return false;
        }
        Location pasteLocation = yml.getLocation(name + ".MinLocation");
        String worldName = pasteLocation.getWorld().getName();
        BlockVector3 minLocation = BlockVector3.at(yml.getLocation(name + ".MinLocation").getX(), yml.getLocation(name + ".MinLocation").getY(), yml.getLocation(name + ".MinLocation").getZ());
        BlockVector3 maxLocation = BlockVector3.at(yml.getLocation(name + ".MaxLocation").getX(), yml.getLocation(name + ".MaxLocation").getY(), yml.getLocation(name + ".MaxLocation").getZ());
        Region region = new CuboidRegion(new BukkitWorld(pasteLocation.getWorld()), minLocation, maxLocation);
        Location centerLoc = new Location(Bukkit.getWorld(worldName), region.getCenter().getX(), region.getCenter().getY(),region.getCenter().getZ());

        List<Material> keepList = new ArrayList<>();
        keepList.add(Material.VOID_AIR);
        keepList.add(Material.AIR);
        keepList.add(Material.CAVE_AIR);
        keepList.add(Material.END_STONE);
        keepList.add(Material.CHORUS_FLOWER);
        keepList.add(Material.CHORUS_PLANT);
        for (int x = 0; x < region.getWidth(); x++) {
            for (int z = 0; z < region.getLength(); z++) {
                for (int y = 0; y < region.getHeight(); y++) {
                    Block block = pasteLocation.getWorld().getBlockAt(x + minLocation.getX(), y + minLocation.getY(), z + minLocation.getZ());
                    if (!keepList.contains(block.getType())) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }

        File file = new File(plugin.getDataFolder() + File.separator + "undos" + File.separator + name + ".schem");
        Clipboard clipboard = null;

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            clipboard = reader.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(new BukkitWorld(pasteLocation.getWorld()), -1)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .ignoreAirBlocks(true)
                    .to(BlockVector3.at(pasteLocation.getX(), pasteLocation.getY(), pasteLocation.getZ()))
                    // configure here
                    .build();
            Operations.complete(operation);
        } catch (WorldEditException e) {
            e.printStackTrace();
        }
        if (plugin.getConfig().getBoolean("Debug")) {
            plugin.getLogger().info("Removed blocks! Removing entities...");
        }

        Collection<Entity> entites = Bukkit.getWorld(worldName).getNearbyEntities(centerLoc, region.getWidth() / 2, region.getHeight() / 2, region.getLength() / 2);
        int count = 0;
        for (Entity entity : entites) {
            if (entity != null && !(entity instanceof Player)) {
                entity.remove();
                count++;
            }
        }
        if (plugin.getConfig().getBoolean("Debug")) {
            plugin.getLogger().info("Removed " + count + " entities!");
        }
        plugin.getLogger().info("Removed city " + name + " at " + region.getMaximumPoint().toString());
        return true;
    }

    public static void regenCity(World world, String name) {
        Main.pasting = true;
        removeCity(name);
        Main plugin = Main.plugin;
        new BukkitRunnable() {

            @Override
            public void run() {
                summonEndCity(world, name);
            }

        }.runTaskLater(plugin, 600);
    }
}
