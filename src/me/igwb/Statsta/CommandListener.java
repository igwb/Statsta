package me.igwb.Statsta;
import java.util.HashMap;

import me.igwb.Statsta.messages.Messages;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandListener implements CommandExecutor {

    private Messages msg;
    private static Integer PAGESIZE = 5;


    public CommandListener() {
        msg = ((Statsta) Bukkit.getServer().getPluginManager().getPlugin("Statsta")).getMessages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String firstArg, String[] args) {

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
                cmd_info(sender, args);
                return true;
            case "top":
                cmd_top(sender, args);
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

    private void cmd_info(CommandSender sender, String[] args) {
        DatabaseConnector db = ((Statsta) Bukkit.getServer().getPluginManager().getPlugin("Statsta")).getDbConnector();

        String name;

        if(args.length >= 2) {
            name = args[1];
        } else {
            name = sender.getName();
        }

        if(db.getFirstJoin(name) == null) {
            sender.sendMessage(msg.getMsg("unknown_player").replace("%player%", name));
            return;
        }

        Integer playTime =  Math.round(db.getPlaytime(name) / 1000);
        Integer aloneTime = Math.round(db.getAloneTime(name) / 1000);

        String alonePercent;
        if(aloneTime > 0) {
            alonePercent = String.valueOf((aloneTime.floatValue() / playTime.floatValue()) * 100);
            alonePercent = alonePercent.substring(0, Math.min(alonePercent.length(), 4));
        } else {
            alonePercent = "-";
        }

        sender.sendMessage(msg.getMsg("header").replace("%player%", name));
        sender.sendMessage(msg.getMsg("stat_playTime") + timeToString(playTime));
        sender.sendMessage(msg.getMsg("stat_aloneTime") + timeToString(aloneTime) + " (" + alonePercent + "%)");
        sender.sendMessage(msg.getMsg("stat_loginCount") + db.getSessionCount(name));
    }

    private void cmd_top(CommandSender sender, String[] args) {
      
        if(args.length < 2) {
            sender.sendMessage(msg.getMsg("cmd_top_usage"));
            return;
        }

        switch(args[1]) {
        case "online":
            cmd_top_online(sender, args);
            return;
        case "alone":
            cmd_top_alone(sender, args);
            return;
        case "login":
            cmd_top_login(sender, args);
            return;
        default:
            sender.sendMessage(msg.getMsg("cmd_top_usage"));
        }
    }

    private void cmd_top_online(CommandSender sender, String[] args) {
        DatabaseConnector db = ((Statsta) Bukkit.getServer().getPluginManager().getPlugin("Statsta")).getDbConnector();

        HashMap<Integer, PlayerDataPair> stats = db.getTopPlaytime();



        Integer page = 1;
        if(args.length > 2) {
            page = Integer.parseInt(args[2]);
        }

        sender.sendMessage(msg.getMsg("header_top_online"));

        Integer rank = ((page - 1) * PAGESIZE) + 1;

        while(rank < stats.keySet().size() && rank <  ((page - 1) * PAGESIZE) + PAGESIZE + 1) {
            sender.sendMessage(ChatColor.GOLD + String.format("%02d", rank) + "| " + stats.get(rank).data1 + " " + timeToString(Integer.parseInt(stats.get(rank).data2) / 1000));
            rank ++;
        }
    }

    private void cmd_top_alone(CommandSender sender, String[] args) {
        DatabaseConnector db = ((Statsta) Bukkit.getServer().getPluginManager().getPlugin("Statsta")).getDbConnector();

        HashMap<Integer, PlayerDataPair> stats = db.getTopAlonetime();



        Integer page = 1;
        if(args.length > 2) {
            page = Integer.parseInt(args[2]);
        }

        sender.sendMessage(msg.getMsg("header_top_alone"));

        Integer rank = ((page - 1) * PAGESIZE) + 1;

        while(rank < stats.keySet().size() && rank <  ((page - 1) * PAGESIZE) + PAGESIZE + 1) {
            sender.sendMessage(ChatColor.GOLD + String.format("%02d", rank) + "| " + stats.get(rank).data1 + " " + timeToString(Integer.parseInt(stats.get(rank).data2) / 1000));
            rank ++;
        }
    }
    
    private void cmd_top_login(CommandSender sender, String[] args) {
        DatabaseConnector db = ((Statsta) Bukkit.getServer().getPluginManager().getPlugin("Statsta")).getDbConnector();

        HashMap<Integer, PlayerDataPair> stats = db.getTopLoginCount();
        
        Integer page = 1;
        if(args.length > 2) {
            page = Integer.parseInt(args[2]);
        }

        sender.sendMessage(msg.getMsg("header_top_login"));

        Integer rank = ((page - 1) * PAGESIZE) + 1;

        while(rank < stats.keySet().size() && rank <  ((page - 1) * PAGESIZE) + PAGESIZE + 1) {
            sender.sendMessage(ChatColor.GOLD + String.format("%02d", rank) + "| " + stats.get(rank).data1 + " " + stats.get(rank).data2);
            rank ++;
        }
    }
    
    public String timeToString(int time) {

        return (int)(time / 60 / 60) + "h " + (int)((time / 60) % 60) + "m " + (int)(time % 60) + "s";
    }
}
