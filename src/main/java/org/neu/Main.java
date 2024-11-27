package org.neu;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static String webpage = "https://athk.dev";
    public static void main(String[] args) throws MalformedURLException, IOException {
        Crawler webcrawler = new Crawler();
        webcrawler.run(webpage);
    }
}