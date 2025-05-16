package com.amazonaws.project.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UploadImagesToS3 {

    private static final String BUCKET_NAME = "artist-images-danish"; // Replace with your bucket name
    private static final String JSON_FILE_PATH = "2025a1.json"; // Place the file in your project root or adjust path
    private static final Region REGION = Region.US_EAST_1;
    private static final String OUTPUT_MAPPING_FILE = "s3_image_links.json";// Or your S3 region

    private static final S3Client s3 = S3Client.builder()
            .region(Region.US_EAST_1)
            .build();


    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Read and parse JSON file
        // NEW — extract the "songs" array from the root object
        Map<String, Object> root = mapper.readValue(
                new File(JSON_FILE_PATH), new TypeReference<Map<String, Object>>() {}
        );

        List<Map<String, Object>> records = (List<Map<String, Object>>) root.get("songs");
        Map<String, String> songToS3UrlMap = new HashMap<>();

        int index =0;
        for (Map<String, Object> record : records) {
            String imageUrl = (String) record.get("img_url");
            String artist = (String) record.get("artist");
            String title = (String) record.get("title");
            String album = (String) record.get("album");

            if (imageUrl == null || artist == null) continue;

            try {
                // Download image to temp file
                String extension = imageUrl.endsWith(".png") ? ".png" : ".jpg"; // simple guess
                String uniqueKey = "images/" + artist.replaceAll("\\s+", "_") + "_" + UUID.randomUUID() + extension;

                File tempFile = File.createTempFile("image", extension);
                try (InputStream in = new URL(imageUrl).openStream()) {
                    Files.copy(in, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                // Upload to S3
                PutObjectRequest request = PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(uniqueKey)
                        .contentType("image/jpeg")
                        .build();

                s3.putObject(request, RequestBody.fromFile(tempFile));
//                String s3Url = "https://" + BUCKET_NAME + ".s3." + REGION.id() + ".amazonaws.com/" + uniqueKey;
                String s3Url = "https://" + BUCKET_NAME + ".s3." + REGION.id() + ".amazonaws.com/" + uniqueKey;

                System.out.println("🖼️ Accessible at: " + s3Url);

                String songKey = title + "|||" + album + "|||" + index;
                songToS3UrlMap.put(songKey, s3Url);

                System.out.println("✅ Uploaded image for " + artist + " to: " + uniqueKey);

                // Clean up temp file
                tempFile.delete();
                index++;

            } catch (IOException e) {
                System.err.println("❌ Failed to process image for artist: " + artist);
                e.printStackTrace();
            }
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(OUTPUT_MAPPING_FILE), songToS3UrlMap);
        System.out.println("✅ Saved song-to-S3 mapping to " + OUTPUT_MAPPING_FILE);
    }
}
