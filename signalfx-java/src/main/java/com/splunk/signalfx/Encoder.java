package com.splunk.signalfx;

interface Encoder {

    String getType();

    byte[] encode(Iterable<Point> points);

}
