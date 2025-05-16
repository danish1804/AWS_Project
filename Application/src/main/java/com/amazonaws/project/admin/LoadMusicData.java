package com.amazonaws.project.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class LoadMusicData {

    public static void main(String[] args) {
        String filePath = "2025a1.json"; // ✅ Ensure this path is correct and JSON file is in the root directory

        // ✅ Connect to local DynamoDB (you can change URI if running on AWS cloud)
        DynamoDbClient ddb = DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .build();

        ObjectMapper mapper = new ObjectMapper(); // ✅ For reading JSON file
        // Get the "songs" array from the root object
        //❌ Error: JSON root is not an array.
        //You're getting the error ❌ Error: JSON root is not an array. because your JSON file does not start with a JSON array ([...]),
        // but with an object ({}) containing a field (probably like "songs": [...]).
        //getting the elements() of the root, which is an array, so it's returning a single array node, not the individual song records.
        //Iterator<JsonNode> songs = root.elements();
        //Entire JSON is being read as a single JSON array (which is correct ✅).
//            while (songs.hasNext()) {
//                JsonNode song = songs.next();
//
//                // ✅ Null checks before accessing fields
//                if (!song.has("title") || !song.has("artist") || !song.has("year") || !song.has("album") || !song.has("img_url")) {
//                    System.out.println("⚠️ Skipping song due to missing fields: " + song.toString());
//                    skipped++;
//                    continue;
//                }
        // But mistakenly calling .elements() directly on the array, which returns just one element — the full array itself — instead of looping through each object in the array.
        // Hence, song.get("title") returns null, because treating the array as if it were an object.
        try {
            // ✅ Read the root of the JSON file
            JsonNode root = mapper.readTree(new File(filePath));

            // ✅ Extract the array of songs from a field called "songs"
            JsonNode songsArray = root.get("songs");

            // ✅ Sanity check: must be an array
            if (songsArray == null || !songsArray.isArray()) {
                System.out.println("❌ Error: 'songs' array not found in JSON root object.");
                return;
            }

            int count = 0;
            int skipped = 0;

            // ✅ Iterate over each song object in the array
            for (JsonNode song : songsArray) {
                // ✅ Check required fields before inserting
                if (song.get("title") == null || song.get("artist") == null || song.get("year") == null ||
                        song.get("album") == null || song.get("img_url") == null) {
                    System.out.println("⚠️ Skipping song due to missing fields: " + song.toString());
                    skipped++;
                    continue;
                }

                // ✅ Extract values safely
                String title = song.get("title").asText();
                String artist = song.get("artist").asText();
                int year = song.get("year").asInt();
                String album = song.get("album").asText();
                String imageUrl = song.get("img_url").asText();

                // ✅ Construct item to insert
                Map<String, AttributeValue> item = new HashMap<>();
                item.put("title", AttributeValue.fromS(title));
                item.put("artist", AttributeValue.fromS(artist));
                item.put("year", AttributeValue.fromN(String.valueOf(year)));
                item.put("album", AttributeValue.fromS(album));
                item.put("img_url", AttributeValue.fromS(imageUrl));

                // ✅ Create request and put item
                PutItemRequest request = PutItemRequest.builder()
                        .tableName("music")
                        .item(item)
                        .build();

                ddb.putItem(request);
                count++;
            }

            System.out.println("✅ Inserted " + count + " songs into 'music' table.");
            if (skipped > 0)
                System.out.println("⚠️ Skipped " + skipped + " entries due to missing fields.");

        } catch (IOException e) {
            System.err.println("❌ Error reading JSON file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error writing to DynamoDB: " + e.getMessage());
        } finally {
            ddb.close(); // ✅ Always close your client
        }
    }
}
