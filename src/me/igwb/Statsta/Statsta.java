package me.igwb.Statsta;

import java.util.logging.Level;

import me.igwb.Statsta.commands.CommandListener;
import me.igwb.Statsta.messages.Messages;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Statsta extends JavaPlugin {

    private DatabaseConnector dbConnector;
    private MyEventListener eventListener;
    private CommandListener myCommandListener;
    private Messages msg;

    @Override
    public void onEnable() {

        msg = new Messages();

        dbConnector = new DatabaseConnector(this);
        eventListener = new MyEventListener();
        myCommandListener = new CommandListener();



        getServer().getPluginManager().registerEvents(eventListener, this);

        this.getCommand("stats").setExecutor(myCommandListener);
        this.getCommand("statsta").setExecutor(myCommandListener);
    }



    public String getDbPath() {
        return this.getDataFolder() + "/data.db";
    }

    public void logSevere(String message) {

        getServer().getLogger().log(Level.SEVERE, message);
    }

    protected DatabaseConnector getDbConnector() {

        return dbConnector;
    }

    public static DatabaseConnector getDb() {

        return ((Statsta)Bukkit.getServer().getPluginManager().getPlugin("Statsta")).getDbConnector();
    }

    public static Messages getMsg() {
        return ((Statsta)Bukkit.getServer().getPluginManager().getPlugin("Statsta")).getMessageConnector();
    }

    protected Messages getMessageConnector() {
        return msg;
    }

    public static String timeToString(int time) {

        return timeToString(time, false);
    }

    public static String timeToString(int time, boolean includeSeconds) {

        Integer hours, minutes, seconds;

        hours = (int) (time / 60 / 60);
        minutes = (int) ((time / 60) % 60);
        seconds = (int) (time % 60);

        String result;

        //Round the minutes if there are more than 30 seconds.
        if (!includeSeconds && seconds >= 30) {
            minutes++;

            if (minutes >= 60) {
                minutes = 0;
                hours++;
            }
        }

        //Do not include the hours if there are 0.
        if (hours > 0) {
            result = hours + "h " + minutes + "m";
        } else {
            result = minutes + "m";
        }

        //Include seconds if requested.
        if (includeSeconds) {
            //Do not include the seconds if there are 0.
            if (seconds > 0) {
                result += " " + seconds + "s";
            }
        }

        return result;
    }
}
