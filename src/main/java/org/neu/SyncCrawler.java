package org.neu;

import org.neu.neo4j.SyncNeo4jTransactionHandler;
import org.neu.neo4j.URLRank;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import java.util.HashSet;
import java.util.Set;

public class SyncCrawler {

    private SyncCrawler() {}

    /**
     * Singleton getInstance method for maintaining a single crawler instance throughout the running period of the application.
     *
     * @return SyncCrawler
     */
    public static SyncCrawler getInstance() {
        if (instance == null) {
            instance = new SyncCrawler();
        }

        return instance;
    }

    /**
     * Initialize the crawler. All initializations such as file, network, and DB must be declared here.
     */
    public void init() throws InterruptedException {
        this.db = new SyncNeo4jTransactionHandler();
        this.db.initialize();
        this.visited = new HashSet<>();
    }

    /**
     * Close the crawler. All closing such as file, network, and DB closing must be done here.
     */
    public void close() throws InterruptedException {
        this.db.close();
    }

    /**
     * A wrapper over the BFS method to for a clean API.
     *
     * @param url
     * @param timeoutMillis
     * @throws IOException
     */
    public void run(String url, long timeoutMillis) throws IOException {
        bfsTraversal(url,timeoutMillis);
    }


    /**
     * Given a root URL, start BFS synchronously on that by processing that URL and adding any valid neighbor URLs to the BFS queue.
     *
     * Note: to understand processing a URL, see `processURLAsync(String webpage)`
     *
     * @param webpage
     * @param timeoutMillis
     * @throws IOException
     */
    public void bfsTraversal(String webpage, long timeoutMillis) throws IOException {
        Queue<String> queue = new LinkedList<>();
        queue.add(webpage);
        visited.add(webpage);
        long startTime = System.currentTimeMillis();

        while (!queue.isEmpty()) {
            if ((System.currentTimeMillis() - startTime) > timeoutMillis) {
                System.out.println("Time limit reached. Stopping the BFS traversal.");
                break;
            }

            String url = queue.poll();
            List<String> childLinks = processURL(url);

            for (String _link : childLinks) {
                // Check time limit for each child link processing
                if ((System.currentTimeMillis() - startTime) > timeoutMillis) {
                    System.out.println("Time limit reached during child processing. Stopping the BFS traversal.");
                    return;
                }

                if (visited.contains(_link)) continue;
                visited.add(_link);
                this.db.mergeNodeWithChildURL(url, _link);
                queue.add(_link);
            }
        }

        System.out.println("BFS traversal finished.");
    }
    /**
     * Process URLs synchronously using Java's CompletableFuture API.
     * Processing of a URL includes fetching the HTML content available at the URL, grepping any URLs in that content, and finally adding those URLs to the BFS queue.
     *
     * @param webpage
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    private List<String> processURL(String webpage) throws MalformedURLException, IOException {
        List<String> lines = new ArrayList<>();
        List<String> hyperlinks = new ArrayList<>();

        try {

            URL url = new URL(webpage);

            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

            String line;

            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }


            for (String _line: lines) {
                grepHyperLinks(hyperlinks, _line);
            }

            reader.close();

        }
        catch (MalformedURLException e) {
//            System.out.println(e.getMessage());
        }
        catch (IOException e) {
//            System.out.println(e.getMessage());
        }

        return hyperlinks;
    }

    /**
     * Display URLs from the database sorted by order of their in-degrees
     */
    public void displayURLsByRank() {
        List<URLRank> urls = db.getURLsByInDegree();

        System.out.println("\n(SYNC) URL Rankings by In-Degree:");
        urls.forEach(System.out::println);

    }

    /**
     * Extracts URLs in O(n) runtime from within HTML content by looking for the substring `http`.
     *
     * @param links
     * @param html
     */
    public static void grepHyperLinks(List<String> links, String html) {
        Crawler.grepHyperLinks(links, html);
    }

    /**
     * Fetch and return the total number of nodes (URLs) inserted in the database in the specified time period.
     *
     * @return long Total number of nodes (URLs) inserted in the database.
     */
    public long getAllNodes(){
        return this.db.getAllNodes();
    }

    private static SyncCrawler instance;
    private Set<String> visited;
    private SyncNeo4jTransactionHandler db;
}
