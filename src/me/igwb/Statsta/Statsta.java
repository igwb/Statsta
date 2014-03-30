package me.igwb.Statsta;

import java.util.logging.Level;

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
        eventListener = new MyEventListener(this);
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

    public DatabaseConnector getDbConnector() {

        return dbConnector;
    }

    public Messages getMessages() {
        return msg;
    }
}
