package com.amazonaws.project;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class CreateLoginTable {

    public static void main(String[] args) {
        String tableName = "login_Table";

        // ✅ Create client for DynamoDB Local
        DynamoDbClient ddb = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:8000"))
                .region(Region.US_EAST_1)
                .build();

        createTable(ddb, tableName);
        insertUsers(ddb, tableName);

        ddb.close();
    }

    private static void createTable(DynamoDbClient ddb, String tableName) {
        try {
            CreateTableRequest request = CreateTableRequest.builder()
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("email")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("email")
                            .keyType(KeyType.HASH)
                            .build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(5L)
                            .writeCapacityUnits(5L)
                            .build())
                    .tableName(tableName)
                    .build();

            ddb.createTable(request);
            System.out.println("✅ Table creation requested. Waiting for it to become ACTIVE...");
            waitUntilActive(ddb, tableName);

        } catch (ResourceInUseException e) {
            System.out.println("⚠️ Table already exists. Skipping creation.");
        }
    }

    private static void waitUntilActive(DynamoDbClient ddb, String tableName) {
        boolean isActive = false;
        while (!isActive) {
            DescribeTableRequest describeRequest = DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build();
            String status = ddb.describeTable(describeRequest).table().tableStatusAsString();
            System.out.println("Status: " + status);
            if (status.equals("ACTIVE")) isActive = true;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static void insertUsers(DynamoDbClient ddb, String tableName) {
        String studentId = "s4065642";
        String firstName = "MohammedDanishAlam";

        for (int i = 0; i < 10; i++) {
            String email = studentId + i + "@student.rmit.edu.au";
            String userName = firstName + i;
            String password = String.format("%06d", i * 111111);

            Map<String, AttributeValue> item = new HashMap<>();
            item.put("email", AttributeValue.fromS(email));
            item.put("user_name", AttributeValue.fromS(userName));
            item.put("password", AttributeValue.fromS(password));

            PutItemRequest putRequest = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();

            ddb.putItem(putRequest);
            System.out.println("✅ Inserted: " + email);
        }

        System.out.println("🎉 All users inserted successfully into DynamoDB Local!");
    }
}
