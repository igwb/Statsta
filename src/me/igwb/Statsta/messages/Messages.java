package me.igwb.Statsta.messages;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import me.igwb.Statsta.Statsta;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Messages {

    //Configurations
    private FileConfiguration msgConfig = null;
    private File msgConfigFile = null;

    private HashMap<String, String> msg;

    /**
     * Creates a new instance of the Messages.class and loads the locale file.
     */
    public Messages() {
        saveDefaultConfig();

        msg = new HashMap<String, String>();

        Map<String, Object> temp;
        String strKey;

        //  Bukkit.getServer().getLogger().log(Level.INFO, "test: " + getMsgConfig().getCurrentPath());

        temp = getMsgConfig().getConfigurationSection("Messages").getValues(false);

        Bukkit.getServer().getLogger().log(Level.INFO, "Messages:");
        for (Object obj : temp.keySet()) {
            strKey = obj.toString();

            msg.put(strKey.toString(), temp.get(strKey).toString().replace("&", "§"));
            Bukkit.getServer().getLogger().log(Level.INFO, strKey + " : " + msg.get(strKey));
        }


    }

    /**
     * Returns the message assigned to a key.
     * @param key The key.
     * @return The message.
     */
    public String getMsg(String key) {

        return msg.get("prefix") + msg.get(key);
    }

    /**
     * Gets the FileConfiguration for the messages.
     * @return The configuration
     */
    public FileConfiguration getMsgConfig() {
        if (msgConfig == null) {
            reloadMsgConfig();
        }

        // Bukkit.getServer().getLogger().log(Level.INFO, "get: " + (msgConfig == null));
        return msgConfig;
    }

    /**
     * Saves the default configuration to file if it doesn't exist already.
     */
    public void saveDefaultConfig() {
        Statsta pl = (Statsta) Bukkit.getServer().getPluginManager().getPlugin("Statsta");

        if (msgConfigFile == null) {
            msgConfigFile = new File(pl.getDataFolder().getAbsolutePath() + "/messages/messages.yml");
        }
        if (!msgConfigFile.exists()) {
            pl.saveResource("messages\\messages.yml", false);
            File temp = new File(pl.getDataFolder().getAbsolutePath() + "/messages/messages.yml");
            temp.renameTo(new File(pl.getDataFolder().getAbsolutePath() + "/messages/messages.yml"));
        }
    }

    /**
     * Reloads the messages from File.
     */
    public void reloadMsgConfig() {
        Statsta pl = (Statsta) Bukkit.getServer().getPluginManager().getPlugin("Statsta");

        if (msgConfigFile == null) {
            msgConfigFile = new File(pl.getDataFolder(), "/messages/messages.yml");
        }

        msgConfig = YamlConfiguration.loadConfiguration(msgConfigFile);

        //Bukkit.getServer().getLogger().log(Level.INFO, "reload: " + (msgConfig == null));

        // Look for defaults in the jar
        InputStream defConfigStream = pl.getResource("\\messages\\messages.yml");

        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            msgConfig.setDefaults(defConfig);
        }
    }
}
