package bonn2.movingendcities.utils;

import bonn2.movingendcities.Main;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class BoundingBox {

    private final World world;
    private final int x1, y1, z1, x2, y2, z2;
    private final boolean valid;

    public BoundingBox(Chunk chunk) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Main plugin = Main.plugin;

        // NMS Classes and Methods
        Class<?> CraftWorld = plugin.getCraftWorld();
        Method getHandle = CraftWorld.getDeclaredMethod("getHandle");
        Class<?> NMSWorld = plugin.getNMSWorld();
        Method getChunkAt = NMSWorld.getMethod("getChunkAt", int.class, int.class);
        Class<?> NMSChunk = plugin.getNMSChunk();
        Method getStructureStartMap = NMSChunk.getDeclaredMethod("h");
        Class<?> StructureStart = plugin.getStructureStart();
        Method isValid = StructureStart.getDeclaredMethod("e");
        Method getStructureBoundingBox = StructureStart.getDeclaredMethod("c");
        Class<?> StructureBoundingBox = plugin.getStructureBoundingBox();

        // Logic
        world = chunk.getWorld();
        Object craftWorld = CraftWorld.cast(world);
        Object nmsWorld = NMSWorld.cast(getHandle.invoke(craftWorld));
        Object nmsChunk = getChunkAt.invoke(nmsWorld, chunk.getX(), chunk.getZ());
        Object structureStart = ((Map<String, Class<?>>) getStructureStartMap.invoke(nmsChunk)).get("EndCity");
        Object boundingBox = getStructureBoundingBox.invoke(structureStart);

        // Cache Values
        valid = (boolean) isValid.invoke(structureStart);
        x1 = StructureBoundingBox.getDeclaredField("a").getInt(boundingBox);
        y1 = StructureBoundingBox.getDeclaredField("b").getInt(boundingBox);
        z1 = StructureBoundingBox.getDeclaredField("c").getInt(boundingBox);
        x2 = StructureBoundingBox.getDeclaredField("d").getInt(boundingBox);
        y2 = StructureBoundingBox.getDeclaredField("e").getInt(boundingBox);
        z2 = StructureBoundingBox.getDeclaredField("f").getInt(boundingBox);
    }

    public boolean isValid() {
        return valid;
    }

    public Region getWorldEditRegion() {
        return new CuboidRegion(new BukkitWorld(world), BlockVector3.at(x1, y1, z1), BlockVector3.at(x2, y2, z2));
    }
}
