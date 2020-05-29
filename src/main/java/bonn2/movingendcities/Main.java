package bonn2.movingendcities;

import bonn2.movingendcities.commands.OpenInvCommand;
import bonn2.movingendcities.commands.TabComplete;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class Main extends JavaPlugin {

    public static Main plugin;
    public static boolean pasting;
    public static YamlConfiguration citiesYml;

    private static File citiesYmlFile;
    private static Class<?> CraftWorld;
    private static Class<?> NMSWorld;
    private static Class<?> NMSChunk;
    private static Class<?> StructureStart;
    private static Class<?> StructureBoundingBox;

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

        citiesYmlFile = new File(plugin.getDataFolder() + File.separator + "cities.yml");
        citiesYml = YamlConfiguration.loadConfiguration(citiesYmlFile);

        getServer().getPluginManager().registerEvents(new ChunkGenerateListener(), this);

        Objects.requireNonNull(this.getCommand("movingendcities")).setExecutor(new OpenInvCommand());
        Objects.requireNonNull(this.getCommand("movingendcities")).setTabCompleter(new TabComplete());

        TimedCheck.start();
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

    public static void saveCitiesYml() throws IOException {
        citiesYml.save(citiesYmlFile);
    }

    private void setupNMSClasses() throws ClassNotFoundException, ArrayIndexOutOfBoundsException {
        String version;
        version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

        CraftWorld = Class.forName("org.bukkit.craftbukkit." + version + ".CraftWorld");
        NMSWorld = Class.forName("net.minecraft.server." + version + ".World");
        NMSChunk = Class.forName("net.minecraft.server." + version + ".Chunk");
        StructureStart = Class.forName("net.minecraft.server." + version + ".StructureStart");
        StructureBoundingBox = Class.forName("net.minecraft.server." + version + ".StructureBoundingBox");
    }

    public Class<?> getCraftWorld() {
        return CraftWorld;
    }

    public Class<?> getNMSWorld() {
        return NMSWorld;
    }

    public Class<?> getNMSChunk() {
        return NMSChunk;
    }

    public Class<?> getStructureStart() {
        return StructureStart;
    }

    public Class<?> getStructureBoundingBox() {
        return StructureBoundingBox;
    }
}
