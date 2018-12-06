package com.signalfx.metrics.connection;

import java.io.IOException;

import javax.net.ssl.SSLSocket;

import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;

public class HttpClientConnectionManagerFactory {

  private HttpClientConnectionManagerFactory() {
    // prevent instantiation
  }

  public static HttpClientConnectionManager withTimeoutMs(int timeoutMs) {
    BasicHttpClientConnectionManager httpClientConnectionManager = new BasicHttpClientConnectionManager(
        RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", new SSLConnectionSocketFactoryWithTimeout(timeoutMs))
            .build());

    httpClientConnectionManager.setSocketConfig(
        SocketConfig.custom().setSoTimeout(timeoutMs).build());

    return httpClientConnectionManager;
  }

  /**
   * Uses STRICT_HOSTNAME_VERIFIER and sets a socket timeout before attempting the SSL handshake
   */
  private static class SSLConnectionSocketFactoryWithTimeout extends SSLConnectionSocketFactory {
    private final int timeoutMs;

    public SSLConnectionSocketFactoryWithTimeout(int timeoutMs) {
      super(SSLContexts.createDefault(), SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER);
      this.timeoutMs = timeoutMs;
    }

    @Override
    protected void prepareSocket(SSLSocket socket) throws IOException {
      socket.setSoTimeout(timeoutMs);
    }
  }
}
