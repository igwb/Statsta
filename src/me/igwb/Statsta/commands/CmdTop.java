package me.igwb.Statsta.commands;

import me.igwb.Statsta.Statsta;
import me.igwb.Statsta.messages.Messages;

import org.bukkit.command.CommandSender;

public final class CmdTop {

    /**
     * Not a constructor.
     */
    private CmdTop() {
    }

    /**
     * Execute one of the top listing commands depending on the parameters given.
     * @param sender The sender of the command.
     * @param args Arguments as supplied by bukkit.
     */
    protected static void execute(CommandSender sender, String[] args) {

        //We need the messages.
        Messages msg = Statsta.getMsg();

        if (args.length < 2) {
            sender.sendMessage(msg.getMsg("cmd_top_usage"));
            return;
        }

        switch(args[1]) {
        case "online":
            CmdTopOnline.execute(sender, args);
            return;
        case "alone":
            CmdTopAlone.execute(sender, args);
            return;
        case "login":
            CmdTopLogin.execute(sender, args);
            return;
        default:
            sender.sendMessage(msg.getMsg("cmd_top_usage"));
        }
    }

}
