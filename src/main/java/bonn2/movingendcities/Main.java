package bonn2.movingendcities;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Objects;

public final class Main extends JavaPlugin {

    public static Main plugin;

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;

        setupConfig();

        Objects.requireNonNull(this.getCommand("newcity")).setExecutor(new NewCommand());
        this.getCommand("removecity").setExecutor(new RemoveCommand());
        new BukkitRunnable() {

            @Override
            public void run() {
                TimedCheck.Check();
            }

        }.runTaskLater(plugin, 2400);
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
