package com.splunk.signalfx;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

interface HttpClient extends Closeable {

    void write(String url, Map<String, String> headers, byte[] bytes, String type) throws IOException;

}
