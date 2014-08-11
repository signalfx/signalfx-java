package com.signalfuse.metrics.endpoint;

/**
 * Date: 5/6/14
 * Time: 4:21 PM
 *
 * @author jack
 */
public interface DataPointReceiverEndpoint {
    String getScheme();
    String getHostname();
    int getPort();
}
