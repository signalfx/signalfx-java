package com.splunk.signalfx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Encodes datapoints to a JSON format appropriate for SignalFx ingest.
 */
public class JsonEncoder implements Encoder {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getType() {
        return "application/json";
    }

    @Override
    public byte[] encode(Iterable<Point> points) {
        Map<String, List<Point>> types = new HashMap<>();
        for (Point point : points) {
            String type = point.getType().toString().toLowerCase();
            List<Point> pointsOfType = types.computeIfAbsent(type, ignored -> new ArrayList<>());
            pointsOfType.add(point);
        }
        String json = toJson(types);
        return json.getBytes();
    }

    private String toJson(Map<String, List<Point>> list) {
        try {
            return mapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
