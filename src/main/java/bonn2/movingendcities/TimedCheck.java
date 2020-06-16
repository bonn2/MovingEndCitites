package bonn2.movingendcities;

import bonn2.movingendcities.utils.EndCityManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.IOException;
import java.util.*;

import static org.bukkit.Bukkit.getServer;

public class TimedCheck {

    // TODO: Schedule "Moving" End Cities to generate when server starts

    public static void start() {
        List<String> cities = new ArrayList<>();
        for (String key : Main.citiesYml.getKeys(false)) {
            if (Main.citiesYml.getBoolean(key + ".Moving")) {
                cities.add(key);
            }
        }
        for (String key : cities) {
            BukkitScheduler scheduler = getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(Main.plugin, () -> EndCityManager.summonEndCity(Bukkit.getWorld(Objects.requireNonNull(Main.citiesYml.getString(key + ".World"))), key), cities.indexOf(key) + (2 * cities.indexOf(key)));
        }
        scheduleCheckRegen();
        schedulePlayers();
    }

    private static void schedulePlayers() {
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
        for (Player player : players) {
            if (plugin.getConfig().getStringList("Worlds").contains(player.getWorld().getName())) {
                for(String key : Main.citiesYml.getKeys(false)) {
                    if (Main.citiesYml.getBoolean(key + ".Moving")) {
                        continue;
                    }
                    Location location = Main.citiesYml.getLocation(key + ".MinLocation");
                    assert location != null;
                    if (location.distance(player.getLocation()) <= 500) {
                        Main.citiesYml.get(key + ".MostRecentPlayer", new Date());
                        plugin.getLogger().info("Found player near " + key);
                    }
                }
            }
        }
        try {
            Main.saveCitiesYml();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (reschedule) {
            schedulePlayers();
        }
    }

    private static void scheduleCheckRegen() {
        Main plugin = Main.plugin;
        String[] timeString = Objects.requireNonNull(plugin.getConfig().getString("RegenTime")).split("/");
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
        String[] gracePeriod = Objects.requireNonNull(plugin.getConfig().getString("GracePeriod")).split("/");
        Date now = new Date();

        Map<String, Date> regenList = new HashMap<>();
        for(String key : Main.citiesYml.getKeys(false)) { // Populate list of cities that are ready to regen
            if (Main.citiesYml.getBoolean(key + ".Moving")) {
                continue;
            }
            Calendar cal = Calendar.getInstance();
            Date mostRecentPlayer = (Date) Main.citiesYml.get(key + ".MostRecentPlayer");
            Date createdDate = (Date) Main.citiesYml.get(key + ".CreatedDate");
            try {
                assert mostRecentPlayer != null;
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

        World world = Objects.requireNonNull(Main.citiesYml.getLocation(regen + ".MinLocation")).getWorld();
        EndCityManager.regenCity(world, regen);

        scheduleCheckRegen();
    }

}
