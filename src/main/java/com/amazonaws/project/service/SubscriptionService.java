package com.amazonaws.project.service;

import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

import java.util.*;

public class SubscriptionService {

    private static final String tableName = "subscription";
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Returns a list of subscriptions for a given email
     */
    public static Map<String, Object> getSubscriptions(Map<String, String> body, DynamoDbClient ddb) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("✅ [Subscription] getSubscriptions() called");

            String email = body.get("email");
            if (email == null) {
                return buildResponse(400, "fail", "Missing email in request");
            }

            // Query all items for this email
            QueryRequest query = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("email = :email")
                    .expressionAttributeValues(Map.of(":email", AttributeValue.fromS(email)))
                    .consistentRead(true)
                    .build();

            QueryResponse queryResponse = ddb.query(query);
            List<Map<String, AttributeValue>> items = queryResponse.items();

            // Transform results into a clean list
            List<Map<String, Object>> subscriptions = new ArrayList<>();
            for (Map<String, AttributeValue> item : items) {
                Map<String, Object> song = new HashMap<>();
                String title = item.getOrDefault("title", AttributeValue.fromS("")).s();
                String album = item.getOrDefault("album", AttributeValue.fromS("")).s();
                song.put("title", title);
                song.put("artist", item.getOrDefault("artist", AttributeValue.fromS("")).s());
                song.put("album", album);
                song.put("year", item.getOrDefault("year", AttributeValue.fromN("0")).n());

                // ✅ Dynamically fetch image_url from the music table
                try {
                    GetItemRequest musicRequest = GetItemRequest.builder()
                            .tableName("music")
                            .key(Map.of(
                                    "title", AttributeValue.fromS(title),
                                    "album", AttributeValue.fromS(album)
                            ))
                            .build();

                    GetItemResponse musicResp = ddb.getItem(musicRequest);

                    if (musicResp.hasItem() && musicResp.item().containsKey("image_s3_url")) {
                        String imageUrl = musicResp.item().get("image_s3_url").s();
                        song.put("image_s3_url", imageUrl);
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to fetch image_url for: " + title + " - " + album);
                    e.printStackTrace();
                }
                subscriptions.add(song);
            }

            Map<String, Object> successBody = new HashMap<>();
            successBody.put("status", "success");
            successBody.put("subscriptions", subscriptions);

            response.put("statusCode", 200);
            response.put("headers", getCorsHeaders());
            response.put("body", mapper.writeValueAsString(successBody));
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(500, "error", e.getMessage());
        }
    }

    public static Map<String, Object> subscribeSong(Map<String, String> body, DynamoDbClient ddb) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("✅ [Subscription] subscribeSong() called");

            String email = body.get("email");
            String title = body.get("title");
            String album = body.get("album");

            if (email == null || title == null || album == null) {
                return buildResponse(400, "fail", "Missing email, title, or album");
            }

            // Check if user exists
            GetItemRequest checkUser = GetItemRequest.builder()
                    .tableName("login")
                    .key(Map.of("email", AttributeValue.fromS(email)))
                    .build();

            if (!ddb.getItem(checkUser).hasItem()) {
                return buildResponse(403, "fail", "Email is not registered");
            }

            // Check if song exists
            Map<String, AttributeValue> musicKey = Map.of(
                    "title", AttributeValue.fromS(title),
                    "album", AttributeValue.fromS(album)
            );

            GetItemRequest musicRequest = GetItemRequest.builder()
                    .tableName("music")
                    .key(musicKey)
                    .build();

            GetItemResponse musicResponse = ddb.getItem(musicRequest);

            if (!musicResponse.hasItem()) {
                return buildResponse(404, "fail", "Song not found in music library");
            }

            Map<String, AttributeValue> song = musicResponse.item();
            String artist = song.getOrDefault("artist", AttributeValue.fromS("")).s();
            String year = song.getOrDefault("year", AttributeValue.fromN("0")).n();
            String titleAlbumKey = title + "#" + album;

            Map<String, AttributeValue> subItem = new HashMap<>();
            subItem.put("email", AttributeValue.fromS(email));
            subItem.put("title_album", AttributeValue.fromS(titleAlbumKey));
            subItem.put("title", AttributeValue.fromS(title));
            subItem.put("album", AttributeValue.fromS(album));
            subItem.put("artist", AttributeValue.fromS(artist));
            subItem.put("year", AttributeValue.fromN(year));

            System.out.println("📥 Subscribing user to song: " + subItem);

            PutItemRequest putRequest = PutItemRequest.builder()
                    .tableName("subscription")
                    .item(subItem)
                    .conditionExpression("attribute_not_exists(email) AND attribute_not_exists(title_album)")
                    .build();

            PutItemResponse result = ddb.putItem(putRequest);

            if (result.sdkHttpResponse().isSuccessful()) {
                return buildResponse(200, "success", "Song subscribed successfully");
            } else {
                return buildResponse(500, "fail", "DynamoDB putItem failed");
            }

        } catch (ConditionalCheckFailedException dup) {
            return buildResponse(409, "fail", "You already subscribed to this song");
        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(500, "error", e.getMessage());
        }
    }

    public static Map<String, Object> unsubscribeSong(Map<String, String> body, DynamoDbClient ddb) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("✅ [Subscription] unsubscribeSong() called");

            String email = body.get("email");
            String title = body.get("title");
            String album = body.get("album");

            if (email == null || title == null || album == null) {
                return buildResponse(400, "fail", "Missing email, title, or album");
            }

            String titleAlbumKey = title + "#" + album;

            Map<String, AttributeValue> key = Map.of(
                    "email", AttributeValue.fromS(email),
                    "title_album", AttributeValue.fromS(titleAlbumKey)
            );

            DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                    .tableName("subscription")
                    .key(key)
                    .build();

            ddb.deleteItem(deleteRequest);

            return buildResponse(200, "success", "Unsubscribed from song successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(500, "error", e.getMessage());
        }
    }





    // Reusable response and CORS helpers
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
