package org.neu;

import java.util.Map;
import java.util.List;

import org.neo4j.driver.*;
import org.neo4j.driver.Record;

public class SyncNeo4jTransactionHandler {

    private static String hostname;
    private static String username;
    private static String password;
    private Driver driver;
    private Session session;

    public SyncNeo4jTransactionHandler() {
        ConfigReader configReader = new ConfigReader();
        hostname = configReader.getHostname();
        username = configReader.getUsername();
        password = configReader.getPassword();
        System.out.println("Environment (dev)\n"+"hostname : "+hostname+"\nusername : "+username+"\npassword : "+password);
    }

    public void initialize(){
        this.driver = GraphDatabase.driver(hostname, AuthTokens.basic(username, password));
        try {
            this.driver.verifyConnectivity();
            System.out.println("Connection established");
        } catch (Exception e) {
            System.out.println("Failed to connect to the database: " + e.getMessage());
        }
        try{
            this.session = this.driver.session(SessionConfig.builder().withDatabase("neo4j").build());
            createConstraints();
        }
        catch(Exception e){
            System.out.println("Failed to initiate session: " + e.getMessage());
            this.close();
        }
    }

    public void close(){
        if (driver != null) {
            driver.close();
            System.out.println("Driver closed.");
        }
    }

    private void createConstraints() {
        try{
            this.session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (u:url) REQUIRE u.address IS UNIQUE");
        }
        catch(Exception e){
            System.out.println("Failed to create constraints: " + e.getMessage());
        }

    }

    public List<String> getAllInboundNodes(String url){
        return null;
    }

    public List<String> getAllOutboundNodes(String url){
        return null;
    }

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

    public long getAllNodes() {
        try {
            // Query to count all nodes in the database
            String query = "MATCH (n) RETURN count(n) AS count";

            // Execute the query to get the count of all nodes
            return this.session.readTransaction(tx -> {
                Result result = tx.run(query);  // Run the query
                if (result.hasNext()) {
                    // Get the count from the result
                    Record record = result.next();
                    return record.get("count").asLong();  // Return the count value as long
                }
                return 0L;  // If no nodes are found, return 0
            });
        } catch (Exception e) {
            System.out.println("Failed to retrieve node count: " + e.getMessage());
            return 0L;  // Return 0 in case of failure
        }
    }


}
