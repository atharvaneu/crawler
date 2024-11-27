package org.neu;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.NoSuchRecordException;

public class Neo4jTransactionHandler {

    private static String hostname;
    private static String username;
    private static String password;
    private Driver driver;
    private Session session;

    public Neo4jTransactionHandler() {
        ConfigReader configReader = new ConfigReader();
        hostname = configReader.getHostname();
        username = configReader.getUsername();
        password = configReader.getPassword();
        System.out.println("Environment (dev)\n"+"hostname : "+hostname+"\nusername : "+username+"\npassword : "+password);
    }

    public void initialize(){
        this.driver = GraphDatabase.driver(hostname, AuthTokens.basic(username, password));
        try {
            driver.verifyConnectivity();
            System.out.println("Connection established.");
        } catch (Exception e) {
            System.out.println("Failed to connect to the database: " + e.getMessage());
        }
    }

    public void close(){
        if (driver != null) {
            driver.close();
            System.out.println("Driver closed.");
        }
    }

    public List<String> getAllInboundNodes(String url){
        return null;
    }

    public List<String> getAllOutboundNodes(String url){
        return null;
    }


}
