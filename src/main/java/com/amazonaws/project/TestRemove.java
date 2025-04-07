package com.amazonaws.project;

import java.util.HashMap;
import java.util.Map;

public class TestRemove {
    public static void main(String[] args) {
        RemoveSongLambda lambda = new RemoveSongLambda();

        Map<String, Object> input = new HashMap<>();
        input.put("email", "s123456_1@student.rmit.edu.au");
        input.put("title", "Let It Be");
        input.put("album", "The Beatles");

        String result = lambda.handleRequest(input, null);
        System.out.println(result);
    }
}
