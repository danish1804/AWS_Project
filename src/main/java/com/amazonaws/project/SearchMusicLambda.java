package com.amazonaws.project;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class SearchMusicLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDbClient ddb = DynamoDbClient.builder()
            .endpointOverride(URI.create("http://host.docker.internal:4566"))
            .region(Region.US_EAST_1)
            .build();

    private static final String MUSIC_TABLE = "music";

    private Map<String, String> corsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "OPTIONS,POST");
        headers.put("Access-Control-Allow-Headers", "*");
        headers.put("Content-Type", "application/json");
        return headers;
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        Map<String, Object> response = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            System.out.println("🎯 SearchMusicLambda triggered");

            String rawBody = (String) input.get("body");
            Map<String, String> body = mapper.readValue(rawBody, Map.class);

            // Optional filters
            String titleFilter = Optional.ofNullable(body.get("title")).orElse("").toLowerCase();
            String artistFilter = Optional.ofNullable(body.get("artist")).orElse("").toLowerCase();
            String albumFilter = Optional.ofNullable(body.get("album")).orElse("").toLowerCase();
            String yearFilter = Optional.ofNullable(body.get("year")).orElse("");

            // Scan all items
            ScanRequest scanRequest = ScanRequest.builder().tableName(MUSIC_TABLE).build();
            ScanResponse scanResponse = ddb.scan(scanRequest);

            List<Map<String, AttributeValue>> allItems = scanResponse.items();

            // Filter results manually (since we can't do partial match with Scan+FilterExpression easily)
            List<Map<String, String>> matched = allItems.stream()
                    .filter(item -> item.get("title").s().toLowerCase().contains(titleFilter))
                    .filter(item -> item.get("artist").s().toLowerCase().contains(artistFilter))
                    .filter(item -> item.get("album").s().toLowerCase().contains(albumFilter))
                    .filter(item -> yearFilter.isEmpty() || item.get("year").n().equals(yearFilter))
                    .map(item -> Map.of(
                            "title", item.get("title").s(),
                            "artist", item.get("artist").s(),
                            "album", item.get("album").s(),
                            "year", item.get("year").n()
                    ))
                    .collect(Collectors.toList());

            response.put("statusCode", 200);
            response.put("headers", corsHeaders());
            response.put("body", mapper.writeValueAsString(Map.of("status", "success", "results", matched)));

        } catch (Exception e) {
            e.printStackTrace();
            response.put("statusCode", 500);
            response.put("headers", corsHeaders());
            response.put("body", String.format("{\"status\":\"error\",\"message\":\"%s\"}", e.getMessage()));
        }

        return response;
    }
}
