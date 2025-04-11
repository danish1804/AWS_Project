package com.amazonaws.project.service;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class RegisterService {

    private static final String tableName = "login";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Object> doRegister(Map<String, String> body, DynamoDbClient ddb) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("✅ [Register] RegisterService.doRegister() called");

            String email = body.get("email");
            String password = body.get("password");
            String userName = body.getOrDefault("user_name", body.get("username")); // Accept either

            if (email == null || password == null || userName == null) {
                return buildResponse(400, "fail", "Missing required fields");
            }

            // Check if user already exists
            GetItemRequest checkRequest = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("email", AttributeValue.fromS(email)))
                    .build();

            GetItemResponse checkResponse = ddb.getItem(checkRequest);

            if (checkResponse.hasItem()) {
                return buildResponse(400, "fail", "The email already exists");
            }

            // Insert new user
            PutItemRequest putRequest = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(Map.of(
                            "email", AttributeValue.fromS(email),
                            "password", AttributeValue.fromS(password),
                            "user_name", AttributeValue.fromS(userName)
                    ))
                    .build();

            ddb.putItem(putRequest);

            response.put("statusCode", 200);
            response.put("headers", getCorsHeaders());
            response.put("body", "{\"status\":\"success\",\"message\":\"User registered successfully\"}");
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
