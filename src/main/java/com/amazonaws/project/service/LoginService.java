package com.amazonaws.project.service;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class LoginService {

    private static final String tableName = "login";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Object> doLogin(Map<String, String> body, DynamoDbClient ddb) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("✅ [Login] LoginService.doLogin() called");

            String email = body.get("email");
            String password = body.get("password");

            if (email == null || password == null) {
                return buildResponse(400, "fail", "Missing email or password");
            }

            // Fetch user from DynamoDB
            GetItemRequest getRequest = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("email", AttributeValue.fromS(email)))
                    .build();

            GetItemResponse getResponse = ddb.getItem(getRequest);

            if (!getResponse.hasItem()) {
                return buildResponse(401, "fail", "User not found");
            }

            String storedPassword = getResponse.item().get("password").s();

            if (!password.equals(storedPassword)) {
                return buildResponse(403, "fail", "Incorrect password");
            }

            String userName = getResponse.item().get("user_name").s();

            Map<String, String> successBody = new HashMap<>();
            successBody.put("status", "success");
            successBody.put("message", "Login successful");
            successBody.put("user_name", userName);

            response.put("statusCode", 200);
            response.put("headers", getCorsHeaders());
            response.put("body", mapper.writeValueAsString(successBody));
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
