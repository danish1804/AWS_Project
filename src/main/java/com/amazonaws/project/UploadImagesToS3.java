package com.amazonaws.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class UploadImagesToS3 {

    public static void main(String[] args) throws IOException {
        String jsonPath = "2025a1.json"; // ✅ Your JSON file path
        String bucketName = "music-subscription-images-s4065642"; // ✅ Bucket name

        // ✅ Set up S3 client for LocalStack
        S3Client s3 = S3Client.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create("http://localhost:4566"))
                .forcePathStyle(true) // 👈 THIS LINE FIXES THE UNKNOWN HOST
                .build();


        // ✅ Step 1: Check and Create Bucket if needed
        try {
            HeadBucketRequest headRequest = HeadBucketRequest.builder().bucket(bucketName).build();
            s3.headBucket(headRequest); // throws if bucket doesn't exist
            System.out.println("🪣 Bucket '" + bucketName + "' already exists.");
        } catch (S3Exception e) {
            System.out.println("🪣 Bucket does not exist. Creating bucket: " + bucketName);
            CreateBucketRequest createRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3.createBucket(createRequest);
        }

        // ✅ Load JSON
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(jsonPath));

        Iterator<JsonNode> songs;
        if (root.isArray()) {
            songs = root.elements();
        } else if (root.has("songs") && root.get("songs").isArray()) {
            songs = root.get("songs").elements();
        } else {
            System.out.println("❌ Error: JSON must be an array or contain a 'songs' array.");
            return;
        }

        Set<String> uploaded = new HashSet<>();
        int count = 0;

        // ✅ Loop through songs
        while (songs.hasNext()) {
            JsonNode song = songs.next();

            if (!song.has("img_url") || song.get("img_url").isNull()) {
                System.out.println("⚠️ Skipping song due to missing 'img_url': " + song);
                continue;
            }

            String imageUrl = song.get("img_url").asText();
            String imageName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);

            if (uploaded.contains(imageName)) continue;

            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(imageUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                int status = conn.getResponseCode();
                System.out.println("🌐 Checking " + imageUrl + " → Status: " + status);
                if (status != 200) {
                    System.err.println("❌ Skipping (bad status): " + imageUrl);
                    continue;
                }

                InputStream in = conn.getInputStream();
                Path tempFile = Files.createTempFile("img-", imageName);
                Files.copy(in, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                PutObjectRequest putReq = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key("artist-images/" + imageName)
                        .acl("public-read")
                        .build();

                s3.putObject(putReq, RequestBody.fromFile(tempFile));
                uploaded.add(imageName);
                count++;

                System.out.println("✅ Uploaded: " + imageName);
                Files.deleteIfExists(tempFile);

            } catch (Exception e) {
                System.err.println("❌ Failed to upload: " + imageUrl + " | Error: " + e.getMessage());
            }
        }

        System.out.println("\n🎉 Upload complete. Total images uploaded: " + count);
    }
}
