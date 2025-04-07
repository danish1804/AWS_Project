package com.amazonaws.project;

import java.net.*;

public class TestDNS {
    public static void main(String[] args) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/YingZhang2015/cc/main/JimmyBuffett.jpg");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int status = con.getResponseCode();
            System.out.println("✅ Response code: " + status);

        } catch (Exception e) {
            System.err.println("❌ DNS or HTTP Error: " + e.getMessage());
        }
    }
}
