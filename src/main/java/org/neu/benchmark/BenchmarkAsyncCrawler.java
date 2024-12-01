package org.neu.benchmark;


import org.neu.Crawler;
import org.neu.RuntimeConfig;

import java.io.IOException;
import java.net.MalformedURLException;

public class BenchmarkAsyncCrawler implements Benchmarker {

    @Override
    public void benchmark() throws MalformedURLException, IOException, InterruptedException {
        String[] args = null;

        RuntimeConfig runtimeConfig = RuntimeConfig.getInstance();

        if (!runtimeConfig.asyncMode) {
            return;
        }

        long ms = runtimeConfig.asyncTime;
        Crawler webcrawler = Crawler.getInstance();
        webcrawler.init();


        Thread crawlerThread = new Thread(() -> {
            try {
                webcrawler.run("https://www.wikipedia.org/");
            } catch (Exception e) {
                System.out.println("Crawler interrupted or finished execution: " + e.getMessage());
            }
        });

        crawlerThread.start();

        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted: " + e.getMessage());
        }

        crawlerThread.interrupt();
        webcrawler.close();
        System.out.println("Crawler stopped after " + ms + " milliseconds.");

    }
}

