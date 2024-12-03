package org.neu.benchmark;

import org.neu.Crawler;
import org.neu.RuntimeConfig;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Benchmarks the performance of an asynchronous web crawler.
 */
public class BenchmarkAsyncCrawler implements Benchmarker {

    /**
     * Benchmarks the asynchronous operation of the Crawler.
     *
     * <p>
     * If the runtime configuration does not enable asynchronous mode,
     * the benchmark exits early. Otherwise, it initializes the crawler,
     * runs it in a separate thread for a specified duration, and then
     * interrupts the crawler thread.
     * </p>
     *
     * @throws MalformedURLException If the URL provided to the crawler is malformed.
     * @throws IOException If an I/O exception occurs during the crawler's operation.
     * @throws InterruptedException If the main thread is interrupted during sleep.
     */
    @Override
    public void benchmark(String[] pages) throws MalformedURLException, IOException, InterruptedException {

        // Retrieve runtime configuration instance
        RuntimeConfig runtimeConfig = RuntimeConfig.getInstance();

        // Exit if asynchronous mode is not enabled
        if (!runtimeConfig.asyncMode) {
            return;
        }

        // Get the configured duration for the benchmark
        long ms = runtimeConfig.asyncTime;

        // Initialize the crawler

        Crawler webcrawler = Crawler.getInstance();
        // Create and start the crawler thread
        Thread crawlerThread = new Thread(() -> {
            try {
                webcrawler.init();
                webcrawler.run("https://www.wikipedia.org/");

            } catch (Exception e) {
                System.out.println("Crawler interrupted or finished execution: " + e.getMessage());
            }
            finally {
            }
        });

        crawlerThread.start();

//        crawlerThread.join(ms);
//        // Main thread sleeps for the configured duration
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted: " + e.getMessage());
        }
//
//        // Interrupt the crawler thread and close the crawler
//        crawlerThread.interrupt();
        System.out.println("\n(ASYNC) Benchmark for " + ms + "ms -> " + webcrawler.getAllNodes() + " URLs crawled.\n");

        webcrawler.displayURLsByRank();

        // UNCOMMENT BELOW LINES FOR DATABASE STATISTICS DEBUGGING
//        System.out.println("\nDatabase Statistics:");
//        webcrawler.getDb().printDatabaseCounts().join();
//
//        System.out.println("\nDetailed Relationships:");
//        webcrawler.getDb().printDatabaseContents().join();

        System.out.println("CLOSING YEYEYEYEYE");
        webcrawler.close();

        System.exit(1);
    }
}