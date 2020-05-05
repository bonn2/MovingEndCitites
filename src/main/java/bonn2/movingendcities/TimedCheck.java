package bonn2.movingendcities;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TimedCheck {

    public static void schedulePlayers() {
        Main plugin = Main.plugin;
        new BukkitRunnable() {

            @Override
            public void run() {
                checkPlayers(true);
            }

        }.runTaskLater(plugin, 1200);
    }

    private static void checkPlayers(boolean reschedule) {
        Main plugin = Main.plugin;
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        File endcityYml = new File(plugin.getDataFolder() + File.separator + "cities.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(endcityYml);
        for (Player player : players) {
            if (plugin.getConfig().getStringList("Worlds").contains(player.getWorld().getName())) {
                for(String key : yml.getKeys(false)) {
                    Location location = yml.getLocation(key + ".MinLocation");
                    if (location.distance(player.getLocation()) <= 500) {
                        yml.set(key + ".MostRecentPlayer", new Date());
                        plugin.getLogger().info("Found player near " + key);
                    }
                }
            }
        }
        try {
            yml.save(endcityYml);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (reschedule) {
            schedulePlayers();
        }
    }

    public static void scheduleCheckRegen() {
        Main plugin = Main.plugin;
        String[] timeString = plugin.getConfig().getString("RegenTime").split("/");
        int[] time = new int[timeString.length];
        try{
            for(int i = 0; i < timeString.length; i++) {
                time[i] = Integer.parseInt(timeString[i]);
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid RegenTime in config.yml, please use 'days/hours/minutes' format");
        }
        int delay = 0;
        delay += time[2] * 60 * 20; // Minutes
        delay += time[1] * 60 * 60 * 20; // Hours
        delay += time[0] * 24 * 60 * 60 * 20; // Days

        new BukkitRunnable() {

            @Override
            public void run() {
                checkRegen();
            }

        }.runTaskLater(plugin, delay);
    }

    private static void checkRegen() {
        Main plugin = Main.plugin;
        if (Main.pasting) {
            plugin.getLogger().warning("Still pasting previous job! Rescheduling regen!");
            scheduleCheckRegen();
            return;
        }
        checkPlayers(false);
        File endcityYml = new File(plugin.getDataFolder() + File.separator + "cities.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(endcityYml);
        String[] gracePeriod = plugin.getConfig().getString("GracePeriod").split("/");
        Date now = new Date();

        Map<String, Date> regenList = new HashMap<>();
        for(String key : yml.getKeys(false)) { // Populate list of cities that are ready to regen
            Calendar cal = Calendar.getInstance();
            Date mostRecentPlayer = (Date) yml.get(key + ".MostRecentPlayer");
            Date createdDate = (Date) yml.get(key + ".CreatedDate");
            try {
                cal.setTime(mostRecentPlayer);
                cal.add(Calendar.HOUR, 24 * Integer.parseInt(gracePeriod[0]));
                cal.add(Calendar.HOUR, Integer.parseInt(gracePeriod[1]));
                cal.add(Calendar.MINUTE, Integer.parseInt(gracePeriod[2]));
                if (cal.getTime().before(now)) {
                    regenList.put(key, createdDate);
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid GracePeriod in config.yml! Use format 'days/hours/minutes'");
                scheduleCheckRegen();
                return;
            } catch (NullPointerException e) {
                regenList.put(key, createdDate);
            }
        }

        if (regenList.isEmpty()) { // Handle empty regenList
            scheduleCheckRegen();
            return;
        }

        String regen = "";
        for(String key : regenList.keySet()) { // Select oldest city
            if (regen.equals("")) {
                regen = key;
            } else if (regenList.get(key).before(regenList.get(regen))) {
                regen = key;
            }
        }

        World world = yml.getLocation(regen + ".MinLocation").getWorld();
        EndCityManager.regenCity(world, regen);

        scheduleCheckRegen();
    }

}
