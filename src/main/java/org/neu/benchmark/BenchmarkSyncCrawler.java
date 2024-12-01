package org.neu.benchmark;

import org.neu.RuntimeConfig;
import org.neu.SyncCrawler;

import java.io.*;
import java.net.MalformedURLException;

public class BenchmarkSyncCrawler implements Benchmarker {
    @Override
    public void benchmark() throws MalformedURLException, IOException, InterruptedException {
        String[] args = null;

        RuntimeConfig runtimeConfig = RuntimeConfig.getInstance();

        if (!runtimeConfig.syncMode) {
            return;
        }

        SyncCrawler webcrawler = SyncCrawler.getInstance();
        /**
         * Making the webcrawler instantiation into a singleton, with init and close methods - this is because it would help in calling webcrawler.run(webpage)
         * method on multiple start pages (preferrably on an array) instead of just a single link
         */
        long ms = runtimeConfig.syncTime;
        webcrawler.init();

        for (String _page: pages) {
            webcrawler.run(_page, ms);
            System.out.println("Benchmark for "+ms+" -> "+webcrawler.getAllNodes());
        }

        webcrawler.close();
    }


    private static String[] pages = {
            "https://athk.dev/"
    };
}