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

    /**
     * Singleton getInstance method for maintaining a single crawler instance throughout the running period of the application.
     *
     * @return Crawler
     */
    public static Crawler getInstance() {
        if (instance == null) {
            logger.info("Creating Crawler singleton instance");
            instance = new Crawler();
        }
        return instance;
    }

    /**
     * Initialize the crawler. All initializations for files, network, DB, and executorService must be declared here.
     */
    public void init() {
        this.db = new Neo4jTransactionHandler();
        this.db.initialize();

        this.exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.visited = ConcurrentHashMap.newKeySet();
        this.childToParent = new ConcurrentHashMap<>();

        logger.info("Crawler successfully initialized and is ready to begin crawling");
    }

    /**
     * Close the crawler. All closing such as file, network, DB, and executorService closing must be done here.
     */
    public void close() {
        this.exec.shutdown();
        try {
            if (!this.exec.awaitTermination(60, TimeUnit.SECONDS)) {
                this.exec.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.exec.shutdownNow();
        }
        this.db.close();

        logger.info("Crawler terminated");
    }

    /**
     * A wrapper over the BFS method to for a clean API.
     *
     * @param url
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MalformedURLException
     */
    public void run(String url) throws InterruptedException, ExecutionException, MalformedURLException {
        logger.info("Starting crawling with base URL: '{}'", url);

        bfsTraversal(url);
    }

    /**
     * Given a root URL, start BFS asynchronously on that by processing that URL and adding any valid neighbor URLs to the BFS queue.
     *
     * Note: to understand processing a URL, see `processURLAsync(String webpage)`
     *
     * @param rootUrl
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws MalformedURLException
     */
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

    /**
     * Process URLs asynchronously using Java's CompletableFuture API.
     * Processing of a URL includes fetching the HTML content available at the URL, grepping any URLs in that content, and finally adding those URLs to the BFS queue.
     *
     * @param webpage
     * @return CompletableFuture
     * @throws MalformedURLException
     */
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

    /**
     * Filters and returns a list of valid URLs from the input list.
     * A URL is considered valid if it can be successfully parsed by the Java URL class.
     * The method also removes any duplicate URLs from the list.
     *
     * @param urls
     * @return List<String> of valid URLs
     */
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

    /**
     * Check if adding a URL would create a cycle by checking if a child->parent relation already exists in the map.
     *
     * @param sourceUrl
     * @param targetUrl
     * @return true if cycle exists, else false
     */
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

    /**
     * Extracts URLs in O(n) runtime from within HTML content by looking for the substring `http`.
     *
     * @param links A List<String> in which the extracted URLs with be added.
     * @param html The HTML content string to search for hyperlinks.
     *
     */
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
     * Fetch and return the total number of nodes (URLs) asynchronously inserted in the database in the specified time period.
     *
     * @return long Total number of nodes (URLs) inserted in the database.
     */
    public long getAllNodes(){
        return this.db.getAllNodes().join();
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

}