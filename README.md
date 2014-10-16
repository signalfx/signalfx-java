SignalFuse client libraries
===========================

This repository contains libraries for instrumenting applications in a
variety of languages and reporting these metrics to SignalFuse. You will
need a SignalFuse account and organization API token to use those. For
more information on SignalFuse and to create an account, go to
http://www.signalfuse.com.

The recommended way to send metrics with java is to use codahale metrics.
If you want to send metrics without integrating with codahale, the module
signalfuse-java will support this.

Supported languages
-------------------

* Java 6+ with `signalfuse-metrics`.

Install from source (Not recommended)
-------------------------------------

    git clone https://github.com/signalfx/signalfuse-java.git
    cd signalfuse-java
    mvn install


Setup in maven
--------------

    <dependency>
        <groupId>com.signalfuse.public</groupId>
        <artifactId>signalfuse-codahale</artifactId>
        <version>0.0.9</version>
    </dependency>


Setting up Codahale
------------------

    MetricRegistry registry = new MetricRegistry();
    new SignalFuseReporter.Builder(
        registry,
        "SIGNALFUSE_AUTH_TOKEN"
    ).build().start(1, TimeUnit.SECONDS);

After setting up Codahale
-------------------------

After setting up a SignalFuseReporter, you can use codahale metrics like normal
and at the set frequency your metrics will be reported to SignalFuse.  More
information about codahale metrics [on their web page](http://metrics.codahale.com/).

Build Status
------------

[![Build Status](https://travis-ci.org/signalfx/signalfuse-java.svg?branch=master)](https://travis-ci.org/signalfx/signalfuse-java)
