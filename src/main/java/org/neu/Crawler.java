package org.neu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class Crawler {

    public static void run(String webpage) throws IOException {
//        bfsTraversal(webpage);
        Neo4jTransactionHandler tx = new Neo4jTransactionHandler();
        tx.initialize();
        tx.close();
        System.out.println(tx.getAllInboundNodes(webpage)+" "+tx.getAllOutboundNodes(webpage));
    }

    private static List<String> processURL(String webpage) throws MalformedURLException, IOException {
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

            for(String _link: hyperlinks) {
                System.out.println(_link);
            }


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

    /**
     * Recursively extract any links present inside a html string
     *
     * @param links
     * @param html
     */
    private static void grepHyperLinksRecursive(List<String> links, String html) {
        if (html.contains("https") || html.contains("http")) {
            int start = html.indexOf("http");
            int end = html.indexOf("\"", start + 1);

            String url = html.substring(start, end);
            links.add(url);

            html = html.replace(url, "");

            grepHyperLinksRecursive(links, html);
        }

    }

    /**
     * Iteratively extract any links present inside a html string
     *
     * @param links
     * @param html
     */
    public static void grepHyperLinks(List<String> links, String html) {
        if (html.contains("https") || html.contains("http")) {
            try{
                int start = html.indexOf("http");
                int end = html.indexOf("\"", start + 1);

                String url = html.substring(start, end);
                links.add(url);

                html = html.replace(url, "");

                grepHyperLinks(links, html);
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }

        }
    }

    private static void bfsTraversal(String webpage) throws IOException {
        //create a queue for bfs
        Queue<String> queue = new LinkedList<>();
        //Initialize the visited set
        Set<String> visited  = new HashSet<>();
        //push the root URL vertex into the queue
        queue.add(webpage);
        //mark the root URL - visited
        visited.add(webpage);
        //Iterate over the queue
        while(!queue.isEmpty()) {
            String url = queue.poll();
            List<String> childLinks = processURL(url);
            for(String _link: childLinks){
                if(!visited.contains(_link)) {
                    System.out.println(url);
                    visited.add(_link);
                    queue.add(_link);
                }
            }

        }
    }


}
