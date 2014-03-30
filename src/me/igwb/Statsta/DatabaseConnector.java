package me.igwb.Statsta;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.logging.Level;
public class DatabaseConnector {

    private Statsta parentPlugin;

    public DatabaseConnector(final Statsta parent)  {

        try {
            parentPlugin = parent;

            Class.forName("org.sqlite.JDBC");
            initialize();

        } catch (ClassNotFoundException e) {

            e.printStackTrace();
        }
    }

    private void initialize() {

        Connection conn = null;
        ResultSet rs = null;

        try {

            conn = getConnection();
            Statement stat = conn.createStatement();


            stat.executeUpdate("PRAGMA foreign_keys = ON;");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS Players (Id INTEGER PRIMARY KEY, Name TEXT NOT NULL UNIQUE, FirstJoin STRING, OptOut INTEGER);");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS Sessions (Id INTEGER PRIMARY KEY, Player STRING, Start STRING, End STRING, Duration INTEGER, FOREIGN KEY(Player) REFERENCES Players(Name));");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS Encounters (Id INTEGER PRIMARY KEY, Player STRING, OtherPlayer STRING, Start STRING, End STRING, Duration INTEGER, FOREIGN KEY(Player) REFERENCES Players(Name), FOREIGN KEY(OtherPlayer) REFERENCES Players(Name));");

        } catch (SQLException e) {
            //TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Connection getConnection() {
        try {

            String dbLocation;

            dbLocation = parentPlugin.getDbPath();

            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbLocation);

            return conn;
        } catch (SQLException e) {
            parentPlugin.logSevere(e.getMessage());
            return null;
        }
    }

    public DBAddResult addPlayer(String name) {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;

        try {

            con = getConnection();
            st = con.createStatement();
            rs = st.executeQuery("SELECT * FROM Players WHERE Name= \'" + name + "\';");

            //Check if the player is already in the database
            if (rs.next()) {

                return DBAddResult.exists;
            } else {

                st.execute("INSERT INTO Players(Name, OptOut, FirstJoin) Values(" + "\'" + name + "\'," + "\'0\'" + "," + "\'" + getCurrentTime() + "\'" + ");");
                return DBAddResult.success;
            }

        } catch (SQLException e) {
            parentPlugin.logSevere(e.getMessage());
            return DBAddResult.error;
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                parentPlugin.logSevere(e.getMessage());
            }
        }
    }

    public DBAddResult startSession(Player player) {


        //Add the player to the database
        addPlayer(player.getName());

        Connection con = null;
        PreparedStatement pst = null;
        PreparedStatement secondPst = null;
        ResultSet rs = null;

        String currentTime = getCurrentTime();

        try {

            con = getConnection();
            pst = con.prepareStatement("INSERT INTO Sessions(Player, Start) Values(?,?)");
            pst.setString(1, player.getName());
            pst.setString(2, currentTime);
            pst.execute();
            pst.close();

            /* Get a list of online players and make an entry for everyone in the encounters table.
             * This is used to tell whom a player has met and how long they've played together.
             */
            Player[] players = Bukkit.getServer().getOnlinePlayers();

            for (Player p : players) {
                if(p.getName() != player.getName()) {
                    pst = con.prepareStatement("INSERT INTO Encounters(Player, OtherPlayer, Start) Values(?,?,?)");

                    pst.setString(1, player.getName());
                    pst.setString(2, p.getName());
                    pst.setString(3, currentTime);
                    pst.execute();
                    pst.close();
                }
            }

            /* If there is only one player now, create an entry in the encounters table with OtherPlayer set to null.
             * This is used to determine that the player is playing alone.
             * 
             * If there is more than one player, finish the entry in the encounter table where OtherPlayer is null.
             */
            
            Bukkit.getServer().getLogger().log(Level.INFO, "Online:" + players.length);
            if(players.length == 1) {
                Bukkit.getServer().getLogger().log(Level.INFO, "Begun an alone session!");
                pst = con.prepareStatement("INSERT INTO Encounters(Player, Start) Values(?,?)");
                pst.setString(1, players[0].getName());
                pst.setString(2, currentTime); 
                pst.executeUpdate();
            } else {
                
                /*
                 * Close all encounters.
                 * Find all Encounters in the database where OtherPlayer is null.
                 */

                pst = con.prepareStatement("SELECT Id, Start FROM Encounters WHERE OtherPlayer IS NULL AND End IS NULL");
                rs = pst.executeQuery();

                /*
                 * Iterate thru the result set and update the end time and duration.
                 */
                
                while(rs.next()) {
                    Bukkit.getServer().getLogger().log(Level.INFO, "Ended an alone session!");
                    secondPst = con.prepareStatement("UPDATE Encounters SET End=?, Duration=? WHERE Id=?");
                    secondPst.setString(1, currentTime);
                    secondPst.setInt(2, Integer.valueOf((int) (getTimeFromString(currentTime) - getTimeFromString(rs.getString("Start")))));
                    secondPst.setString(3, rs.getString("Id"));
                    secondPst.executeUpdate();
                    secondPst.close();
                }
            }
            
            return DBAddResult.success;


        } catch (SQLException e) {
            parentPlugin.logSevere(e.getMessage());
            return DBAddResult.error;
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
                if (secondPst != null) {
                    secondPst.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                parentPlugin.logSevere(e.getMessage());
            }
        }
    }

    public DBAddResult endSession(Player player) {

        //Add the player to the database
        addPlayer(player.getName());

        Connection con = null;
        PreparedStatement pst = null;
        PreparedStatement secondPst = null;
        ResultSet rs = null;

        Long start;
        Long end = System.currentTimeMillis();


        try {

            con = getConnection();

            /*
             * Find the currently open session of the player that quit.
             * Store the id in the sId variable-
             * Store the start time in the start variable.
             */
            
            Integer sId;
            
            pst = con.prepareStatement("SELECT Id, Start FROM Sessions WHERE Player=\"" + player.getName() + "\" ORDER BY Id DESC LIMIT 1;");
            rs = pst.executeQuery();

            rs.next();
            sId = Integer.valueOf(rs.getInt("Id"));
            start = getTimeFromString(rs.getString("Start"));

            rs.close();
            pst.close();

            /*
             * Update the end time and the duration of the found session.
             */
            
            pst = con.prepareStatement("UPDATE Sessions SET End=?, Duration=? WHERE Id=?");
            pst.setString(1, getTimeFromLong(end));
            pst.setInt(2, Integer.valueOf((int) (end - start)));
            pst.setInt(3, sId);
            pst.execute();

            pst.close();
            rs.close();
            
            /*
             * Close all encounters.
             * Find all Encounters in the database that include the player that quit where the end time is null.
             */

            pst = con.prepareStatement("SELECT Id, Start FROM Encounters WHERE (Player=? OR OtherPlayer=?) AND End IS NULL;");
            pst.setString(1, player.getName());
            pst.setString(2, player.getName());
            rs = pst.executeQuery();

            /*
             * Iterate thru the result set and update the end time and duration.
             */
            
            while(rs.next()) {
                Bukkit.getServer().getLogger().log(Level.INFO, "Ended a session");
                secondPst = con.prepareStatement("UPDATE Encounters SET End=?, Duration=? WHERE Id=?");
                secondPst.setString(1, getTimeFromLong(end));
                secondPst.setInt(2, Integer.valueOf((int) (end - getTimeFromString(rs.getString("Start")))));
                secondPst.setString(3, rs.getString("Id"));
                secondPst.executeUpdate();
            }
            
            pst.close();
            rs.close();
            
            /*
             * Check how many players are online.
             * If there is only one, make an entry in the Encounters table with OtherPlayer set to null.
             * This is used to track the time a player is spending alone on the server.
             */
            
            Player[] players = Bukkit.getServer().getOnlinePlayers();
            
            Integer playerCount = 0;

            
            for (Player p : players) {
                if(p.getName() != player.getName()) {
                    playerCount ++;
                }
                if(playerCount > 1) {
                    break;
                }
            }
            
            if(playerCount == 1) {
                Bukkit.getServer().getLogger().log(Level.INFO, "Begun an alone session!");
                
                pst = con.prepareStatement("INSERT INTO Encounters(Player, Start) Values(?,?)");
                pst.setString(1, players[0].getName());
                pst.setString(2, getTimeFromLong(end)); //end is the time when the session of the player that just quit ended. aka now
                pst.executeUpdate();
            }
            
            return DBAddResult.success;


        } catch (SQLException e) {
            parentPlugin.logSevere(e.getMessage());
            return DBAddResult.error;
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
                if (secondPst != null) {
                    secondPst.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                parentPlugin.logSevere(e.getMessage());
            }
        }
    }

    public Integer getSessionCount(String name) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT COUNT(*) FROM Sessions WHERE Player=? COLLATE NOCASE;");
            pst.setString(1, name);

            rs = pst.executeQuery();

            //Check if the player is in the database
            if (rs.next()) {

                return rs.getInt(1);
            } else {

                return -1;
            }

        } catch (SQLException e) {
            parentPlugin.logSevere(e.getMessage());
            return 0;
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                parentPlugin.logSevere(e.getMessage());
            }
        }
    }

    public Integer getPlaytime(String name) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT SUM(Duration) FROM Sessions WHERE Player=? COLLATE NOCASE;");
            pst.setString(1, name);

            rs = pst.executeQuery();

            //Check if the player is in the database
            if (rs.next()) {

                return rs.getInt("SUM(Duration)");
            } else {

                return -1;
            }

        } catch (SQLException e) {
            parentPlugin.logSevere(e.getMessage());
            return 0;
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                parentPlugin.logSevere(e.getMessage());
            }
        }
    }

    public Integer getAloneTime(String name) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT SUM(Duration) FROM Encounters WHERE Player=? COLLATE NOCASE AND OtherPlayer IS NULL GROUP BY Player;");
            pst.setString(1, name);

            rs = pst.executeQuery();

            //Check if the player is in the database
            if (rs.next()) {

                return rs.getInt("SUM(Duration)");
            } else {

                return -1;
            }

        } catch (SQLException e) {
            parentPlugin.logSevere(e.getMessage());
            return 0;
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                parentPlugin.logSevere(e.getMessage());
            }
        }
    }
    
    public String getLongestPlayTime() {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT SUM(Duration), Player FROM Sessions GROUP BY Player ORDER BY SUM(Duration) DESC LIMIT 1;");

            rs = pst.executeQuery();

            //Check if the player is in the database
            if (rs.next()) {

                return rs.getString("Player");
            } else {

                return "";
            }

        } catch (SQLException e) {
            parentPlugin.logSevere(e.getMessage());
            return "";
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                parentPlugin.logSevere(e.getMessage());
            }
        }
    }
 
    public String getLongestAloneTime() {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT SUM(Duration), Player FROM Encounters WHERE OtherPlayer IS NULL GROUP BY Player ORDER BY SUM(Duration) DESC LIMIT 1;");

            rs = pst.executeQuery();

            //Check if the player is in the database
            if (rs.next()) {

                return rs.getString("Player");
            } else {

                return null;
            }

        } catch (SQLException e) {
            parentPlugin.logSevere(e.getMessage());
            return null;
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                parentPlugin.logSevere(e.getMessage());
            }
        }
    }

    public HashMap<Integer, PlayerDataPair> getTopPlaytime() {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT SUM(Duration), Player FROM Sessions GROUP BY Player ORDER BY SUM(Duration) DESC;");

            rs = pst.executeQuery();

            Integer i = 0;
            HashMap<Integer, PlayerDataPair> result = new HashMap<Integer, PlayerDataPair>();
            while(rs.next()) {
                i++;
                
                result.put(i, new PlayerDataPair(rs.getString("Player"), String.valueOf(rs.getInt("SUM(Duration)"))));
            }
            
            return result;

        } catch (SQLException e) {
            parentPlugin.logSevere(e.getMessage());
            return null;
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                parentPlugin.logSevere(e.getMessage());
            }
        }
    }

    public HashMap<Integer, PlayerDataPair> getTopAlonetime() {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT SUM(Duration), Player FROM Encounters WHERE OtherPlayer IS NULL GROUP BY Player ORDER BY SUM(Duration) DESC;");

            rs = pst.executeQuery();

            Integer i = 0;
            HashMap<Integer, PlayerDataPair> result = new HashMap<Integer, PlayerDataPair>();
            while(rs.next()) {
                i++;
                
                result.put(i, new PlayerDataPair(rs.getString("Player"), String.valueOf(rs.getInt("SUM(Duration)"))));
            }
            
            return result;

        } catch (SQLException e) {
            parentPlugin.logSevere(e.getMessage());
            return null;
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                parentPlugin.logSevere(e.getMessage());
            }
        }
    }

    public HashMap<Integer, PlayerDataPair> getTopLoginCount() {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT COUNT(*), Player FROM Sessions GROUP BY Player ORDER BY COUNT(*) DESC;");

            rs = pst.executeQuery();

            Integer i = 0;
            HashMap<Integer, PlayerDataPair> result = new HashMap<Integer, PlayerDataPair>();
            while(rs.next()) {
                i++;
                
                result.put(i, new PlayerDataPair(rs.getString("Player"), String.valueOf(rs.getInt("COUNT(*)"))));
            }
            
            return result;

        } catch (SQLException e) {
            parentPlugin.logSevere(e.getMessage());
            return null;
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                parentPlugin.logSevere(e.getMessage());
            }
        }
    }
    
    public String getMostLogins() {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT COUNT(*), Player FROM Sessions GROUP BY Player ORDER BY COUNT(*) DESC LIMIT 1;");

            rs = pst.executeQuery();

            //Check if the player is in the database
            if (rs.next()) {

                return rs.getString("Player");
            } else {

                return null;
            }

        } catch (SQLException e) {
            parentPlugin.logSevere(e.getMessage());
            return null;
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                parentPlugin.logSevere(e.getMessage());
            }
        }  
    }

    public String getTimeFromLong(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        Date now = new Date(time);
        return sdf.format(now);
    }

    public String getCurrentTime() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        Date now = new Date(System.currentTimeMillis());
        return sdf.format(now);
    }

    public String getFirstJoin(String name) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pst = con.prepareStatement("SELECT FirstJoin FROM Players WHERE Name=? COLLATE NOCASE;");
            pst.setString(1, name);

            rs = pst.executeQuery();

            //Check if the player is in the database
            if (rs.next()) {

                return rs.getString("FirstJoin");
            } else {

                return null;
            }

        } catch (SQLException e) {
            parentPlugin.logSevere(e.getMessage());
            return null;
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                parentPlugin.logSevere(e.getMessage());
            }
        }
    }
    
    public long getTimeFromString(String dateString) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        try {
            return sdf.parse(dateString).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return -1;
    }
}
