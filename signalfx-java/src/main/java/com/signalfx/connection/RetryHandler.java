package com.signalfx.connection;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;

/**
 * Compared to the {@link DefaultHttpRequestRetryHandler} we allow retry on {@link
 * javax.net.ssl.SSLException}, because it gets thrown when we try to send data points over a
 * connection that our server has already closed. It is still unknown how exactly our server closes
 * "stale" connections in such a way that http client is unable to detect this.
 */
class RetryHandler extends DefaultHttpRequestRetryHandler {
  public static final Integer DEFAULT_MAX_RETRIES = 3;
  public static final List<Class<? extends IOException>> DEFAULT_NON_RETRYABLE_EXCEPTIONS = Arrays.asList(
          InterruptedIOException.class,
          UnknownHostException.class,
          ConnectException.class);

  public RetryHandler(final int maxRetries) {
    super(maxRetries, true, DEFAULT_NON_RETRYABLE_EXCEPTIONS);
  }

  public RetryHandler() {
    super(DEFAULT_MAX_RETRIES, true, DEFAULT_NON_RETRYABLE_EXCEPTIONS);
  }

  public RetryHandler(final int maxRetries, List<Class<? extends IOException>> clazzes) {
    super(maxRetries, true, clazzes);
  }
}
