package com.signalfx.connection;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RetryStrategyTest {
    @Test
    public void shouldSetRetryOnRequestTimeout() {
        final RetryStrategy retryStrategy = new RetryStrategy(3);

        final StatusLine mockStatusLine = generateStatusLineByCode(HttpStatus.SC_REQUEST_TIMEOUT);
        final HttpContext mockHttpContext = new HttpClientContext();
        final HttpResponse mockResp = DefaultHttpResponseFactory.INSTANCE.newHttpResponse(mockStatusLine, mockHttpContext);

        assertTrue(retryStrategy.retryRequest(mockResp, 1, mockHttpContext));
    }

    @Test
    public void shouldSetRetryOnGatewayTimeout() {
        final RetryStrategy retryStrategy = new RetryStrategy(3);

        final StatusLine mockStatusLine = generateStatusLineByCode(HttpStatus.SC_GATEWAY_TIMEOUT);
        final HttpContext mockHttpContext = new HttpClientContext();
        final HttpResponse mockResp = DefaultHttpResponseFactory.INSTANCE.newHttpResponse(mockStatusLine, mockHttpContext);

        assertTrue(retryStrategy.retryRequest(mockResp, 1, mockHttpContext));
    }

    @Test
    public void shouldSetRetryOnNegativeStatus() {
        final RetryStrategy retryStrategy = new RetryStrategy(3);

        final StatusLine mockStatusLine = generateStatusLineByCode(-1);
        final HttpContext mockHttpContext = new HttpClientContext();
        final HttpResponse mockResp = DefaultHttpResponseFactory.INSTANCE.newHttpResponse(mockStatusLine, mockHttpContext);

        assertTrue(retryStrategy.retryRequest(mockResp, 1, mockHttpContext));
    }

    @Test
    public void shouldSetRetryOnInvalidStatusCode() {
        final RetryStrategy retryStrategy = new RetryStrategy(3);

        final StatusLine mockStatusLine = generateStatusLineByCode(598);
        final HttpContext mockHttpContext = new HttpClientContext();
        final HttpResponse mockResp = DefaultHttpResponseFactory.INSTANCE.newHttpResponse(mockStatusLine, mockHttpContext);

        assertTrue(retryStrategy.retryRequest(mockResp, 1, mockHttpContext));
    }

    @Test
    public void shouldNotRetryOnOtherStatusCode() {
        final RetryStrategy retryStrategy = new RetryStrategy(3);

        final StatusLine mockStatusLine = generateStatusLineByCode(HttpStatus.SC_BAD_GATEWAY);
        final HttpContext mockHttpContext = new HttpClientContext();
        final HttpResponse mockResp = DefaultHttpResponseFactory.INSTANCE.newHttpResponse(mockStatusLine, mockHttpContext);

        assertFalse(retryStrategy.retryRequest(mockResp, 1, mockHttpContext));
    }

    @Test
    public void shouldNotRetryIfRetriesExceeded() {
        final RetryStrategy retryStrategy = new RetryStrategy(3);

        final StatusLine mockStatusLine = generateStatusLineByCode(HttpStatus.SC_GATEWAY_TIMEOUT);
        final HttpContext mockHttpContext = new HttpClientContext();
        final HttpResponse mockResp = DefaultHttpResponseFactory.INSTANCE.newHttpResponse(mockStatusLine, mockHttpContext);

        assertFalse(retryStrategy.retryRequest(mockResp, 4, mockHttpContext));
    }

    private StatusLine generateStatusLineByCode(final int statusCode) {
        return new StatusLine() {
            @Override
            public ProtocolVersion getProtocolVersion() {
                return null;
            }

            @Override
            public int getStatusCode() {
                return statusCode;
            }

            @Override
            public String getReasonPhrase() {
                return null;
            }
        };
    }
}