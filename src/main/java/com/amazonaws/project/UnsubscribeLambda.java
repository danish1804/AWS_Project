package com.amazonaws.project;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class UnsubscribeLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final String tableName = "subscription";

    private final DynamoDbClient ddb = DynamoDbClient.builder()
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create("http://host.docker.internal:4566"))
            .build();

    private Map<String, String> getCorsHeaders() {
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

        try {
            ObjectMapper mapper = new ObjectMapper();
            String rawBody = (String) input.get("body");
            System.out.println("Request Body: " + rawBody);

            Map<String, String> body = mapper.readValue(rawBody, Map.class);
            String email = body.get("email");
            String title = body.get("title");
            String album = body.get("album");

            if (email == null || title == null || album == null) {
                response.put("statusCode", 400);
                response.put("headers", getCorsHeaders());
                response.put("body", "{\"status\":\"fail\",\"message\":\"Missing fields\"}");
                return response;
            }

            String titleAlbumKey = title + "#" + album;

            DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of(
                            "email", AttributeValue.fromS(email),
                            "title_album", AttributeValue.fromS(titleAlbumKey)
                    ))
                    .build();

            ddb.deleteItem(deleteRequest);

            response.put("statusCode", 200);
            response.put("headers", getCorsHeaders());
            response.put("body", "{\"status\":\"success\",\"message\":\"Unsubscribed successfully\"}");

        } catch (Exception e) {
            e.printStackTrace();
            response.put("statusCode", 500);
            response.put("headers", getCorsHeaders());
            response.put("body", String.format("{\"status\":\"error\",\"message\":\"%s\"}", e.getMessage()));
        }

        return response;
    }
}
