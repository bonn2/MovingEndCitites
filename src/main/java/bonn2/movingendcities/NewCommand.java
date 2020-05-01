package bonn2.movingendcities;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;

import java.io.*;
import java.util.Collection;
import java.util.Date;
import java.util.Random;

public class NewCommand implements CommandExecutor {

    Main plugin = Main.plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.getWorld().getEnvironment() == World.Environment.THE_END) {
                int count = 0;
                Location destination = getEndCityLocation(player.getWorld());
                while (destination == null) {
                    count++;
                    if (count > 10) {
                        player.sendMessage("Failed to find suitable location.");
                        return true;
                    }
                    destination = getEndCityLocation(player.getWorld());
                }
                player.sendMessage("Got Location at " + destination.getBlockX() + " " + destination.getBlockY() + " " + destination.getBlockZ());
                player.teleport(destination);
                Clipboard clipboard = getEndCityClipboard();
                summonEndCity(destination, clipboard, args[0]);
            }
        }
        return false;
    }

    private Location getEndCityLocation(World world) {
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
        while (destination.getBlock().getType() != Material.END_STONE) {
            if (destination.getY() < 56) {
                return null;
            }
            destination.setY(destination.getBlockY() - 1);
        }
        if (destination.getBlock().getBiome() != Biome.END_HIGHLANDS) {
            return null;
        }
        return destination;
    }

    private Clipboard getEndCityClipboard() {
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

    private void savePrePasteLocation(Location destination, Clipboard clipboard, String name) {
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

        File outputFile = new File(plugin.getDataFolder().getAbsolutePath() + File.separator + "undos" + File.separator + name + ".schem");
        if (!outputFile.exists()) {
            try {
                outputFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
    }

    private void summonEndCity(Location destination, Clipboard clipboard, String name) {
        savePrePasteLocation(destination, clipboard, name);
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
    }
}
