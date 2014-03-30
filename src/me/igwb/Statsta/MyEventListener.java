package me.igwb.Statsta;

import me.igwb.Statsta.messages.Messages;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MyEventListener implements Listener {


    /**
     * Create an instance of the EventListener.
     */
    public MyEventListener() {
    }

    /**
     * Called by bukkit.
     * @param e The event.
     */
    @EventHandler
    public void onPlayerJoinEvent(final PlayerJoinEvent e) {

        //We need a database connection first.
        DatabaseConnector db = Statsta.getDb();

        //Also we need the messages.
        Messages msg = Statsta.getMsg();

        db.startSession(e.getPlayer());

        if (e.getPlayer().getName().equalsIgnoreCase(db.getLongestPlayTime())) {
            Bukkit.getServer().broadcastMessage(msg.getMsg("record_mostOnline").replace("%player%", e.getPlayer().getName()));
        }

        if (e.getPlayer().getName().equalsIgnoreCase(db.getLongestAloneTime())) {
            Bukkit.getServer().broadcastMessage(msg.getMsg("record_foreverAlone").replace("%player%", e.getPlayer().getName()));
        }

        if (e.getPlayer().getName().equalsIgnoreCase(db.getMostLogins())) {
            Bukkit.getServer().broadcastMessage(msg.getMsg("record_mostSessions").replace("%player%", e.getPlayer().getName()));
        }
    }

    /**
     * Called by bukkit.
     * @param e The event.
     */
    @EventHandler
    public void onPlayerQuitEvent(final PlayerQuitEvent e) {

        Statsta.getDb().endSession(e.getPlayer());
    }
}
