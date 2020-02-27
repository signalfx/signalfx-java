package com.splunk.signalfx;

public enum Realm {

    US0("https://ingest.us0.signalfx.com"),
    US1("https://ingest.us1.signalfx.com"),
    US2("https://ingest.us2.signalfx.com"),
    EU0("https://ingest.eu0.signalfx.com"),
    AP0("https://ingest.ap0.signalfx.com");

    private static final String INGEST_PATH = "/v2/datapoint";
    private final String baseUrl;

    Realm(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getIngestUrl() {
        return baseUrl + INGEST_PATH;
    }
}
