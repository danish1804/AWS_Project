package com.amazonaws.project;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class CreateAndSeedLoginTable {

    public static void main(String[] args) {
        // Step 1: Create a DynamoDbClient connected to the us-east-1 region
        // ✅ Create client for DynamoDB Local
        DynamoDbClient ddb = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:4566"))
                .region(Region.US_EAST_1)
                .build();

        String tableName = "login";

        // Step 1: Create Table
        createTable(ddb, tableName);

        // Step 2: Insert 10 users
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
            System.out.println("Table created successfully. Waiting for activation...");

            // Wait until table is active
            waitUntilActive(ddb, tableName);
        } catch (ResourceInUseException e) {
            System.out.println("Table already exists. Skipping creation.");
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
        String studentId = "s123456_";
        String firstName = "User_";

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
            System.out.println("Inserted: " + email);
        }

        System.out.println("All users inserted successfully.");
    }
}
