package org.neu;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws MalformedURLException, IOException {
        Crawler webcrawler = Crawler.getInstance();
        /**
         * Making the webcrawler instantiation into a singleton, with init and close methods - this is because it would help in calling webcrawler.run(webpage)
         * method on multiple start pages (preferrably on an array) instead of just a single link
         */

        webcrawler.init();

        webcrawler.run(webpage);

        webcrawler.close();
    }

    private static String webpage = "https://athk.dev";
}