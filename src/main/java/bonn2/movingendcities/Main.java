package bonn2.movingendcities;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class Main extends JavaPlugin {

    public static Main plugin;
    public static boolean pasting;

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        pasting = false;

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
}
