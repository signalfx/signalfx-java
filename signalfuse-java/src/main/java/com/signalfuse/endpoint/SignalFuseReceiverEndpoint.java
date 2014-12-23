package com.signalfuse.endpoint;

/**
 * Date: 5/6/14
 * Time: 4:21 PM
 *
 * @author jack
 */
public interface SignalFuseReceiverEndpoint {
    String getScheme();
    String getHostname();
    int getPort();
}
