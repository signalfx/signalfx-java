package com.signalfx.metrics.connection;

import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.ssl.SSLContexts;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocket;

public class HttpClientConnectionManagerFactory {

  private HttpClientConnectionManagerFactory() {
    // prevent instantiation
  }

  public static HttpClientConnectionManager withTimeoutMs(int timeoutMs) {
    PoolingHttpClientConnectionManager httpClientConnectionManager = new PoolingHttpClientConnectionManager(
        RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", new SSLConnectionSocketFactoryWithTimeout(timeoutMs))
            .build());

    httpClientConnectionManager.setDefaultSocketConfig(
        SocketConfig.custom().setSoTimeout(timeoutMs, TimeUnit.MILLISECONDS).build());

    return httpClientConnectionManager;
  }

  /**
   * Uses STRICT_HOSTNAME_VERIFIER and sets a socket timeout before attempting the SSL handshake
   */
  private static class SSLConnectionSocketFactoryWithTimeout extends SSLConnectionSocketFactory {
    private final int timeoutMs;

    public SSLConnectionSocketFactoryWithTimeout(int timeoutMs) {
      super(SSLContexts.createDefault(), new DefaultHostnameVerifier());
      this.timeoutMs = timeoutMs;
    }

    @Override
    protected void prepareSocket(SSLSocket socket) throws IOException {
      socket.setSoTimeout(timeoutMs);
    }
  }
}
