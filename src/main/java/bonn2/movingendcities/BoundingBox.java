package bonn2.movingendcities;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import net.minecraft.server.v1_15_R1.StructureBoundingBox;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;

public class BoundingBox {

    private World world;
    private net.minecraft.server.v1_15_R1.World nmsWorld;
    private net.minecraft.server.v1_15_R1.Chunk nmsChunk;
    StructureBoundingBox boundingBox;

    public BoundingBox(Chunk chunk) {
        world = chunk.getWorld();
        nmsWorld  = ((CraftWorld) world).getHandle();
        nmsChunk = nmsWorld.getChunkAt(chunk.getX(), chunk.getZ());
        boundingBox = nmsChunk.h().get("EndCity").c();
    }

    public boolean isValid() {
        return !(boundingBox.a == 2147483647);
    }

    public Region getWorldEditRegion() {
        return new CuboidRegion(new BukkitWorld(world), BlockVector3.at(boundingBox.a, boundingBox.b, boundingBox.c), BlockVector3.at(boundingBox.d, boundingBox.e, boundingBox.f));
    }
}
