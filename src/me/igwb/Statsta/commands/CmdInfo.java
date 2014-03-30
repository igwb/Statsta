package me.igwb.Statsta.commands;

import me.igwb.Statsta.DatabaseConnector;
import me.igwb.Statsta.PlayerDataPair;
import me.igwb.Statsta.Statsta;
import me.igwb.Statsta.messages.Messages;

import org.bukkit.command.CommandSender;

public final class CmdInfo {

    /**
     * Not a constructor.
     */
    private CmdInfo() {

    }

    /**
     * Execute the info command. This will display various information about a player.
     * @param sender The sender of the command.
     * @param args Arguments as supplied by bukkit.
     */
    public static void execute(CommandSender sender, String[] args) {

        //We need a database connection first.
        DatabaseConnector db = Statsta.getDb();

        //Also we need the messages.
        Messages msg = Statsta.getMsg();

        //If a name was supplied, use it. Else use the name of the sender.
        String name;
        if (args.length >= 2) {
            name = args[1];
        } else {
            name = sender.getName();
        }

        //Check if the player is known, else print an error and return.
        if (db.getFirstJoin(name) == null) {
            sender.sendMessage(msg.getMsg("unknown_player").replace("%player%", name));
            return;
        }

        /*Ask the database how long the player as played and how much time he spent alone on the server.
         * Convert the numbers from milliseconds to seconds by dividing by 1000.
         */
        Integer playTime =  Math.round(db.getPlaytime(name) / 1000);
        Integer aloneTime = Math.round(db.getAloneTime(name) / 1000);

        /* Calculate how many percent of his online time the player was alone and truncate the percentage to 2 decimal places.
         * Set the value to 0 if there is no time he spent alone to avoid a division by 0 exception.
         */
        String alonePercent;
        if (aloneTime > 0) {
            alonePercent = String.valueOf((aloneTime.floatValue() / playTime.floatValue()) * 100);
            alonePercent = alonePercent.substring(0, Math.min(alonePercent.length(), 4));
        } else {
            alonePercent = "0";
        }

        /* Send the data to the command sender.
         * Don't forget to format the times into a readable string.
         */
        sender.sendMessage(msg.getMsg("header").replace("%player%", name));
        sender.sendMessage(msg.getMsg("stat_playTime") + Statsta.timeToString(playTime));
        sender.sendMessage(msg.getMsg("stat_aloneTime") + Statsta.timeToString(aloneTime) + " (" + alonePercent + "%)");
        sender.sendMessage(msg.getMsg("stat_loginCount") + db.getSessionCount(name));

        Integer currentSession = db.getCurrentSessionDuration(name);
        if (currentSession != null) {
            sender.sendMessage(msg.getMsg("stat_isOnline").replace("%player%", name).replace("%time%", Statsta.timeToString(currentSession / 1000)));
        } else {
            PlayerDataPair offlineSince = db.getOfflineSince(name);

            sender.sendMessage(msg.getMsg("stat_isOffline").replace("%player%", offlineSince.data1).replace("%time%", Statsta.timeToString(Integer.valueOf(offlineSince.data2) / 1000, true)));
        }
    }
}
