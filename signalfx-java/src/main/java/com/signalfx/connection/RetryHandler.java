package com.signalfx.connection;

import java.io.IOException;
import java.util.List;

import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;

import static com.signalfx.connection.RetryDefaults.DEFAULT_MAX_RETRIES;
import static com.signalfx.connection.RetryDefaults.DEFAULT_NON_RETRYABLE_EXCEPTIONS;

/**
 * Compared to the {@link DefaultHttpRequestRetryHandler} we allow retry on {@link
 * javax.net.ssl.SSLException}, because it gets thrown when we try to send data points over a
 * connection that our server has already closed. It is still unknown how exactly our server closes
 * "stale" connections in such a way that http client is unable to detect this.
 */
class RetryHandler extends DefaultHttpRequestRetryHandler {

  public RetryHandler(final int maxRetries) {
    this(maxRetries, DEFAULT_NON_RETRYABLE_EXCEPTIONS);
  }

  public RetryHandler() {
    this(DEFAULT_MAX_RETRIES, DEFAULT_NON_RETRYABLE_EXCEPTIONS);
  }

  public RetryHandler(final int maxRetries, List<Class<? extends IOException>> clazzes) {
    super(maxRetries, true, clazzes);
  }
}
