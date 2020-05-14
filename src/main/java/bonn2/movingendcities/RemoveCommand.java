package bonn2.movingendcities;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RemoveCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            if (EndCityManager.removeCity(args[0])) {
                EndCityManager.deleteCity(args[0]);
                return true;
            }
            return false;
        }
        return false;
    }
}
