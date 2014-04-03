package org.durapix.forecast.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.Logger;

public class DBConnect {

    public final static Logger LOGGER = Logger.getLogger(DBConnect.class.getName());
    private static Connection con = null;
    private static String dbName = PropertyLoader.dbName;
    private static String dbUser = PropertyLoader.dbUser;
    private static String dbPass = PropertyLoader.dbPass;

    public Connection connect() {
        if (con == null) {
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
                con = DriverManager.getConnection("jdbc:mysql://" + dbName + "?autoReconnect=true", dbUser, dbPass);

                if (!con.isClosed()) {
                    LOGGER.info("Successfully connected to " + "MySQL server using TCP/IP.");
                }
            } catch (Exception e) {
                LOGGER.info("Exception: " + e.getMessage());
            }
        }
        return con;
    }
}
