package com.amazonaws.project.service;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

public class MusicService {

    private static final String MUSIC_TABLE = "music";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Object> searchMusic(Map<String, String> body, DynamoDbClient ddb) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("🎯 [Music] searchMusic() called");

            // Optional filters
            String titleFilter = Optional.ofNullable(body.get("title")).orElse("").toLowerCase();
            String artistFilter = Optional.ofNullable(body.get("artist")).orElse("").toLowerCase();
            String albumFilter = Optional.ofNullable(body.get("album")).orElse("").toLowerCase();
            String yearFilter = Optional.ofNullable(body.get("year")).orElse("");

            // Full table scan
            ScanRequest scanRequest = ScanRequest.builder().tableName(MUSIC_TABLE).build();
            ScanResponse scanResponse = ddb.scan(scanRequest);
            List<Map<String, AttributeValue>> allItems = scanResponse.items();
            System.out.println("🔍 Total songs in music table: " + allItems.size());
            allItems.forEach(item -> {
                System.out.println("🎵 Raw song: " + item.get("title").s() + ", fields: " + item.keySet());
            });


            // Filter and include image_url
            List<Map<String, String>> matched = allItems.stream()
                    .filter(item -> item.get("title").s().toLowerCase().contains(titleFilter))
                    .filter(item -> item.get("artist").s().toLowerCase().contains(artistFilter))
                    .filter(item -> item.get("album").s().toLowerCase().contains(albumFilter))
                    .filter(item -> yearFilter.isEmpty() || item.get("year").n().equals(yearFilter))
                    .map(item -> {
                        Map<String, String> song = new HashMap<>();
                        song.put("title", item.get("title").s());
                        song.put("artist", item.get("artist").s());
                        song.put("album", item.get("album").s());
                        song.put("year", item.get("year").n());

                        // ⬇️ Debug log here
                        System.out.println("🎯 Mapping song: " + item.get("title").s());
                        if (item.containsKey("image_s3_url")) {
                            song.put("image_s3_url", item.get("image_s3_url").s());
                            System.out.println("🖼 Found image URL: " + item.get("image_s3_url").s());
                        } else {
                            System.out.println("❌ image_s3_url not found for: " + item.get("title").s());
                        }

                        return song;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> resultBody = new HashMap<>();
            resultBody.put("status", "success");
            resultBody.put("results", matched);
            matched.forEach(song -> {
                System.out.println("🖼 Song: " + song.get("title") + ", image_s3_url: " + song.get("image_s3_url"));
            });


            response.put("statusCode", 200);
            response.put("headers", getCorsHeaders());
            response.put("body", mapper.writeValueAsString(resultBody));
            System.out.println("Matched songs:");
            matched.forEach(song -> System.out.println(song));
            System.out.println("Final response JSON:");
            System.out.println(mapper.writeValueAsString(resultBody));


            System.out.println("Returning search results: " + mapper.writeValueAsString(matched));

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(500, "error", e.getMessage());
        }
    }

    private static Map<String, Object> buildResponse(int statusCode, String status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);
        response.put("headers", getCorsHeaders());
        response.put("body", "{\"status\":\"" + status + "\",\"message\":\"" + message.replace("\"", "'") + "\"}");
        return response;
    }

    private static Map<String, String> getCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "OPTIONS,POST");
        headers.put("Access-Control-Allow-Headers", "*");
        headers.put("Content-Type", "application/json");
        return headers;
    }
}
