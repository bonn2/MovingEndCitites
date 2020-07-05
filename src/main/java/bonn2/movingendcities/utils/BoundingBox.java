package bonn2.movingendcities.utils;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import net.minecraft.server.v1_16_R1.StructureBoundingBox;
import net.minecraft.server.v1_16_R1.StructureGenerator;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R1.CraftWorld;

public class BoundingBox {

    private int x1, y1, z1, x2, y2, z2;
    private final boolean valid;
    private final World world;
    StructureBoundingBox boundingBox;

    public BoundingBox(Chunk chunk) {

        world = chunk.getWorld();
        net.minecraft.server.v1_16_R1.World nmsWorld = ((CraftWorld) world).getHandle();
        net.minecraft.server.v1_16_R1.Chunk nmsChunk = nmsWorld.getChunkAt(chunk.getX(), chunk.getZ());
        try {
            boundingBox = nmsChunk.h().get(StructureGenerator.ENDCITY).c();
        } catch (NullPointerException e) {
            valid = false;
            return;
        }


        // Cache Values
        valid = nmsChunk.h().get(StructureGenerator.ENDCITY).e();
        x1 = boundingBox.a;
        y1 = boundingBox.b;
        z1 = boundingBox.c;
        x2 = boundingBox.d;
        y2 = boundingBox.e;
        z2 = boundingBox.f;
    }

    public boolean isValid() {
        return valid;
    }

    public Region getWorldEditRegion() {
        return new CuboidRegion(new BukkitWorld(world), BlockVector3.at(x1, y1, z1), BlockVector3.at(x2, y2, z2));
    }
}
