package org.neu;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neu.neo4j.ConfigReader;
import org.neu.neo4j.Neo4jTransactionHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Crawler {

    private Crawler() {
    }

    public static Crawler getInstance() {
        if (instance == null) {
            logger.info("Creating Crawler singleton instance");
            instance = new Crawler();
        }
        return instance;
    }

    public void init() {
        this.db = new Neo4jTransactionHandler();
        this.db.initialize();
        this.exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.visited = ConcurrentHashMap.newKeySet();
        this.childToParent = new ConcurrentHashMap<>();

        logger.info("Crawler successfully initialized and is ready to begin crawling");
    }

    public void close() {
        this.db.close();
        this.exec.shutdown();
        try {
            if (!this.exec.awaitTermination(60, TimeUnit.SECONDS)) {
                this.exec.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.exec.shutdownNow();
        }

        logger.info("Crawler terminated");
    }

    public void run(String url) throws InterruptedException, ExecutionException, MalformedURLException {
        logger.info("Starting crawling with base URL: '" + url + "'");

        bfsTraversal(url);
    }

    private void bfsTraversal(String rootUrl) throws InterruptedException, ExecutionException, MalformedURLException {
        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
        queue.add(rootUrl);
        visited.add(rootUrl);
        childToParent.put(rootUrl, "");

        for (int depth = 0; depth < 3; depth++) {
            List<String> currentLevel = new ArrayList<>();

            while (!queue.isEmpty()) {
                currentLevel.add(queue.poll());
            }

            if (currentLevel.isEmpty()) break;

            List<CompletableFuture<Void>> levelFutures = new ArrayList<>();

            for (String currentUrl : currentLevel) {
                CompletableFuture<Void> urlProcessingFuture = processURLAsync(currentUrl)
                        .thenCompose(childLinks -> {
                            List<CompletableFuture<Void>> childFutures = new ArrayList<>();

                            for (String childLink : childLinks) {
                                logger.info("Current link being processed: " + childLink);

                                CompletableFuture<Void> childFuture;
                                if (visited.add(childLink)) {
                                    queue.add(childLink);
                                    childToParent.put(childLink, currentUrl);

                                    childFuture = db.mergeNodeWithChildURL(currentUrl, childLink)
                                            .toCompletableFuture()
                                            .thenAccept(v -> {
//                                                System.out.println(currentUrl + " >>> " + childLink);
                                            });

                                } else {
                                    String existingParent = childToParent.get(childLink);
                                    if (existingParent != null && !existingParent.equals(currentUrl) && !wouldCreateCycle(currentUrl, childLink)) {
                                        childFuture = db.mergeNodeWithChildURL(currentUrl, childLink)
                                                .toCompletableFuture()
                                                .thenAccept(v -> {
//                                                    System.out.println("Additional edge: " + currentUrl + " >>> " + childLink);
                                                });
                                    } else {
                                        childFuture = CompletableFuture.completedFuture(null);
                                    }
                                }

                                childFutures.add(childFuture.exceptionally(ex -> {
//                                    System.err.println("Error processing link " + childLink + ": " + ex.getMessage());
                                    return null;
                                }));
                            }

                            return CompletableFuture.allOf(childFutures.toArray(new CompletableFuture[0]));
                        });

                levelFutures.add(urlProcessingFuture);
            }

            CompletableFuture.allOf(levelFutures.toArray(new CompletableFuture[0])).get();
        }
    }

    public CompletableFuture<List<String>> processURLAsync(String webpage) throws MalformedURLException {
        URL url = new URL(webpage);
        return CompletableFuture.supplyAsync(() -> {
            List<String> hyperlinks = new ArrayList<>();
            try {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        grepHyperLinks(hyperlinks, line);
                    }
                }
            } catch (IOException e) {
//                System.err.println("Failed to process " + webpage + ": " + e.getMessage());
            }
            return filterValidUrls(hyperlinks);
        }, exec);
    }

    private List<String> filterValidUrls(List<String> urls) {
        return urls.stream()
                .filter(url -> {
                    try {
                        new URL(url);
                        return true;
                    } catch (MalformedURLException e) {
                        return false;
                    }
                })
                .distinct()
                .collect(Collectors.toList());
    }

    public boolean wouldCreateCycle(String sourceUrl, String targetUrl) {

        String current = sourceUrl;
        while (current != null) {
            if (current.equals(targetUrl)) {
                return true;
            }
            current = childToParent.get(current);
        }
        return false;
    }


    public static void grepHyperLinks(List<String> links, String html) {
        int start = 0;

        while (start != -1 && start < html.length()) {
            start = html.indexOf("http", start);
            if (start != -1) {
                int end = html.indexOf("\"", start + 1);
                if (end == -1) {  // If no closing quote is found, end the search
                    break;
                }
                String url = html.substring(start, end);
                if (url.endsWith("/")) {
                    url = url.substring(0, url.length() - 1);
                }
                if(url.length()<=1000) links.add(url);
                start = end + 1;  // Move start to just past the last found URL to continue search
            }
        }
    }

    /**
     * This method is use for testing via Mockito
     * @param db
     */
    public void setDb(Neo4jTransactionHandler db) {
        this.db = db;
    }

    private static Crawler instance;
    private Set<String> visited;
    public Map<String, String> childToParent; // made public for testing
    private Neo4jTransactionHandler db;
    private ExecutorService exec;
    private static final Logger logger = LogManager.getLogger(Crawler.class);

    public static class SyncNeo4jTransactionHandler {

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
                String query = "MATCH (n) RETURN count(n) AS count";

                return this.session.readTransaction(tx -> {
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


    }
}