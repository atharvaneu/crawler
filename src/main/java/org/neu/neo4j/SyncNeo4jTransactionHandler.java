package org.neu.neo4j;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neu.Crawler;

import java.util.List;
import java.util.Map;

public class SyncNeo4jTransactionHandler {

    public SyncNeo4jTransactionHandler() {
        ConfigReader configReader = new ConfigReader();
        hostname = configReader.getHostname();
        username = configReader.getUsername();
        password = configReader.getPassword();
        System.out.println("Environment (dev)\n"+"hostname : "+hostname+"\nusername : "+username+"\npassword : "+password);
    }

    /**
     * Initialize the Neo4J driver as per the credentials specified in config.properties.
     */
    public void initialize() {
        this.driver = GraphDatabase.driver(hostname, AuthTokens.basic(username, password));
        try {
            this.driver.verifyConnectivity();
            System.out.println("Connection established");
        } catch (Exception e) {
            System.out.println("Failed to connect to the database: " + e.getMessage());
        }
        try {
            this.session = this.driver.session(SessionConfig.builder().withDatabase("neo4j").build());
            createConstraints();
        }
        catch(Exception e){
            System.out.println("Failed to initiate session: " + e.getMessage());
            this.close();
        }
    }

    /**
     * Close the Neo4J driver.
     */
    public void close() {
        if (driver != null) {
            this.clearDatabase();
//            Thread.sleep(1000);
            driver.close();
            System.out.println("Driver closed.");
        }
    }

    /**
     * Create a UNIQUE constraint on the u.address parameter such that for MERGE calls, a newer node with the same address is never created,
     * rather reference the already existing node.
     */
    private void createConstraints() {
        try{
            this.session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (u:url) REQUIRE u.address IS UNIQUE");
        }
        catch(Exception e){
            System.out.println("Failed to create constraints: " + e.getMessage());
        }

    }

    /**
     * For a parent URL and child URL, create (or retrieve if a node exists already) a node for each, create a relationship between them, and finally insert the
     * relationship too.
     *
     * @param url The parent URL that will have an outgoing relationship
     * @param dependent_url The child URL that will have an incoming relationship
     */
    public void mergeNodeWithChildURL(String url, String dependent_url){
        try{
            this.session.executeWrite(tx->{
                tx.run(
                        "MERGE (u:url {address: $url}) " +
                                "MERGE (u_child:url {address: $dependent_url}) " + // Ensure the dependent node is defined separately
                                "MERGE (u)-[:contains]->(u_child)",              // Create the relationship
                        Map.of("url", url, "dependent_url", dependent_url) // Pass both parameters
                ).consume();
                return null;
            });
        }
        catch(Exception e){
            System.out.println("Failed to insert node: " + e.getMessage());
        }
    }


    /**
     * Clear DB data. This is crucial for benchmarking and multiple runs.
     */
    public void clearDatabase() {
        try{
            this.session.executeWrite(tx->{
                tx.run(
                        "MATCH (a) -[r] -> () " +
                                "DELETE a, r " +
                                "WITH count(*) as dummy " +  // Added WITH clause
                                "MATCH (a) " +
                                "DELETE (a)"
                ).consume();
                return null;
            });
        }
        catch(Exception e){
            logger.fatal("Failed to clear database: {}", e.getMessage());
        }

    }

    /**
     * Fetch and return the total number of nodes (URLs) inserted within the dedicated time limit.
     *
     * @return long Total number of nodes (URLs) in the database.
     */
    public long getAllNodes() {
        try {
            String query = "MATCH (n) RETURN count(n) AS count";

            return this.session.executeRead(tx -> {
                Result result = tx.run(query);
                if (result.hasNext()) {

                    Record record = result.next();
                    return record.get("count").asLong();  // return the count value as long
                }
                return 0L;
            });
        } catch (Exception e) {
            System.out.println("Failed to retrieve node count: " + e.getMessage());
            return 0L;
        }
    }

    private static String hostname;
    private static String username;
    private static String password;
    private Driver driver;
    private Session session;
    private static final Logger logger = LogManager.getLogger(SyncNeo4jTransactionHandler.class);

}
