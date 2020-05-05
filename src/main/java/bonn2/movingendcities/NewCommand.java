package bonn2.movingendcities;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NewCommand implements CommandExecutor {

    Main plugin = Main.plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && args.length == 1) {
            Player player = (Player) sender;
            if (plugin.getConfig().getStringList("Worlds").contains(player.getWorld().getName())) {
                player.sendMessage("Attempting to summon end city in world " + player.getWorld().getName() + ".\nFurther info will be printed to console.");
                EndCityManager.summonEndCity(player.getWorld(), args[0]);
            } else {
                player.sendMessage("This world is not whitelisted in config.yml!");
            }
            return true;
        }
        return false;
    }
}
