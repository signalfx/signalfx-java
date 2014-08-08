/**
 * Copyright (C) 2014 SignalFuse, Inc.
 */
package com.signalfuse.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.signalfuse.metrics.auth.AuthToken;
import com.signalfuse.metrics.auth.ConfigAuthToken;
import com.signalfuse.metrics.auth.StaticAuthToken;
import com.signalfuse.metrics.connection.DataPointReceiverFactory;
import com.signalfuse.metrics.connection.HttpDataPointProtobufReceiverFactory;
import com.signalfuse.metrics.datumhandler.DatumHandlerFactory;
import com.signalfuse.metrics.datumhandler.DatumHandlerThreadQueue;
import com.signalfuse.metrics.endpoint.DataPointEndpoint;
import com.signalfuse.metrics.jmx.JmxAwareMetricFactory;
import com.signalfuse.metrics.metric.periodic.internal.PeriodicGaugeScheduler;
import com.signalfuse.metrics.metricbuilder.MetricFactory;
import com.signalfuse.metrics.metricbuilder.MetricFactoryWrapper;
import com.signalfuse.metrics.metricbuilder.errorhandler.ExitOnSendError;
import com.signalfuse.metrics.metricbuilder.errorhandler.OnSendErrorHandler;
import com.signalfuse.metrics.metricbuilder.internal.MetricFactoryImpl;

/**
 * Metric factory builder pattern.
 * 
 * <p>
 * The metric factory builder implements the abstract factory pattern to allow you to easily create
 * a new {@link MetricFactory} by specifying how it will be configured and how it will behave.
 * </p>
 * 
 * @author jack
 */
public class MetricFactoryBuilder {
    private final Set<OnSendErrorHandler> onSendErrorHandler = new HashSet<OnSendErrorHandler>();
    private DataPointReceiverFactory dataPointReceiverFactory =
                                                new HttpDataPointProtobufReceiverFactory();
    private DatumHandlerFactory datumHandlerFactory = new DatumHandlerThreadQueue.Factory();
    private AuthToken authToken = new ConfigAuthToken();
    private DataPointEndpoint dataPointEndpoint = new DataPointEndpoint();
    private String sourceName = null;
    private final List<MetricFactoryWrapper> wrappers = new ArrayList<MetricFactoryWrapper>();

    public MetricFactoryBuilder() {}

    public MetricFactoryBuilder(MetricFactoryBuilder factory) {
        this.datumHandlerFactory = factory.datumHandlerFactory;
        this.dataPointReceiverFactory = factory.dataPointReceiverFactory;
        this.authToken = factory.authToken;
        this.dataPointEndpoint = factory.dataPointEndpoint;
        this.sourceName = factory.sourceName;
        this.onSendErrorHandler.addAll(factory.onSendErrorHandler);
    }

    /**
     * Use the given auth token during requests
     * 
     * @param token    Auth token to use
     * @return this
     */
    public MetricFactoryBuilder usingToken(String token) {
        this.authToken = new StaticAuthToken(token);
        return this;
    }

    /**
     * Use the default algorithm for auth tokens. See ConfigAuthToken
     * 
     * @return this
     */
    public MetricFactoryBuilder usingDefaultToken() {
        this.authToken = new ConfigAuthToken();
        return this;
    }

    /**
     * Connect this source to a specific SignalFuse host. Useful if you connect to SignalFuse using
     * a tunnel (or if you're a SignalFuse employee testing the product)
     *
     * @param host Host to connect to (without port)
     * @return this
     */
    public MetricFactoryBuilder connectedTo(String host) {
        this.dataPointEndpoint = new DataPointEndpoint(host, this.dataPointEndpoint.getPort());
        return this;
    }

    /**
     * Connect this source to a specific SignalFuse host and port. Useful if you connect to
     * SignalFuse using a tunnel (or if you're a SignalFuse employee testing the product)
     *
     * @param host    Host to connect to
     * @param port    Port on the host
     * @return this
     */
    public MetricFactoryBuilder connectedTo(String host, int port) {
        this.dataPointEndpoint = new DataPointEndpoint(host, port);
        return this;
    }
    
    /**
     * Connect this source to a specific SignalFuse host and port. Useful if you connect to
     * SignalFuse using a tunnel (or if you're a SignalFuse employee testing the product)
     * @param scheme    http/https/etc
     * @param host      Host to connect to
     * @param port      Port on the host
     * @return this
     */
    public MetricFactoryBuilder connectedTo(String scheme, String host, int port) {
        this.dataPointEndpoint = new DataPointEndpoint(scheme, host, port);
        return this;
    }


