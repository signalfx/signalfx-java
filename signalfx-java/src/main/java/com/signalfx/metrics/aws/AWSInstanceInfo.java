/**
* Copyright (C) 2015 SignalFx, Inc.
*/
package com.signalfx.metrics.aws;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AWSInstanceInfo {

    public static final String DIMENSION_NAME = "AWSUniqueId";
    private static final ObjectMapper MAPPER = new ObjectMapper();


    private static final String URL = "http://169.254.169.254/latest/dynamic/instance-identity/document";
    private static final Logger log = LoggerFactory.getLogger(AWSInstanceInfo.class);

    private static final String INSTANCE_ID = "instanceId";
    private static final String REGION = "region";
    private static final String ACCOUNT_ID = "accountId";

    public static String get(int timeoutInMs) {
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(timeoutInMs).build();
        HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig)
                .build();
        HttpGet request = new HttpGet(URL);

        try {
            HttpResponse response = client.execute(request);
            JsonNode object = MAPPER.readTree(response.getEntity().getContent());

            return object.get(INSTANCE_ID).asText() + "_" + object.get(REGION).asText() + "_"
                    + object.get(ACCOUNT_ID).asText();

        } catch (Exception e) {
            log.trace("Exception trying to execute {}, Exception: {} ", request, e);
        }

        return null;
    }

    public static String get() {
        return get(1000);
    }

}
