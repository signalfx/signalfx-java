package com.splunk.signalfx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * A sender of datapoints to SignalFx.
 *
 * Pass in an Encoder and an HttpClient: either one in this package, or a custom implementation.
 * Use the provided Builder for sensible defaults.
 *
 * Once constructed, must be start()ed to read messages off of the datapoint queue.
 *
 * Message enqueuing methods e.g. recordGaugeValue() are thread safe.
 *
 * Call stop() to flush remaining messages and shutdown the queue reader thread.
 */
public class MetricSender {

    private final String ingestUrl;
    private final String token;
    private final Encoder encoder;
    private final HttpClient client;
    private final BlockingQueue<Point> queue;
    private final int maxBatchSize;
    private final long maxShutdownWaitMillis;
    private Thread thread;

    /**
     * Use the supplied Builder for sensible defaults.
     *
     * @param ingestUrl the full URL for ingest for the realm e.g. "https://ingest.us1.signalfx.com/v2/datapoint"
     * @param token the Access Token for the organization
     * @param encoder the encoder for the HTTP request payload. Either JsonEncoder or ProtobufEncoder.
     * @param client the HttpClient. Either ApacheClient, MultiThreadedApacheClient, or one of your own.
     * @param maxBuffered the maximum size of the queue of unsent datapoints. After this limit is reached, queueing calls block.
     * @param maxBatchSize the maximum number of datapoints that can be in a single request.
     * @param maxShutdownWaitMillis how long to wait in milliseconds for sender shutdown before bailing out.
     */
    public MetricSender(String ingestUrl, String token, Encoder encoder, HttpClient client,
                 int maxBuffered, int maxBatchSize, long maxShutdownWaitMillis) {
        this.ingestUrl = ingestUrl;
        this.token = token;
        this.encoder = encoder;
        this.client = client;
        this.maxBatchSize = maxBatchSize;
        this.maxShutdownWaitMillis = maxShutdownWaitMillis;
        queue = new ArrayBlockingQueue<>(maxBuffered);
    }

    /**
     * @return a MetricSender Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Enqueues a gauge datapoint with an integer value. Applies the current time as the datapoint's timestamp.
     */
    public void recordGaugeValue(String metric, int value, Map<String, String> dimensions) {
        recordGaugeValue(metric, value, dimensions, System.currentTimeMillis());
    }

    /**
     * Enqueues a gauge datapoint with an integer value.
     */
    public void recordGaugeValue(String metric, int value, Map<String, String> dimensions, long timestamp) {
        enqueue(new IntPoint(metric, dimensions, MetricType.GAUGE, timestamp, value));
    }

    /**
     * Enqueues a gauge datapoint with a double value. Applies the current time as the datapoint's timestamp.
     */
    public void recordGaugeValue(String metric, double value, Map<String, String> dimensions) {
        recordGaugeValue(metric, value, dimensions, System.currentTimeMillis());
    }

    /**
     * Enqueues a gauge datapoint with a double value.
     */
    public void recordGaugeValue(String metric, double value, Map<String, String> dimensions, long timestamp) {
        enqueue(new DoublePoint(metric, dimensions, MetricType.GAUGE, timestamp, value));
    }

    /**
     * Enqueues a counter datapoint with an int value. Applies the current time as the datapoint's timestamp.
     */
    public void recordCounterValue(String metric, int value, Map<String, String> dimensions) {
        recordCounterValue(metric, value, dimensions, System.currentTimeMillis());
    }

    /**
     * Enqueues a counter datapoint with an int value.
     */
    public void recordCounterValue(String metric, int value, Map<String, String> dimensions, long timestamp) {
        enqueue(new IntPoint(metric, dimensions, MetricType.COUNTER, timestamp, value));
    }

    /**
     * Enqueues a counter datapoint with a double value. Applies the current time as the datapoint's timestamp.
     */
    public void recordCounterValue(String metric, double value, Map<String, String> dimensions) {
        recordCounterValue(metric, value, dimensions, System.currentTimeMillis());
    }

    /**
     * Enqueues a counter datapoint with a double value.
     */
    public void recordCounterValue(String metric, double value, Map<String, String> dimensions, long timestamp) {
        enqueue(new DoublePoint(metric, dimensions, MetricType.COUNTER, timestamp, value));
    }

    /**
     * Enqueues a cumulative counter datapoint with an int value. Applies the current time as the datapoint's timestamp.
     */
    public void recordCumulativeCounterValue(String metric, int value, Map<String, String> dimensions) {
        recordCumulativeCounterValue(metric, value, dimensions, System.currentTimeMillis());
    }

    /**
     * Enqueues a cumulative counter datapoint with an int value.
     */
    public void recordCumulativeCounterValue(String metric, int value, Map<String, String> dimensions, long timestamp) {
        enqueue(new DoublePoint(metric, dimensions, MetricType.CUMULATIVE_COUNTER, timestamp, value));
    }

    /**
     * Enqueues a cumulative counter datapoint with a double value. Applies the current time as the datapoint's timestamp.
     */
    public void recordCumulativeCounterValue(String metric, double value, Map<String, String> dimensions) {
        recordCumulativeCounterValue(metric, value, dimensions, System.currentTimeMillis());
    }

