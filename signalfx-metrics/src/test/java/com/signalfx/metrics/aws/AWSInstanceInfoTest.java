package com.signalfx.metrics.aws;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;

public class AWSInstanceInfoTest {
    private static final String document = "{\n" +
            "    \"devpayProductCodes\" : null,\n" +
            "    \"marketplaceProductCodes\" : [ \"1abc2defghijklm3nopqrs4tu\" ], \n" +
            "    \"availabilityZone\" : \"us-west-2b\",\n" +
            "    \"privateIp\" : \"10.158.112.84\",\n" +
            "    \"version\" : \"2017-09-30\",\n" +
            "    \"instanceId\" : \"i-1234567890abcdef0\",\n" +
            "    \"billingProducts\" : null,\n" +
            "    \"instanceType\" : \"t2.micro\",\n" +
            "    \"accountId\" : \"123456789012\",\n" +
            "    \"imageId\" : \"ami-5fb8c835\",\n" +
            "    \"pendingTime\" : \"2016-11-19T16:32:11Z\",\n" +
            "    \"architecture\" : \"x86_64\",\n" +
            "    \"kernelId\" : null,\n" +
            "    \"ramdiskId\" : null,\n" +
            "    \"region\" : \"us-west-2\"\n" +
            "}";

    @Test
    public void testParse() throws Exception {
        try (InputStream inputStream = new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8))) {
            String result = AWSInstanceInfo.parse(inputStream);
            Assert.assertEquals("i-1234567890abcdef0_us-west-2_123456789012", result);
        }
    }
}
