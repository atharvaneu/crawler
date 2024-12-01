package org.neu;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SequencedSet;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws RuntimeException, ExecutionException, InterruptedException, MalformedURLException {

        handleArgs(args);

        Crawler webcrawler = Crawler.getInstance();
        /**
         * Making the webcrawler instantiation into a singleton, with init and close methods - this is because it would help in calling webcrawler.run(webpage)
         * method on multiple start pages (preferrably on an array) instead of just a single link
         */

        webcrawler.init();

        for (String _page: pages) {
            webcrawler.run(_page);
        }

        webcrawler.close();
    }

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
    private static String[] pages = {
            "https://athk.dev",
            "https://www.wikipedia.org/"
    };
}