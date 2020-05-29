package bonn2.movingendcities.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class TabComplete implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        List<String> output = new ArrayList<>();
        if (args.length == 1) {
            if ("version".startsWith(args[0].toLowerCase())) {
                output.add("version");
            }
        }
        return output;
    }
}
