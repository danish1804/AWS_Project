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

public class SubscribeSongLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDbClient ddb = DynamoDbClient.builder()
            .endpointOverride(URI.create("http://host.docker.internal:4566")) // LocalStack endpoint
            .region(Region.US_EAST_1)
            .build();

    private static final String MUSIC_TABLE = "music";
    private static final String SUBS_TABLE = "subscription";
    private static final String LOGIN_TABLE = "login";

    private Map<String, String> corsHeaders() {
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
            System.out.println("✅ SUBSCRIBE Lambda triggered - Subscribe to Song");

            String rawBody = (String) input.get("body");
            Map<String, String> body = mapper.readValue(rawBody, Map.class);

            String email = body.get("email");
            String title = body.get("title");
            String album = body.get("album");

            if (email == null || title == null || album == null) {
                response.put("statusCode", 400);
                response.put("headers", corsHeaders());
                response.put("body", "{\"status\":\"fail\",\"message\":\"Missing email, title, or album\"}");
                return response;
            }

            // Check if user exists
            GetItemRequest checkUser = GetItemRequest.builder()
                    .tableName(LOGIN_TABLE)
                    .key(Map.of("email", AttributeValue.fromS(email)))
                    .build();
            if (!ddb.getItem(checkUser).hasItem()) {
                response.put("statusCode", 403);
                response.put("headers", corsHeaders());
                response.put("body", "{\"status\":\"fail\",\"message\":\"Email is not registered\"}");
                return response;
            }

            // Check if song exists
            Map<String, AttributeValue> musicKey = Map.of(
                    "title", AttributeValue.fromS(title),
                    "album", AttributeValue.fromS(album)
            );

            GetItemRequest musicRequest = GetItemRequest.builder()
                    .tableName(MUSIC_TABLE)
                    .key(musicKey)
                    .build();

            GetItemResponse musicResponse = ddb.getItem(musicRequest);
            if (!musicResponse.hasItem()) {
                response.put("statusCode", 404);
                response.put("headers", corsHeaders());
                response.put("body", "{\"status\":\"fail\",\"message\":\"Song not found in music library\"}");
                return response;
            }

            // Extract metadata
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
            subItem.put("year", AttributeValue.fromN(String.valueOf(Integer.parseInt(year))));

            System.out.println("📥 Inserting subscription into DynamoDB: " + subItem);

            // ✅ Use condition to avoid duplicates
            PutItemRequest putRequest = PutItemRequest.builder()
                    .tableName(SUBS_TABLE)
                    .item(subItem)
                    .conditionExpression("attribute_not_exists(email) AND attribute_not_exists(title_album)")
                    .build();

            ddb.putItem(putRequest);


            System.out.println("✅ Successfully inserted into DynamoDB.");
            PutItemResponse result = ddb.putItem(putRequest);
            if (result.sdkHttpResponse().isSuccessful()) {
                System.out.println("✅ Item successfully inserted.");
            } else {
                System.out.println("❌ Failed to insert item. Response: " + result.sdkHttpResponse().statusCode());
            }
            System.out.println("🔁 DynamoDB putItem response: " + result);

            response.put("statusCode", 200);
            response.put("headers", corsHeaders());
            response.put("body", mapper.writeValueAsString(Map.of(
                    "status", "success",
                    "message", "Song subscribed successfully"
            )));
            System.out.println("✅ PutItemResponse: " + result);

        } catch (ConditionalCheckFailedException dup) {
            response.put("statusCode", 409);
            response.put("headers", corsHeaders());
            response.put("body", "{\"status\":\"fail\",\"message\":\"You already subscribed to this song\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("statusCode", 500);
            response.put("headers", corsHeaders());
            response.put("body", String.format("{\"status\":\"error\",\"message\":\"%s\"}", e.getMessage()));
        }

        return response;
    }
}
