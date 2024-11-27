package org.neu;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public  class ConfigReader {
    private static String hostname;
    private static String username;
    private static String password;

    public static String getHostname() {
        return hostname;
    }
    public static void setHostname(String hostname) {
        ConfigReader.hostname = hostname;
    }
    public static String getUsername() {
        return username;
    }
    public static void setUsername(String username) {
        ConfigReader.username = username;
    }
    public static String getPassword() {
        return password;
    }
    public static void setPassword(String password) {
        ConfigReader.password = password;
    }

    public ConfigReader() {
        Properties properties = new Properties();
        String configFilePath = "config.properties"; // Path to your properties file

        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            // Load properties file
            properties.load(fis);

            // Read keys
            setHostname(properties.getProperty("hostname"));
            setUsername(properties.getProperty("username"));
            setPassword(properties.getProperty("password"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
