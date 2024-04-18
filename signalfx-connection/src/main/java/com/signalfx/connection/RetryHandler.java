package com.signalfx.connection;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.util.TimeValue;

import static com.signalfx.connection.RetryDefaults.DEFAULT_MAX_RETRIES;
import static com.signalfx.connection.RetryDefaults.DEFAULT_NON_RETRYABLE_EXCEPTIONS;

/**
 * Compared to the {@link DefaultHttpRequestRetryStrategy} we allow retry on {@link
 * javax.net.ssl.SSLException}, because it gets thrown when we try to send data points over a
 * connection that our server has already closed. It is still unknown how exactly our server closes
 * "stale" connections in such a way that http client is unable to detect this.
 */
class RetryHandler extends DefaultHttpRequestRetryStrategy {

  // NOTE: The default is Arrays.asList(429, 503) but we keep our own special list here for historical reasons
  private static final List<Integer> RETRYABLE_CODES = Arrays.asList(HttpStatus.SC_REQUEST_TIMEOUT, HttpStatus.SC_GATEWAY_TIMEOUT,
          598, -1);

  public RetryHandler(final int maxRetries) {
    this(maxRetries, DEFAULT_NON_RETRYABLE_EXCEPTIONS);
  }

  public RetryHandler() {
    this(DEFAULT_MAX_RETRIES, DEFAULT_NON_RETRYABLE_EXCEPTIONS);
  }

  public RetryHandler(final int maxRetries, List<Class<? extends IOException>> clazzes) {
    super(maxRetries, TimeValue.ofSeconds(1), clazzes, RETRYABLE_CODES);
  }
}
