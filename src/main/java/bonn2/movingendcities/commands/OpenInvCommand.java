package bonn2.movingendcities.commands;

import bonn2.movingendcities.Main;
import bonn2.movingendcities.inventories.ManageCitiesInventory;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

public class OpenInvCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (args.length == 0) {
            if (commandSender instanceof Player) {
                Player player = (Player) commandSender;
                ManageCitiesInventory manageCitiesInventory = new ManageCitiesInventory(player);
                manageCitiesInventory.open();
            } else {
                commandSender.sendMessage(ChatColor.RED + "This command can only be run by a player!");
            }
            return true;
        }
        if (args.length == 1) {
            if (args[0].toLowerCase().equals("version")) {
                PluginDescriptionFile pdf = Main.plugin.getDescription();
                commandSender.sendMessage(ChatColor.LIGHT_PURPLE + "MovingEndCities");
                commandSender.sendMessage("By: bonn2");
                commandSender.sendMessage("Version: " + pdf.getVersion());
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}
