package me.igwb.Statsta.commands;

import java.util.HashMap;

import me.igwb.Statsta.DatabaseConnector;
import me.igwb.Statsta.PlayerDataPair;
import me.igwb.Statsta.Statsta;
import me.igwb.Statsta.messages.Messages;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class CmdTopAlone {

    private static final Integer PAGESIZE = 5;

    /**
     * Not a constructor.
     */
    private CmdTopAlone() {
    }

    /**
     * Send a ranking of the players with the most logins to the command sender.
     * @param sender The sender of the command.
     * @param args Arguments as supplied by bukkit.
     */
    protected static void execute(CommandSender sender, String[] args) {

        //We need a database connection first.
        DatabaseConnector db = Statsta.getDb();

        //Also we need the messages.
        Messages msg = Statsta.getMsg();

        //Get the data.
        HashMap<Integer, PlayerDataPair> stats = db.getTopAlonetime();

        //Figure out what page to start on.
        Integer page = 1;
        if (args.length > 2) {
            page = Integer.parseInt(args[2]);
        }

        sender.sendMessage(msg.getMsg("header_top_alone"));

        Integer rank = ((page - 1) * PAGESIZE) + 1;

        //Iterate thru the ranking and send the data. Starting at the first rank on the current page.
        while (rank < stats.keySet().size() && rank <  ((page - 1) * PAGESIZE) + PAGESIZE + 1) {
            sender.sendMessage(ChatColor.GOLD + String.format("%02d", rank) + "| " + stats.get(rank).data1 + " §c" + Statsta.timeToString(Integer.parseInt(stats.get(rank).data2) / 1000));
            rank++;
        }
    }
}
