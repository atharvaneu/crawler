package org.neu.neo4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.async.*;
import org.neo4j.driver.async.AsyncSession;
import org.neu.Crawler;

/**
 * Provides utility methods for interacting with a Neo4j database, including creating constraints,
 * managing connections, and executing asynchronous transactions.
 */
public class Neo4jTransactionHandler {

    /**
     * Constructs a new Neo4jTransactionHandler instance and initializes connection credentials
     * using the {@link ConfigReader}.
     */
    public Neo4jTransactionHandler() {
        ConfigReader configReader = new ConfigReader();
        hostname = configReader.getHostname();
        username = configReader.getUsername();
        password = configReader.getPassword();
        System.out.println("Environment (dev)\n"+"hostname : "+hostname+"\nusername : "+username+"\npassword : "+password);
    }

    /**
     * Creates a unique constraint on the `address` property for nodes with the `url` label.
     * Ensures that duplicate entries are avoided in the Neo4j database.
     */
    public void createConstraints() {
        try (Session session = driver.session(SessionConfig.forDatabase("neo4j"))) {
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (u:url) REQUIRE u.address IS UNIQUE");
        }
    }

    /**
     * Initializes the Neo4j driver and establishes a connection to the database.
     * Verifies connectivity and creates constraints. Also initializes an asynchronous session.
     */
    public void initialize(){
        this.driver = GraphDatabase.driver(hostname, AuthTokens.basic(username, password), Config.builder().withMaxConnectionPoolSize(50)
                .withConnectionTimeout(0, TimeUnit.MILLISECONDS)
                .withMaxTransactionRetryTime(0, TimeUnit.MILLISECONDS)
                .withFetchSize(1000).build());
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

    /**
     * Closes the Neo4j driver and releases resources. Ensures proper shutdown of the database connection.
     */
    public void close(){
        if (driver != null) {
            this.clearDatabase().join();
            driver.close();
            logger.info("Neo4J connection closed");
            System.out.println("(ASYNC) Driver closed.");
        }
    }

    /**
     * Merges a node with a given URL and its child node, establishing a "contains" relationship between them.
     *
     * <p>
     * The method executes asynchronously and ensures that both nodes and their relationship are
     * created in the database if they do not already exist.
     * </p>
     *
     * @param url The URL of the parent node.
     * @param dependent_url The URL of the child node.
     * @return A {@link CompletableFuture} representing the completion of the transaction.
     */
    public synchronized CompletableFuture<Void> mergeNodeWithChildURL(String url, String dependent_url) {
        AsyncSession session = driver.session(AsyncSession.class, SessionConfig.forDatabase("neo4j"));
        return session.executeWriteAsync(tx ->
                tx.runAsync("MERGE (u:url {address: $url}) " +
                                        "WITH u " +
                                        "MERGE (u_child:url {address: $dependent_url}) " +
                                        "WITH u, u_child " +  // Keep both nodes in context
                                        "MERGE (u)-[r:contains]->(u_child) " +
                                        "RETURN u.address, u_child.address",  // Return both for verification
                                Values.parameters("url", url, "dependent_url", dependent_url))
                        .thenCompose(ResultCursor::consumeAsync)        ).thenCompose(resultSummary -> {
            return session.closeAsync();
        }).exceptionally(error -> {
            System.out.println("Failed to insert node due to: " + error.getMessage());
            session.closeAsync();
            return null;
        }).toCompletableFuture();
    }

    /**
     * Clear DB data. This is crucial for benchmarking and multiple runs.
     */
    public CompletableFuture<Void> clearDatabase() {
        AsyncSession session = driver.session(AsyncSession.class, SessionConfig.forDatabase("neo4j"));

        return session.executeWriteAsync(tx ->
                tx.runAsync(
                        "MATCH (a) -[r] -> () " +
                                "DELETE a, r " +
                                "WITH count(*) as dummy " +  // Added WITH clause
                                "MATCH (a) " +
                                "DELETE (a)"
                ).thenCompose(ResultCursor::consumeAsync)
        ).thenCompose(ignored -> {
            logger.info("Database cleared successfully");
            return session.closeAsync();
        }).exceptionally(error -> {
            logger.error("Failed to clear database: " + error.getMessage());
            session.closeAsync();
            return null;
        }).toCompletableFuture();
    }
    /**
     * Fetch and return the total number of nodes (URLs) inserted within the dedicated time limit.
     *
     * @return long Total number of nodes (URLs) in the database.
     */
    public CompletableFuture<Long> getAllNodes() {
        AsyncSession session = driver.session(AsyncSession.class, SessionConfig.forDatabase("neo4j"));
        return session.executeReadAsync(tx ->
                tx.runAsync("MATCH (n) RETURN count(n) AS count")
                        .thenCompose(ResultCursor::singleAsync)
                        .thenApply(record -> record.get("count").asLong())
        ).thenCompose(count -> {
            return session.closeAsync().thenApply(ignored -> count);
        }).exceptionally(error -> {
            logger.error("Failed to retrieve node count: {}", error.getMessage());
            session.closeAsync();
            return 0L;
        }).toCompletableFuture();
    }

    public CompletableFuture<List<URLRank>> getURLsByInDegree() {
        AsyncSession session = driver.session(AsyncSession.class, SessionConfig.forDatabase("neo4j"));

        return session.executeReadAsync(tx ->
                tx.runAsync(
                                "MATCH (u:url) " +
                                        "WITH u, COUNT { (u)<-[:contains]-() } as inDegree " +
                                        "RETURN u.address as url, inDegree " +
                                        "ORDER BY inDegree DESC"
                        )
                        .thenCompose(cursor ->
                                cursor.listAsync(record -> new URLRank(
                                        record.get("url").asString(),
                                        record.get("inDegree").asInt()
                                ))
                        )
        ).thenCompose(results -> {
            return session.closeAsync().thenApply(ignored -> results);
        }).exceptionally(error -> {
            logger.error("Failed to retrieve URLs by in-degree: {}", error.getMessage());
            session.closeAsync();
            return List.of();
        }).toCompletableFuture();
    }

    public CompletableFuture<Void> printDatabaseContents() {
        AsyncSession session = driver.session(AsyncSession.class, SessionConfig.forDatabase("neo4j"));

        return session.executeReadAsync(tx ->
                tx.runAsync(
                                "MATCH (u:url)-[r:contains]->(u2:url) " +
                                        "RETURN u.address as source, u2.address as target"
                        )
                        .thenCompose(cursor ->
                                cursor.listAsync(record -> {
                                            System.out.println("Relationship: " +
                                                    record.get("source").asString() + " -> " +
                                                    record.get("target").asString());
                                            return true; // Return something for the mapping function
                                        })
                                        .thenApply(list -> null) // Convert the List<Boolean> to Void
                        )
        ).thenCompose(results ->
                session.closeAsync()
        ).exceptionally(error -> {
            logger.error("Failed to print database contents: " + error.getMessage());
            session.closeAsync();
            return null;
        }).toCompletableFuture();


    }

    public CompletableFuture<Void> printDatabaseCounts() {
        AsyncSession session = driver.session(AsyncSession.class, SessionConfig.forDatabase("neo4j"));

        return session.executeReadAsync(tx ->
                tx.runAsync(
                                "MATCH (u:url) " +
                                        "WITH count(u) as nodeCount " +
                                        "MATCH ()-[r:contains]->() " +
                                        "RETURN nodeCount, count(r) as relCount"
                        )
                        .thenCompose(cursor ->
                                cursor.singleAsync()
                                        .thenAccept(record -> {
                                            System.out.println("Total nodes: " + record.get("nodeCount").asInt());
                                            System.out.println("Total relationships: " + record.get("relCount").asInt());
                                        })
                        )
        ).thenCompose(results ->
                session.closeAsync()
        ).exceptionally(error -> {
            logger.error("Failed to print database counts: " + error.getMessage());
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