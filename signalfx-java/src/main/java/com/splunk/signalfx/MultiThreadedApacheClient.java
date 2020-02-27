package com.splunk.signalfx;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Wraps the single-threaded ApacheClient with an ExecutorService using a configurable number of threads.
 */
public class MultiThreadedApacheClient implements HttpClient {

    private final ApacheClient client = new ApacheClient();
    private final ExecutorService executor;
    private final long timeout;
    private final TimeUnit timeUnit;

    /**
     * @param numThreads the number of threads in the ExecutorService
     * @param timeout the maximum time to wait for pending requests to send after shutdown
     * @param timeUnit the time unit of the timeout argument
     */
    public MultiThreadedApacheClient(int numThreads, long timeout, TimeUnit timeUnit) {
        executor = Executors.newFixedThreadPool(numThreads);
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    @Override
    public void write(String url, Map<String, String> headers, byte[] bytes, String type) {
        executor.execute(() -> trySending(url, headers, bytes, type));
    }

    private void trySending(String url, Map<String, String> headers, byte[] bytes, String type) {
        try {
            client.write(url, headers, bytes, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        executor.shutdown();
        boolean ok = awaitTermination();
        if (!ok) {
            throw new IOException("ThreadedApacheClient was unable to complete all requests before timeout");
        }
    }

    private boolean awaitTermination() {
        try {
            return executor.awaitTermination(timeout, timeUnit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
