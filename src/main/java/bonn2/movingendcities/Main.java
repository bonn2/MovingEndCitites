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

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        pasting = false;

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

}
