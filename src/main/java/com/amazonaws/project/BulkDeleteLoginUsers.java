package com.amazonaws.project;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable;

import java.net.URI;
import java.util.*;

public class BulkDeleteLoginUsers {

    public static void main(String[] args) {
        String tableName = "login_Table";
        String namePrefix = "MohammedDanishAlam"; // Your prefix pattern

        DynamoDbClient ddb = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:8000"))
                .region(Region.US_EAST_1)
                .build();

        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tableName)
                .projectionExpression("email, user_name")
                .build();

        ScanIterable scanResult = ddb.scanPaginator(scanRequest);
        int deletedCount = 0;

        for (ScanResponse page : scanResult) {
            for (Map<String, AttributeValue> item : page.items()) {
                String userName = item.get("user_name").s();
                String email = item.get("email").s();

                if (userName.startsWith(namePrefix)) {
                    Map<String, AttributeValue> key = new HashMap<>();
                    key.put("email", AttributeValue.fromS(email));

                    DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                            .tableName(tableName)
                            .key(key)
                            .build();

                    ddb.deleteItem(deleteRequest);
                    System.out.println("✅ Deleted: " + email);
                    deletedCount++;
                }
            }
        }

        System.out.println("✅ Total deleted: " + deletedCount);
        ddb.close();
    }
}

