package com.splunk.signalfx;

class Dimension {

    final String key, value;

    Dimension(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
