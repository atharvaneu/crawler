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

        RuntimeConfig runtimeConfig = RuntimeConfig.getInstance();

        if (!runtimeConfig.syncMode) {
            return;
        }

        SyncCrawler webcrawler = SyncCrawler.getInstance();

        long ms = runtimeConfig.syncTime;
        webcrawler.init();

        for (String _page : pages) {
            webcrawler.run(_page, ms);
            System.out.println("Benchmark for " + ms + " -> " + webcrawler.getAllNodes());
        }

        webcrawler.close();
    }

    /**
     * List of pages to benchmark with the synchronous crawler.
     */
    private static String[] pages = {
            "https://www.wikipedia.org/"
    };
}
