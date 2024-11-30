package org.neu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Crawler {
    private static Crawler instance;
    private Set<String> visited;
    private Map<String, String> childToParent;
    private Neo4jTransactionHandler db;
    private ExecutorService exec;

    private Crawler() {
    }

    public static Crawler getInstance() {
        if (instance == null) {
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
    }

    public void run(String url) throws InterruptedException, ExecutionException {
        bfsTraversal(url);
    }

    private void bfsTraversal(String rootUrl) throws InterruptedException, ExecutionException {
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
                                CompletableFuture<Void> childFuture;
                                if (visited.add(childLink)) {
                                    queue.add(childLink);
                                    childToParent.put(childLink, currentUrl);

                                    childFuture = db.mergeNodeWithChildURL(currentUrl, childLink)
                                            .toCompletableFuture()
                                            .thenAccept(v -> System.out.println(currentUrl + " >>> " + childLink));

                                } else {
                                    String existingParent = childToParent.get(childLink);
                                    if (existingParent != null && !existingParent.equals(currentUrl) && !wouldCreateCycle(currentUrl, childLink)) {
                                        childFuture = db.mergeNodeWithChildURL(currentUrl, childLink)
                                                .toCompletableFuture()
                                                .thenAccept(v -> System.out.println("Additional edge: " + currentUrl + " >>> " + childLink));
                                    } else {
                                        childFuture = CompletableFuture.completedFuture(null);
                                    }
                                }

                                childFutures.add(childFuture.exceptionally(ex -> {
                                    System.err.println("Error processing link " + childLink + ": " + ex.getMessage());
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

    private CompletableFuture<List<String>> processURLAsync(String webpage) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> hyperlinks = new ArrayList<>();
            try {
                URL url = new URL(webpage);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        grepHyperLinks(hyperlinks, line);
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to process " + webpage + ": " + e.getMessage());
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

    private boolean wouldCreateCycle(String sourceUrl, String targetUrl) {

        String current = sourceUrl;
        while (current != null) {
            if (current.equals(targetUrl)) {
                return true;
            }
            current = childToParent.get(current);
        }
        return false;
    }

    private CompletableFuture<List<String>> processURLAsyncc(String webpage) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> hyperlinks = new ArrayList<>();
            try {
                URL url = new URL(webpage);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        grepHyperLinks(hyperlinks, line);
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to process " + webpage + ": " + e.getMessage());
            }
            return hyperlinks;
        }, exec);
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
}