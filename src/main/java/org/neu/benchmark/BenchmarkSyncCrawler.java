package org.neu.benchmark;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neu.Crawler;
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
    public void benchmark(String page) throws MalformedURLException, IOException, InterruptedException {

        RuntimeConfig runtimeConfig = RuntimeConfig.getInstance();

        if (!runtimeConfig.syncMode) {
            return;
        }

        SyncCrawler webcrawler = SyncCrawler.getInstance();

        long ms = runtimeConfig.syncTime;

        System.out.println("\n======================================(SYNC) CRAWLER INIT===========================================\n");

        webcrawler.init();

        webcrawler.run(page, ms);
        System.out.println("(SYNC) Benchmark for " + ms + "ms -> " + webcrawler.getAllNodes() + " URLs crawled.");

        if (runtimeConfig.isVerbose) {
            webcrawler.displayURLsByRank();
        }

        webcrawler.close();
        System.out.println("\n======================================(SYNC) CRAWLER CLOSED===========================================\n");
    }

    private static final Logger logger = LogManager.getLogger(BenchmarkSyncCrawler.class);
}
