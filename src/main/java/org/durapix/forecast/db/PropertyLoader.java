package org.durapix.forecast.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class PropertyLoader {

    public final static Logger LOGGER = Logger.getLogger(PropertyLoader.class.getName());
    public static Properties properties = new Properties();
    public static String dbName = null;
    public static String dbUser = null;
    public static String dbPass = null;
    public static String cronExp = null;

    public PropertyLoader() {
        InputStream inputStream = PropertyLoader.class.getResourceAsStream("/forecaster.properties");
        try {
            properties.load(inputStream);
            getDbProperties();
        } catch (IOException e) {
            LOGGER.info("Error occurred while loading data from properties file" + e);
        }
    }

    private void getDbProperties() {
        try {
            dbUser = properties.getProperty("db.user");
            dbPass = properties.getProperty("db.pass");
            dbName = properties.getProperty("db.name");
            cronExp = properties.getProperty("app.cron.expression");

        } catch (Exception e) {
            LOGGER.info("Error occurred while loading data from properties file" + e);
        }
    }

}
