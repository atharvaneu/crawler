package org.neu;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neu.benchmark.BenchmarkAsyncCrawler;
import org.neu.benchmark.BenchmarkSyncCrawler;
import org.neu.benchmark.Benchmarker;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SequencedSet;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws RuntimeException, ExecutionException, InterruptedException, MalformedURLException, IOException {

        handleArgs(args);

        RuntimeConfig runtimeConfig = RuntimeConfig.getInstance();


        if (runtimeConfig.syncMode) {
            Benchmarker syncBenchmarker = new BenchmarkSyncCrawler();

            syncBenchmarker.benchmark(startingPage);
        }

        if (runtimeConfig.asyncMode) {
            Benchmarker asyncBenchmarker = new BenchmarkAsyncCrawler();

            asyncBenchmarker.benchmark(startingPage);
        }

    }

    /**
     * Handle arguments for running the application smoothly, and validating any errors.
     *
     * @param args
     * @throws RuntimeException
     * @throws NumberFormatException
     */
    public static void handleArgs(String[] args) throws RuntimeException, NumberFormatException {
        if (args.length == 0) {
            logger.fatal("\nPlease provide method of running, and time for benchmarks.\nSee --help for more usage.");
            throw new RuntimeException("InvalidArgumentException");
        }

        RuntimeConfig runtimeConfig = RuntimeConfig.getInstance();

        for (String arg: args) {
            if (!arg.startsWith("--")) {
                logger.fatal("\nIncorrect format of arguments, please see --help for correct usage.");
                throw new RuntimeException("InvalidArgumentException");
            }

            arg = arg.replace("--", "");


            if (arg.equals("help")) {
                logger.fatal("\nTo run the crawler in either sync or async mode, provide arguments in this way: --[method]=[time_in_milliseconds]\nFor eg.\njava Main --async=40000\njava Main --sync=20000");
                System.exit(1);
            }

            if (arg.equals("verbose")) {
                runtimeConfig.isVerbose = true;
                continue;
            }

            if (arg.contains("=")) {
                String[] pair = arg.split("=");
                String key = pair[0];
                String value = pair[1];

                if (key.equals("async")) {
                    runtimeConfig.asyncMode = true;
                    runtimeConfig.asyncTime = Long.parseLong(value);
                }
                else if (key.equals("sync")) {
                    runtimeConfig.syncMode = true;
                    runtimeConfig.syncTime = Long.parseLong(value);
                }
                else {
                    logger.fatal("Invalid argument key: {} in argument --{}", key, arg);
                    throw new RuntimeException("InvalidArgumentException");
                }
            }

        }

    }

    private static final Logger logger = LogManager.getLogger(Main.class);
    private static String startingPage = "https://www.wikipedia.org/";
}