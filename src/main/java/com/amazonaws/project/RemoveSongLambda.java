package com.amazonaws.project;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class RemoveSongLambda implements RequestHandler<Map<String, Object>, String> {

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        // ✅ Extract parameters
        String email = (String) event.get("email");
        String title = (String) event.get("title");
        String album = (String) event.get("album");

        if (email == null || title == null || album == null) {
            return "❌ Error: Missing email, title, or album.";
        }

        String titleAlbumKey = title + "#" + album;

        // ✅ Connect to DynamoDB Local
        DynamoDbClient ddb = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:4566"))
                .region(Region.US_EAST_1)
                .build();

        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("email", AttributeValue.fromS(email));
            key.put("title_album", AttributeValue.fromS(titleAlbumKey));

            DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                    .tableName("subscription")
                    .key(key)
                    .build();

            ddb.deleteItem(deleteRequest);
            return "✅ Unsubscribed from: " + titleAlbumKey;

        } catch (Exception e) {
            return "❌ Error unsubscribing: " + e.getMessage();
        } finally {
            ddb.close();
        }
    }
}
