package org.neu.benchmark;

import org.neu.RuntimeConfig;
import org.neu.SyncCrawler;

import java.io.*;
import java.net.MalformedURLException;

/**
 * Benchmarks the performance of a synchronous web crawler.
 */
public class BenchmarkSyncCrawler implements Benchmarker {

    /**
     * Benchmarks the synchronous operation of the SyncCrawler.
     *
     * <p>
     * If the runtime configuration does not enable synchronous mode, 
     * the benchmark exits early. Otherwise, it initializes the crawler, 
     * runs it on a list of predefined webpages for a specified duration, 
     * and prints the results.
     * </p>
     *
     * @throws MalformedURLException If any URL in the list of pages is malformed.
     * @throws IOException If an I/O exception occurs during the crawler's operation.
     * @throws InterruptedException If the benchmark is interrupted during its operation.
     */
    @Override
    public void benchmark() throws MalformedURLException, IOException, InterruptedException {
        String[] args = null;

        // Retrieve runtime configuration instance
        RuntimeConfig runtimeConfig = RuntimeConfig.getInstance();

        // Exit if synchronous mode is not enabled
        if (!runtimeConfig.syncMode) {
            return;
        }

        // Get the singleton instance of SyncCrawler
        SyncCrawler webcrawler = SyncCrawler.getInstance();

        /**
         * Making the webcrawler a singleton with init and close methods 
         * to facilitate reuse for multiple start pages.
         */
        long ms = runtimeConfig.syncTime;
        webcrawler.init();

        // Run the crawler for each page in the list
        for (String _page : pages) {
            webcrawler.run(_page, ms);
            System.out.println("Benchmark for " + ms + " -> " + webcrawler.getAllNodes());
        }

        // Close the crawler
        webcrawler.close();
    }

    /**
     * List of pages to benchmark with the synchronous crawler.
     */
    private static String[] pages = {
            "https://athk.dev/"
    };
}
