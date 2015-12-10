/**
* Copyright (C) 2015 SignalFx, Inc.
*/
package com.signalfx.metrics.aws;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AWSInstanceInfo {

    public static final String AWS_UNIQUE_ID_DIMENSION_NAME = "AWSUniqueId";

    private static final String URL = "http://169.254.169.254/latest/dynamic/instance-identity/document";
    private static final Logger log = LoggerFactory.getLogger(AWSInstanceInfo.class);
    
    
    private static final String INSTANCE_ID = "instanceId";
    private static final String REGION = "region";
    private static final String ACCOUNT_ID = "accountId";

    private String instanceId;
    private String region;
    private String accountId;

    private AWSInstanceInfo(String instanceId, String region, String accountId) {
        this.instanceId = instanceId;
        this.region = region;
        this.accountId = accountId;
    }

    public String getId() {
        return instanceId + "_" + region + "_" + accountId;
    }

    public static AWSInstanceInfo obtain() {

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(1000).build();
        HttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig).build();
        HttpGet request = new HttpGet(URL);

        try {
            HttpResponse response = client.execute(request);
            String json = EntityUtils.toString(response.getEntity());
            JSONObject object = new JSONObject(json);

            return new AWSInstanceInfo(object.getString(INSTANCE_ID),
                    object.getString(REGION), object.getString(ACCOUNT_ID));

        } catch (Exception e) {
            log.trace("Exception trying to execute {}, Exception: {} ", request, e);
        }

        return null;

    }

}