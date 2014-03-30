package me.igwb.Statsta;

import me.igwb.Statsta.messages.Messages;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MyEventListener implements Listener {

    private Messages msg;

    public MyEventListener(final Statsta parent) {
        msg = ((Statsta) Bukkit.getServer().getPluginManager().getPlugin("Statsta")).getMessages();
    }

    @EventHandler
    public void onPlayerJoinEvent(final PlayerJoinEvent e) {

        DatabaseConnector db = ((Statsta) Bukkit.getServer().getPluginManager().getPlugin("Statsta")).getDbConnector();
        
        db.startSession(e.getPlayer());
        
        if(e.getPlayer().getName().equalsIgnoreCase(db.getLongestPlayTime())) {
            Bukkit.getServer().broadcastMessage(msg.getMsg("record_mostOnline").replace("%player%", e.getPlayer().getName()));
        }
        
        if(e.getPlayer().getName().equalsIgnoreCase(db.getLongestAloneTime())) {
            Bukkit.getServer().broadcastMessage(msg.getMsg("record_foreverAlone").replace("%player%", e.getPlayer().getName()));
        }
        
        if(e.getPlayer().getName().equalsIgnoreCase(db.getMostLogins())) {
            Bukkit.getServer().broadcastMessage(msg.getMsg("record_mostSessions").replace("%player%", e.getPlayer().getName()));
        }
    }
    
    @EventHandler
    public void onPlayerQuitEvent(final PlayerQuitEvent e) {

        ((Statsta) Bukkit.getServer().getPluginManager().getPlugin("Statsta")).getDbConnector().endSession(e.getPlayer());
    }
}
