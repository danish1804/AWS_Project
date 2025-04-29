package com.amazonaws.project.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class UpdateMusicTableWithImageLinks {

    private static final String TABLE_NAME = "music";
    private static final String S3_MAPPING_FILE = "s3_image_links.json"; // Format: { "title#album": "s3_url", ... }

    public static void main(String[] args) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> songToS3Map = mapper.readValue(
                new File(S3_MAPPING_FILE), new TypeReference<Map<String, String>>() {}
        );

        DynamoDbClient ddb = DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .build();
        //

        for (Map.Entry<String, String> entry : songToS3Map.entrySet()) {
            String[] keys = entry.getKey().split("\\|\\|\\|");
            String title = keys[0];
            String album = keys[1];
            String s3Url = entry.getValue();

            Map<String, AttributeValue> itemKey = new HashMap<>();
            itemKey.put("title", AttributeValue.fromS(title));
            itemKey.put("album", AttributeValue.fromS(album));

            Map<String, AttributeValue> values = new HashMap<>();
            values.put(":url", AttributeValue.fromS(s3Url));
            System.out.println("🔍 Trying to update song: ");
            System.out.println("   ➤ Title: " + title);
            System.out.println("   ➤ Album: " + album);
//            System.out.println("   ➤ Artist: " + artist);
            System.out.println("   ➤ S3 URL: " + s3Url);
            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(itemKey)
                    .updateExpression("SET image_s3_url = :url")
                    .expressionAttributeValues(values)
                    .build();

            ddb.updateItem(request);
            System.out.println("✅ Updated: " + title + " [" + album + "] with image URL: " + s3Url);
        }

        System.out.println("🎉 All image URLs updated in DynamoDB.");
    }
}
