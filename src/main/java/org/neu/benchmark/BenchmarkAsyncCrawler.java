package org.neu.benchmark;


import org.neu.Crawler;

public class BenchmarkAsyncCrawler {

        public static void main(String[] args) {
            // Set the duration in milliseconds
            int ms = 2*60*1000;
            Crawler webcrawler = Crawler.getInstance();
            webcrawler.init();

            // Create a thread to run the webcrawler
            Thread crawlerThread = new Thread(() -> {
                try {
                    // Call the run method here
                    webcrawler.run("https://www.wikipedia.org/");
                } catch (Exception e) {
                    System.out.println("Crawler interrupted or finished execution: " + e.getMessage());
                }
            });

            // Start the thread
            crawlerThread.start();

            // Wait for the specified duration and interrupt the thread
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                System.out.println("Main thread interrupted: " + e.getMessage());
            }

            // Interrupt the crawler thread
            crawlerThread.interrupt();
            webcrawler.close();
            System.out.println("Crawler stopped after " + ms + " milliseconds.");

//            SyncCrawler syncCrawler = SyncCrawler.getInstance();
//            syncCrawler.init();
//            System.out.println("Benchmark for "+ms+" -> "+syncCrawler.getAllNodes());
//            syncCrawler.close();
        }
    }

