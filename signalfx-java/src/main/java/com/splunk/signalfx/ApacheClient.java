package com.splunk.signalfx;

import java.io.IOException;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class ApacheClient implements HttpClient {

    private final CloseableHttpClient client = HttpClients.createDefault();

    @Override
    public void write(String url, Map<String, String> headers, byte[] bytes, String type) throws IOException {
        HttpPost post = new HttpPost(url);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            post.setHeader(header.getKey(), header.getValue());
        }
        ByteArrayEntity entity = new ByteArrayEntity(bytes, ContentType.create(type));
        post.setEntity(entity);
        try (CloseableHttpResponse resp = client.execute(post)) {
            EntityUtils.consume(resp.getEntity());
        }
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
