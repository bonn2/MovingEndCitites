package bonn2.movingendcities;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

public class TimedCheck {

    public static void Check() {
        Main plugin = Main.plugin;
        checkPlayers();
        checkRegen();
        new BukkitRunnable() {

            @Override
            public void run() {
                Check();
            }

        }.runTaskLater(plugin, 1200);
    }

    private static void checkPlayers() {
        Main plugin = Main.plugin;
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        File endcityYml = new File(plugin.getDataFolder() + File.separator + "cities.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(endcityYml);
        for (Player player : players) {
            System.out.println("Found player in " + player.getWorld().getName());
            if (plugin.getConfig().getStringList("Worlds").contains(player.getWorld().getName())) {
                for(String key : yml.getKeys(false)) {
                    Location location = yml.getLocation(key + ".MinLocation");
                    if (location.distance(player.getLocation()) <= 500) { //TODO: Make 500 configurable
                        yml.set(key + ".MostRecentPlayer", new Date());
                        System.out.println("Found player near " + key);
                    }
                }
            }
        }
        try {
            yml.save(endcityYml);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void checkRegen() {
        Main plugin = Main.plugin;
        File endcityYml = new File(plugin.getDataFolder() + File.separator + "cities.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(endcityYml);
        String[] maxAge = plugin.getConfig().getString("EndCityMaxAge").split("/");
        String[] gracePeriod = plugin.getConfig().getString("GracePeriod").split("/");
        Date now = new Date();
        for(String key : yml.getKeys(false)) {
            Calendar cal = Calendar.getInstance();
            Date mostRecentPlayer = (Date) yml.get(key + ".MostRecentPlayer");
            Boolean gracePeriodOver;
            try {
                cal.setTime(mostRecentPlayer);
                cal.add(Calendar.HOUR, 24 * Integer.parseInt(gracePeriod[0]));
                cal.add(Calendar.HOUR, Integer.parseInt(gracePeriod[1]));
                cal.add(Calendar.SECOND, Integer.parseInt(gracePeriod[2]));
                gracePeriodOver = cal.getTime().before(now);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid GracePeriod in config.yml! Use format 'days/hours/minutes'");
                return;
            } catch (NullPointerException e) {
                gracePeriodOver = true;
            }

            Date createdDate = (Date) yml.get(key + ".CreatedDate");
            Date removeDate;
            cal.setTime(createdDate);
            try {
                cal.add(Calendar.HOUR, 24 * Integer.parseInt(maxAge[0]));
                cal.add(Calendar.HOUR, Integer.parseInt(maxAge[1]));
                cal.add(Calendar.SECOND, Integer.parseInt(maxAge[2]));
                removeDate = cal.getTime();
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid EndCityMaxAge in config.yml! Use format 'days/hours/minutes'");
                return;
            }

            if (removeDate.before(now) && gracePeriodOver) {
                World world = yml.getLocation(key + ".MinLocation").getWorld();
                System.out.println("Removing " + key);
                EndCityManager.removeCity(key);
                System.out.println("Respawing " + key);
                EndCityManager.summonEndCity(world, key);
            }
        }
    }

}
