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

public class RegisterUserLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final String tableName = "login";
    private final DynamoDbClient ddb = DynamoDbClient.builder()
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create("http://host.docker.internal:4566"))  // For LocalStack
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
            System.out.println("Lambda triggered - Register User");

            // Step 0: Handle CORS preflight request
            String httpMethod = (String) input.get("httpMethod");
            if ("OPTIONS".equalsIgnoreCase(httpMethod)) {
                response.put("statusCode", 200);
                response.put("headers", getCorsHeaders());
                response.put("body", "{\"status\":\"ok\"}");
                return response;
            }

            // Step 1: Extract and parse JSON body
            String rawBody = (String) input.get("body");
            System.out.println("Request Body: " + rawBody);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> body = mapper.readValue(rawBody, Map.class);

            // Step 2: Extract fields and support both `username` and `user_name`
            String email = body.get("email");
            System.out.println("Parsed email: " + email);
            String password = body.get("password");
            System.out.println("Parsed password: " + password);
            String userName = body.getOrDefault("user_name", body.get("username"));
            System.out.println("Parsed username: " + userName);
            // Step 3: Basic validation
            if (email == null || password == null || userName == null) {
                response.put("statusCode", 400);
                response.put("headers", getCorsHeaders());
                response.put("body", "{\"status\":\"fail\",\"message\":\"Missing required fields\"}");
                return response;
            }

            // Step 4: Check if user already exists
            GetItemRequest checkRequest = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("email", AttributeValue.fromS(email)))
                    .build();

            GetItemResponse checkResponse = ddb.getItem(checkRequest);

            if (checkResponse.hasItem()) {
                response.put("statusCode", 400);
                response.put("headers", getCorsHeaders());
                response.put("body", "{\"status\":\"fail\",\"message\":\"The email already exists\"}");
                return response;
            }

            // Step 5: Insert new user into DynamoDB
            PutItemRequest putRequest = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(Map.of(
                            "email", AttributeValue.fromS(email),
                            "password", AttributeValue.fromS(password),
                            "user_name", AttributeValue.fromS(userName)
                    ))
                    .build();

            ddb.putItem(putRequest);

            // Step 6: Return success response
            response.put("statusCode", 200);
            response.put("headers", getCorsHeaders());
            response.put("body", "{\"status\":\"success\",\"message\":\"User registered successfully\"}");

        } catch (Exception e) {
            e.printStackTrace();
            response.put("statusCode", 500);
            response.put("headers", getCorsHeaders());
            response.put("body", "{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }

        return response;
    }
}
