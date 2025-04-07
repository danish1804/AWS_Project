package com.amazonaws.project;

import java.io.InputStream;
import java.net.URL;

public class TestImageDownload {
    public static void main(String[] args) {
        String imageUrl = "https://raw.githubusercontent.com/YingZhang2015/cc/main/Radiohead.jpg";
        try (InputStream in = new URL(imageUrl).openStream()) {
            System.out.println("✅ Success! Image stream opened.");
        } catch (Exception e) {
            System.err.println("❌ Failed: " + e.getMessage());
        }
    }
}