    /**
     * Connect this source to a specific SignalFuse host and port. Useful if you connect to
     * SignalFuse using a tunnel (or if you're a SignalFuse employee testing the product)
     * @param dataPointEndpoint     Endpoint to connect to
     * @return this
     */
    public MetricFactoryBuilder connectedTo(DataPointEndpoint dataPointEndpoint) {
        this.dataPointEndpoint = dataPointEndpoint;
        return this;
    }

    /**
     * Specify a different way to create DataPointReceiver interfaces
     * 
     * @param dataPointReceiverFactory    Factory to create receives
     * @return this
     */
    public MetricFactoryBuilder usingDataPointReceiverFactory(DataPointReceiverFactory
                                                              dataPointReceiverFactory) {
        this.dataPointReceiverFactory = dataPointReceiverFactory;
        return this;
    }

    public MetricFactoryBuilder usingDatumHandlerFactory(DatumHandlerFactory datumHandlerFactory) {
        this.datumHandlerFactory = datumHandlerFactory;
        return this;
    }

    public MetricFactoryBuilder usingAuthTokenFinder(AuthToken authToken) {
        this.authToken = authToken;
        return this;
    }

    /**
     * Add a failure handler that does System.exit() on any connection failure
     * 
     * @param shouldFail    True if connection errors cause system exit
     * @return this
     */
    public MetricFactoryBuilder shouldFailOnConnectError(boolean shouldFail) {
        this.onSendErrorHandler.clear();
        if (shouldFail) {
            this.onSendErrorHandler.add(new ExitOnSendError());
        }
        return this;
    }

    /**
     * Specify a default source name for metrics generated by the factory. If not set explicitly by
     * this method, the default source name is obtained using the following logic:
     * 
     * <ol>
     * <li>If the system property {@code com.signalfuse.signalfuse.sourceName} is specified, it is used as the
     * source name</li>
     * <li>Otherwise, if the environment variable {@code SIGNALFUSE_SOURCE_NAME} is specified,
     * it is used</li>
     * <li>Otherwise, the output of {@code InetAddress.getLocalHost().getHostName()} is used</li>
     * </ol>
     * 
     * @param sourceName
     *            The default source name
     * 
     * @return this
     */
    public MetricFactoryBuilder usingDefaultSource(String sourceName) {
        this.sourceName = sourceName;
        return this;
    }

    /**
     * Wrap the metric factory with the specified wrapper. This can be used to add custom logic to
     * the metric creation process
     * 
     * @param wrapper
     *            The wrapper
     * @return this
     */
    public MetricFactoryBuilder wrappedWith(MetricFactoryWrapper wrapper) {
        this.wrappers.add(wrapper);
        return this;
    }

    /**
     * Make metrics available via JMX
     * 
     * @return this
     */
    public MetricFactoryBuilder registerJmx() {
        return this.wrappedWith(JmxAwareMetricFactory.wrapper());
    }

    /**
     * Add a handler to deal with send data point failures.
     * 
     * @param errorHandler an instance of OnSendErrorHandler
     * 
     * @return this builder
     */
    public MetricFactoryBuilder addSendErrorHandler(OnSendErrorHandler errorHandler) {
        if (errorHandler != null) {
            onSendErrorHandler.add(errorHandler);
        }
        return this;
    }

    /**
     * If a default source name is not set, we try to build one for you.
     * @return A string that we can use as a default source name in metric creation.
     */
    private String getUsableDefaultSourceName() {
        if (sourceName != null) {
            return sourceName;
        }
        return SourceNameHelper.getDefaultSourceName();
    }
    
    /**
     * Create your factory.
     * 
     * @return this
     */
    public MetricFactory build() {
        MetricFactory mf = new MetricFactoryImpl(getUsableDefaultSourceName(),
                datumHandlerFactory.createDatumHandler(dataPointReceiverFactory, dataPointEndpoint,
                        authToken, Collections.unmodifiableSet(onSendErrorHandler)),
                new PeriodicGaugeScheduler());
        for (MetricFactoryWrapper wrapper : wrappers) {
            mf = wrapper.wrap(mf);
        }
        return mf;
    }
}
