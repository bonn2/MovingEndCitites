package bonn2.movingendcities;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

public class RemoveCommand implements CommandExecutor {
    Main plugin = Main.plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            if (EndCityManager.removeCity(args[0])) {
                EndCityManager.deleteCity(args[0]);
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean removeCity(String name) {
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

        File file = new File(plugin.getDataFolder() + File.separator + "undos" + File.separator + name + ".schem");
        Clipboard clipboard = null;

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            clipboard = reader.read();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(new BukkitWorld(pasteLocation.getWorld()), -1)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(pasteLocation.getX(), pasteLocation.getY(), pasteLocation.getZ()))
                    // configure here
                    .build();
            Operations.complete(operation);
        } catch (WorldEditException e) {
            e.printStackTrace();
        }

        Collection<Entity> entites = Bukkit.getWorld(worldName).getNearbyEntities(centerLoc, region.getWidth() / 2, region.getHeight() / 2, region.getLength() / 2);
        for (Entity entity : entites) {
            if (entity != null && !(entity instanceof Player)) {
                entity.remove();
            }
        }
        return true;
    }
}
