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
    public void benchmark(String page) throws MalformedURLException, IOException, InterruptedException {

        RuntimeConfig runtimeConfig = RuntimeConfig.getInstance();

        if (!runtimeConfig.asyncMode) {
            return;
        }

        long ms = runtimeConfig.asyncTime;

        Crawler webcrawler = Crawler.getInstance();

        // Create and start the crawler thread
        Thread crawlerThread = new Thread(() -> {
            try {
                System.out.println("\n======================================(ASYNC) CRAWLER INIT===========================================");
                System.out.println("======================================(ASYNC) URL: " + runtimeConfig.rootUrl + "===========================================\n");

                webcrawler.init();
                webcrawler.run(page);

            } catch (Exception e) {
                System.out.println("Crawler interrupted or finished execution: " + e.getMessage());
            }
            finally {
            }
        });

        crawlerThread.start();

       // Main thread sleeps for the configured duration
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted: " + e.getMessage());
        }

        System.out.println("\n(ASYNC) Benchmark for " + ms + "ms -> " + webcrawler.getAllNodes() + " URLs crawled.\n");

        if (runtimeConfig.isVerbose) {
            webcrawler.displayURLsByRank();
        }

        // UNCOMMENT BELOW LINES FOR DATABASE STATISTICS DEBUGGING
//        System.out.println("\nDatabase Statistics:");
//        webcrawler.getDb().printDatabaseCounts().join();
//
//        System.out.println("\nDetailed Relationships:");
//        webcrawler.getDb().printDatabaseContents().join();

        webcrawler.close();

        System.out.println("\n======================================(ASYNC) CRAWLER CLOSED===========================================\n");

        System.exit(1);
    }
}