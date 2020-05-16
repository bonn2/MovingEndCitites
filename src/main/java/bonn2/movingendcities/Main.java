package bonn2.movingendcities;

import bonn2.movingendcities.commands.NewCommand;
import bonn2.movingendcities.commands.RemoveCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class Main extends JavaPlugin {

    public static Main plugin;
    public static boolean pasting;

    private static Class<?> craftWorld;
    private static Class<?> nmsWorld;
    private static Class<?> nmsChunk;
    private static Class<?> structureStart;
    private static Class<?> structureBoundingBox;

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        pasting = false;

        try {
            setupNMSClasses();
        } catch (ClassNotFoundException | ArrayIndexOutOfBoundsException e) {
            getLogger().warning("Failed to initialize NMS classes!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        setupConfig();

        getServer().getPluginManager().registerEvents(new ChunkGenerateListener(), this);

        Objects.requireNonNull(this.getCommand("newcity")).setExecutor(new NewCommand());
        Objects.requireNonNull(this.getCommand("removecity")).setExecutor(new RemoveCommand());
        TimedCheck.scheduleCheckRegen();
        TimedCheck.schedulePlayers();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static void setupConfig() {
        File configyml = new File(plugin.getDataFolder() + File.separator + "config.yml");
        if (!configyml.exists()) { // Checks if config file exists
            plugin.getLogger().warning("No config.yml found, making a new one!");
            plugin.saveResource("config.yml", false);
        }
    }

    private void setupNMSClasses() throws ClassNotFoundException, ArrayIndexOutOfBoundsException {
        String version;
        version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

        craftWorld = Class.forName("org.bukkit.craftbukkit." + version + ".CraftWorld");
        nmsWorld = Class.forName("net.minecraft.server." + version + ".World");
        nmsChunk = Class.forName("net.minecraft.server." + version + ".Chunk");
        structureStart = Class.forName("net.minecraft.server." + version + ".StructureStart");
        structureBoundingBox = Class.forName("net.minecraft.server." + version + ".StructureBoundingBox");
    }

    public Class<?> getCraftWorld() {
        return craftWorld;
    }

    public Class<?> getNMSWorld() {
        return nmsWorld;
    }

    public Class<?> getNMSChunk() {
        return nmsChunk;
    }

    public Class<?> getStructureStart() {
        return structureStart;
    }

    public Class<?> getStructureBoundingBox() {
        return structureBoundingBox;
    }
}
