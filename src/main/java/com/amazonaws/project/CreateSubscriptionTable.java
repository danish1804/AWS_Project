package com.amazonaws.project;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;

public class CreateSubscriptionTable {

    public static void main(String[] args) {
        // Connect to LocalStack DynamoDB
        DynamoDbClient ddb = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:4566"))
                .region(Region.US_EAST_1)
                .build();

        String tableName = "subscription";

        try {
            CreateTableRequest request = CreateTableRequest.builder()
                    .tableName(tableName)
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("email")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("title_album")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    )
                    .keySchema(
                            KeySchemaElement.builder()
                                    .attributeName("email")
                                    .keyType(KeyType.HASH)
                                    .build(),
                            KeySchemaElement.builder()
                                    .attributeName("title_album")
                                    .keyType(KeyType.RANGE)
                                    .build()
                    )
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(5L)
                            .writeCapacityUnits(5L)
                            .build())
                    .build();

            ddb.createTable(request);
            System.out.println("✅ Subscription table created. Waiting for activation...");

            waitForActive(ddb, tableName);
        } catch (ResourceInUseException e) {
            System.out.println("⚠️ Table already exists. Skipping creation.");
        }

        ddb.close();
    }

    private static void waitForActive(DynamoDbClient ddb, String tableName) {
        while (true) {
            DescribeTableRequest describeRequest = DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build();
            String status = ddb.describeTable(describeRequest).table().tableStatusAsString();
            System.out.println("Status: " + status);
            if ("ACTIVE".equals(status)) break;

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
        System.out.println("✅ Table is now ACTIVE and ready to use.");
    }
}
