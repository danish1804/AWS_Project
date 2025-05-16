//package com.amazonaws.project.handler;
//
//import com.amazonaws.services.lambda.runtime.Context;
//import com.amazonaws.services.lambda.runtime.RequestHandler;
//import com.amazonaws.project.service.LoginService;
//import com.amazonaws.project.service.RegisterService;
//import com.amazonaws.project.service.SubscriptionService;
//import com.amazonaws.project.service.MusicService;
//
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
//
//import java.net.URI;
//import java.util.HashMap;
//import java.util.Map;
//
//public class AppLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
//
//    private final ObjectMapper mapper = new ObjectMapper();
//
//    // Set up your DynamoDB client (LocalStack override if needed)
//    private final DynamoDbClient ddb = DynamoDbClient.builder()
//            .region(Region.US_EAST_1)
//            .build();
//
//    @Override
//    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
//        Map<String, Object> response = new HashMap<>();
//
//        try {
//            String path = (String) input.get("resource"); // API Gateway resource path
//            String method = (String) input.get("httpMethod");
//
//            System.out.println("📥 Request to: " + method + " " + path);
//
//            // CORS preflight handling
//            if ("OPTIONS".equalsIgnoreCase(method)) {
//                return buildCorsResponse();
//            }
//
//            // Parse the body
//            String rawBody = (String) input.get("body");
//            System.out.println("📨 Raw Body: " + rawBody);
//            Map<String, String> body = mapper.readValue(rawBody, Map.class);
//
//            // Router logic
//            if ("/login".equals(path) && "POST".equalsIgnoreCase(method)) {
//                return LoginService.doLogin(body, ddb);
//            } else if ("/register".equals(path) && "POST".equalsIgnoreCase(method)) {
//                return RegisterService.doRegister(body, ddb);
//            } else if ("/getSubscriptions".equals(path) && "POST".equalsIgnoreCase(method)) {
//                return SubscriptionService.getSubscriptions(body, ddb);
//            } else if ("/subscribe".equals(path) && "POST".equalsIgnoreCase(method)) {
//                return SubscriptionService.subscribeSong(body, ddb);
//            } else if ("/unsubscribe".equals(path) && "POST".equalsIgnoreCase(method)) {
//                return SubscriptionService.unsubscribeSong(body, ddb);
//            } else if ("/searchMusic".equals(path) && "POST".equalsIgnoreCase(method)) {
//                return MusicService.searchMusic(body, ddb);
//            }
//
//
//
//
//            // Fallback 404
//            response.put("statusCode", 404);
//            response.put("headers", getCorsHeaders());
//            response.put("body", "{\"status\":\"fail\", \"message\":\"Unknown path or method\"}");
//            return response;
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            response.put("statusCode", 500);
//            response.put("headers", getCorsHeaders());
//            response.put("body", "{\"status\":\"error\", \"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
//            return response;
//        }
//    }
//
//    private Map<String, Object> wrapResponse(Map<String, Object> result) {
//        Map<String, Object> response = new HashMap<>();
//        response.put("statusCode", 200);
//        response.put("headers", withJsonContentType(getCorsHeaders()));
//        try {
//            ObjectMapper mapper = new ObjectMapper();
//            String jsonBody = mapper.writeValueAsString(result);
//            response.put("body", jsonBody);
//        } catch (Exception e) {
//            response.put("body", "{\"status\":\"error\", \"message\":\"Failed to serialize body\"}");
//        }
//        return response;
//    }
//    private Map<String, String> withJsonContentType(Map<String, String> headers) {
//        headers.put("Content-Type", "application/json");
//        return headers;
//    }
//
//    private Map<String, Object> buildCorsResponse() {
//        Map<String, Object> response = new HashMap<>();
//        response.put("statusCode", 200);
//        response.put("headers", getCorsHeaders());
//        response.put("body", "{\"status\":\"ok\"}");
//        return response;
//    }
//
//    private Map<String, String> getCorsHeaders() {
//        Map<String, String> headers = new HashMap<>();
//        headers.put("Access-Control-Allow-Origin", "*");
//        headers.put("Access-Control-Allow-Methods", "OPTIONS,POST");
//        headers.put("Access-Control-Allow-Headers", "*");
//        headers.put("Content-Type", "application/json");
//        return headers;
//    }
//}

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

import java.util.HashMap;
import java.util.Map;

public class AppLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final ObjectMapper mapper = new ObjectMapper();
    private final DynamoDbClient ddb = DynamoDbClient.builder()
            .region(Region.US_EAST_1)
            .build();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            String path = (String) input.get("rawPath");
            Map<String, Object> requestContext = (Map<String, Object>) input.get("requestContext");
            Map<String, String> http = (Map<String, String>) requestContext.get("http");
            String method = http.get("method");

            System.out.println("📥 Request: " + method + " " + path);

            if ("OPTIONS".equalsIgnoreCase(method)) {
                return buildCorsResponse();
            }

            Map<String, Object> result;
            Map<String, String> requestData = new HashMap<>();

            if ("POST".equalsIgnoreCase(method)) {
                // Parse request body for POST
                String rawBody = (String) input.get("body");
                if (rawBody != null) {
                    requestData = mapper.readValue(rawBody, Map.class);
                }
            } else if ("GET".equalsIgnoreCase(method)) {
                // Read query parameters for GET
                requestData = (Map<String, String>) input.get("queryStringParameters");
                if (requestData == null) requestData = new HashMap<>();
            }

            // Now route normally
            if ("/login".equals(path) && "POST".equalsIgnoreCase(method)) {
                result = LoginService.doLogin(requestData, ddb);
            } else if ("/register".equals(path) && "POST".equalsIgnoreCase(method)) {
                result = RegisterService.doRegister(requestData, ddb);
            } else if ("/getSubscriptions".equals(path) && "GET".equalsIgnoreCase(method)) {
                result = SubscriptionService.getSubscriptions(requestData, ddb);
            } else if ("/subscribe".equals(path) && "POST".equalsIgnoreCase(method)) {
                result = SubscriptionService.subscribeSong(requestData, ddb);
            } else if ("/unsubscribe".equals(path) && "POST".equalsIgnoreCase(method)) {
                result = SubscriptionService.unsubscribeSong(requestData, ddb);
            } else if ("/searchMusic".equals(path) && "GET".equalsIgnoreCase(method)) {
                result = MusicService.searchMusic(requestData, ddb);
            } else {
                result = new HashMap<>();
                result.put("status", "fail");
                result.put("message", "Unknown path or method");
                return wrapResponse(result, 404);
            }

            return wrapResponse(result, 200);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage().replace("\"", "'"));
            return wrapResponse(error, 500);
        }
    }


    private Map<String, Object> wrapResponse(Map<String, Object> result, int statusCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);
        response.put("headers", getCorsHeaders());
        try {
            String jsonBody = mapper.writeValueAsString(result);
            response.put("body", jsonBody);
        } catch (Exception e) {
            response.put("body", "{\"status\":\"error\",\"message\":\"Serialization failed\"}");
        }
        return response;
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
