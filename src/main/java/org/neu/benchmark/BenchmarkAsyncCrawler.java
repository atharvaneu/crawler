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
        String[] args = null;

        RuntimeConfig runtimeConfig = RuntimeConfig.getInstance();

        if (!runtimeConfig.asyncMode) {
            return;
        }

        long ms = runtimeConfig.asyncTime;


        Thread crawlerThread = new Thread(() -> {
            Crawler webcrawler = Crawler.getInstance();
            try {
                webcrawler.init();
                for (String _page: pages)
                    webcrawler.run(_page);
            } catch (Exception e) {
                System.out.println("Benchmark for " + ms + " -> " + webcrawler.getAllNodes());
            } finally {
                webcrawler.close();
            }
        });

        crawlerThread.start();

        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted: " + e.getMessage());
        }

        crawlerThread.interrupt();
//        webcrawler.close();
        System.out.println("Crawler stopped after " + ms + " milliseconds.");
    }

}
