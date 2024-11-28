package org.neu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class Crawler {

    private Crawler() {}

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
//        this.visited = new HashSet<>();
        this.visited = ConcurrentHashMap.newKeySet(); // thread safe hashSet
    }

    public void close() {
        this.db.close();
    }

    public void run(String url) throws IOException, InterruptedException, ExecutionException {
        bfsTraversal(url);
    }

    private void bfsTraversall(String rootUrl) throws InterruptedException, ExecutionException {
        Queue<String> queue = new LinkedList<>();
        queue.add(rootUrl);
        visited.add(rootUrl);

        int depth = 0;
        while (!queue.isEmpty()) {
            if (depth >= 3) break;

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            String url = queue.poll();

            CompletableFuture<List<String>> future = processURLAsync(url);
            future.thenAccept(childLinks -> {
                for (String link : childLinks) {
                    if (!visited.contains(link)) {
                        visited.add(link);
                        System.out.println(url + "\t>>>\t" + link);
                        db.mergeNodeWithChildURL(url, link);
                        queue.add(link);
                    }
                }
            }).exceptionally(ex -> {
                System.out.println("Error processing URL " + url + ": " + ex.getMessage());
                return null;
            });

            depth += 1;

            futures.add(future.thenRun(() -> {}));
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        }
    }

    private void bfsTraversal(String webpage) throws IOException {
        //create a queue for bfs
        Queue<String> queue = new LinkedList<>();
//        //Initialize the visited set
//        Set<String> visited  = new HashSet<>();
        //push the root URL vertex into the queue
        queue.add(webpage);
        //mark the root URL - visited
        visited.add(webpage);
        //Iterate over the queue
        int cnt=0;
        while(!queue.isEmpty()) {
            if(cnt==3) break;
            String url = queue.poll();

            List<String> childLinks = processURL(url);
            for(String _link: childLinks){
                if(visited.contains(_link)) continue;
                visited.add(_link);

                System.out.println(url + "\t>>>\t" + _link);

                this.db.mergeNodeWithChildURL(url, _link);
                queue.add(_link);
            }
            cnt+=1;
        }
    }

    private CompletableFuture<List<String>> processURLAsync(String webpage) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> hyperlinks = new ArrayList<>();
            try {
                URL url = new URL(webpage);
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    grepHyperLinks(hyperlinks, line);
                }
                reader.close();
            } catch (IOException e) {
                System.out.println("Failed to process " + webpage + ": " + e.getMessage());
            }
            return hyperlinks;
        }, exec);
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

//            for(String _link: hyperlinks) {
//                System.out.println(_link);
//            }


            reader.close();

        }
        catch (MalformedURLException e) {
            System.out.println(e.getMessage());
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
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
                links.add(url);
                start = end + 1;  // Move start to just past the last found URL to continue search
            }
        }
    }

    /**
     * Iteratively extract any links present inside a html string
     *
     * @param links
     * @param html
     */
    public void grepHyperLinkss(List<String> links, String html) {
        if (html.contains("https") || html.contains("http")) {
            try{
                int start = html.indexOf("http");
                int end = html.indexOf("\"", start + 1);

                String url = html.substring(start, end);
                if (!visited.contains(url)) {
                    links.add(url);
                }

                html = html.replace(url, "");

                grepHyperLinks(links, html);
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }

        }
    }



    private static Crawler instance;
    private Set<String> visited;
    private Neo4jTransactionHandler db;
    private Executor exec;
}
