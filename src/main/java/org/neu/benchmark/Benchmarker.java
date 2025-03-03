package org.neu.benchmark;

import java.io.IOException;
import java.net.MalformedURLException;

public interface Benchmarker {
    public void benchmark(String page) throws MalformedURLException, IOException, InterruptedException;
}
