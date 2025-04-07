package com.amazonaws.project;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;
import java.util.*;

public class
GetSubscriptionsLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final String tableName = "subscription";
    private final DynamoDbClient ddb = DynamoDbClient.builder()
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create("http://host.docker.internal:4566")) // LocalStack internal access
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
        ObjectMapper mapper = new ObjectMapper();

        try {
            System.out.println("✅ GET SUBSCRIPTION Lambda triggered - Get Subscriptions");

            // Step 1: Extract request body
            String rawBody = (String) input.get("body");
            Map<String, String> body = mapper.readValue(rawBody, Map.class);
            String email = body.get("email");

            System.out.println("📨 Email: " + email);

            if (email == null) {
                response.put("statusCode", 400);
                response.put("headers", getCorsHeaders());
                response.put("body", "{\"status\":\"fail\",\"message\":\"Missing email in request\"}");
                return response;
            }

            // Step 2: Query all items for this email
            QueryRequest query = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("email = :email")
                    .expressionAttributeValues(Map.of(":email", AttributeValue.fromS(email)))
                    .consistentRead(true)
                    .build();

            QueryResponse queryResponse = ddb.query(query);
            List<Map<String, AttributeValue>> items = queryResponse.items();

            // Step 3: Build response list
            List<Map<String, Object>> subscriptions = new ArrayList<>();
            for (Map<String, AttributeValue> item : items) {
                Map<String, Object> song = new HashMap<>();
                song.put("title", item.getOrDefault("title", AttributeValue.fromS("")).s());
                song.put("artist", item.getOrDefault("artist", AttributeValue.fromS("")).s());
                song.put("album", item.getOrDefault("album", AttributeValue.fromS("")).s());
                song.put("year", item.getOrDefault("year", AttributeValue.fromS("")).n());
                subscriptions.add(song);
            }

            Map<String, Object> successBody = new HashMap<>();
            successBody.put("status", "success");
            successBody.put("subscriptions", subscriptions);

            response.put("statusCode", 200);
            response.put("headers", getCorsHeaders());
            response.put("body", mapper.writeValueAsString(successBody));

        } catch (Exception e) {
            e.printStackTrace();
            response.put("statusCode", 500);
            response.put("headers", getCorsHeaders());
            response.put("body", "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
        }

        return response;
    }
}
