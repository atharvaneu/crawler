package org.neu;

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

    public static SyncCrawler getInstance() {
        if (instance == null) {
            instance = new SyncCrawler();
        }

        return instance;
    }

    public void init() {
        this.db = new Crawler.SyncNeo4jTransactionHandler();
        this.db.initialize();
        this.visited = new HashSet<>();
    }

    public void close() {
        this.db.close();
    }

    public void run(String url, long timeoutMillis) throws IOException {
        bfsTraversal(url,timeoutMillis);
    }


    public void bfsTraversal(String webpage, long timeoutMillis) throws IOException {
        // Create a queue for BFS
        Queue<String> queue = new LinkedList<>();
        // Push the root URL vertex into the queue
        queue.add(webpage);
        // Mark the root URL as visited
        visited.add(webpage);

        // Track the start time
        long startTime = System.currentTimeMillis();

        // Iterate over the queue
        while (!queue.isEmpty()) {
            // Check if the elapsed time exceeds the timeout
            if ((System.currentTimeMillis() - startTime) > timeoutMillis) {
                System.out.println("Time limit reached. Stopping the BFS traversal.");
                break; // Exit the loop if time limit is exceeded
            }

            String url = queue.poll();

            List<String> childLinks = processURL(url);
            for (String _link : childLinks) {
                if (visited.contains(_link)) continue;
                visited.add(_link);
                this.db.mergeNodeWithChildURL(url, _link);
                queue.add(_link);
            }
        }

        // Optionally, perform any necessary cleanup or final operations here
        System.out.println("BFS traversal finished.");
    }

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

    public long getAllNodes(){
        return this.db.getAllNodes();
    }

    private static SyncCrawler instance;
    private Set<String> visited;
    private Crawler.SyncNeo4jTransactionHandler db;
}
