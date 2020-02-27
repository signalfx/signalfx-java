package com.splunk.signalfx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

class FakeClient implements HttpClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<Map<String, List<Map<String, Object>>>> results = new ArrayList<>();

    @Override
    public void write(String url, Map<String, String> headers, byte[] bytes, String type) {
        Map<String, List<Map<String, Object>>> map = fromJson(bytes);
        results.add(map);
    }

    private Map<String, List<Map<String, Object>>> fromJson(byte[] bytes) {
        Map<String, List<Map<String, Object>>> map;
        try {
            map = mapper.readValue(bytes, new TypeReference<Map<String, List<Map<String, Object>>>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    public List<Map<String, List<Map<String, Object>>>> getResults() {
        return results;
    }

    int getNumPts() {
        List<Map<String, List<Map<String, Object>>>> results = getResults();
        int numPts = 0;
        for (Map<String, List<Map<String, Object>>> map : results) {
            for (List<Map<String, Object>> points : map.values()) {
                numPts += points.size();
            }
        }
        return numPts;
    }

    @Override
    public void close() {
    }
}
