package com.signalfx.jvm.agent;

import com.signalfx.endpoint.SignalFxEndpoint;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.SourceNameHelper;
import com.signalfx.metrics.auth.StaticAuthToken;
import com.signalfx.metrics.connection.HttpDataPointProtobufReceiverFactory;
import com.signalfx.metrics.errorhandler.OnSendErrorHandler;
import com.signalfx.metrics.flush.AggregateMetricSender;
import org.jmxtrans.agent.AbstractOutputWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

/**
 *
 */
public class SignalFxOutputWriter extends AbstractOutputWriter {
    private String hostUrl;
    private String authToken;
    private AggregateMetricSender aggregateMetricSender;
    private AggregateMetricSender.Session currentSession;

    @Override public void postConstruct(@Nonnull Map<String, String> settings) {
        super.postConstruct(settings);
        hostUrl = getOrFromSysProperty(settings, "hostUrl", "com.signalfx.hostUrl",
                "https://ingest.signalfx.com:443");
        authToken = getOrFromSysProperty(settings, "authToken", "com.signalfx.authToken", null);
        try {
            final URL url = new URL(hostUrl);
            SignalFxReceiverEndpoint endpoint =
                    new SignalFxEndpoint(url.getProtocol(),
                            url.getHost(),
                            url.getPort());
            aggregateMetricSender =
                    new AggregateMetricSender(SourceNameHelper.getDefaultSourceName(),
                            new HttpDataPointProtobufReceiverFactory(
                                    endpoint)
                                    .setVersion(2),
                            new StaticAuthToken(authToken),
                            Collections.<OnSendErrorHandler>emptyList());

        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        logger.info(String.format("Using host %s with authToken %s", hostUrl, authToken));
    }

    private String getOrFromSysProperty(@Nonnull Map<String, String> settings, String key,
                                      String sysPropertyName, String fallback) {
        if (settings.containsKey(key)) {
            return settings.get(key);
        } else {
            String urlSystemProperty = System.getProperty(sysPropertyName);
            if (urlSystemProperty != null) {
                return urlSystemProperty;
            } else {
                if (fallback != null) {
                    return fallback;
                } else {
                    throw new IllegalArgumentException("Couldnot resolve " + key);
                }
            }
        }
    }

    @Override public void preCollect() throws IOException {
        super.preCollect();
        currentSession = aggregateMetricSender.createSession();
    }

    @Override public void writeInvocationResult(@Nonnull String invocationName,
                                                @Nullable Object value) throws IOException {
        logger.warning("Invocations are not supported in the SignalFx plugin");
    }

    @Override public void writeQueryResult(@Nonnull String metricName, @Nullable String metricType,
                                           @Nullable Object value) throws IOException {
        if (value == null) {
            return;
        }
        if (!(value instanceof Number)) {
            logger.warning(String.format("Metric's %s value is not numeric but of type %s, skipping", metricName,
                value.getClass().getSimpleName()));
            return;
        }
        boolean isGauge = metricType == null || metricType.isEmpty() || metricType.toLowerCase().equals("gauge");
        if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
            if (isGauge) {
                currentSession.setGauge(metricName, ((Number) value).longValue());
            } else {
                currentSession.setCumulativeCounter(metricName, ((Number) value).longValue());
            }
        } else {
            if (isGauge) {
                currentSession.setGauge(metricName, ((Number) value).doubleValue());
            } else {
                currentSession.setCumulativeCounter(metricName, ((Number) value).longValue());
            }
        }
    }

    @Override public void postCollect() throws IOException {
        currentSession.close();
        super.postCollect();
    }

    @Override public void preDestroy() {
        super.preDestroy();
    }
}
