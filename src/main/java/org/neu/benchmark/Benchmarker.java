package org.neu.benchmark;

import java.io.IOException;
import java.net.MalformedURLException;

public interface Benchmarker {
    public void benchmark() throws MalformedURLException, IOException, InterruptedException;
}
