package com.amazonaws.project;

import java.util.HashMap;
import java.util.Map;

public class TestRegisterUser {
    public static void main(String[] args) {
        RegisterUserLambda lambda = new RegisterUserLambda();

        Map<String, Object> request = new HashMap<>();
        request.put("email", "newuser@example.com");
        request.put("user_name", "NewUser");
        request.put("password", "123456");

        Map<String, Object> result = lambda.handleRequest(request, null);
        System.out.println(result);
    }
}
