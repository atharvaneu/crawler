package org.neu.neo4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.driver.*;
import org.neo4j.driver.async.*;
import org.neo4j.driver.async.AsyncSession;

public class Neo4jTransactionHandler {

    public Neo4jTransactionHandler() {
        ConfigReader configReader = new ConfigReader();
        hostname = configReader.getHostname();
        username = configReader.getUsername();
        password = configReader.getPassword();
        System.out.println("Environment (dev)\n"+"hostname : "+hostname+"\nusername : "+username+"\npassword : "+password);
    }

    public void createConstraints() {
        try (Session session = driver.session(SessionConfig.forDatabase("neo4j"))) {
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (u:url) REQUIRE u.address IS UNIQUE");
        }
    }

    public void initialize(){
        this.driver = GraphDatabase.driver(hostname, AuthTokens.basic(username, password));
        try {
            this.driver.verifyConnectivity();
            logger.info("Connection to Neo4J established");
            System.out.println("Connection established");
            createConstraints();
        } catch (Exception e) {
            logger.fatal("Failed to connect to the database: " + e.getMessage());
            System.exit(1);
        }
        try{
            this.session = this.driver.session(AsyncSession.class, SessionConfig.builder().withDatabase("neo4j").build());
        }
        catch(Exception e){
            logger.fatal("Failed to initiate session: " + e.getMessage());
            this.close();
            System.exit(1);
        }
    }

    public void close(){
        if (driver != null) {
            driver.close();
            logger.info("Neo4J connection closed");
        }
    }

    public List<String> getAllInboundNodes(String url){
        return null;
    }

    public List<String> getAllOutboundNodes(String url){
        return null;
    }

    public CompletableFuture<Void> mergeNodeWithChildURL(String url, String dependent_url) {
        AsyncSession session = driver.asyncSession(SessionConfig.forDatabase("neo4j"));
        return session.executeWriteAsync(tx ->
                tx.runAsync("MERGE (u:url {address: $url}) " +
                                        "MERGE (u_child:url {address: $dependent_url}) " +
                                        "MERGE (u)-[r:contains]->(u_child) " +
                                        "RETURN u.address",
                                Values.parameters("url", url, "dependent_url", dependent_url))
                        .thenCompose(ResultCursor::consumeAsync)        ).thenCompose(resultSummary -> {

            return session.closeAsync();
        }).exceptionally(error -> {
            System.out.println("Failed to insert node due to: " + error.getMessage());
            session.closeAsync();
            return null;
        }).toCompletableFuture();
    }

    private static String hostname;
    private static String username;
    private static String password;
    private Driver driver;
    private AsyncSession session;
    private static final Logger logger = LogManager.getLogger(Neo4jTransactionHandler.class);


}