    /**
     * Enqueues a cumulative counter datapoint with a double value.
     */
    public void recordCumulativeCounterValue(String metric, double value, Map<String, String> dimensions, long timestamp) {
        enqueue(new DoublePoint(metric, dimensions, MetricType.CUMULATIVE_COUNTER, timestamp, value));
    }

    private void enqueue(Point pt) {
        try {
            queue.put(pt);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // todo ENUM type?

    /**
     * Starts the thread which reads from the queue of datapoints and sends them to the client.
     * Call stop() to gracefully stop this thread.
     */
    public void start() {
        thread = new Thread(new QueueReader(queue, this::trySending, maxBatchSize));
        thread.start();
    }

    private void trySending(Iterable<Point> buf) {
        try {
            send(buf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void send(Iterable<Point> buf) throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-SF-TOKEN", token);
        String type = encoder.getType();
        headers.put("content-type", type);
        byte[] encoded = encoder.encode(buf);
        client.write(ingestUrl, headers, encoded, type);
    }

    /**
     * Sends remaining datapoints and stops the queue reader thread. Waits a maximum of maxShutdownWaitMillis
     * for this process to complete.
     */
    public void stop() throws InterruptedException, IOException {
        thread.interrupt();
        thread.join(maxShutdownWaitMillis);
        client.close();
    }

    public static class Builder {

        private static final int DEFAULT_MAX_BUFFER_SIZE = 10_000;
        private static final int DEFAULT_MAX_BATCH_SIZE = 1_000;
        private static final int DEFAULT_MAX_SHUTDOWN_WAIT_MILLIS = 10_000;

        private String ingestUrl;
        private String token;
        private Encoder encoder;
        private HttpClient client;
        private int maxBufferSize;
        private int maxBatchSize;
        private int maxShutdownWaitMillis;

        /**
         * Sets the encoder to ProtobufEncoder, the client to ApacheClient, and calls withDefaultParams().
         */
        public Builder withDefaults() {
            encoder = new ProtobufEncoder();
            client = new ApacheClient();
            return withDefaultParams();
        }

        /**
         * Set maxBufferSize, maxBatchSize, and maxShutdownWaitMillis to their default values.
         */
        public Builder withDefaultParams() {
            maxBufferSize = DEFAULT_MAX_BUFFER_SIZE;
            maxBatchSize = DEFAULT_MAX_BATCH_SIZE;
            maxShutdownWaitMillis = DEFAULT_MAX_SHUTDOWN_WAIT_MILLIS;
            return this;
        }

        /**
         * Sets full ingest URL of the MetricSender associated with the passed-in realm.
         * May be more convenient than calling ingestUrl().
         */
        public Builder realm(Realm realm) {
            return ingestUrl(realm.getIngestUrl());
        }

        /**
         * Sets the full ingest URL for the realm.
         * @param ingestUrl e.g. "https://ingest.us1.signalfx.com/v2/datapoint"
         */
        public Builder ingestUrl(String ingestUrl) {
            this.ingestUrl = ingestUrl;
            return this;
        }

        /**
         * Sets the Access Token for the organization
         */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * Sets the encoder for the HTTP request payload. Either JsonEncoder or ProtobufEncoder.
         */
        public Builder encoder(Encoder encoder) {
            this.encoder = encoder;
            return this;
        }

        /**
         * Sets the HttpClient. Either ApacheClient, MultiThreadedApacheClient, or one of your own.
         */
        public Builder client(HttpClient client) {
            this.client = client;
            return this;
        }

        /**
         * Sets the maximum size of the queue of unsent datapoints. After this limit is reached, queueing calls block.
         * Default: 10,000
         */
        public Builder maxBufferSize(int maxBufferSize) {
            this.maxBufferSize = maxBufferSize;
            return this;
        }

        /**
         * Sets the maximum number of datapoints that can be in a single request.
         * Default: 1,000
         */
        public Builder maxBatchSize(int maxBatchSize) {
            this.maxBatchSize = maxBatchSize;
            return this;
        }

        /**
         * Sets how long to wait in milliseconds for sender shutdown before bailing out.
         * Default: 10,000 (10s)
         */
        public Builder maxShutdownWaitMillis(int maxShutdownWaitMillis) {
            this.maxShutdownWaitMillis = maxShutdownWaitMillis;
            return this;
        }

        public MetricSender build() {
            return new MetricSender(ingestUrl, token, encoder, client, maxBufferSize, maxBatchSize, maxShutdownWaitMillis);
        }
    }

    private static class QueueReader implements Runnable {

        final BlockingQueue<Point> queue;
        final Consumer<Iterable<Point>> sender;
        final int maxBatchSize;

        QueueReader(BlockingQueue<Point> queue, Consumer<Iterable<Point>> sender, int maxBatchSize) {
            this.queue = queue;
            this.sender = sender;
            this.maxBatchSize = maxBatchSize;
        }

        @Override
        public void run() {
            List<Point> batch = new ArrayList<>(maxBatchSize);
            while (true) {
                try {
                    batch.add(queue.take());
                } catch (InterruptedException e) {
                    if (!batch.isEmpty()) {
                        sender.accept(batch);
                    }
                    if (!queue.isEmpty()) {
                        sender.accept(queue);
                    }
                    return;
                }
                if (queue.isEmpty() || batch.size() == maxBatchSize) {
                    sender.accept(batch);
                    batch = new ArrayList<>(maxBatchSize);
                }
            }
        }
    }
}
