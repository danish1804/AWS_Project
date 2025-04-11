package com.amazonaws.project.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.project.service.LoginService;
import com.amazonaws.project.service.RegisterService;
import com.amazonaws.project.service.SubscriptionService;
import com.amazonaws.project.service.MusicService;


import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class AppLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final ObjectMapper mapper = new ObjectMapper();

    // Set up your DynamoDB client (LocalStack override if needed)
    private final DynamoDbClient ddb = DynamoDbClient.builder()
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create("http://host.docker.internal:4566")) // for LocalStack
            .build();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        Map<String, Object> response = new HashMap<>();

        try {
            String path = (String) input.get("resource"); // API Gateway resource path
            String method = (String) input.get("httpMethod");

            System.out.println("📥 Request to: " + method + " " + path);

            // CORS preflight handling
            if ("OPTIONS".equalsIgnoreCase(method)) {
                return buildCorsResponse();
            }

            // Parse the body
            String rawBody = (String) input.get("body");
            Map<String, String> body = mapper.readValue(rawBody, Map.class);

            // Router logic
            if ("/login".equals(path) && "POST".equalsIgnoreCase(method)) {
                return LoginService.doLogin(body, ddb);
            } else if ("/register".equals(path) && "POST".equalsIgnoreCase(method)) {
                return RegisterService.doRegister(body, ddb);
            } else if ("/subscriptions".equals(path) && "POST".equalsIgnoreCase(method)) {
                return SubscriptionService.getSubscriptions(body, ddb);
            } else if ("/subscribe".equals(path) && "POST".equalsIgnoreCase(method)) {
                return SubscriptionService.subscribeSong(body, ddb);
            } else if ("/unsubscribe".equals(path) && "POST".equalsIgnoreCase(method)) {
                return SubscriptionService.unsubscribeSong(body, ddb);
            } else if ("/search".equals(path) && "POST".equalsIgnoreCase(method)) {
                return MusicService.searchMusic(body, ddb);
            }




            // Fallback 404
            response.put("statusCode", 404);
            response.put("headers", getCorsHeaders());
            response.put("body", "{\"status\":\"fail\", \"message\":\"Unknown path or method\"}");
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            response.put("statusCode", 500);
            response.put("headers", getCorsHeaders());
            response.put("body", "{\"status\":\"error\", \"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
            return response;
        }
    }

    private Map<String, Object> buildCorsResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 200);
        response.put("headers", getCorsHeaders());
        response.put("body", "{\"status\":\"ok\"}");
        return response;
    }

    private Map<String, String> getCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "OPTIONS,POST");
        headers.put("Access-Control-Allow-Headers", "*");
        headers.put("Content-Type", "application/json");
        return headers;
    }
}
