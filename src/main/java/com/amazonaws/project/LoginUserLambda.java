package com.amazonaws.project;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class LoginUserLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final String tableName = "login";
    private final DynamoDbClient ddb = DynamoDbClient.builder()
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create("http://host.docker.internal:4566")) // LocalStack access
            .build();

    // Reusable CORS header map
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
            System.out.println("✅ Lambda triggered - Login User");

            // Step 1: Extract raw JSON body
            String rawBody = (String) input.get("body");
            System.out.println("📦 Request Body: " + rawBody);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> body = mapper.readValue(rawBody, Map.class);

            String email = body.get("email");
            String password = body.get("password");

            System.out.println("📨 Email: " + email);
            System.out.println("🔐 Password: " + password);

            // Step 2: Validate inputs
            if (email == null || password == null) {
                response.put("statusCode", 400);
                response.put("headers", getCorsHeaders());
                response.put("body", "{\"status\":\"fail\",\"message\":\"Missing email or password\"}");
                return response;
            }

            // Step 3: Fetch user by email
            GetItemRequest getRequest = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("email", AttributeValue.fromS(email)))
                    .build();

            GetItemResponse getResponse = ddb.getItem(getRequest);

            if (!getResponse.hasItem()) {
                response.put("statusCode", 401);
                response.put("headers", getCorsHeaders());
                response.put("body", "{\"status\":\"fail\",\"message\":\"User not found\"}");
                return response;
            }

            String storedPassword = getResponse.item().get("password").s();

            if (!password.equals(storedPassword)) {
                response.put("statusCode", 403);
                response.put("headers", getCorsHeaders());
                response.put("body", "{\"status\":\"fail\",\"message\":\"Incorrect password\"}");
                return response;
            }

            String userName = getResponse.item().get("user_name").s();

            // Step 4: Login success
            Map<String, String> successBody = new HashMap<>();
            successBody.put("status", "success");
            successBody.put("message", "Login successful");
            successBody.put("user_name", userName);

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
