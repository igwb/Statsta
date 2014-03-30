package me.igwb.Statsta.commands;

import me.igwb.Statsta.Statsta;
import me.igwb.Statsta.messages.Messages;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandListener implements CommandExecutor {


    /**
     * Create an instance of the CommandListener.
     */
    public CommandListener() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String firstArg, String[] args) {

        //We need the messages.
        Messages msg = Statsta.getMsg();

        String commandName = cmd.getName();

        if (commandName.equals("statsta") | commandName.equals("stats")) {

            //Check if there were any arguments. Else return.
            if (args == null || args.length == 0) {
                sender.sendMessage(msg.getMsg("cmd_statsta_usage"));
                return true;
            }

            String commandBase = args[0].toLowerCase();

            switch (commandBase) {
            case "info":
                CmdInfo.execute(sender, args);
                return true;
            case "top":
                CmdTop.execute(sender, args);
                return true;
            default:
                sender.sendMessage(msg.getMsg("cmd_statsta_usage"));
                break;
            }

        } else {
            return false;
        }
        return true;
    }



}
