package org.neu;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws MalformedURLException, IOException {
        Main.run();
    }

    private static void run() throws MalformedURLException, IOException {
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

    }

    /**
     * Recursively extract any links present inside a html string
     *
     * @param links
     * @param html
     */
    private static void grepHyperLinks(List<String> links, String html) {
        if (html.contains("https") || html.contains("http")) {
            int start = html.indexOf("http");
            int end = html.indexOf("\"", start + 1);

            String url = html.substring(start, end);
            links.add(url);

            html = html.replace(url, "");

            grepHyperLinks(links, html);
        }

    }

    private static String webpage = "https://athk.dev";
}