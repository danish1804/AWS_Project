package com.amazonaws.project.admin;

// Import required AWS SDK classes

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;

public class CreateMusicTable {

    public static void main(String[] args) {
        // Step 1: Create a DynamoDbClient connected to the us-east-1 region
        // ✅ Create client for DynamoDB Local
        DynamoDbClient ddb = DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .build();

        String tableName = "music"; // Desired table name

        try {
            // Step 2: Define the table schema and provisioned throughput
            CreateTableRequest request = CreateTableRequest.builder()
                    .tableName(tableName)

                    // Define attributes used in the key schema
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("title") // Partition key (HASH)
                                    .attributeType(ScalarAttributeType.S) // String type
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("album") // Sort key (RANGE)
                                    .attributeType(ScalarAttributeType.S) // String type
                                    .build()
                    )

                    // Define the primary key schema
                    .keySchema(
                            KeySchemaElement.builder()
                                    .attributeName("title") // HASH key (partition key)
                                    .keyType(KeyType.HASH)
                                    .build(),
                            KeySchemaElement.builder()
                                    .attributeName("album") // RANGE key (sort key)
                                    .keyType(KeyType.RANGE)
                                    .build()
                    )

                    // Define read and write capacity (for provisioned mode)
                    .provisionedThroughput(
                            ProvisionedThroughput.builder()
                                    .readCapacityUnits(5L)
                                    .writeCapacityUnits(5L)
                                    .build()
                    )
                    .build();

            // Step 3: Send create table request to DynamoDB
            ddb.createTable(request);
            System.out.println("✅ 'music' table creation requested.");

            // Step 4: Wait for the table to become ACTIVE before proceeding
            waitUntilActive(ddb, tableName);

        } catch (ResourceInUseException e) {
            // If table already exists, we catch the exception and skip creation
            System.out.println("⚠️ 'music' table already exists.");
        }

        // Step 5: Close the client connection
        ddb.close();
    }

    /**
     * Utility method to poll the table status until it becomes ACTIVE.
     */
    private static void waitUntilActive(DynamoDbClient ddb, String tableName) {
        boolean isActive = false;
        while (!isActive) {
            DescribeTableRequest describeRequest = DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build();

            // Fetch current status of the table
            String status = ddb.describeTable(describeRequest).table().tableStatusAsString();
            System.out.println("Status: " + status);

            // If table status becomes ACTIVE, exit loop
            if (status.equals("ACTIVE")) isActive = true;

            // Sleep for 1 second before checking again
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
        System.out.println("✅ Table is now ACTIVE.");
    }
}
